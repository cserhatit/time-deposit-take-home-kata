# Initial Analysis — Time Deposit Refactoring Kata

Date: 2026-11-18. Baseline commit: `c4ea358` (upstream `main`).

Scope:
- `README.md` (repository root)
- `java/pom.xml`
- `java/src/main/java/org/ikigaidigital/TimeDeposit.java`
- `java/src/main/java/org/ikigaidigital/TimeDepositCalculator.java`
- `java/src/test/java/org/ikigaidigital/TimeDepositCalculatorTest.java`

> Produced with AI assistance (Claude Code) and reviewed by the author.

## How to read this document

Each claim carries one of these markers:

| Marker | Meaning | Strength |
|---|---|---|
| **[T]** | **Verified by test.** Proven by a named characterization test in `TimeDepositCalculatorTest`, with expected values recorded from the original implementation. | Strongest |
| **[C]** | **Confirmed by reading.** Read directly from the original code or the README, but not proven by a test. | Strong |
| **[A]** | **Assumption.** A decision made where the README is silent or ambiguous. It must be documented, and justified in code comments where it is applied. | Needs justification |

The tests were checked against two deliberate mistakes in the calculator: using `BigDecimal.valueOf` instead of `new BigDecimal`, and changing `days < 366` to `days <= 366`. Both made tests fail. The calculator was then restored from git.

---

## 1. Requirements

### 1.1 Functional (README) [C]
- **Exactly two REST endpoints:**
  - update the balances of all time deposits;
  - return all time deposits with the schema `id`, `planType`, `balance`, `days`, `withdrawals`.
- **Database tables:**
  - `timeDeposits`: `id` (Integer, PK), `planType` (String, required), `days` (Integer, required), `balance` (Decimal, required).
  - `withdrawals`: `id` (Integer, PK), `timeDepositId` (Integer, FK, required), `amount` (Decimal, required), `date` (Date, required).
- **Interest (monthly):**
  - Basic 1%.
  - Student 3%, no interest after 1 year.
  - Premium 5%, interest starts after 45 days.
  - No plan earns interest in its first 30 days.

### 1.2 Technical constraints (README) [C]
- No breaking changes to the shared `TimeDeposit` class. The `updateBalance` signature must not change.
- `TimeDepositCalculator.updateBalance` "is functioning correctly": its behavior must stay the same.
- The design must be extensible for future interest rules, and follow SOLID and clean code.
- Handling invalid input and exceptions is **not** required.
- Ambiguities must be resolved by logical assumptions, justified in code comments.
- Preferred stack: an OpenAPI/Swagger contract, hexagonal architecture, atomic commits, Testcontainers, AI-assisted development.
- AI documentation: the tools and setup used, custom rules/prompts, and which parts were AI-assisted and why.
- Submission: instructions for calling the endpoints through Swagger, and a public fork (no PR to the original repository).

---

## 2. Original Code

### 2.1 Project state at baseline [C]
- Plain Java 17 / Maven project with no framework, database, API or withdrawals model.
- `pom.xml` contains only `junit-jupiter-engine` 5.9.0 and `assertj-core` 3.23.1 (test scope), plus `maven-surefire-plugin` 3.0.0-M3 and `maven-compiler-plugin` 3.8.1.
- `mvn test` passed with one test, which asserted nothing.

### 2.2 `TimeDeposit` [C]

```java
public class TimeDeposit {
    private int id;
    private String planType;
    private Double balance;
    private int days;

    public TimeDeposit(int id, String planType, Double balance, int days) { ... }

    public int getId() { return id; }
    public String getPlanType() { return planType; }
    public Double getBalance() { return balance; }
    public int getDays() { return days; }
    public void setBalance(Double balance) { this.balance = balance; }
}
```

- A mutable data holder.
- Its public API (constructor, four getters, `setBalance`) is the "shared" contract that must not break.

### 2.3 `TimeDepositCalculator` [C]

