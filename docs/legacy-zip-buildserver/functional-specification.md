# Functional Specification: Zip-Based Build and Test Verification Service

## 1. Purpose

The service shall allow a user or an AI assistant workflow to verify an updated source-code package by uploading or referencing a zip archive, running predefined build and test checks in an isolated execution environment, and returning a concise, structured verification report.

The service is intended to support development workflows where a source repository is exchanged as a zip file, modified by an assistant, and then verified without requiring the user to run the build and tests locally.

The service shall not act as an autonomous coding agent. Its primary responsibility is verification, not source-code modification.

## 2. Scope

### 2.1 In Scope

The service shall support:

- Creating a verification session.
- Receiving or referencing a zip archive containing source code.
- Extracting and inspecting the archive.
- Detecting common project structures.
- Selecting a safe predefined verification plan.
- Running build and test commands in an isolated environment.
- Capturing command outcomes, failure summaries, and limited logs.
- Returning a structured verification result.
- Preserving full logs as retrievable artifacts subject to access control and retention rules.
- Enforcing execution limits and safety controls.
- Supporting iterative verification of updated zip files.

### 2.2 Out of Scope

The service shall not:

- Modify source code.
- Automatically fix test failures.
- Commit changes to version control.
- Deploy applications.
- Run arbitrary user-supplied shell commands without policy approval.
- Provide unrestricted network access to executed code.
- Store source packages indefinitely unless explicitly configured.
- Replace human review for security, licensing, or production readiness.

## 3. Actors

### 3.1 User

A person who wants to verify that an updated source-code package builds and passes tests.

The user may interact directly with the service or indirectly through a Custom GPT Action or similar assistant integration.

### 3.2 Assistant

An AI assistant that prepares or receives an updated zip file, requests verification, receives the verification summary, and uses the result to inform the user or prepare a corrected package.

The assistant must not be treated as trusted. All inputs from the assistant shall be validated by the service.

### 3.3 Verification Service

The system that manages verification sessions, zip uploads, job execution, result collection, and report generation.

### 3.4 Execution Worker

An isolated runtime environment that extracts the source package and runs approved build and test checks.

### 3.5 Administrator

A person responsible for configuring allowed verification plans, resource limits, retention policies, authentication, and operational monitoring.

## 4. Core Concepts

### 4.1 Verification Session

A verification session represents a logical verification context for one development task or package iteration.

A session may contain one or more verification runs.

A session shall have:

- A unique session identifier.
- Creation timestamp.
- Optional human-readable label.
- Optional source package metadata.
- Authentication and authorization context.
- Retention policy.
- Current status.

### 4.2 Verification Run

A verification run represents one attempt to verify one submitted package.

A run shall have:

- A unique run identifier.
- Associated session identifier.
- Input package reference.
- Selected verification plan.
- Execution status.
- Started and completed timestamps.
- Command-level results.
- Summary status.
- Links or references to retained artifacts.

### 4.3 Source Package

A source package is a zip archive containing one or more source-code projects.

The archive may contain:

- A single project at the root.
- Multiple projects in subdirectories.
- Documentation such as `AGENTS.md`, `README.md`, or files under `docs/`.
- Frontend and backend subprojects.
- Build files such as `package.json`, `pom.xml`, `build.gradle`, or similar.

The service shall treat package contents as untrusted.

### 4.4 Verification Plan

A verification plan defines which checks may be run for a detected project type.

A plan shall contain:

- Plan identifier.
- Supported project indicators.
- Ordered checks.
- Allowed commands or command templates.
- Working directory rules.
- Timeout rules.
- Network policy.
- Artifact collection rules.
- Log summarization rules.

A verification plan shall be controlled by service configuration or administrator policy, not by arbitrary instructions inside the uploaded package.

### 4.5 Verification Report

A verification report is the concise result returned to the user or assistant.

It shall include:

- Overall status.
- Commands executed.
- Project types detected.
- Passed checks.
- Failed checks.
- Failure summaries.
- Relevant file paths and error snippets.
- Whether the result is complete or partial.
- References to full logs or artifacts where available.

## 5. Functional Requirements

## 5.1 Session Management

### FR-001: Create Verification Session

The service shall allow an authorized caller to create a verification session.

The caller may provide:

- Optional label.
- Optional expected project type.
- Optional metadata about the development task.
- Optional retention preference within allowed limits.

The service shall return:

- Session identifier.
- Session status.
- Accepted retention policy.
- Available upload or package submission options.

### FR-002: Read Verification Session

