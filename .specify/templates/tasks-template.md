# Tasks: <feature name>

## Task Format

Use short, verifiable tasks with an owner surface and a clear completion check.
Mark tasks that can run in parallel with `P`.

## Setup

- T001 Confirm the Constitution Check from the plan is complete.
- T002 Identify existing tests, build commands, and smoke checks for affected
  surfaces.

## Implementation

- T003 Implement the smallest boundary-respecting change for the feature.
- T004 Keep backend API/router, service, schema/model responsibilities
  separated.
- T005 Keep frontend api, store, and view responsibilities separated.
- T006 Keep Android bridge or desktop integration isolated from shared
  business behavior when those surfaces are touched.

## Testing and Error Handling

- T007 Add or update risk-driven automated tests for core behavior, error
  paths, external 12306 interactions, and regressions.
- T008 Mock or isolate external 12306/network behavior in tests.
- T009 Verify user-visible errors are structured and do not expose internal
  exceptions, stack traces, credentials, session data, or raw third-party
  responses.
- T010 Run the relevant automated tests, build checks, or documented smoke
  checks.

## Review

- T011 Confirm no unrelated refactors were included.
- T012 Confirm legal-use expectations, credential minimization, and sensitive
  data logging constraints are preserved.
- T013 Update documentation when behavior, setup, or governance expectations
  change.
