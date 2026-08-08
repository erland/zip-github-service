# Implementation status — zip-github

Version 1.0  
Updated: 8 August 2026

## Purpose

This file is the authoritative execution ledger for the prompt-by-prompt implementation. The command **“kör nästa steg”** always means: implement the single row whose status is `NEXT`, then update this file and package a new ZIP.

## Invariants

- Exactly one step is `NEXT` in every normal delivered revision.
- A step can become `DONE` only after its required deliverables and verification are documented.
- Completed steps are never silently reopened; a correction is recorded in the affected step report and revision history.
- Every delivered ZIP records its revision, completed step, next step, changed files and verification result.
- Authorization and ownership isolation are mandatory cross-cutting requirements for all user-owned resources.

## Current position

- Repository revision: `r0100`
- Last completed step: `9.6`
- Next step: blocked at `9.7`
- Overall state: `MVP RELEASE CANDIDATE — PHASE 9 BLOCKED ON SIGNED SHORTCUT ARTIFACT`

## Step ledger

| Step | Phase | Description | Status | Completed | Evidence |
|---|---|---|---|---|---|
| `0.1` | Fas 0 — inventering och verifierbar baslinje | Packa upp och inventera legacyprojektet | **DONE** | 2026-08-06 | `docs/legacy-inventory.md` |
| `0.2` | Fas 0 — inventering och verifierbar baslinje | Bygg och testa legacybaslinjen | **DONE** | 2026-08-06 | `docs/baseline-verification.md`, `docs/step-0.2-report.md` |
| `0.3` | Fas 0 — inventering och verifierbar baslinje | Skapa återanvändnings- och migreringskarta | **DONE** | 2026-08-06 | `docs/reuse-assessment.md`, `docs/step-0.3-report.md` |
| `0.4` | Fas 0 — inventering och verifierbar baslinje | Skapa ren zip-github-bas | **DONE** | 2026-08-06 | `docs/step-0.4-report.md` |
| `1.1` | Fas 1 — ny domän och applikationsskal | Definiera domänmodell och statusmaskiner | **DONE** | 2026-08-06 | `docs/domain-model.md`, `docs/step-1.1-report.md` |
| `1.2` | Fas 1 — ny domän och applikationsskal | Skapa databasmodell och Flyway-migreringar | **DONE** | 2026-08-06 | `docs/database-model.md`, `docs/step-1.2-report.md` |
| `1.3` | Fas 1 — ny domän och applikationsskal | Skapa API-skelett och felkontrakt | **DONE** | 2026-08-06 | `docs/api-contract.md`, `docs/step-1.3-report.md` |
| `1.4` | Fas 1 — ny domän och applikationsskal | Skapa frontendskal och routing | **DONE** | 2026-08-06 | `docs/step-1.4-report.md` |
| `2.1` | Fas 2 — GitHub-login och GitHub App | Genomför GitHub-teknikspike | **DONE** | 2026-08-06 | `docs/github-technical-spike.md`, `docs/step-2.1-report.md` |
| `2.2` | Fas 2 — GitHub-login och GitHub App | Implementera GitHub-login och webbsession | **DONE** | 2026-08-06 | `docs/authentication-and-sessions.md`, `docs/step-2.2-report.md` |
| `2.3` | Fas 2 — GitHub-login och GitHub App | Implementera GitHub App-installationer och repositorylista | **DONE** | 2026-08-06 | `docs/github-app-access.md`, `docs/step-2.3-report.md` |
| `2.4` | Fas 2 — GitHub-login och GitHub App | Koppla projektkonfiguration till GitHub | **DONE** | 2026-08-06 | `docs/github-project-configuration.md`, `docs/step-2.4-report.md` |
| `3.1` | Fas 3 — uppladdning och säker ZIP-inspektion | Implementera streaminguppladdning och metadata | **DONE** | 2026-08-06 | `docs/upload-streaming.md`, `docs/step-3.1-report.md` |
| `3.2` | Fas 3 — uppladdning och säker ZIP-inspektion | Implementera path- och filtypssäkerhet | **DONE** | 2026-08-06 | `docs/archive-path-and-file-security.md`, `docs/step-3.2-report.md` |
| `3.3` | Fas 3 — uppladdning och säker ZIP-inspektion | Implementera resursgränser och ZIP-bombskydd | **DONE** | 2026-08-06 | `docs/archive-resource-limits.md`, `docs/step-3.3-report.md` |
| `3.4` | Fas 3 — uppladdning och säker ZIP-inspektion | Implementera normalisering och filinventering | **DONE** | 2026-08-06 | `docs/step-3.4-report.md` |
| `3.5` | Fas 3 — uppladdning och säker ZIP-inspektion | Implementera retention och mobil uppladdningsvy | **DONE** | 2026-08-06 | `docs/upload-retention-and-mobile-ui.md`, `docs/step-3.5-report.md` |
| `3.6` | Fas 3 — uppladdning och säker ZIP-inspektion | Etablera komplett CI-baslinje | **DONE** | 2026-08-06 | `docs/ci-baseline.md`, `docs/step-3.6-report.md` |
| `4.1` | Fas 4 — repositorysnapshot och importplan | Välj och implementera repositorysnapshot | **DONE** | 2026-08-06 | `docs/repository-snapshot.md`, `docs/step-4.1-report.md` |
| `4.2` | Fas 4 — repositorysnapshot och importplan | Implementera hashbaserad jämförelse | **DONE** | 2026-08-06 | `docs/hash-based-comparison.md`, `docs/step-4.2-report.md` |
| `4.3` | Fas 4 — repositorysnapshot och importplan | Implementera importpolicy och blockerare | **DONE** | 2026-08-06 | `docs/import-policy.md`, `docs/step-4.3-report.md` |
| `4.4` | Fas 4 — repositorysnapshot och importplan | Spara immutable importplan | **DONE** | 2026-08-06 | `docs/immutable-import-plan.md`, `docs/step-4.4-report.md` |
| `4.5` | Fas 4 — repositorysnapshot och importplan | Bygg granskningsvyn | **DONE** | 2026-08-06 | `docs/import-review-view.md`, `docs/step-4.5-report.md` |
| `5.1` | Fas 5 — godkännande och Git-leverans | Implementera godkännande av exakt plan | **DONE** | 2026-08-06 | `docs/exact-plan-approval.md`, `docs/step-5.1-report.md` |
| `5.2` | Fas 5 — godkännande och Git-leverans | Implementera temporär Git-arbetsyta och filapplicering | **DONE** | 2026-08-06 | `docs/temporary-git-workspace.md`, `docs/step-5.2-report.md` |
| `5.3` | Fas 5 — godkännande och Git-leverans | Implementera branch, atomisk commit och push | **DONE** | 2026-08-06 | `docs/branch-commit-and-push.md`, `docs/step-5.3-report.md` |
| `5.4` | Fas 5 — godkännande och Git-leverans | Implementera pull request och resultatmetadata | **DONE** | 2026-08-06 | `docs/pull-request-and-result-metadata.md`, `docs/step-5.4-report.md` |
| `5.5` | Fas 5 — godkännande och Git-leverans | Implementera idempotens, retry och felåterhämtning | **DONE** | 2026-08-06 | `docs/idempotency-retry-and-recovery.md`, `docs/step-5.5-report.md` |
| `6.1` | Fas 6 — Actions-länkar och resultatsida | Bygg resultatsidan med beständiga GitHub-länkar | **DONE** | 2026-08-06 | `docs/import-result-page.md`, `docs/step-6.1-report.md` |
| `6.2` | Fas 6 — Actions-länkar och resultatsida | Lägg grundläggande checkstatus | **DONE** | 2026-08-06 | `docs/basic-check-status.md`, `docs/step-6.2-report.md` |
| `6.3` | Fas 6 — Actions-länkar och resultatsida | Lägg importhistorik och återöppning | **DONE** | 2026-08-06 | `docs/import-history-and-reopening.md`, `docs/step-6.3-report.md` |
| `7.1` | Fas 7 — mobil, säkerhet och driftsättning | Genomför komplett mobil- och tillgänglighetsgenomgång | **DONE** | 2026-08-06 | `docs/mobile-and-accessibility-review.md`, `docs/step-7.1-report.md` |
| `7.2` | Fas 7 — mobil, säkerhet och driftsättning | Härda webb- och API-säkerheten | **DONE** | 2026-08-06 | `docs/web-and-api-security-hardening.md`, `docs/step-7.2-report.md` |
| `7.3` | Fas 7 — mobil, säkerhet och driftsättning | Skapa driftmodell och operationsdokumentation | **DONE** | 2026-08-06 | `docs/operations.md`, `docs/github-app-setup.md`, `docs/step-7.3-report.md` |
| `7.4` | Fas 7 — mobil, säkerhet och driftsättning | Genomför hotmodell och slutlig säkerhetsregression | **DONE** | 2026-08-06 | `docs/threat-model.md`, `docs/security-regression.md`, `docs/step-7.4-report.md` |
| `7.5` | Fas 7 — mobil, säkerhet och driftsättning | MVP-release och Definition of Done | **DONE** | 2026-08-06 | `docs/mvp-release.md`, `docs/release-checklist.md`, `docs/step-7.5-report.md` |
| `7.6` | Fas 7 — flexibel granskning | Inför blockerarnivåer och icke-fatala policyblockeringar | **DONE** | 2026-08-07 | `docs/import-policy.md`, `docs/step-7.6-report.md` |
| `7.7` | Fas 7 — flexibel granskning | Skapa immutable selection-modell och API | **DONE** | 2026-08-07 | `docs/step-7.7-report.md`, `docs/api-contract.md` |
| `7.8` | Fas 7 — flexibel granskning | Bygg hierarkiskt fil- och katalogurval i granskningsvyn | **DONE** | 2026-08-07 | `docs/step-7.8-report.md` |
| `7.9` | Fas 7 — flexibel granskning | Implementera explicita overrides och exakt selected delivery | **DONE** | 2026-08-07 | `docs/step-7.9-report.md` |
| `7.10` | Fas 7 — flexibel granskning | Genomför selection-, override- och säkerhetsregression | **DONE** | 2026-08-07 | `docs/step-7.10-report.md`, `docs/threat-model.md`, `docs/release-checklist.md` |
| `7.11` | Fas 7 — importkärna | Generalisera ZIP-ingestion och lagring | **DONE** | 2026-08-07 | `docs/upload-streaming.md`, `docs/step-7.11-report.md` |
| `7.12` | Fas 7 — importkärna | Skapa vanlig Import från redan lagrad ZIP | **DONE** | 2026-08-07 | `docs/upload-streaming.md`, `docs/step-7.12-report.md` |
| `7.13` | Fas 7 — importkärna | Formalisera importkälla och auditmetadata | **DONE** | 2026-08-07 | `docs/step-7.13-report.md`, `docs/domain-model.md`, `docs/database-model.md` |
| `7.14` | Fas 7 — importkärna | Regression för alternativ ZIP-ingestion | **DONE** | 2026-08-07 | `docs/step-7.14-report.md` |
| `7.15` | Fas 7 — policy/UX | Korrigera policy för oförändrade skyddade sökvägar | **DONE** | 2026-08-07 | `docs/step-7.15-report.md`, `docs/import-policy.md` |
| `7.16` | Fas 7 — policy/UX | Automatisera upload till granskningsplan | **DONE** | 2026-08-07 | `docs/step-7.16-report.md`, `docs/api-contract.md` |
| `7.17` | Fas 7 — policy/UX | Gör godkännande och commit till en användaråtgärd | **DONE** | 2026-08-07 | `docs/step-7.17-report.md`, `docs/api-contract.md` |
| `7.18` | Fas 7 — policy/UX | E2E-regression för det förenklade importflödet | **DONE** | 2026-08-07 | `docs/step-7.18-report.md`, `frontend/src/pages/SimplifiedImportFlow.test.tsx` |
| `7.19` | Fas 7 — resume/Work UX | Gör pågående import fullt återupptagningsbar | **DONE** | 2026-08-07 | `docs/step-7.19-report.md` |
| `7.20` | Fas 7 — resume/Work UX | Förenkla Work-vyn till Git-historik och pågående import | **DONE** | 2026-08-07 | `docs/step-7.20-report.md` |
| `7.21` | Fas 7 — resume/Work UX | Slutregression för resume och Work-vy | **DONE** | 2026-08-07 | `docs/step-7.21-report.md` |
| `7.22` | Fas 7 — importlivscykel | Avbryt och stäng pågående import | **DONE** | 2026-08-07 | `docs/step-7.22-report.md`, `docs/api-contract.md` |
| `7.23` | Fas 7 — Work UX | State-baserade Work-actions och borttagna redundanta vägar | **DONE** | 2026-08-07 | `docs/step-7.23-report.md` |
| `7.24` | Fas 7 — regression | Regression för cancel och state-baserade Work-actions | **DONE** | 2026-08-07 | `docs/step-7.24-report.md` |
| `8.1` | Fas 8 — efter MVP: integrerade Actions-resultat | Workflow runs och jobs | **DONE** | 2026-08-07 | `docs/workflow-runs-and-jobs.md`, `docs/step-8.1-report.md` |
| `8.2` | Fas 8 — efter MVP: integrerade Actions-resultat | Artifacts och kondenserade fel | **DONE** | 2026-08-07 | `docs/actions-artifacts-and-condensed-errors.md`, `docs/step-8.2-report.md` |
| `8.3` | Fas 8 — efter MVP: integrerade Actions-resultat | Kontrollerad workflow dispatch och omkörning | **DONE** | 2026-08-07 | `docs/controlled-workflow-actions.md`, `docs/step-8.3-report.md` |
| `8.4` | Fas 8 — tidigare plan | AI- och integrationsyta — flyttad till framtida backlog | **SKIPPED** | 2026-08-07 | `docs/r0084-phase8-9-handoff-planning.md` |
| `9.1` | Fas 9 — Shortcut/StagingImport | Definiera och persistiera StagingImport-livscykeln | **DONE** | 2026-08-08 | `docs/staging-import-lifecycle.md`, `docs/step-9.1-report.md` |
| `9.2` | Fas 9 — Shortcut/StagingImport | Capability-skyddad staging-upload | **DONE** | 2026-08-08 | `docs/staging-upload.md`, `docs/step-9.2-report.md` |
| `9.3` | Fas 9 — Shortcut/StagingImport | Autentiserad claim från webbläsaren | **DONE** | 2026-08-08 | `docs/staging-claim.md`, `docs/step-9.3-report.md` |
| `9.4` | Fas 9 — Shortcut/StagingImport | Projektval och promotion till vanlig Import | **DONE** | 2026-08-08 | `docs/staging-promotion.md`, `docs/step-9.4-report.md` |
| `9.5` | Fas 9 — gemensam commit UX | Användarstyrt commitmeddelande i approval/delivery | **DONE** | 2026-08-08 | `docs/user-controlled-commit-message.md`, `docs/step-9.5-report.md` |
| `9.6` | Fas 9 — Shortcut/StagingImport | Retention, abuse-skydd och säkerhetsregression | **DONE** | 2026-08-08 | `docs/staging-retention-and-abuse.md`, `docs/step-9.6-report.md` |
| `9.7` | Fas 9 — Shortcut/StagingImport | iOS Shortcut referensklient och installationsguide | **BLOCKED** | — | `docs/signed-shortcut-release.md`, `docs/step-9.7-report.md` |
| `9.8` | Fas 9 — Shortcut/StagingImport | E2E-regression, drift och releasegrind | **PENDING** | — | — |

