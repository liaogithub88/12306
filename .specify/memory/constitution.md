<!--
Sync Impact Report
Version change: none -> 1.0.0
Modified principles: None
Added principles:
- I. Risk-Driven Test Coverage
- II. Structured, Non-Leaking Error Handling
- III. Progressive Maintainability
Added sections:
- Governance
Removed sections: None
Templates requiring updates:
- .specify/templates/plan-template.md: updated
- .specify/templates/spec-template.md: updated
- .specify/templates/tasks-template.md: updated
Follow-up TODOs: None
-->

# 12306 Ticket Helper Constitution

## Core Principles

### I. Risk-Driven Test Coverage

Changes to core business behavior, authentication, task scheduling, external
12306 interactions, error paths, or regression fixes MUST include automated
tests that cover the relevant success and failure paths. External network
behavior MUST be mocked or isolated in tests. Coverage is judged by risk and
behavioral confidence, not by a fixed percentage threshold.

Rationale: the project currently has limited automated coverage, so the
enforceable baseline must protect high-risk behavior without creating a fake
coverage target.

### II. Structured, Non-Leaking Error Handling

User-visible failures MUST use a structured response shape with a readable
message and a stable error code when the caller can act on it. Internal
exceptions, stack traces, credentials, session data, and raw third-party
responses MUST NOT be returned to clients. Internal details SHOULD be recorded
in logs with enough context to diagnose the failure.

Rationale: the app handles account sessions and ticketing workflows, so errors
must help users recover while keeping implementation details and sensitive data
out of public responses.

### III. Progressive Maintainability

Changes MUST preserve the existing module boundaries unless the implementation
plan explicitly justifies a boundary change. Backend routes SHOULD stay thin
and delegate business behavior to services; schemas and models SHOULD remain
separate. Frontend API clients, stores, and views SHOULD keep separate
responsibilities. Android bridge code SHOULD isolate platform integration from
shared Python behavior. A single feature or fix MUST NOT include unrelated
large refactors.

Rationale: this repository spans backend, frontend, desktop, and Android
surfaces, so small boundary-respecting changes are safer than broad rewrites.

## Governance

This constitution is the source of truth for project engineering principles.
Every feature specification, implementation plan, and task list MUST include a
Constitution Check covering testing, error handling, maintainability, and
governance constraints.

Compliance review MUST happen before implementation starts and again before a
change is considered complete. If a principle cannot be satisfied immediately,
the plan MUST document the reason, the risk, and the follow-up task.

The project handles 12306 account sessions, passenger data, and ticketing
automation. Plans and implementations MUST preserve legal use expectations,
minimize credential exposure, and avoid storing or logging sensitive data beyond
what the feature requires.

Amendments MUST update this file, the Sync Impact Report, the version, and any
affected Spec Kit templates in the same change. Version changes follow semantic
versioning: MAJOR for incompatible governance or principle redefinitions, MINOR
for new or materially expanded principles or sections, and PATCH for wording
clarifications that do not change meaning.

**Version**: 1.0.0 | **Ratified**: 2026-06-12 | **Last Amended**: 2026-06-12
