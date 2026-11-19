# AI-Assisted Development

This document records how AI assistance was used in this take-home exercise. It covers the analysis, characterization tests, domain refactoring, the architecture plan, and the Spring Boot implementation (persistence, REST API, integration tests).

## 1. Tools and Setup

| Item | Value |
|---|---|
| AI coding assistant | Claude Code CLI 2.1.327 (version at the time of writing) |
| Model | Claude Opus 5.5 (`claude-opus-5-5`) |
| IDE | Eclipse (formatting and manual review) |
| JDK / build | OpenJDK 21.0.8 running a Java 17 build (`java.version = 17`), Apache Maven 3.9.9 |
| Framework | Spring Boot 3.5.16, springdoc-openapi 2.10.0, Flyway 11.7.2, PostgreSQL 17 (Docker Engine 29.7.2) |
| Test libraries | JUnit 5.12.2, AssertJ 3.27.7, Testcontainers 1.21.4 (managed by Spring Boot). The domain phase used JUnit 5.9.0 and AssertJ 3.23.1. |

### Project configuration
- **`CLAUDE.md`** (committed in `e164ca6`) holds the project rules the assistant follows. In summary:
  - the README is the source of truth;
  - the behavior of `updateBalance` must be preserved; never silently change formulas, eligibility boundaries or rounding;
  - add characterization tests before refactoring, then make small, verifiable steps and run the tests after each;
  - hexagonal architecture without needless abstractions;
  - Java 17+, JUnit 5 and AssertJ;
  - Javadoc on every class and public method (rule added at the developer's request);
  - no production changes during analysis; ask for approval before significant architectural changes;
  - no commits without approval.
- **Ponytail plugin** (a Claude Code plugin in the developer's local setup, loaded at session start). It tells the assistant to make the smallest complete change and avoid speculative abstractions. It reinforced the "keep it proportionate" rule in `CLAUDE.md`.
- **Plan mode** was used for the design review (§3.4) and the architecture plan (§3.5): the assistant wrote findings and a proposal, and made changes only after explicit approval.
- **Memory:** one saved preference: never edit the assignment `README.md` in the repository root; solution documentation lives under `java/`.

### Reproducing the setup
Open the `java/` directory in Claude Code. `CLAUDE.md` is loaded automatically. The developer's prompts are listed in §2.

## 2. Workflow and Prompts

Every step followed the same loop:
1. The developer gives a scoped prompt.
2. The assistant analyzes or implements.
3. Maven tests run.
4. The developer reviews the result and commits it manually.

The assistant never commits.

| Phase | Developer prompt (summary) | Result | Commit |
|---|---|---|---|
| Setup | Add a Javadoc rule to `CLAUDE.md` | `CLAUDE.md` rule | `e164ca6` |
| Analysis | Read README, sources, tests and pom; analyze requirements, behavior, smells, missing tests, ambiguities; propose a plan and architecture; **no file changes** | `docs/INITIAL_ANALYSIS.md` | `e164ca6` |
| Decisions | The developer chose code-first OpenAPI, Spring Data JPA, the interest window per plan, and `POST /time-deposits/balance-updates` → 204 | Recorded in `INITIAL_ANALYSIS.md` §8 | `e164ca6` |
| Characterization tests | Pin the existing behavior: boundaries, exact rates, rounding, multiple and repeated updates, unknown plans; parameterized JUnit 5 + AssertJ; no production changes | `TimeDepositCalculatorTest` | `a9f0265` |
| Clean Code refactoring | Improve readability without changing behavior; no Strategy pattern or extra layers; plus developer requests: `PlanType` enum, rate and free days as enum fields, a more readable `earnsInterest`, rename `roundToCents` | `PlanType`, refactored calculator, Javadoc | `e87cd95` |
| Design review | Review against README and SOLID; recommend keeping the design unchanged if it is sufficient | Eligibility moved into `PlanType.earnsInterestOn` | `3302aca` |
| AI usage documentation | Document the workflow from the repository history; don't invent versions or results | This file | `35762e0` |
| Architecture plan | Plan Spring Boot, REST, JPA, Flyway, Testcontainers: contracts, layers, schema, transactions, tests, steps; then two rounds of developer revisions (§3.5) | Approved plan rev. 3 | — |
| Spring Boot skeleton + database | Implement plan steps 1–2 | Boot parent, Flyway schema, dev-only demo data, `compose.yaml`, Testcontainers | `4cbe0a2` |
| Persistence | Implement plan step 3 | Entities, port, adapter, adapter tests | `5b20e7a` |
| REST API | Implement plan step 4 | Service, controller, response records, API test | `1bdb393` |
| Finish | Implement plan step 5 | `java/README.md`, documentation updates, dependency review | `a7ee4e0` |
| Calculation extensibility | Review enum vs. strategy; add a minimal strategy extension point without changing behavior; developer adjustments: method name `calculateInterest`, the "gold" example only in tests | `InterestCalculationStrategy`, `MonthlyInterestCalculation`, `TimeDepositCalculatorStrategyTest` | `33d853e` |

## 3. AI-Assisted Work

### 3.1 Initial analysis
- **Behavior documented:** the assistant read the original code and documented its exact behavior.
  - Eligibility: basic from day 31; student days 31–365; premium from day 46.
  - Formula: `balance * rate / 12` in `double`.
  - Rounding: `new BigDecimal(double)` with `HALF_UP` to two decimals, applied to the interest only.
  - Updates happen in place and are not idempotent.
- **Other findings:** the code smells, risks and 14 ambiguities.
- **Labels:** every claim is marked *verified by test*, *confirmed by reading* or *assumption* (`docs/INITIAL_ANALYSIS.md`).
- **Original test:** it asserted nothing (`assertThat(1).isEqualTo(1)`).

### 3.2 Characterization tests
- **Expected values:** recorded by running the original calculator, not calculated by hand. The tests compare exact `double` values.
- **Coverage:**
  - day boundaries (basic 0/29/30/31/3650, student 30/31/365/366, premium 30/31/44/45/46);
  - exact rates for each plan;
  - rounding: below half, exactly half, and binary values just below half;
  - zero, negative and very large balances;
  - an unrounded balance;
  - repeated updates, several deposits in one list, unknown or wrongly cased plan names, an empty list.
- **Unusual behavior found and pinned:**
  - **Binary half-cent case:** 18.00 basic gives interest `0.01499999…`, which rounds to 0.01. `BigDecimal.valueOf` would give 0.02.
  - **Floating-point noise:** student 1234567.89 gives a balance of `1237654.3099999998`.
- **Dependency:** the only addition was `junit-jupiter-params`, test scope.

### 3.3 Clean Code refactoring
- **Calculator:**
  - a for-each loop replaces the indexed loop;
  - the nested conditionals became a pipeline: look up the plan, check eligibility, compute the monthly interest;
  - the unnamed numbers became named constants;
  - the formula and the rounding were extracted into small methods.
- **`PlanType` enum (developer request):**
  - holds each plan's code, annual rate, interest-free days and last interest day;
  - `fromCode` matches case-sensitively and returns `Optional.empty()` for unknown codes, so unknown plans still earn nothing.
- **`TimeDeposit`:**
  - the unchanging fields became `final`, and Javadoc was added;
  - the public API is unchanged (compared with `javap`).
- **Comments in the code:** they explain the two choices that must not be "fixed": the operation order and `new BigDecimal(double)`.

### 3.4 Design review
- **Verdict:** the design is sufficient. A new plan with the same rule shape is one enum line, and the calculator has one clear job.
- **One change made:** the eligibility window moved into `PlanType.earnsInterestOn(days)`, and the getters that exposed it were removed (`3302aca`).

### 3.5 Architecture plan and its revisions
The assistant produced the plan; the developer revised it twice before approving it.
- **Rev. 2 (developer review):**
  - **Bootstrap order:** database dependencies are added only together with Testcontainers, so no test needs PostgreSQL before it is available.
  - **Query count:** the update path dropped an extra `findAllById` SELECT.
  - **Lock ordering:** deterministic, by id.
  - **Demo data:** separated from the schema migrations and loaded only in a `dev` profile.
  - **Money boundary:** documented and tested.
  - **Tests:** a slimmer suite, with the overlapping mock-based tests dropped.
- **Rev. 3 (developer decisions):**
  - short-lived transactions;
  - no application-level transaction for GET;
  - one short `@Transactional` for POST;
  - **no locking**, because concurrency is outside the README;
  - Open Session in View disabled;
  - an explicit explanation of why the POST is not split into a read outside the transaction and a write inside it.

### 3.6 Spring Boot implementation
The assistant implemented the plan in steps; the developer committed each step after review.
- **Application:**
  - `TimeDepositService` reuses `TimeDepositCalculator.updateBalance` unchanged;
  - the output port `TimeDepositRepository` is implemented by a JPA adapter;
  - entities never leave the persistence package.
- **REST:** `POST /time-deposits/balance-updates` → 204 and `GET /time-deposits` → 200, documented code-first with springdoc.
- **Corrections found during implementation:**
  - **Plan error:** the plan said `saveBalances` would be "still correct, just slower" outside a transaction. That was wrong: `getReferenceById` would return an uninitialized proxy, and setting the balance would throw. The method is now `@Transactional` and joins the caller's transaction.
  - **Order-dependent test:** `DemoDataMigrationTest` failed in the full suite. It shared a database with tests that use different Flyway settings, so the result depended on test order. It now has its own container.

### 3.7 Interest calculation strategy
- **Review:** the enum varies plan *data*; the strategy varies calculation *behavior*. The gain is narrow but real, and it meets the README's extensibility requirement.
- **Change:**
  - the legacy rule moved verbatim into `MonthlyInterestCalculation` (first named `StandardInterestCalculation`; renamed by the developer);
  - `TimeDepositCalculator` keeps the loop and the `new BigDecimal(double)` / `HALF_UP` rounding;
  - an added constructor accepts a strategy, and the no-argument constructor still uses the standard rule.
- **Developer adjustments:** the method is named `calculateInterest`; the hypothetical "gold" plan exists only in a test.
- **Verification:**
  - the 43 characterization tests are unchanged and pass;
  - the bit-for-bit comparison against the original calculator still shows 0 differences over 2,000,000 random deposits;
  - `javap` shows the only public API change is the new constructor;
  - two new behavior tests: a custom strategy is used and rounded the legacy way; a new rule can delegate to the standard one.

## 4. Decisions and Rejected Alternatives

| Decision | Rejected alternative | Reason |
|---|---|---|
| Keep `new BigDecimal(double)` | `BigDecimal.valueOf(double)` | Changes legacy rounding (e.g. 18.00 basic). |
| Keep `balance * rate / 12` and the `double` arithmetic | `balance * (rate / 12)`; `BigDecimal` throughout | The README requires unchanged behavior. Reordering *may* change results (not shown by a test). |
| Plan terms as data in the `PlanType` enum | Per-plan `InterestPolicy` / strategy classes (in the original plan); a string-keyed map | The plans differ only in data, not behavior. The enum prevents typing mistakes. |
| One behavior-level `InterestCalculationStrategy`, with the legacy rule as `MonthlyInterestCalculation` *(revisits the earlier "no strategy" decision, at the developer's request)* | Keeping the rule inside `TimeDepositCalculator`; per-plan strategies; factories or registries | The README requires calculation extensibility: a new rule shape becomes a new class instead of an edit. Rounding stays in the calculator, so every strategy is rounded the legacy way. |
| `PlanType.fromCode` returns `Optional` | `Enum.valueOf` | `valueOf` throws on unknown plans, which earned 0 before. |
| `TimeDeposit` keeps `String planType` and has no `equals`/`hashCode` | Change the field type; add `equals` | It's a shared class: no breaking changes, and no change to how deposits compare. |
| Code-first OpenAPI (springdoc) | Contract-first YAML + generator | Developer decision. |
| Spring Data JPA | `JdbcClient` with plain SQL (the assistant's original suggestion) | Developer decision. |
| No locking; lost-update risk documented | Pessimistic `SELECT … FOR UPDATE` (proposed in rev. 2); optimistic `@Version` | Developer decision: concurrency control is outside the README. Neither option would make POST idempotent. |
| One short transaction for POST; none in the application layer for GET | Reading outside the transaction and writing inside it | A split would add stale-read and lost-update risk and lose atomicity. GET is a single query. |
| Demo data in a dev-only repeatable Flyway script | Seed data in the versioned schema migrations | Demo data shouldn't reach every environment or the tests. |
| Real-stack API test (Testcontainers), no Mockito | `@WebMvcTest` with mocks; a service unit test with a fake | They overlapped with the API test, which checks the real JSON, transactions and SQL. |
| Balance rounded explicitly when stored (`HALF_UP`, 2 decimals) | Leaving the rounding to PostgreSQL | Explicit and tested; the calculator's raw `double` is unchanged. |
| Don't preserve the original `null` plan-type behavior | A global 30-day check before the plan lookup | The README says invalid input need not be handled. |

**How the `null` plan-type decision developed:** moving the interest-free days into `PlanType` meant a `null` plan type threw at every age; the original threw only after day 30. The developer first asked for the original behavior to be restored, which was done and tested. The developer then reconsidered and accepted the change, citing the README, so the restoration and its test were removed again. The final state is documented as N2 in `INITIAL_ANALYSIS.md`.

**Corrections the developer made to AI output:**
- Introducing the `PlanType` enum, and moving the rate and free days into it.
- A more readable `earnsInterest` (the developer supplied the target code).
- Renaming `roundToCents` to `roundToTwoDecimals`, since "cents" refers to currency.
- The `null` plan-type decision above.

## 5. Verification

Every result below was actually run during this work:

- **Baseline:** `mvn test` passed with the original single test, which asserted nothing.
- **Characterization tests:** 43 tests passed against the unchanged production code.
- **Do the tests catch regressions?** Two deliberate mistakes were temporarily put into the calculator: `BigDecimal.valueOf`, and `days <= 366` for students. Each made tests fail. The calculator was then restored from git.
- **After each refactoring step:** the full suite was run; the latest run is 53 tests (43 characterization + 10 `PlanTypeTest`), 0 failures.
- **Comparison with the original:**
  - the original `TimeDepositCalculator` (from git) was compiled next to the refactored one;
  - both ran on the same 2,000,000 random deposits (valid and unknown plans, ages 0–499, negative and very large balances, up to three repeated updates);
  - result: **0 bit differences**, after every refactoring step.
  - This comparison was a temporary script outside the repository and is **not** part of the committed test suite.
- **Public API:** `javap` output for `TimeDeposit` and `TimeDepositCalculator` was identical before and after the refactoring.
- **Committed characterization tests:** `git diff` confirmed they were never changed by the refactoring or by the Spring Boot work.

Spring Boot implementation:
- **Full suite (after the strategy change):** 63 tests, 0 failures: 43 characterization, 10 `PlanTypeTest`, 2 strategy, 3 persistence adapter, 4 API, 1 demo migration.
- **Query count (Hibernate SQL log):**
  - update path: 1 SELECT, then UPDATEs only for rows that changed, with no extra SELECT;
  - read path: 1 fetch-join SELECT.
- **Test-order independence:** after the `DemoDataMigrationTest` fix, the database tests also passed when run as separate subsets.
- **Manual end-to-end:** `docker compose up`, then the app with the `dev` profile.
  - Flyway applied `V1` and the demo script; a restart reported "Schema is up to date" with no duplicate rows.
  - GET → POST (`204`) → GET showed the expected balances (`10008.33`, `20050.00`, `100416.67`; the deposits that weren't eligible stayed unchanged).
  - Swagger UI returned `200`.
- **Dependency review:** `mvn dependency:analyze` reported no dependency that is actually unused. Its "unused" entries are starters, Flyway and springdoc, which Spring Boot loads through auto-configuration at runtime.

## 6. Responsibility

The AI was used as an assistant, not as an authority on requirements or correctness. The developer made all business-rule interpretations, design decisions, reviews and commits; several AI proposals were changed or overruled (§4).