## Revision history

| Revision | Date | Completed step | Verification | Next step |
|---|---|---|---|---|
| `r0001` | 2026-08-06 | Documentation package created | Documentation conversion/package only | `0.1` |
| `r0002` | 2026-08-06 | `0.1` Legacy project unpacked and inventoried | Structural inspection; no builds or tests run by design | `0.2` |
| `r0003` | 2026-08-06 | `0.2` Legacy baseline build/test paths verified | Shell syntax and ZIP integrity passed; Maven/Docker unavailable; npm proxy dependency 404 documented | `0.3` |
| `r0004` | 2026-08-06 | `0.3` Reuse and migration map completed | Static package/component classification completed; Markdown and ZIP integrity verified | `0.4` |
| `r0005` | 2026-08-06 | `0.4` Clean zip-github baseline created | Structural, XML, JSON, shell and ZIP integrity checks passed; builds limited by documented environment | `1.1` |
| `r0006` | 2026-08-06 | `1.1` Domain model and state machines defined | Pure domain sources compiled with Java 21; state/ownership tests added; structure and ZIP integrity passed | `1.2` |
| `r0007` | 2026-08-06 | `1.2` Database model and Flyway migrations created | XML/static schema checks passed; PostgreSQL/Testcontainers test added but not executable without Maven/Docker | `1.3` |
| `r0008` | 2026-08-06 | `1.3` API skeleton and problem contract created | Structure/XML checks passed; ownership/API tests added but not executable without Maven | `1.4` |
| `r0009` | 2026-08-06 | `1.4` Frontend shell and routing created | Route/component static checks passed; npm install blocked by documented internal registry 404 | `2.1` |
| `r0010` | 2026-08-06 | `2.1` GitHub technical spike completed | Repository access, frozen SHA, branch, commit, draft PR and status lookup verified against `erland/got-test-repo` | `2.2` |
| `r0011` | 2026-08-06 | `2.2` GitHub login and web session implemented | Java/static/structure checks passed; OAuth/session tests added but Maven unavailable | `2.3` |

