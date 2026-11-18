# Ikigai Time Deposit Kata - Claude Code Instructions

## Project context

This repository contains a Java take-home refactoring exercise.

The goal is to improve code quality, introduce the required REST API and persistence capabilities, and demonstrate sound engineering practices.

## Core principles

- Follow the README requirements as the source of truth.
- Read and understand existing implementation and tests before making changes.
- Preserve the observable behavior of `TimeDepositCalculator.updateBalance()` unless a requirement explicitly calls for a change.
- Never silently change interest calculation formulas, eligibility boundaries, or rounding behavior.
- Do not assume that existing tests are meaningful or sufficient.
- Prefer simple, maintainable solutions over unnecessary abstractions.

## Development workflow

1. Analyze requirements and existing code.
2. Identify missing behavior tests and edge cases.
3. Add characterization tests before refactoring.
4. Run the tests and establish a baseline.
5. Propose a refactoring plan and explain the trade-offs.
6. Implement one small, independently verifiable change at a time.
7. Run relevant tests after every change.
8. Review dependencies, remove dead code, and run the full suite.

## Architecture

- Prefer hexagonal architecture where it provides clear separation of concerns.
- Keep domain logic independent of frameworks, HTTP, and persistence.
- Separate input adapters, application use cases, domain logic, and output adapters.
- Use dependency inversion for infrastructure boundaries.
- Avoid interfaces or design patterns without a concrete benefit.
- Keep the project proportionate to the exercise.

## Java conventions

- Use Java 17 or newer.
- Prefer clear, descriptive class and method names.
- Always write Javadoc on every class and on every public method.
- Use JUnit 5 and AssertJ.
- Test observable outcomes rather than implementation details.
- Use BigDecimal for new monetary domain logic where appropriate, but preserve legacy calculation behavior.
- Avoid unrelated changes, unnecessary dependencies, and speculative features.

## AI collaboration

- Explain findings before suggesting major changes.
- Do not modify production code during the initial analysis phase.
- Ask for approval before significant architectural changes.
- Explain important design decisions and alternatives.
- Do not commit, push, or modify Git history without explicit approval.
- Document assumptions, AI assistance, and verification steps.

## Completion criteria

- Requirements are satisfied.
- Existing behavior is preserved where required.
- Relevant unit and integration tests pass.
- No unnecessary or dead code remains.
- Architecture and dependencies are reviewed.
- README and AI usage documentation are complete.
