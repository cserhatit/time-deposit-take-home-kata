# Time Deposit Kata: Java solution

A Spring Boot service with the two REST endpoints required by the [assignment](../README.md), backed by PostgreSQL. The legacy interest calculation (`TimeDepositCalculator.updateBalance`) is reused with its behavior unchanged.

- Design and assumptions: [`docs/INITIAL_ANALYSIS.md`](docs/INITIAL_ANALYSIS.md)
- AI-assisted workflow: [`docs/AI_USAGE.md`](docs/AI_USAGE.md)

## Requirements
- JDK 17 or newer (developed on JDK 21), Maven 3.9+
- Docker: needed for the local database and for the integration tests (Testcontainers)

## Run locally and try it in Swagger UI
```bash
cd java
docker compose up -d                                   # PostgreSQL 17 on localhost:5432
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # dev profile = schema + demo data
```
Open **http://localhost:8080/swagger-ui.html**. The OpenAPI document is at http://localhost:8080/v3/api-docs.

1. **GET `/time-deposits`**: *Try it out* → *Execute*. Returns the 6 demo deposits with their withdrawals (`200`).
2. **POST `/time-deposits/balance-updates`**: *Try it out* → *Execute*. Applies one month of interest to every deposit (`204`).
3. **GET `/time-deposits`** again to see the new balances:

| id | plan | days | before | after one POST | why |
|---|---|---|---|---|---|
| 1 | basic | 20 | 5000.00 | 5000.00 | first 30 days: no interest |
| 2 | basic | 31 | 10000.00 | 10008.33 | 1% / 12 |
| 3 | student | 200 | 20000.00 | 20050.00 | 3% / 12 |
| 4 | student | 400 | 15000.00 | 15000.00 | no interest after 365 days |
| 5 | premium | 45 | 50000.00 | 50000.00 | first 45 days: no interest |
| 6 | premium | 90 | 100000.00 | 100416.67 | 5% / 12 |

Without Swagger:
```bash
curl http://localhost:8080/time-deposits
curl -X POST http://localhost:8080/time-deposits/balance-updates
```
Stop and reset the database: `docker compose down -v`. Connection settings can be overridden with `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

## API
| Endpoint | Success | Body |
|---|---|---|
| `POST /time-deposits/balance-updates` | `204 No Content` | none |
| `GET /time-deposits` | `200 OK` | array of `{id, planType, balance, days, withdrawals: [{id, amount, date}]}`, ordered by id |

Money values are returned with two decimals (`10008.33`), dates as ISO strings (`2026-01-15`). Invalid input and errors are not specially handled (README: not required), so failures return Spring's default `500`.

## Tests
```bash
mvn test        # Docker must be running
```
| Test | What it covers |
|---|---|
| `TimeDepositCalculatorTest` (43), `PlanTypeTest` (10) | Legacy interest behavior: boundaries, exact rates, rounding, repeated updates. Unit tests, no Docker. |
| `TimeDepositCalculatorStrategyTest` (2) | A custom interest rule can be plugged in, and its result is rounded like the standard rule's. |
| `TimeDepositPersistenceAdapterTest` | JPA mapping against PostgreSQL, withdrawals loaded in one query, the `double` → `NUMERIC(19,2)` boundary |
| `TimeDepositApiTest` | Both endpoints over HTTP: exact JSON, interest results, repeated POST, the OpenAPI document lists exactly two paths |
| `DemoDataMigrationTest` | The dev-only demo data migrates cleanly (in its own database) |

## Architecture
A pragmatic hexagonal design. Dependencies point inward: `adapter → application → domain`.

```
org.ikigaidigital                          domain: TimeDeposit, TimeDepositCalculator, PlanType, Withdrawal,
                                           InterestCalculationStrategy, StandardInterestCalculation (framework-free)
org.ikigaidigital.application              TimeDepositService (use cases), TimeDepositRepository (output port)
org.ikigaidigital.adapter.in.web           TimeDepositController, response records, OpenAPI annotations
org.ikigaidigital.adapter.out.persistence  JPA entities, Spring Data repository, TimeDepositPersistenceAdapter
```
- **Schema:** Flyway owns it (`db/migration`); Hibernate only validates the mapping. Demo data lives separately in `db/demo` and is loaded only by the `dev` profile.
- **Interest rules:**
  - Plan-specific rates and eligibility windows are data in the `PlanType` enum, so a plan with the same rule shape is one new enum line.
  - A different *calculation* rule (e.g. tiered rates) is a new `InterestCalculationStrategy`, passed to `new TimeDepositCalculator(strategy)`. It can delegate to `StandardInterestCalculation` for the plans it doesn't handle, so the existing classes stay unchanged.
  - `TimeDepositCalculator` always does the rounding.

## Design decisions and assumptions
The README leaves these open; each is a deliberate choice.

- **Endpoints:** the verbs, paths and status codes above are assumptions. Swagger's own `/v3/api-docs` and `/swagger-ui` are documentation (required by the README), not API endpoints.
- **Interest:** annual rate / 12 per update. Basic from day 31; student days 31–365; premium from day 46. Unknown plan types earn nothing.
- **Not idempotent:** every POST applies another month of interest. `days` is stored data and is not incremented.
- **Transactions:**
  - POST runs load → calculate → save in **one short transaction**, so it is all or nothing.
  - GET has no application transaction: deposits and withdrawals are loaded in **one query** and mapped inside the persistence adapter.
  - Open Session in View is disabled.
- **Concurrency is not handled (known limitation):** two POSTs running at the same time can both read the same balances. The second commit then overwrites the first, so one month is lost (a *lost update*) even though both return `204`.
  - Possible fixes: `SELECT … FOR UPDATE ORDER BY id`, or an optimistic `@Version` column.
  - Neither would make the POST idempotent: concurrency safety and idempotency are separate concerns.
- **Money boundary:**
  - The legacy calculator works with `double`; the database stores `NUMERIC(19,2)`. Balances are rounded `HALF_UP` to two decimals **when stored**.
  - So the stored value can differ from the calculator's raw `double`: student 1234567.89 → calculator `1237654.3099999998`, stored and returned `1237654.31`.
  - Each POST calculates from the stored value, so repeated monthly updates through the API can, in rare cases, differ by a cent from repeated in-memory calls.
  - A 2-decimal value maps to the nearest `double` and back without loss up to about 9×10¹³.
- **Database names:** the README's camelCase names (`timeDeposits`, `planType`, `timeDepositId`) are kept exactly, using quoted identifiers.
- **No constraint on `planType`:** unknown plan types can be stored, as before.
- **IDs:** plain integer primary keys with no generation, since no endpoint creates records.
- **Withdrawals:** returned ordered by id, `[]` when there are none. They do not affect interest.

# comments

I intentionally kept `PlanType` as an enum because the existing plans differ only in their configuration, not in their calculation behavior. Introducing a separate strategy for each plan would add unnecessary complexity at this stage.

At the same time, I introduced `InterestCalculationStrategy` to separate the interest calculation algorithm from balance updates and rounding.

This allows more complex calculation algorithms to be introduced without modifying the existing calculator or its public API.

If future requirements involve different calculation methods per product, daily accrual, or more sophisticated interest rules, the design can be extended with strategy resolution and additional calculation context.

The goal was to provide a clear and maintainable solution that is extensible without overengineering the current requirements.