| `r0012` | 2026-08-06 | `2.3` GitHub App installations and user-scoped repository listing implemented | Static/Java/structure checks passed; integration tests added but Maven and runtime credentials unavailable | `2.4` |
| `r0013` | 2026-08-06 | `2.4` GitHub-backed project configuration implemented | Static/structure/contract checks passed; Quarkus tests added but Maven unavailable | `3.1` |
| `r0014` | 2026-08-06 | `3.1` Streaming upload and metadata implemented | Static/structure checks passed; streaming, digest, limit and cleanup tests added; Maven unavailable | `3.2` |
| `r0015` | 2026-08-06 | `3.2` ZIP path and file-type security implemented | Java compilation and standalone security self-test passed; JUnit fixtures added; Maven unavailable | `3.3` |
| `r0016` | 2026-08-06 | `3.3` ZIP resource limits and bomb protection implemented | Java compilation and live-inflation standalone self-test passed; JUnit fixtures added; Maven unavailable | `3.4` |
| `r0017` | 2026-08-06 | `3.4` archive normalization and deterministic inventory implemented | Java compilation and standalone normalization self-test passed; Maven unavailable | `3.5` |
| `r0018` | 2026-08-06 | `3.5` upload retention and mobile upload flow implemented | Static structure/configuration checks passed; automated iPhone Safari and full Maven/npm tests unavailable | `4.1` |
| `r0019` | 2026-08-06 | `3.6` complete CI baseline established | Workflow, wrapper, ignore rules, status and structure checks validated locally; full CI execution awaits repository push | `4.1` |
| `r0020` | 2026-08-06 | Corrective follow-up to `3.6` | Added missing Quarkus Scheduler dependency and isolated frontend routing tests with DOM cleanup after first full local run | `4.1` |
| `r0021` | 2026-08-06 | Corrective follow-up to `3.6` | Marked `StreamingUploadService` configuration constructor as the explicit CDI injection constructor after Quarkus Arc validation failure | `4.1` |
| `r0023` | 2026-08-06 | Corrective follow-up to `3.6` | Made GitHub App private-key configuration optional at startup, added runtime credential validation, and removed deprecated Hibernate schema-generation configuration | `4.1` |
| `r0024` | 2026-08-06 | `4.1` repository snapshot implemented | Exact branch SHA locking, shallow fetch, deterministic Git-tree inventory and temporary workspace cleanup implemented and tested | `4.2` |