```java
public class TimeDepositCalculator {
    public void updateBalance(List<TimeDeposit> xs) {
        for (int i = 0; i < xs.size(); i++) {
            double interest = 0;

            if (xs.get(i).getDays() > 30) {
                if (xs.get(i).getPlanType().equals("student")) {
                    if (xs.get(i).getDays() < 366) {
                        interest += xs.get(i).getBalance() * 0.03 / 12;
                    }
                } else if (xs.get(i).getPlanType().equals("premium")) {
                    if (xs.get(i).getDays() > 45) {
                        interest += xs.get(i).getBalance() * 0.05 / 12;
                    }
                } else if (xs.get(i).getPlanType().equals("basic")) {
                    interest += xs.get(i).getBalance() * 0.01 / 12;
                }
            }

            double a2d = xs.get(i).getBalance() + (new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP)).doubleValue();
            xs.get(i).setBalance(a2d);
        }
    }
}
```

### 2.4 Original `TimeDepositCalculatorTest` [C]
- Ran `updateBalance` on `basic`, `1234567.00`, 45 days, then asserted `assertThat(1).isEqualTo(1)`.
- **It checked nothing.** The calculator effectively had no tests.
- It has since been replaced by the characterization tests described in §5.

---

## 3. Business Rules — as implemented

These are the rules the code actually applies today. They are the behavior to preserve.

### 3.1 Eligibility and rates

| `planType` (exact text) | Earns interest when | Monthly interest (`double`) | Evidence |
|---|---|---|---|
| `"basic"` | `days > 30` (day 31 onwards, no upper limit) | `balance * 0.01 / 12` | [T] `appliesOneMonthOfInterestOnlyWithinEligibleDays` (0, 29, 30, 31, 3650) |
| `"student"` | `30 < days < 366` (days 31–365) | `balance * 0.03 / 12` | [T] same test (30, 31, 365, 366) |
| `"premium"` | `days > 45` (day 46 onwards; the 30-day check is redundant here) | `balance * 0.05 / 12` | [T] same test (30, 31, 44, 45, 46) |
| anything else | never | `0` | [T] `unknownOrDifferentlyCasedPlanTypeEarnsNoInterest` |

### 3.2 Calculation and rounding

| # | Rule | Evidence |
|---|---|---|
| R1 | The rates are annual and are divided by 12: one call applies **one month** of interest. | [T] e.g. 10000.00 basic → 10008.33 |
| R2 | Matching the plan type is **case-sensitive and exact**: `"Basic"`, `"PREMIUM"`, `" student"` and `""` earn nothing. | [T] `unknownOrDifferentlyCasedPlanTypeEarnsNoInterest` |
| R3 | **Only the interest is rounded:** to 2 decimals, `HALF_UP`. | [T] `roundsOnlyTheInterestHalfUpToTwoDecimalsFromItsExactBinaryValue` |
| R4 | **Rounding starts from the exact binary value of the `double`** (`new BigDecimal(double)`). For example, 18.00 basic gives interest `0.01499999…`, which becomes 0.01. `BigDecimal.valueOf` would give 0.02. | [T] same test (18.00 basic, 25.20 premium) |
| R5 | **The balance itself is never rounded.** Its extra decimals are kept, and the `double` sum can pick up floating-point noise: student 1234567.89 → `1237654.3099999998`. | [T] `doesNotRoundTheBalanceItself`, `appliesTheAnnualRateDividedByTwelve` |
| R6 | **Negative balances** get negative interest; `HALF_UP` rounds away from zero (−6.00 → −6.01). | [T] rounding test |
| R7 | The sum is calculated in `double`. Very large balances keep only `double` precision (1e15 → 1.0008333333333334E15). | [T] `keepsDoublePrecisionForVeryLargeBalances` |
| R8 | The expression order is `balance * rate / 12`. Rewriting it as `balance * (rate / 12)` *may* change the last bit of the result. | [C] code; the effect is not demonstrated by a test |

### 3.3 State and side effects

