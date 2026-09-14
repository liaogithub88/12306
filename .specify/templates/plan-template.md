# Implementation Plan: <feature name>

## Summary

- Goal:
- User outcome:
- In scope:
- Out of scope:

## Technical Approach

- Backend changes:
- Frontend changes:
- Android or desktop changes:
- Data or configuration changes:

## Constitution Check

- Risk-driven tests: list the core behavior, error paths, external 12306
  interactions, and regressions that require automated tests.
- Error handling: describe the structured user-visible errors and confirm that
  internal exceptions, stack traces, credentials, session data, and raw
  third-party responses will not be exposed.
- Maintainability: confirm API/router, service, schema/model, frontend
  api/store/view, and Android bridge boundaries remain clear, or justify any
  boundary change.
- Governance and sensitive data: confirm legal-use expectations, credential
  minimization, and sensitive-data logging constraints are preserved.

## Test Plan

- Automated tests:
- Manual checks:
- Existing smoke checks:
- Deferred tests and rationale:

## Risks and Rollback

- Main risks:
- Mitigations:
- Rollback or disablement:
