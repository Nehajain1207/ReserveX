import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Load test for ReserveX. Talks to the RUNNING app over HTTP, like real users would.
 *
 * Run (app must already be running on port 8080):
 *     java loadtest\LoadTest.java
 *     java loadtest\LoadTest.java 300        (300 users instead of 200)
 *
 * Scenario A - "hot seat": every user tries to book seat A1 at the same instant.
 *              Exactly one booking may succeed.
 * Scenario B - "rush":     every user tries to book one random seat out of the other 119.
 *              No seat may be sold twice, and the show's seat counter must match.
 */
public class LoadTest {

    static final String BASE = "http://localhost:8080";
    static final String PASSWORD = "LoadTest#123";

    static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    static final AtomicInteger RETRIES = new AtomicInteger();
    static volatile String LAST_ERROR = "";

    record Result(int status, long millis, String seat) { }

    public static void main(String[] args) throws Exception {

        int users = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        String runId = Long.toString(System.currentTimeMillis(), 36);

        System.out.println("ReserveX load test - " + users + " users, run " + runId);

        // ---------- 1. Register and log in every user ----------
        System.out.println("Registering and logging in " + users + " users (takes a little while)...");

        List<String> tokens = Collections.synchronizedList(new ArrayList<>());
        ExecutorService setupPool = Executors.newFixedThreadPool(8);
        List<Future<?>> setup = new ArrayList<>();

        for (int i = 0; i < users; i++) {

            String email = "loadtest-" + runId + "-" + i + "@example.com";

            setup.add(setupPool.submit(() -> {

                post("/api/auth/register", null,
                        "{\"name\":\"Load Test\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");

                HttpResponse<String> login = post("/api/auth/login", null,
                        "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");

                String token = find(login.body(), "\"token\"\\s*:\\s*\"([^\"]+)\"");

                if (token != null) {
                    tokens.add(token);
                }

                return null;
            }));
        }

        for (Future<?> f : setup) {
            f.get();
        }
        setupPool.shutdown();

        if (tokens.size() < users) {
            System.out.println("Only " + tokens.size() + " of " + users + " users could log in. Is the app running on port 8080?");
            if (tokens.isEmpty()) {
                return;
            }
        }

        // ---------- 2. Create a movie and a show (120 seats: A1..J12) ----------
        String admin = tokens.get(0);

        HttpResponse<String> movie = post("/api/movies", admin,
                "{\"title\":\"Load Test " + runId + "\",\"language\":\"English\",\"duration\":120,\"genre\":\"Test\"}");
        String movieId = find(movie.body(), "\"id\"\\s*:\\s*(\\d+)");

        HttpResponse<String> show = post("/api/shows", admin,
                "{\"movieId\":" + movieId + ",\"showDate\":\"" + LocalDate.now().plusDays(7)
                        + "\",\"showTime\":\"18:30:00\",\"screenNumber\":1,\"ticketPrice\":250}");
        String showId = find(show.body(), "\"id\"\\s*:\\s*(\\d+)");

        if (movieId == null || showId == null) {
            System.out.println("Could not create the movie/show.");
            System.out.println("Movie response: " + movie.statusCode() + " " + movie.body());
            System.out.println("Show response : " + show.statusCode() + " " + show.body());
            return;
        }

        System.out.println("Created show " + showId + " with 120 seats.");

        // Warm-up: a few ordinary requests so the first burst does not hit a cold server
        for (int i = 0; i < 20; i++) {
            get("/api/shows/" + showId, admin);
        }

        // ---------- 3. Scenario A: everyone wants seat A1 ----------
        System.out.println("Scenario A: " + tokens.size() + " users book seat A1 at the same instant...");

        List<Result> hot = fire(tokens, showId, i -> "A1");

        // ---------- 4. Scenario B: everyone wants a random seat out of the other 119 ----------
        List<String> otherSeats = new ArrayList<>();
        for (char row = 'A'; row <= 'J'; row++) {
            for (int col = 1; col <= 12; col++) {
                String seat = row + String.valueOf(col);
                if (!seat.equals("A1")) {
                    otherSeats.add(seat);
                }
            }
        }

        Random random = new Random(42);
        List<String> picks = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            picks.add(otherSeats.get(random.nextInt(otherSeats.size())));
        }

        System.out.println("Scenario B: " + tokens.size() + " users each book one random seat out of 119...");

        List<Result> rush = fire(tokens, showId, picks::get);

        // ---------- 5. Check the results ----------
        long hotWins = hot.stream().filter(r -> r.status() == 201).count();
        long hotRejected = hot.stream().filter(r -> r.status() == 400).count();
        long hotOther = hot.size() - hotWins - hotRejected;

        Map<String, Integer> winsPerSeat = new ConcurrentHashMap<>();
        rush.stream().filter(r -> r.status() == 201).forEach(r -> winsPerSeat.merge(r.seat(), 1, Integer::sum));

        long rushWins = rush.stream().filter(r -> r.status() == 201).count();
        long rushRejected = rush.stream().filter(r -> r.status() == 400).count();
        long rushOther = rush.size() - rushWins - rushRejected;
        long distinctSeatsWanted = picks.stream().distinct().count();
        long doubleSold = winsPerSeat.values().stream().filter(n -> n > 1).count();