| # | Rule | Evidence |
|---|---|---|
| S1 | Deposits are updated **in place**. Only `balance` changes; `id`, `planType` and `days` stay the same. | [T] `changesOnlyTheBalance` |
| S2 | `days` is **never incremented** by the calculator. | [T] `repeatedUpdatesCompoundInterestWithoutChangingDays` |
| S3 | **Not idempotent:** each call adds another month, compounded on the new balance (10000.00 → 10008.33 → 10016.67). | [T] same test |
| S4 | A deposit that isn't eligible stays unchanged after any number of calls. | [T] `repeatedUpdatesLeaveIneligibleDepositUnchanged` |
| S5 | Each deposit in the list is calculated on its own. | [T] `updatesEachDepositInTheListIndependently` |
| S6 | An empty list is a no-op. | [T] `acceptsAnEmptyList` |
| S7 | Withdrawals play no part: `TimeDeposit` has no withdrawals. | [C] |
| S8 | The README example `basic`, 1234567.00, 45 days → 1235595.81. | [T] `updatesTheOriginalExampleDeposit` |

### 3.4 Invalid input
The README says invalid-input handling is not required, so these are documented only and not preserved as a compatibility requirement.

| # | Input | Behavior | Evidence |
|---|---|---|---|
| N1 | `null` balance | Always throws `NullPointerException` (unboxing on the rounding line). | [C] |
| N2 | `null` planType | Original: throws NPE only when `days > 30`, because the 30-day check ran before any plan comparison. **Changed during refactoring** (interest-free days moved into `PlanType`, so the plan is looked up first): now always throws NPE. Accepted by the author, because the README says invalid input and exceptions need not be handled; valid input is unaffected (2,000,000 random cases bit-identical). | [C] |
| N3 | `null` list | Throws NPE. | [C] |

---

## 4. Identified Issues and Risks

### 4.1 Code smells [C]

| # | Location | Issue |
|---|---|---|
| I1 | `TimeDepositCalculator.updateBalance` | Indexed loop calling `xs.get(i)` repeatedly; unclear names (`xs`, `a2d`). |
| I2 | `TimeDepositCalculator.updateBalance` | Nested `if`/`else if` on strings. Adding a plan means editing this method, which breaks the open/closed principle (the README asks for extensibility). |
| I3 | `TimeDepositCalculator.updateBalance` | Magic numbers and strings: `30`, `45`, `366`, `0.01`, `0.03`, `0.05`, `12`, plan names. |
| I4 | `TimeDepositCalculator.updateBalance` | Money held in `double`, with `double` and `BigDecimal` mixed only for rounding. Kept on purpose, because the behavior must be preserved. |
| I5 | `TimeDepositCalculator.updateBalance` | Unknown plan types are silently ignored. |
| I6 | `TimeDeposit` | Boxed, nullable `Double` balance; fields that never change aren't `final`; no `toString`/`equals`/`hashCode`. Can't be fixed without breaking the shared API. |
| I7 | `TimeDeposit` | No withdrawals, although the GET schema needs them. |
| I8 | `TimeDepositCalculatorTest` | The assertion checked nothing. **Resolved** by the characterization tests (§5). |

### 4.2 Architectural weaknesses [C]
- One package, no layers.
- The persistence, API and application layers don't exist yet.

### 4.3 Build [C]
- `maven-surefire-plugin` 3.0.0-M3 is an old milestone release.
- Spring, database and OpenAPI dependencies are still to be added.
- `time-deposit-kata.iml` (an IntelliJ project file) is committed to git.

### 4.4 Risks

| # | Risk | Mitigation |
|---|---|---|
| K1 | **Rounding regression:** a well-meant "fix" (`BigDecimal.valueOf`, rounding the balance, reordering the formula) silently changes results. | R3–R7 are pinned by tests. R8 is not pinned; keep the expression exactly as written. |
| K2 | **Boundary regression** at day 30/31, 45/46 and 365/366. | Pinned by tests. |
| K3 | **Repeated updates:** the update endpoint applies interest on every call; two calls, or two at the same time, double-apply it. | Documented, not guarded: every POST applies a month by design (A5). Concurrent POSTs can lose an update, because there is no locking (A13). |
| K4 | **Database precision:** converting between `double` (domain) and `DECIMAL` (database) can round or add floating-point noise. | Conversion happens only in `TimeDepositPersistenceAdapter`: rounded explicitly with `HALF_UP` to 2 decimals on save; covered by `TimeDepositPersistenceAdapterTest` and `TimeDepositApiTest`. |
| K5 | **The "shared" `TimeDeposit`:** changing its public API could break consumers we can't see. | Keep it unchanged [A]. |
| K6 | **Environment:** Testcontainers needs a running Docker daemon; Docker was not running at analysis time. | Documented in `java/README.md`. |

