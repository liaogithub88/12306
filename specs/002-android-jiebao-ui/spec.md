# Feature Specification: Android Jiebao UI Refactor

## Overview

- Problem: the current Android client still uses a project-defined layout and
  visual language instead of the `D:\code\project\12306-解包\jiebao` interface and
  asset style the user wants as the baseline experience.
- Target users: Android users who log in to 12306, query tickets, create and
  manage抢票任务, inspect task progress, and complete the existing payment-entry
  flow.
- Desired outcome: every Android surface that this project already supports
  adopts the interface structure, visual style, and reusable art assets from
  `D:\code\project\12306-解包\jiebao`; project-only capabilities without a direct
  counterpart use the closest `jiebao` style; `jiebao` features not implemented
  in this project are not shown.

## User Scenarios

- Primary scenario: a logged-in user opens the Android app and experiences a
  `jiebao`-style home journey from ticket search through result selection,
  task creation, task tracking, and payment entry without encountering screens
  that look unrelated to the reference app.
- Alternate scenarios: a logged-out user reaches a `jiebao`-style login/account
  entry and completes password or QR login; an existing user manages task list,
  task detail, and task editing screens that visually align with the reference
  app even when the exact workflow is project-specific.
- Failure scenarios: when initialization, login, query, task save, task action,
  or payment-entry steps fail, the app still shows readable failure feedback
  within the refactored UI and does not expose internal details or unsupported
  reference-only destinations.

## Requirements

- Functional requirements:
  - The Android client MUST treat
    `D:\code\project\12306-解包\jiebao` as the default visual reference for all
    currently implemented Android modules.
  - Every currently implemented Android user flow, including login, account
    view, ticket query, train selection, task creation, task editing, task
    list, task detail, task lifecycle actions, and payment entry, MUST remain
    available after the refactor.
  - If a currently implemented project feature has a matching or closely
    comparable screen pattern in `jiebao`, the refactored UI MUST use that
    layout structure and corresponding visual treatment.
  - If a currently implemented project feature has no direct `jiebao`
    equivalent, the refactored UI MUST use the closest `jiebao` navigation,
    card, form, button, dialog, and empty-state style rather than keeping a
    separate visual language.
  - If `jiebao` includes features or destinations that this project does not
    implement, those features MUST NOT be displayed as active modules, tabs,
    menu entries, shortcuts, banners, or calls to action in this project.
  - Navigation labels, tabs, and page grouping MUST reflect the subset of
    capabilities that actually exist in this project, even when the underlying
    styling comes from broader `jiebao` patterns.
  - Project screens that aggregate multiple actions today MUST be reorganized as
    needed so the visible information hierarchy matches `jiebao` conventions
    without removing project behavior.
  - Visual assets used by the refactored Android UI SHOULD come from
    `D:\code\project\12306-解包\jiebao` whenever an appropriate asset exists for
    the displayed feature.
  - When an exact reference asset does not exist for a project-only feature,
    the UI SHOULD reuse nearby `jiebao` assets, colors, typography rhythm,
    spacing, icon language, and container styling so the new screen still feels
    native to the same product family.
- Non-functional requirements:
  - The refactored Android UI MUST present a consistent `jiebao` visual system
    across home, task, account, login, detail, editor, and payment-entry
    surfaces.
  - The primary actions on all supported phone-sized screens MUST remain
    discoverable without clipped controls, overlapping content, or broken
    scrolling.
  - The UI refactor MUST preserve current functional behavior, backend
    contracts, and local task execution behavior.
  - The refactor SHOULD minimize mixed-style remnants so users do not encounter
    obvious switches between old project styling and `jiebao` styling during a
    normal end-to-end task flow.
- Data and privacy requirements:
  - The UI refactor MUST NOT introduce any new exposure of credentials, session
    data, passenger identity details, or raw diagnostic output.
  - Existing masked or privacy-limited displays MUST remain at least as
    restrictive after the visual replacement.
- Compatibility constraints:
  - Existing project capabilities, account/session flows, task data, and
    payment-entry behavior MUST remain compatible with current stored data and
    backend responses.
  - The refactor MUST stay within the Android surface and MUST NOT require the
    project to add missing `jiebao` business features solely for visual parity.

