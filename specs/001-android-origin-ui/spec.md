# Feature Specification: Android Origin 12306 UI

## Overview

- Problem: the Android client currently uses a generic Material layout that does
  not match the 12306-style experience expected for this project.
- Target users: Android users who log in, query train tickets, create local
  ticketing tasks, monitor task progress, and manage their account.
- Desired outcome: the Android app visually follows the local
  `origin_12306_src` 12306 interface for every feature this project actually
  supports, while unsupported origin-only modules are not displayed.

## User Scenarios

- Primary scenario: a logged-in user opens the app, sees an origin-style train
  ticket query home page, enters departure, arrival, and date, queries trains,
  selects trains, and creates a task without learning a new workflow.
- Alternate scenarios: a logged-out user sees an origin-style login/account
  surface; an existing user opens task list, task editor, or task detail and
  sees origin-style order/form presentation.
- Failure scenarios: initialization, login, query, task save, or task lifecycle
  failures continue to show readable messages without exposing internal details.

## Requirements

- Functional requirements:
  - The bottom navigation MUST contain only 首页, 任务, and 账号.
  - 首页 MUST include train-ticket query controls and query results; it MUST not
    require a separate 查票 tab.
  - Existing login, query, task creation, task editing, task list, task detail,
    start, stop, cancel, delete, logout, and payment-entry behavior MUST remain
    available where it exists today.
  - Modules present only in `origin_12306_src`, such as hotel, news, member,
    scan, notification center, and unrelated payment channels, MUST NOT be shown
    unless this project already implements the behavior.
- Non-functional requirements:
  - The Android UI MUST use origin-style colors, spacing, rounded surfaces,
    button treatments, title areas, ticket cards, and order-like list rows.
  - The implementation MUST preserve current local task execution and Python
    bridge behavior.
  - The UI MUST remain usable on normal phone screen sizes without clipped
    primary controls.
- Data and privacy requirements:
  - Passenger identity data MUST remain masked where currently masked.
  - Credentials, session data, stack traces, and raw bridge responses MUST NOT
    be added to visible UI or logs.
- Compatibility constraints:
  - Existing Android package, build configuration, backend API contracts, local
    database schema, and Python bridge command names MUST remain unchanged.

## Error Handling

- User-visible errors: initialization, login, query, passenger loading, task
  save, and task action failures continue to use concise snackbar or inline
  messages.
- Error codes or categories: no new public error-code contract is introduced by
  this visual refactor.
- Logging and diagnostics: no new sensitive logging is required.
- Sensitive data that must not be exposed: credentials, session cookies,
  passenger full ID numbers beyond existing masked displays, internal
  exceptions, stack traces, and raw third-party responses.

## Constitution Check

- Testing expectation: verify login/account, home query, train-result
  selection, task creation/editing, task list actions, task detail refresh, and
  payment-entry flow after the UI refactor.
- Error contract: errors remain structured through existing bridge result
  handling and readable in UI without leaking internal details.
- Maintainability: Android UI styling is extracted into reusable origin-style
  components and tokens; bridge/platform behavior remains separate from display
  composition.
- Governance: only local `origin_12306_src` assets referenced by the redesigned
  UI are used; unsupported origin-only content is hidden.

## Acceptance Criteria

- Scenario acceptance:
  - Logged-out users can reach login from 账号 and complete existing password or
    QR login flows.
  - Logged-in users can query tickets from 首页, select train results, and create
    tasks.
  - Users can view, filter, edit, start, pause, cancel, delete, and inspect
    tasks from 任务.
- Error-path acceptance:
  - Failed bridge calls still show readable messages.
  - No newly visible UI displays stack traces, credentials, session values, or
    full passenger identity numbers.
- Test acceptance:
  - Android debug build succeeds.
  - Manual or emulator smoke checks cover the primary screens.
- Documentation acceptance:
  - This specification and checklist describe the UI refactor scope and exclude
    unsupported origin-only modules.

## Assumptions

- Labels remain project-accurate: 任务 and 账号 are used instead of pretending the
  app supports all official 12306 order or my-page modules.
- Project-only controls without a direct origin counterpart use origin colors,
  spacing, shapes, and selected local origin assets as their fallback style.