The service shall allow an authorized caller to read session metadata and list associated verification runs.

### FR-003: Close Verification Session

The service shall allow an authorized caller to close a session.

Closing a session shall prevent new runs from being created under that session, but shall not necessarily delete existing retained results immediately.

## 5.2 Package Submission

### FR-004: Submit Zip Package

The service shall allow an authorized caller to submit a zip package for verification.

The submission mechanism may be direct upload, upload session reference, pre-signed upload reference, or another controlled package reference.

The service shall validate:

- Archive format.
- Maximum file size.
- Maximum number of files.
- Maximum extracted size.
- Maximum path length.
- File path safety.
- Absence of path traversal entries.
- Absence of disallowed archive structures.

### FR-005: Reject Unsafe Archives

The service shall reject archives that:

- Contain path traversal attempts.
- Exceed configured limits.
- Cannot be extracted safely.
- Contain unsupported compression or malformed entries.
- Attempt to overwrite files outside the extraction directory.

The verification run status shall be marked as rejected, and the report shall explain the rejection reason without exposing sensitive service internals.

### FR-006: Preserve Package Metadata

For each accepted package, the service shall record package metadata including:

- Original filename if available.
- Size.
- Checksum.
- Submission timestamp.
- Number of files.
- Extracted size.
- Top-level directories.
- Detected project indicators.

The service shall not expose sensitive metadata to unauthorized callers.

## 5.3 Project Detection

### FR-007: Detect Project Structure

The service shall inspect the extracted package and detect common project structures.

At minimum, the service should identify:

- Java/Maven projects indicated by `pom.xml`.
- Java/Gradle projects indicated by `build.gradle` or `build.gradle.kts`.
- Node/JavaScript/TypeScript projects indicated by `package.json`.
- Multi-project packages with subdirectories such as `frontend/`, `backend/`, `apps/`, `packages/`, or `services/`.
- Documentation-only packages when no buildable project is detected.

### FR-008: Report Detected Projects

The service shall include detected project information in the verification report.

For each detected project, the service should include:

- Project path.
- Detected technology family.
- Detected build indicators.
- Selected or skipped verification plan.
- Reason for selection or skipping.

### FR-009: Avoid Package-Controlled Command Selection

The service shall not allow uploaded package files to directly define arbitrary commands to be executed.

Files such as `AGENTS.md`, `README.md`, or documentation may be used as descriptive context, but not as authoritative command policy unless explicitly allowed by administrator configuration.

## 5.4 Verification Plan Selection

### FR-010: Select Verification Plan

The service shall select a verification plan based on:

- Detected project structure.
- Caller-provided expected project type, if provided.
- Administrator-defined policy.
- Safety constraints.
- Available execution environment capabilities.

### FR-011: Handle Ambiguous Projects

When multiple plausible verification plans exist, the service shall either:

- Run a safe default set of checks, or
- Mark the run as requiring explicit plan selection, depending on configuration.

The report shall clearly state any ambiguity.

### FR-012: Support Explicit Plan Request

The service may allow the caller to request a specific verification plan.

The service shall accept the requested plan only if:

- The caller is authorized to use it.
- The plan is configured and enabled.
- The package structure is compatible or the plan allows forced execution.
- Safety limits are satisfied.

## 5.5 Execution

### FR-013: Run Verification in Isolation

The service shall run verification jobs in an isolated execution environment.

The execution environment shall not have access to:

- Host filesystem outside the assigned workspace.
- Service secrets.
- Other users’ packages or logs.
- Administrative credentials.
- Unrestricted internal network resources.

### FR-014: Execute Approved Commands Only

The service shall execute only commands approved by the selected verification plan.

The assistant or user may not directly supply arbitrary shell commands for execution unless a separate administrator-approved manual-command mode is enabled.

### FR-015: Command-Level Tracking

For each executed command, the service shall record:

- Command label.
- Working directory.
- Start time.
- End time.
- Exit status.
- Timeout status.
- Captured stdout and stderr subject to log limits.
- Summary of detected failures.

### FR-016: Stop or Continue on Failure

A verification plan shall define whether execution stops after the first failed command or continues with independent checks.

The report shall state which commands were skipped due to earlier failures.

### FR-017: Enforce Time Limits

The service shall enforce:

- Maximum total run duration.
- Maximum duration per command.
- Maximum idle time without output.
- Maximum queue wait time, if applicable.

When a timeout occurs, the service shall terminate the affected command or run and mark the result accordingly.

### FR-018: Enforce Resource Limits

