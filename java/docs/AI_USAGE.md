# AI-Assisted Development

This document records how AI assistance was used in this take-home exercise so far. It covers the analysis, characterization tests and domain refactoring. The REST API, persistence and integration tests have **not** been implemented yet; they will be added here when they are.

## 1. Tools and Setup

| Item | Value |
|---|---|
| AI coding assistant | Claude Code CLI 2.1.327 (version at the time of writing) |
| Model | Claude Opus 5.5 (`claude-opus-5-5`) |
| IDE | Eclipse (formatting and manual review) |
| JDK / build | OpenJDK 21.0.8 running a Java 17 build (`maven.compiler.source/target = 17`), Apache Maven 3.9.9 |
| Test libraries | JUnit 5.9.0 (`junit-jupiter-engine`, `junit-jupiter-params`), AssertJ 3.23.1 |

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
- **Plan mode** was used for the design review (§3.4): the assistant wrote a findings report and proposal, and made changes only after explicit approval.

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

## 4. Decisions and Rejected Alternatives

| Decision | Rejected alternative | Reason |
|---|---|---|
| Keep `new BigDecimal(double)` | `BigDecimal.valueOf(double)` | Changes legacy rounding (e.g. 18.00 basic). |
| Keep `balance * rate / 12` and the `double` arithmetic | `balance * (rate / 12)`; `BigDecimal` throughout | The README requires unchanged behavior. Reordering *may* change results (not shown by a test). |
| Plan terms as data in the `PlanType` enum | `InterestPolicy` / Strategy classes (in the original plan); a string-keyed map | The plans differ only in data, not behavior. The enum prevents typing mistakes. |
| `PlanType.fromCode` returns `Optional` | `Enum.valueOf` | `valueOf` throws on unknown plans, which earned 0 before. |
| `TimeDeposit` keeps `String planType` and has no `equals`/`hashCode` | Change the field type; add `equals` | It's a shared class: no breaking changes, and no change to how deposits compare. |
| Code-first OpenAPI (springdoc) *(decided, not yet implemented)* | Contract-first YAML + generator | Developer decision. |
| Spring Data JPA *(decided, not yet implemented)* | `JdbcClient` with plain SQL (the assistant's original suggestion) | Developer decision. |
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
- **Committed characterization tests:** `git diff` confirmed they were never changed by the refactoring.

Not yet verified: the REST endpoints, persistence and integration tests (Testcontainers needs Docker, which was not running during the analysis).

## 6. Responsibility

The AI was used as an assistant, not as an authority on requirements or correctness. The developer made all business-rule interpretations, design decisions, reviews and commits; several AI proposals were changed or overruled (§4).