| `r0039` | 2026-08-06 | `7.3` Operations model and documentation completed | Compose health checks, backup/restore scripts, configuration/GitHub setup docs, structure/status/shell/YAML/ZIP checks | `7.4` |

| `r0040` | 2026-08-06 | `7.4` Threat model and final security regression completed | Threat model, CI security baseline, rate-limit self-test, structure/status/shell/ZIP checks passed | `7.5` |

## Required update after every step

1. Change the current `NEXT` row to `DONE`, `BLOCKED` or `SKIPPED`.
2. Add completion date and a link to evidence.
3. Mark exactly one following eligible row as `NEXT`.
4. Update **Current position** and **Revision history**.
5. Add a step report containing changed files, verification commands/results, limitations and follow-ups.
6. Increment the ZIP revision.

- r0023: Isolated Quarkus API tests by clearing the temporary in-memory project/import/upload store before each ProjectResourceTest.
- r0024: Locked import branches to exact Git commit SHAs and created deterministic shallow repository snapshots.

| `r0025` | 2026-08-06 | `4.2` Hash-based comparison implemented | Java 21 compilation with framework stubs and standalone comparison self-test passed; Maven download unavailable in this environment | `4.3` |

| `r0026` | 2026-08-06 | `4.3` Import policy and blockers implemented | Java 21 policy compilation and standalone self-test passed; structure/status/ZIP checks passed | `4.4` |