        HttpResponse<String> after = get("/api/shows/" + showId, admin);
        String availableText = find(after.body(), "\"availableSeats\"\\s*:\\s*(\\d+)");
        int available = availableText == null ? -1 : Integer.parseInt(availableText);
        long expectedAvailable = 120 - hotWins - rushWins;

        List<Long> all = new ArrayList<>();
        hot.forEach(r -> all.add(r.millis()));
        rush.forEach(r -> all.add(r.millis()));
        Collections.sort(all);

        boolean checkHot = hotWins == 1 && hotOther == 0;
        boolean checkDouble = doubleSold == 0;
        boolean checkAllSold = rushWins == distinctSeatsWanted;
        boolean checkCounter = available == expectedAvailable;
        boolean checkErrors = hotOther == 0 && rushOther == 0;

        StringBuilder out = new StringBuilder();
        out.append("# ReserveX load test report\n\n");
        out.append("Run: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append(" | users: ").append(tokens.size())
                .append(" | booking requests: ").append(all.size()).append("\n\n");

        out.append("## Scenario A - everyone books seat A1\n");
        out.append("- succeeded: ").append(hotWins).append("\n");
        out.append("- rejected (seat taken): ").append(hotRejected).append("\n");
        out.append("- unexpected responses: ").append(hotOther).append(statuses(hot)).append("\n\n");

        out.append("## Scenario B - everyone books one random seat out of 119\n");
        out.append("- different seats requested: ").append(distinctSeatsWanted).append("\n");
        out.append("- succeeded: ").append(rushWins).append("\n");
        out.append("- rejected (seat taken): ").append(rushRejected).append("\n");
        out.append("- seats sold more than once: ").append(doubleSold).append("\n");
        out.append("- unexpected responses: ").append(rushOther).append(statuses(rush)).append("\n\n");

        out.append("## Seat counter\n");
        out.append("- show says available: ").append(available).append("\n");
        out.append("- should be (120 - bookings): ").append(expectedAvailable).append("\n\n");

        out.append("## Response time (all booking requests, sent at once)\n");
        out.append("- p50: ").append(percentile(all, 50)).append(" ms\n");
        out.append("- p95: ").append(percentile(all, 95)).append(" ms\n");
        out.append("- max: ").append(all.get(all.size() - 1)).append(" ms\n\n");

        out.append("## Connections\n");
        out.append("- connection attempts retried: ").append(RETRIES.get())
                .append(RETRIES.get() > 0 ? "  (last error: " + LAST_ERROR + ")" : "").append("\n\n");

        out.append("## Checks\n");
        out.append(line(checkHot, "exactly one user got seat A1"));
        out.append(line(checkDouble, "no seat was sold twice"));
        out.append(line(checkAllSold, "every requested seat was sold to someone"));
        out.append(line(checkCounter, "seat counter matches the number of bookings"));
        out.append(line(checkErrors, "no unexpected errors (only 201 or 400)"));

        System.out.println();
        System.out.println(out);

        Files.createDirectories(Path.of("loadtest"));
        Files.writeString(Path.of("loadtest", "latest-report.md"), out.toString());
        System.out.println("Saved to loadtest\\latest-report.md");
    }

    /** Sends one booking request per user, all released at the same instant. */
    static List<Result> fire(List<String> tokens, String showId, java.util.function.IntFunction<String> seatFor)
            throws Exception {

        int n = tokens.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            String token = tokens.get(i);
            String seat = seatFor.apply(i);
            String body = "{\"showId\":" + showId + ",\"seatNumbers\":[\"" + seat + "\"]}";

            Callable<Result> task = () -> {

                ready.countDown();
                start.await();

                long begin = System.nanoTime();
                int status;

                status = -1;

                // 200 connections opened in the same millisecond can overflow the
                // server's connection queue. A real client retries a refused
                // connection, so this does too, and the retries are counted.
                for (int attempt = 1; attempt <= 4 && status == -1; attempt++) {

                    try {

                        status = post("/api/bookings", token, body).statusCode();

                    } catch (Exception ex) {

                        RETRIES.incrementAndGet();
                        LAST_ERROR = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                        Thread.sleep(200L * attempt);
                    }
                }

                return new Result(status, (System.nanoTime() - begin) / 1_000_000, seat);
            };

            futures.add(pool.submit(task));
        }

        ready.await(30, TimeUnit.SECONDS);
        start.countDown();

        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) {
            results.add(f.get());
        }

        pool.shutdown();
        return results;
    }

    static HttpResponse<String> post(String path, String token, String json) throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static HttpResponse<String> get(String path, String token) throws Exception {

        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    static String find(String text, String regex) {

        Matcher m = Pattern.compile(regex).matcher(text == null ? "" : text);
        return m.find() ? m.group(1) : null;
    }

    static long percentile(List<Long> sorted, int p) {

        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    static String statuses(List<Result> results) {

        Map<Integer, AtomicInteger> counts = new ConcurrentHashMap<>();
        results.stream()
                .filter(r -> r.status() != 201 && r.status() != 400)
                .forEach(r -> counts.computeIfAbsent(r.status(), k -> new AtomicInteger()).incrementAndGet());

        return counts.isEmpty() ? "" : "  (status codes: " + counts + ")";
    }

    static String line(boolean ok, String text) {

        return "- " + (ok ? "PASS" : "FAIL") + ": " + text + "\n";
    }
}