---

## 5. Test Coverage

### 5.1 Characterization tests — written (step 1)
`TimeDepositCalculatorTest`: 43 test cases, all passing, with values recorded from the original implementation.

| Area | Cases | Test |
|---|---|---|
| Day boundaries | basic 0/29/30/31/3650; student 30/31/365/366; premium 30/31/44/45/46 | `appliesOneMonthOfInterestOnlyWithinEligibleDays` |
| Exact rates | each plan with 50000.00, 999.99 and 1234567.89 | `appliesTheAnnualRateDividedByTwelve` |
| Plan type | `gold`, `Basic`, `PREMIUM`, `' student'`, `''` | `unknownOrDifferentlyCasedPlanTypeEarnsNoInterest` |
| Rounding | below half a cent → 0; exact half → up; binary below half (18.00 basic, 25.20 premium); zero; negative | `roundsOnlyTheInterestHalfUpToTwoDecimalsFromItsExactBinaryValue` |
| Unrounded balance | 100.005 basic | `doesNotRoundTheBalanceItself` |
| Large amount | 1e15 basic | `keepsDoublePrecisionForVeryLargeBalances` |
| README example | 1234567.00 basic, 45 days | `updatesTheOriginalExampleDeposit` |
| Repeated updates | compounding; ineligible deposit unchanged | `repeatedUpdatesCompoundInterestWithoutChangingDays`, `repeatedUpdatesLeaveIneligibleDepositUnchanged` |
| List handling | several deposits; empty list; only the balance changes | `updatesEachDepositInTheListIndependently`, `acceptsAnEmptyList`, `changesOnlyTheBalance` |

Supporting change: `junit-jupiter-params` 5.9.0 (test scope) was added to `pom.xml`.

### 5.2 Still missing
- **Intentionally not tested:** the invalid-input behaviors N1–N3. Out of scope per the README.
- **Not demonstrated by a test:** R8 (calculation order). It is a general floating-point property; there's no concrete input showing a difference.
- **Integration tests (written, PostgreSQL via Testcontainers):**
  - `TimeDepositPersistenceAdapterTest`: entity mapping with camelCase names, withdrawals loaded in one query, the decimal round trip.
  - `TimeDepositApiTest`: both endpoints over HTTP, exact JSON, interest results, a repeated POST, and that the OpenAPI document lists exactly two paths.
  - `DemoDataMigrationTest`: the dev-only demo data applies cleanly.
  - A `@WebMvcTest` with mocks was planned but dropped, because it overlapped with `TimeDepositApiTest`.

---

## 6. Ambiguities and Assumptions