| `r0027` | 2026-08-06 | `4.4` Immutable import plan stored | Canonical digest self-test, structure/status checks and ZIP integrity passed | `4.5` |
| `r0028` | 2026-08-06 | `4.5` Import review view built | Static TypeScript/route/API review; structure/status/XML/JSON/ZIP checks passed; full npm execution delegated to CI/local environment | `5.1` |

| `r0029` | 2026-08-06 | `5.1` Exact immutable plan approval implemented | Java model compilation/self-test, frontend approval test added, structure/status/ZIP checks | `5.2` |

| `r0030` | 2026-08-06 | `5.2` Temporary Git workspace and approved file application | Java 21 self-test with real local Git remote, exact diff/hash verification, structure/status/ZIP checks | `5.3` |

| `r0031` | 2026-08-06 | `5.3` Branch, atomic commit and push implemented | Java compilation, local bare-repository integration self-test, structure/status and ZIP checks passed | `5.4` |

| `r0032` | 2026-08-06 | `5.4` Draft pull request and immutable result metadata implemented | Java self-test, structure/status and ZIP integrity checks passed | `5.5` |

| `r0033` | 2026-08-06 | `5.5` Idempotency, retry and failure recovery implemented | Standalone PR reuse and Git failure classification tests; structure/status/ZIP checks | `6.1` |