The service shall enforce limits for:

- CPU.
- Memory.
- Disk usage.
- Number of processes.
- Output size.
- Network usage, when measurable.

When a limit is exceeded, the service shall stop the run and return a controlled failure report.

## 5.6 Dependency Handling

### FR-019: Dependency Installation

A verification plan may include dependency installation steps such as package restoration, build tool preparation, or test dependency resolution.

The service shall distinguish dependency installation failures from source-code test failures.

### FR-020: Network Policy

The service shall apply a configurable network policy.

Supported modes should include:

- No network access.
- Restricted dependency-fetching access.
- Full outbound access for trusted environments only.

The report shall state which network mode was used.

### FR-021: Cache Policy

The service may use dependency caches to speed up verification.

Caches shall not contain user secrets.

Cache reuse shall be isolated or sanitized to prevent cross-project data leakage.

## 5.7 Log Capture and Summarization

### FR-022: Capture Logs

The service shall capture stdout and stderr for each command subject to configured limits.

### FR-023: Limit Logs Returned to Assistant

The service shall return only concise log excerpts in the default verification report.

The default report should include:

- The most relevant failure lines.
- File paths.
- Test names.
- Error messages.
- Stack trace excerpts where useful.
- Truncation indicators when logs were shortened.

### FR-024: Store Full Logs Separately

The service may store full logs as artifacts.

Full logs shall be retrievable only by authorized callers and within the configured retention period.

### FR-025: Identify Failure Signals

The service should attempt to identify common failure signals, such as:

- Failed test names.
- Compilation errors.
- Type-checking errors.
- Missing dependencies.
- Build configuration errors.
- Lint failures.
- Timeout failures.
- Out-of-memory failures.
- Unsupported project structure.

The service shall not claim certainty when failure parsing is incomplete.

## 5.8 Result Status

### FR-026: Overall Run Status

Each verification run shall have one of the following overall statuses:

- `queued`
- `running`
- `passed`
- `failed`
- `rejected`
- `timed_out`
- `cancelled`
- `incomplete`
- `internal_error`

### FR-027: Check-Level Status

Each check shall have one of the following statuses:

- `passed`
- `failed`
- `skipped`
- `timed_out`
- `cancelled`
- `not_applicable`
- `internal_error`

### FR-028: Structured Result

The service shall return structured results suitable for an AI assistant to consume without needing the full raw logs.

The structured result shall include:

- Overall status.
- Human-readable summary.
- Project detection summary.
- Commands run.
- Failed checks.
- Failure details.
- Suggested focus areas.
- Artifact references.
- Safety or completeness warnings.

### FR-029: Partial Results

If the run is interrupted, times out, or fails internally after some commands completed, the service shall return partial results where available.

The report shall clearly mark the result as partial.

## 5.9 Artifact Management

### FR-030: Store Verification Artifacts

The service may store artifacts such as:

- Full logs.
- Test reports.
- Build reports.
- Coverage reports.
- Extracted metadata.
- Machine-readable result files.

### FR-031: Artifact Access

The service shall restrict artifact access to authorized callers.

Artifact references returned to the assistant should be opaque identifiers, not raw filesystem paths.

### FR-032: Retention and Deletion

The service shall enforce retention policies for:

- Uploaded packages.
- Extracted workspaces.
- Logs.
- Reports.
- Derived artifacts.

The service shall support deletion according to policy or explicit authorized request.

## 5.10 Cancellation

### FR-033: Cancel Verification Run

The service shall allow an authorized caller to cancel a queued or running verification run.

Cancellation shall attempt to stop active commands and release resources.

The final status shall indicate whether cancellation succeeded.

## 5.11 Assistant Integration

### FR-034: Assistant-Friendly Summary

The service shall provide a concise summary optimized for assistant consumption.

The summary shall avoid excessive log volume and should identify likely next files or areas to inspect.

Example fields:

- `summary`
- `status`
- `primaryFailure`
- `failedFiles`
- `failedTests`
- `commandsRun`
- `suggestedFocus`
- `fullLogReference`

### FR-035: Avoid Prompt Injection from Source Package

The service shall not treat any source-package content as instructions for the service itself.

The service may expose selected source metadata to the assistant, but shall not include arbitrary package text in the result unless it is directly relevant to a failure.

### FR-036: Stable Machine-Readable Contract

The service shall expose stable machine-readable response structures so an assistant action can reliably interpret results.

Human-readable text may be included, but shall not be the only representation of the result.

## 6. User Workflows

## 6.1 Basic Verification Workflow