| # | Ambiguity | Resolution | Marker |
|---|---|---|---|
| A1 | Is "1% interest" annual or monthly? | Annual rate, applied as `rate / 12` per update; the code is the source of truth. | [T] |
| A2 | "No interest after 1 year" (Student) | 1 year = 365 days: interest through day 365, none from day 366. | [T] |
| A3 | "First 30 days" / "starts after 45 days" | Strictly greater than: interest from day 31 / day 46. | [T] |
| A4 | Who increments `days`? | Not the update endpoint. `days` is stored data; one update = one month's interest. | [A] |
| A5 | Repeated calls to the update endpoint | Each call applies interest again (S3). A guard would change behavior, so this is documented, not prevented. | [T] calculator / [A] endpoint |
| A6 | Shape of `withdrawals` in GET | An array of `{id, amount, date}` per deposit. | [A] |
| A7 | Do withdrawals affect interest? | No. They are read-model data only. | [C] |
| A8 | Decimal precision and scale | `NUMERIC(19,2)`. `double` ↔ `BigDecimal` conversion happens in the persistence adapter: `BigDecimal.valueOf(double).setScale(2, HALF_UP)` on save. The stored value can differ from the calculator's raw `double` (student 1234567.89: `1237654.3099999998` → `1237654.31`); the calculator itself is unchanged. | [A], tested |
| A9 | camelCase table and column names | Kept exactly as specified, using quoted identifiers and Hibernate's standard naming strategy, so Spring's snake_case conversion doesn't apply. | [A] |
| A10 | Plan type values | Lowercase `basic` / `student` / `premium`, stored as given; unknown values earn nothing. | [T] |
| A11 | HTTP verb and path of the update endpoint | `POST /time-deposits/balance-updates` → `204 No Content`. | [A] decided by the author |
| A12 | Initial data | Demo deposits and withdrawals are in a separate repeatable Flyway script (`db/demo`), loaded **only** by the `dev` profile. The schema (`db/migration`) applies everywhere. | [A] |
| A13 | Concurrent update calls | Out of scope, so **no locking** (author decision). Each POST is one short transaction, but two at the same time can lose one month (lost update). Possible fixes, not implemented: `SELECT … FOR UPDATE ORDER BY id`, or `@Version`. Neither would make POST idempotent. | [A] |
| A14 | Can `TimeDeposit` gain a `withdrawals` field? | No: keep `TimeDeposit` unchanged and model withdrawals in the read model (conservative reading of "no breaking changes"). | [A] |

---

## 7. Recommended Refactoring Steps

Small steps, each a separate atomic commit with all tests green:

| # | Step | Status |
|---|---|---|
| 1 | **Characterization tests** (§5.1), replacing the test that checked nothing. No production change. | Done (`a9f0265`) |
| 2 | **Clean up `updateBalance` internally:** for-each loop, clear names, named constants. Same signature, same arithmetic. | Done (`e87cd95`) |
| 3 | **Plan terms in the `PlanType` enum (the extensibility point)** — *replaces the originally planned `InterestPolicy` classes.* Each constant holds its code, annual rate, interest-free days and last interest day; `PlanType.earnsInterestOn(days)` owns the eligibility window. A new plan with the same rule shape is one enum line; the calculator is unchanged. Formula and rounding stay in the calculator, unchanged. | Done (`3302aca`) |
| 4 | **Spring Boot setup:** Boot 3.5.16 parent, web and springdoc; no database dependencies yet, because they would trigger DataSource auto-configuration. | Done (`4cbe0a2`) |
| 5 | **Database:** JPA, Flyway, PostgreSQL; `V1` schema, dev-only demo data, `compose.yaml`, shared Testcontainers configuration. | Done (`4cbe0a2`) |
| 6 | **Persistence adapter:** `Withdrawal`, entities, Spring Data repository, `TimeDepositRepository` port, adapter, with Testcontainers tests. | Done (`5b20e7a`) |
| 7 | **Application service and REST API:** `TimeDepositService` (one `@Transactional` for the POST only), controller, response records, springdoc annotations, API test. | Done (`1bdb393`) |
| 8 | **Finish:** `java/README.md` (run, Swagger usage, assumptions), documentation updates, dependency and dead-code review. | Done (`a7ee4e0`) |
| 9 | **Interest calculation strategy** (README: "extensible to accommodate future complexities in interest calculations"): `InterestCalculationStrategy.calculateInterest(TimeDeposit)`; the legacy rule moved verbatim into `StandardInterestCalculation`; `TimeDepositCalculator` keeps orchestration and rounding; a new constructor `TimeDepositCalculator(InterestCalculationStrategy)`, while `new TimeDepositCalculator()` still uses the standard rule. | Done (uncommitted) |