| `r0034` | 2026-08-06 | `6.1` Persistent GitHub result page built | Result metadata links, route/component tests, structure/status and ZIP checks | `6.2` |

| `r0035` | 2026-08-06 | `6.2` Basic delivered-commit check status implemented | Java aggregation self-test, bounded frontend polling, structure/status and ZIP checks | `6.3` |

| `r0036` | 2026-08-06 | `6.3` Import history and stage-aware reopening implemented | Owner-scoped history API, project UI, reopen routing, structure/status and ZIP checks | `7.1` |

| `r0037` | 2026-08-06 | `7.1` Mobile and accessibility review completed | Skip link, route focus, touch targets, compact breakpoints, accessibility semantics and static/status/ZIP checks | `7.2` |

| `r0038` | 2026-08-06 | `7.2` Web and API security hardening completed | Same-origin CSRF, restricted CORS, security headers, safe error logging and structure/status/ZIP checks | `7.3` |

| `r0041` | 2026-08-06 | `7.5` MVP release candidate and Definition of Done completed | Release artifacts, structure, status, security regression, shell/XML/JSON and ZIP checks passed; external production acceptance remains required | `8.1` |
| `r0042` | 2026-08-07 | MVP RC compile/test correction | Fixed backend merge regressions and updated frontend routing test mocks; repository checks passed, full Maven/Vitest rerun delegated to local/CI environment | `8.1` |
| `r0043` | 2026-08-07 | MVP RC test correction | Corrected policy blocker expectation, Git ls-tree fixture delimiter, and asynchronous route-heading focus; repository release/security/status checks passed | `8.1` |
| `r0045` | 2026-08-07 | MVP RC CI Flyway correction | Added Flyway PostgreSQL database module required by Flyway 11+ and updated relocated Quarkus test artifacts; `8.1` remains next | `8.1` |
| `r0046` | 2026-08-07 | MVP RC frontend build correction | Removed unused Vitest `describe` import that failed TypeScript `noUnusedLocals`; `8.1` remains next | `8.1` |
| `r0047` | 2026-08-07 | MVP RC PostgreSQL CI test correction | Bound `Instant` test parameters explicitly as JDBC `TIMESTAMP_WITH_TIMEZONE`; `8.1` remains next | `8.1` |
| `r0048` | 2026-08-07 | MVP RC container publication and deployment improvement | Added GHCR image build/publish, image-based server Compose, local build override and deployment/rollback docs; `8.1` remains next | `8.1` |
| `r0049` | 2026-08-07 | Backend container build correction | Use Maven already installed in the Maven build image; removes failing Maven-wrapper bootstrap dependency on unzip; `8.1` remains next | `8.1` |
| `r0050` | 2026-08-07 | MVP RC real authentication/project frontend correction | Replaced demo project shell with GitHub-authenticated API-backed project list/configuration and merged direct GitHub backup/restore fixes; `8.1` remains next | `8.1` |
| `r0051` | 2026-08-07 | MVP RC GitHub App user authorization correction | Replaced separate OAuth App login credentials with the GitHub App user authorization flow required by `/user/installations`; `8.1` remains next | `8.1` |
| `r0052` | 2026-08-07 | MVP RC container runtime/storage correction | Added Git to the non-root backend runtime image and one-shot storage volume ownership initialization before backend start; `8.1` remains next | `8.1` |

| `r0053` | 2026-08-07 | MVP RC persistent project and Git runtime correction | Persisted project configuration and replaced temporary askpass scripts with packaged runtime helper; `8.1` remains next | `8.1` |
| `r0054` | 2026-08-07 | MVP RC Git author/committer identity correction | Default author/committer from authenticated GitHub user; optional per-import alternate author; `8.1` remains next | `8.1` |

| `r0055` | 2026-08-07 | MVP RC persistent work-branch workflow | One active work branch per project; multiple ZIP imports become sequential commits; PR created only when work is finished; `8.1` remains next | `8.1` |
| `r0056` | 2026-08-07 | MVP RC frontend import-history correction | Restored stage-specific accessible link labels for result, review and in-progress imports; no work-branch behavior changes; `8.1` remains next | `8.1` |

