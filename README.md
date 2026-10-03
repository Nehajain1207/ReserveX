# ReserveX

A movie-ticket booking backend built to answer one question: **when many people try to book the same seat at the same moment, does exactly one of them get it?**

Java 17 · Spring Boot 3.5 · PostgreSQL · Redis · Spring Security (JWT) · Docker

## What it does

- **Accounts** – register, log in with JWT, log out (token blacklist in Redis), admin role.
- **Movies and shows** – create, update, delete, search, paginate. Each show gets 120 seats (A1 to J12).
- **Booking** – book one or more seats. Each seat is locked in Redis for 5 minutes while the user pays.
- **Payment** – confirm a booking. Safe to retry: the same `Idempotency-Key` never confirms twice. (Simulated; no payment gateway yet.)
- **Expiry** – a scheduler frees the seats of bookings that were not paid within 5 minutes.
- **Admin** – dashboard, revenue and booking reports.
- **API docs** – Swagger UI at `/swagger-ui.html`.

## How double-booking is prevented

1. Before touching the database, a booking takes a Redis lock per seat with `SET key value NX EX 300`. Redis guarantees only one caller can create the key, so only one booking gets the seat.
2. The lock has a 5-minute time limit, so a seat can never stay stuck if the app crashes.
3. If a booking for several seats fails part-way, the locks it had already taken are released straight away. Locks held by other users are never touched.
4. The show's "seats available" counter is changed with a single `UPDATE ... SET available = available - n` in the database, so concurrent bookings cannot overwrite each other.

## Test results

Measured on a laptop on 3 Oct 2026. Reproduce with the commands below.

**Load test** – 200 users, 400 booking requests over HTTP against the running app (`loadtest/LoadTest.java`):

| Scenario | Result |
|---|---|
| 200 users book the **same seat** at the same instant | 1 succeeded, 199 rejected |
| 200 users each book **one random seat** out of 119 | 99 different seats requested, 99 sold, **0 sold twice** |
| Seat counter after 100 bookings | shows 20, expected 20 |
| Unexpected errors | 0 |

**Automated tests** – 34 JUnit 5 tests, all passing:

- booking and cancellation rules, including lock release on failure (Mockito)
- 100 threads booking the same seat, exactly one succeeds
- 4 tests against a **real Redis**: only one of 100 threads gets a lock, locks expire, locks can be re-taken after release
- movie and show services

### Two bugs these tests found

- **Seats stuck after a failed booking.** Booking A1, A2, A3 where A3 was taken left A1 and A2 locked for 5 minutes. The database rolled back but Redis did not. Fixed by releasing the request's own locks on failure.
- **Seat counter lost updates.** The first load test sold 100 seats but the counter showed 104 available instead of 20. The code read the counter, changed it in Java and saved it back, so concurrent bookings overwrote each other. Fixed with an atomic database update.

## Run it

Needs Java 17+ and Docker.

```bash
docker compose up -d postgres redis     # PostgreSQL on port 5434, Redis on 6379
./mvnw spring-boot:run                  # Windows: mvnw.cmd spring-boot:run
```

Then open http://localhost:8080/swagger-ui.html

```bash
./mvnw test                             # 34 tests (the real-Redis tests are skipped if Redis is not running)
java loadtest/LoadTest.java             # load test; the app must be running
```

### Configuration

No secrets are stored in the code. Defaults are for local development only; set these environment variables anywhere else:

| Variable | Purpose | Local default |
|---|---|---|
| `DB_PASSWORD` | PostgreSQL password | `postgres` |
| `JWT_SECRET` | JWT signing key, at least 32 characters | a dev-only value |

## Main endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | accounts |
| GET / POST / PUT / DELETE | `/api/movies`, `/api/movies/search` | movies |
| GET / POST / PUT / DELETE | `/api/shows`, `/api/shows/search` | shows |
| POST | `/api/bookings` | book seats |
| GET | `/api/bookings/my-bookings` | my bookings |
| DELETE | `/api/bookings/{reference}` | cancel a confirmed booking |
| POST | `/api/payments/{reference}` | confirm payment (`Idempotency-Key` header) |
| GET | `/api/admin/dashboard`, `/api/admin/revenue`, `/api/admin/bookings` | admin only |

## Known limitations

- Payment is simulated; there is no payment gateway.
- Creating movies and shows is open to any logged-in user, not only admins.
- The Redis lock is a single-instance lock, not a multi-node (Redlock) setup.
- The load test ran on one laptop with the app, database and Redis on the same machine.

## Author

Neha Jain