Design notes for step 3 (decided by the author):
- **Plan types are the `PlanType` enum** (decided by the author, to avoid typing mistakes). `TimeDeposit` keeps its `String planType` (shared API); `PlanType.fromCode` matches case-sensitively and returns `Optional.empty()` for unknown codes instead of throwing like `Enum.valueOf`, which keeps R2 and A10.
- **Interest window per plan, as data in `PlanType`:** `interestFreeDays` (30 for basic/student, 45 for premium) and `lastInterestDay` (365 for student, unlimited otherwise). This keeps the README's "for any existing plans" open for future plans with a different window.
- **No per-plan `InterestPolicy` classes:** the plans differ only in data, not in behavior.
- **Revisited in step 9 (developer decision):** one *behavior-level* extension point, `InterestCalculationStrategy`, for future calculation rules (e.g. tiered rates, a different accrual method), because the README explicitly requires extensibility of the calculation.
  - Plan data stays in `PlanType`. A new plan with the same rule shape is one enum line; a new rule shape is a new strategy, which can delegate to `StandardInterestCalculation`.
  - Not added: per-plan strategies, factories, registries.

---

## 8. Architectural Recommendations

Hexagonal architecture, kept proportionate:

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator,     ← domain; package and class names unchanged (shared)
│   PlanType, Withdrawal record,
│   InterestCalculationStrategy,            ← extension point for calculation rules
│   StandardInterestCalculation             ← the legacy rule (default)
├── application/                             ← TimeDepositService (both use cases), TimeDepositRepository (output port),
│                                              TimeDepositWithWithdrawals (read record)
└── adapter/
    ├── in/web/                              ← REST controller with springdoc annotations, DTO mapping
    └── out/persistence/                     ← JPA entities, Spring Data repositories, entity ↔ domain mapping (double ↔ DECIMAL)
```

- **Domain:** no dependency on any framework. `TimeDeposit` and `TimeDepositCalculator` keep their package, class names and public API.
- **Interfaces:** only where there is a concrete benefit: the output port `TimeDepositRepository` (dependency inversion toward the database), and `InterestCalculationStrategy` (README-required extensibility of the calculation). Plan-specific values stay data in the `PlanType` enum. The use cases are plain service classes; input-port interfaces would add nothing here.
- **Read model:** GET returns a `TimeDeposit` together with its `List<Withdrawal>`, mapped to the API DTO. `TimeDeposit` stays untouched.
- **Stack:** Spring Boot 3.5.16 (Java 17), Spring Data JPA, springdoc-openapi 2.10.0, PostgreSQL 17, Flyway, Testcontainers (PostgreSQL), JUnit 5, AssertJ. All versions except springdoc are managed by the Spring Boot BOM.
- **Money:** the legacy `double` calculation is preserved. `BigDecimal` is used at the API and persistence boundaries.

### Decisions (all made by the author)
1. **OpenAPI: code-first.** springdoc-openapi generates the contract from the controller and its annotations; Swagger UI and `/v3/api-docs` are the published contract.
2. **Database access: Spring Data JPA.**
   - The JPA entities (`TimeDepositEntity`, `WithdrawalEntity`) are separate from the domain `TimeDeposit`, so the shared class gets no JPA annotations.
   - Flyway owns the schema; Hibernate only validates it (`ddl-auto=validate`).
   - The camelCase names are mapped explicitly.
   - Withdrawals are fetched together with their deposits (one fetch-join query), to avoid one query per deposit (the N+1 problem).
3. **Interest window per plan:** interest-free days and last interest day are fields of `PlanType` (no policy classes).
4. **Update endpoint:** `POST /time-deposits/balance-updates` → `204 No Content`.
5. **Transactions (rev. 3):**
   - POST: one short `@Transactional` on `TimeDepositService.updateAllBalances()` covering load, calculation and save.
   - GET: no application transaction; the adapter loads and maps everything in one query.
   - `spring.jpa.open-in-view=false`.
   - Not split into a read outside the transaction and a write inside it: that would add stale-read risk and lose atomicity.
6. **No locking:** concurrency control is outside the README (see A13).
7. **Interest calculation strategy:** one interface (`calculateInterest`) with one standard implementation; rounding stays in `TimeDepositCalculator`; no per-plan strategies.