| `r0057` | 2026-08-07 | MVP RC import author selector layout correction | Reset radio controls from generic full-width form-input styling; alternate author fields remain conditional; `8.1` remains next | `8.1` |
| `r0058` | 2026-08-07 | Planning/specification update for flexible review | Added steps `7.6`–`7.10`, hierarchical selection and blocker override target behavior; application version remains RC17 | `7.6` |

| `r0059` | 2026-08-07 | `7.6` blocker taxonomy and non-fatal policy blockers | Added hard/overridable blocker types, mixed-plan default exclusion, API/UI metadata and policy regression coverage; full Maven/Vitest delegated to CI due isolated dependency access | `7.7` |
| `r0060` | 2026-08-07 | `7.7` immutable selection model and API | Added owner/plan/base-bound immutable selection, deterministic selection digest, override audit model and owner-scoped create/read API with validation tests | `7.8` |
| `r0061` | 2026-08-07 | `7.8` hierarchical review selection tree | Added collapsible tri-state directory/file tree, aggregate change counts, responsive/accessibility behavior and safe partial-selection guard | `7.9` |
| `r0062` | 2026-08-07 | `7.9` explicit overrides and exact selected delivery | Bound approval to immutable selection digest, enabled per-path overrides/deletions, and made workspace/commit path-exact to selected changes | `7.10` |
| `r0063` | 2026-08-07 | `7.10` selection, override and security regression | Mixed-tree frontend regressions, exact-selection real-Git workspace regression, hard-block/override audit tests, stale-work-branch delivery test, threat model/security/release checks | `8.1` |
| `r0064` | 2026-08-07 | RC23 test correction | RestAssured list matcher correction and hierarchical review-tree path assertion correction; no production behavior change | `8.1` |
| `r0065` | 2026-08-07 | Phase 7 planning refinement | Added 7.11–7.18 for reusable ZIP ingestion, stored-upload promotion, source audit, unchanged protected-path semantics and streamlined upload/review/approval/commit UX; no production behavior change | `7.11` |
| `r0066` | 2026-08-07 | Step 7.11 | Extracted source-neutral ZIP ingestion/storage into `ZipIngestionService` + `StoredUploadArtifact`; kept web upload as an ownership adapter with unchanged API semantics | `7.12` |

| `r0067` | 2026-08-07 | Step 7.12 | Added idempotent promotion from neutral stored ZIP artifact to the normal user-owned import without re-upload/copy; repository checks documented in step report | `7.13` |
| `r0068` | 2026-08-07 | Step 7.13 | Added explicit non-secret import-source audit metadata, V7 schema columns and history source labels; source remains outside policy/selection/Git semantics | `7.14` |
| `r0069` | 2026-08-07 | Step 7.14 | Added alternative-ingestion regression proving browser/stored ZIP convergence, shared limits, idempotency/ownership/cleanup and digest stability | `7.15` |

| `r0070` | 2026-08-07 | Step 7.15 | Made `.github/**` override classification diff-aware so unchanged workflows require no override; actual add/modify/delete changes remain explicitly overridable | `7.16` |
| `r0071` | 2026-08-07 | Step 7.16 | Automatic idempotent upload-to-review preparation, direct review navigation and retry without re-upload; repository/release checks documented in step report | `7.17` |
| `r0072` | 2026-08-07 | Step 7.17 | One-click review approval through commit/push, persisted approval readback and refresh-safe delivery recovery | `7.18` |

| `r0073` | 2026-08-07 | Step 7.18 | Added cross-page streamlined-flow regression, slow preparation guard, post-approval retry/no-duplicate assertions, unchanged-workflow/override and work-branch coverage; phase 7 quality gate complete | `8.1` |
| `r0074` | 2026-08-07 | Phase 7 planning refinement | Added steps 7.19–7.21 for restart-safe import resume, Git-centric Work history and final resume/Work regression; application version unchanged | `7.19` |
| `r0075` | 2026-08-07 | Step 7.19 | Persisted owner-bound import resume state in PostgreSQL and protected active uploads from terminal cleanup | `7.20` |
| `r0076` | 2026-08-07 | Step 7.20 | Replaced primary import-history UI with Git branch commits plus at most one resumable active import; GitHub history has persisted Work-head fallback | `7.21` |
| `r0077` | 2026-08-07 | Step 7.21 | Added restart/resume, owner-isolation, single-active-import and degraded Work-history regressions; phase 7 quality gate complete | `8.1` |
| `r0078` | 2026-08-07 | RC35 build/test correction | Made resume persistence optional for manually constructed unit-test services and fixed frontend selection fixture typing; no production behavior change | `8.1` |
| `r0079` | 2026-08-07 | RC36 regression correction | Aligned stored-upload cleanup tests with restart-safe active-import retention, added Work commit-history route mock, and awaited loaded review tree in streamlined E2E; no production behavior change | `8.1` |
| `r0080` | 2026-08-07 | Phase 7 Work-lifecycle planning refinement | Added steps 7.22–7.24 for explicit import cancel, state-based Work actions, direct post-commit PR completion and regression; application version unchanged | `7.22` |
| `r0081` | 2026-08-07 | Step 7.22 | Added owner-scoped, idempotent pre-delivery import cancellation, workspace cleanup and cancelled-upload retention eligibility; review UI can explicitly abandon the active import | `7.23` |