1. The user obtains an updated source-code zip.
2. The user or assistant creates a verification session.
3. The zip package is submitted.
4. The service validates and extracts the package.
5. The service detects project structure.
6. The service selects a verification plan.
7. The service runs approved checks in isolation.
8. The service returns a structured verification report.
9. The assistant or user reviews the result.

## 6.2 Iterative Assistant Workflow

1. The user asks an assistant to implement a feature in an uploaded zip.
2. The assistant produces an updated zip.
3. The assistant requests verification through the service.
4. The service returns a concise failure or success report.
5. If failures are reported, the assistant uses the report to prepare another updated zip.
6. The service verifies the new package.
7. The final response includes the updated zip and verification report.

The service shall support this workflow without requiring the service itself to modify code.

## 6.3 Failed Build Workflow

1. The service runs the selected build command.
2. The build fails.
3. The service extracts relevant error information.
4. The report identifies the failed command, relevant files, and failure excerpts.
5. Later test commands may be skipped depending on the verification plan.
6. The result status is `failed`.

## 6.4 Unsupported Project Workflow

1. The service receives a valid zip archive.
2. No supported project structure is detected.
3. The service does not attempt arbitrary command execution.
4. The result status is `incomplete` or `failed` depending on policy.
5. The report explains that no supported verification plan was found.

## 7. Validation Rules

## 7.1 Archive Validation

The service shall validate that:

- The submitted file is a supported archive format.
- The archive can be safely extracted.
- File paths are relative and safe.
- Symlinks, hardlinks, or special files are handled according to policy.
- Configured size and count limits are not exceeded.

## 7.2 Request Validation

The service shall validate that:

- The caller is authenticated if authentication is required.
- The caller is authorized for the session and requested operation.
- Requested plan identifiers are valid.
- Submitted metadata sizes are within limits.
- Required fields are present.

## 7.3 Execution Validation

The service shall validate that:

- The selected plan is enabled.
- Required execution environment capabilities are available.
- The job has sufficient quota.
- Commands are part of the approved plan.
- The workspace is isolated and prepared.

## 8. Security Requirements

## 8.1 Treat Uploaded Code as Untrusted

All uploaded packages shall be treated as malicious until proven otherwise.

The service shall assume that package scripts, tests, and build files may attempt to:

- Read secrets.
- Access local files.
- Use excessive resources.
- Connect to unauthorized network destinations.
- Modify the execution environment.
- Hide failure output.
- Exfiltrate data.

## 8.2 Isolation

The execution worker shall run in an isolated environment with controlled filesystem, process, network, and resource boundaries.

## 8.3 Secret Protection

The execution environment shall not expose service secrets, API keys, user credentials, or administrative tokens to package code.

## 8.4 Least Privilege

All internal components shall operate with the minimum permissions required for their responsibilities.

## 8.5 Controlled Network Access

Outbound network access shall be disabled or restricted unless required by the selected verification plan.

When network access is allowed, the policy shall be explicit and auditable.

## 8.6 Logging Safety

Logs may contain sensitive information accidentally printed by package code.

The service shall:

- Avoid exposing logs to unauthorized users.
- Support log retention limits.
- Allow log deletion according to policy.
- Avoid sending excessive logs to assistants by default.

## 8.7 Command Safety

The service shall not execute commands directly supplied by uploaded package documentation or assistant-generated text unless an explicit administrator-controlled policy allows it.

## 9. Privacy Requirements

The service shall handle uploaded source code as confidential user content.

The service shall:

- Minimize retention.
- Avoid unnecessary copying.
- Restrict access to packages and logs.
- Provide deletion according to policy.
- Avoid using uploaded code for unrelated purposes.
- Clearly document retention behavior.

## 10. Non-Functional Requirements

## 10.1 Reliability

The service should return a controlled result even when verification fails, times out, or encounters unsupported project structures.

## 10.2 Performance

The service should provide timely feedback for common small and medium-sized projects.

The service shall expose run status for longer-running jobs.

## 10.3 Scalability

The service should support multiple concurrent verification sessions subject to configured quotas.

## 10.4 Observability

The service shall provide operational visibility into:

- Job queue length.
- Run duration.
- Failure rates.
- Timeout rates.
- Resource usage.
- Worker health.
- Storage usage.

## 10.5 Maintainability

Verification plans shall be configurable and versioned.

Changes to plans should be traceable.

## 10.6 Portability

The functional behavior shall not depend on a specific implementation technology.