## Key Entities

- Supported Android module: any user-facing Android feature that already exists
  in this project and therefore must be preserved while receiving the new
  `jiebao`-based presentation.
- Reference `jiebao` pattern: a layout, navigation structure, component
  grouping, or art asset in
  `D:\code\project\12306-解包\jiebao` that can be reused directly or adapted for
  a supported Android module.
- Project-only feature: an Android behavior that exists in this project but has
  no one-to-one screen in `jiebao`, and therefore requires a closest-style
  adaptation rather than a direct copy.
- Unsupported reference feature: a `jiebao` module or destination that exists
  in the reference app but has no backing behavior in this project and must be
  omitted from the visible UI.

## Error Handling

- User-visible errors: login, query, task edit, task action, payment-entry, and
  initialization failures MUST continue to show concise, readable feedback in
  the refactored interface.
- Error codes or categories: the visual refactor MUST NOT require a new
  user-facing error taxonomy; existing actionable error categories remain valid.
- Logging and diagnostics: diagnostic detail SHOULD remain internal and must not
  expand visible logging solely because screens are being replaced.
- Sensitive data that must not be exposed: account credentials, session tokens,
  passenger full identity data beyond existing masked presentation, internal
  exceptions, stack traces, and raw third-party or bridge responses.

## Constitution Check

- Testing expectation: cover the highest-risk Android journeys after the
  refactor, especially login/account entry, home ticket query, result selection,
  task creation or editing, task detail and lifecycle actions, and payment
  entry.
- Error contract: verify that the new screens preserve readable user feedback
  and do not leak internal or sensitive details when existing operations fail.
- Maintainability: keep the refactor bounded to Android presentation and shared
  Android-facing assets; do not use this feature to perform unrelated backend,
  desktop, or Python behavior changes.
- Governance: the UI may borrow layout and art direction from the local
  `jiebao` reference, but only implemented project capabilities may be exposed
  to users.

## Success Criteria

- At least 95% of the screens a normal Android user traverses during login,
  ticket query, task creation, task management, and payment entry visibly use
  the `jiebao` visual language instead of the legacy project styling.
- In end-to-end smoke validation, users can complete the existing primary flow
  from app launch to task creation without encountering any entry point to a
  feature that the project does not actually support.
- Users can still complete all currently supported Android flows after the UI
  replacement with no increase in blocked actions caused by missing navigation
  or missing visible controls.
- Reviewers can map each visible Android module to either a direct `jiebao`
  reference pattern or a documented closest-style adaptation with no orphan
  legacy screen left in the main supported workflow.

## Acceptance Criteria

- Scenario acceptance:
  - Supported Android modules use `jiebao`-derived page structure and art
    assets wherever an appropriate reference exists.
  - Project-only modules that lack a direct `jiebao` equivalent still appear
    cohesive with the same visual family rather than retaining the old style.
  - Reference-only `jiebao` features that this project does not implement are
    hidden from primary navigation and visible entry points.
- Error-path acceptance:
  - Failed login, query, task, and payment-entry operations still surface
    readable messages.
  - No refactored screen reveals credentials, tokens, stack traces, or raw
    backend payloads.
- Test acceptance:
  - Android build and targeted UI validation confirm that supported screens
    render correctly after the refactor.
  - Smoke checks confirm that the main supported Android user flows remain
    operable under the new UI.
- Documentation acceptance:
  - This specification documents the rule that supported project features adopt
    `jiebao` layout and assets, unsupported reference features stay hidden, and
    unmatched project-only features follow the closest `jiebao` style.

## Assumptions

- The current Android feature boundary includes login/account, ticket query,
  train selection, task creation and editing, task list and detail, task
  lifecycle actions, and payment entry because those flows are already present
  in the project today.
- The local `D:\code\project\12306-解包\jiebao` directory is the authoritative
  source for reference UI patterns and art assets for this refactor.
- When multiple `jiebao` patterns could fit a project screen, the preferred
  choice is the one that keeps navigation and information density closest to
  the supported project workflow rather than exposing extra reference content.