| `r0082` | 2026-08-07 | Step 7.23 | Added state-based Work actions, server-side single-active-import enforcement and direct post-commit PR completion | `7.24` |
| `r0083` | 2026-08-07 | Step 7.24 | Added final cancellation/Work-state regressions, restart-safe cancel coverage and pull-request retry/no-duplicate recovery checks; phase 7 complete | `8.1` |

| `r0084` | 2026-08-07 | Phase 8–9 planning/handoff refinement | Moved former 8.4 AI/integration work to future backlog; detailed StagingImport/Shortcut phase 9 and new-chat handoff package; application version unchanged | `8.1` |

| `r0085` | 2026-08-07 | Step 8.1 | Added owner-scoped bounded workflow-run/job/check overview with GitHub links, cache/backoff and graceful degradation; Actions remains read-only | `8.2` |

| `r0086` | 2026-08-07 | Step 8.2 | Added bounded artifact metadata and sanitized condensed failed-job errors with GitHub source links; no artifact/log persistence | `8.3` |

| `r0087` | 2026-08-07 | Step 8.3 | Added default-deny controlled workflow dispatch/failed-job rerun with exact Work guards, audit and idempotency; phase 8 complete | `9.1` |
| `r0088` | 2026-08-07 | RC43 CI correction | Made CI shell entrypoints executable-bit-independent, fixed GitHub Actions permission lookup compilation and scoped the controlled-Actions frontend regression | `9.1` |
| `r0089` | 2026-08-07 | RC44 CI correction | Made empty Actions allowlists Quarkus-safe while preserving deny-all defaults and removed nested executable-bit assumptions from release verification | `9.1` |

| `r0090` | 2026-08-08 | Phase 9 file-mode planning refinement | Assigned deterministic Git file-mode preservation across 9.1/9.4/9.7; application version unchanged | `9.1` |
| `r0091` | 2026-08-08 | Phase 9 commit-message planning refinement | Added dedicated step 9.5 for user-controlled, persisted, approval-bound commit messages and renumbered later phase-9 steps; application version unchanged | `9.1` |
| `r0092` | 2026-08-08 | Phase 9 Shortcut distribution/security planning refinement | Locked static pre-signed Shortcut distribution, deployment-scoped low-privilege upload credential, immediate revoke/replacement flow and no hosted-Actions signing dependency; application version unchanged | `9.1` |

| `r0093` | 2026-08-08 | Step 9.1 | Added durable StagingImport lifecycle/persistence, hashed claim-token state and neutral Git file-mode metadata representation | `9.2` |

| `r0094` | 2026-08-08 | Step 9.2 | Added low-privilege capability staging-create endpoint, hash-only 256-bit claim tokens, exact CSRF boundary and dedicated rate limiting | `9.3` |

| `r0095` | 2026-08-08 | Step 9.3 | Added authenticated fragment-preserving browser claim, atomic owner binding, neutral unavailable responses and same-owner retry | `9.4` |
| `r0096` | 2026-08-08 | Step 9.4 | Added owner-scoped project selection, restart-safe ordinary Import promotion and approval-bound Git file-mode preservation | `9.5` |
| `r0097` | 2026-08-08 | Step 9.5 | Added user-controlled, persisted and approval-bound commit messages shared by browser and staging imports | `9.6` |
| `r0098` | 2026-08-08 | Step 9.6 | Added deterministic staging retention, quota/abuse controls, DB-coordinated promotion cleanup and credential-rotation guidance | `9.7` |
| `r0099` | 2026-08-08 | `9.7` implementation complete except external Apple signing/install gate; status BLOCKED | Repository/static/self-test verification; signed artifact unavailable in this environment | blocked at `9.7` |
| `r0100` | 2026-08-08 | CI correction for blocked 9.7 revision | Fixed backend matcher ambiguity and frontend cross-test DOM leakage; phase state unchanged | blocked at `9.7` |
