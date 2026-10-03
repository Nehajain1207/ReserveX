# ReserveX load test report

Run: 2026-10-03 18:16 | users: 200 | booking requests: 400

## Scenario A - everyone books seat A1
- succeeded: 1
- rejected (seat taken): 199
- unexpected responses: 0

## Scenario B - everyone books one random seat out of 119
- different seats requested: 99
- succeeded: 99
- rejected (seat taken): 101
- seats sold more than once: 0
- unexpected responses: 0

## Seat counter
- show says available: 20
- should be (120 - bookings): 20

## Response time (all booking requests, sent at once)
- p50: 4216 ms
- p95: 6104 ms
- max: 6346 ms

## Connections
- connection attempts retried: 6  (last error: ConnectException: null)

## Checks
- PASS: exactly one user got seat A1
- PASS: no seat was sold twice
- PASS: every requested seat was sold to someone
- PASS: seat counter matches the number of bookings
- PASS: no unexpected errors (only 201 or 400)