The service may be implemented using different runtime, container, queue, or storage technologies as long as the functional requirements are met.

## 11. Integration Requirements

## 11.1 API-Oriented Access

The service shall expose operations suitable for assistant integration, such as:

- Create session.
- Submit package or package reference.
- Start verification.
- Read run status.
- Read structured summary.
- Read selected log excerpts.
- Cancel run.
- Delete retained artifacts.

## 11.2 Custom GPT Action Compatibility

The service should provide a stable API contract that can be described through an OpenAPI schema.

Responses should be compact enough for assistant consumption.

## 11.3 Human Access

The service may provide a human-facing interface for:

- Uploading packages.
- Starting verification.
- Viewing status.
- Downloading logs.
- Deleting artifacts.

A human interface is optional if API access is sufficient.

## 12. Reporting Requirements

## 12.1 Default Verification Report

The default report shall include:

- Overall status.
- Session identifier.
- Run identifier.
- Package metadata summary.
- Project detection summary.
- Verification plan used.
- Commands executed.
- Status per command.
- Failure summary.
- Relevant log excerpts.
- Artifact references.
- Retention information.
- Warnings or limitations.

## 12.2 Failure Detail

For each failure, the report should include:

- Failure category.
- Command that failed.
- File path if available.
- Test name if available.
- Error message.
- Short excerpt.
- Suggested focus area.
- Whether the failure parser is confident.

## 12.3 Success Detail

For successful runs, the report should include:

- Commands passed.
- Duration.
- Project types verified.
- Any skipped optional checks.
- Any warnings.

## 13. Error Handling

## 13.1 User-Correctable Errors

The service shall provide clear messages for user-correctable issues such as:

- Invalid archive.
- Archive too large.
- Unsupported project structure.
- Missing required build files.
- Dependency resolution failure.
- Requested plan not available.

## 13.2 Service Errors

The service shall distinguish internal service errors from package verification failures.

Internal service errors shall not be presented as source-code test failures.

## 13.3 Retry Behavior

The service may allow retries for:

- Infrastructure failures.
- Worker startup failures.
- Temporary dependency service failures.
- Queue interruptions.

Retries shall not hide persistent package failures.

## 14. Configuration Requirements

Administrators shall be able to configure:

- Allowed verification plans.
- Maximum archive size.
- Maximum extracted size.
- Maximum file count.
- Maximum run duration.
- Maximum command duration.
- CPU and memory limits.
- Network policy.
- Artifact retention period.
- Log truncation limits.
- Concurrent run limits.
- Authentication policy.
- Allowed users or clients.
- Plan availability per user or client.

## 15. Acceptance Criteria

The service shall be considered functionally acceptable when:

1. A valid zip containing a supported project can be submitted and verified.
2. A passing project returns an overall `passed` status with command-level results.
3. A failing project returns an overall `failed` status with concise, relevant failure details.
4. An invalid or unsafe archive is rejected without extraction outside the workspace.
5. Unsupported project structures do not cause arbitrary command execution.
6. Long-running or stuck commands are timed out.
7. Resource limits are enforced.
8. Full logs are not returned by default to the assistant.
9. Full logs can be retrieved by authorized callers when retained.
10. Uploaded packages and logs are deleted according to retention policy.
11. Package contents cannot access service secrets.
12. Verification plans are controlled by service configuration, not by uploaded package instructions.
13. Multiple verification runs can be associated with one session.
14. A structured machine-readable summary is available for assistant integration.
15. Internal service errors are distinguishable from source-code build or test failures.

## 16. Assumptions

- The user or assistant can provide a zip package or a reference to a previously uploaded package.
- The service is intended primarily for development-time verification, not production deployment.
- Verification commands are predefined by service policy.
- Uploaded packages may contain untrusted code.
- Some projects may require network access to download dependencies.
- The assistant consuming the report benefits from concise structured output rather than full logs.
- The service may be used with Custom GPT Actions, but the functional requirements do not depend on a specific AI platform.

## 17. Open Questions

1. Should the first version support direct zip upload through the assistant action, or should it use a separate upload URL/session flow?
2. Which project types should be supported in the initial release?
3. Should network access be disabled by default or restricted to dependency registries?
4. What maximum package size should be allowed?
5. How long should full logs and uploaded packages be retained?
6. Should users be able to define custom verification plans, or only administrators?
7. Should the service support private dependency registries?
8. Should the service expose a human web interface or API-only access?
9. Should verification runs be synchronous for small projects or always asynchronous?
10. Should the service support comparing verification results across package iterations?
