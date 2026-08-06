# Implementation status — zip-github

Version 1.0  
Updated: 6 August 2026

## Purpose

This file is the authoritative execution ledger for the prompt-by-prompt implementation. The command **“kör nästa steg”** always means: implement the single row whose status is `NEXT`, then update this file and package a new ZIP.

## Invariants

- Exactly one step is `NEXT` in every normal delivered revision.
- A step can become `DONE` only after its required deliverables and verification are documented.
- Completed steps are never silently reopened; a correction is recorded in the affected step report and revision history.
- Every delivered ZIP records its revision, completed step, next step, changed files and verification result.
- Authorization and ownership isolation are mandatory cross-cutting requirements for all user-owned resources.

## Current position

- Repository revision: `r0019`
- Last completed step: `3.6`
- Next step: `4.1`
- Overall state: `IN IMPLEMENTATION`

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
| `4.1` | Fas 4 — repositorysnapshot och importplan | Välj och implementera repositorysnapshot | **NEXT** | — | — |
| `4.2` | Fas 4 — repositorysnapshot och importplan | Implementera hashbaserad jämförelse | **PENDING** | — | — |
| `4.3` | Fas 4 — repositorysnapshot och importplan | Implementera importpolicy och blockerare | **PENDING** | — | — |
| `4.4` | Fas 4 — repositorysnapshot och importplan | Spara immutable importplan | **PENDING** | — | — |
| `4.5` | Fas 4 — repositorysnapshot och importplan | Bygg granskningsvyn | **PENDING** | — | — |
| `5.1` | Fas 5 — godkännande och Git-leverans | Implementera godkännande av exakt plan | **PENDING** | — | — |
| `5.2` | Fas 5 — godkännande och Git-leverans | Implementera temporär Git-arbetsyta och filapplicering | **PENDING** | — | — |
| `5.3` | Fas 5 — godkännande och Git-leverans | Implementera branch, atomisk commit och push | **PENDING** | — | — |
| `5.4` | Fas 5 — godkännande och Git-leverans | Implementera pull request och resultatmetadata | **PENDING** | — | — |
| `5.5` | Fas 5 — godkännande och Git-leverans | Implementera idempotens, retry och felåterhämtning | **PENDING** | — | — |
| `6.1` | Fas 6 — Actions-länkar och resultatsida | Bygg resultatsidan med beständiga GitHub-länkar | **PENDING** | — | — |
| `6.2` | Fas 6 — Actions-länkar och resultatsida | Lägg grundläggande checkstatus | **PENDING** | — | — |
| `6.3` | Fas 6 — Actions-länkar och resultatsida | Lägg importhistorik och återöppning | **PENDING** | — | — |
| `7.1` | Fas 7 — mobil, säkerhet och driftsättning | Genomför komplett mobil- och tillgänglighetsgenomgång | **PENDING** | — | — |
| `7.2` | Fas 7 — mobil, säkerhet och driftsättning | Härda webb- och API-säkerheten | **PENDING** | — | — |
| `7.3` | Fas 7 — mobil, säkerhet och driftsättning | Skapa driftmodell och operationsdokumentation | **PENDING** | — | — |
| `7.4` | Fas 7 — mobil, säkerhet och driftsättning | Genomför hotmodell och slutlig säkerhetsregression | **PENDING** | — | — |
| `7.5` | Fas 7 — mobil, säkerhet och driftsättning | MVP-release och Definition of Done | **PENDING** | — | — |
| `8.1` | Fas 8 — efter MVP: integrerade Actions-resultat | Workflow runs och jobs | **PENDING** | — | — |
| `8.2` | Fas 8 — efter MVP: integrerade Actions-resultat | Artifacts och kondenserade fel | **PENDING** | — | — |
| `8.3` | Fas 8 — efter MVP: integrerade Actions-resultat | Kontrollerad workflow dispatch och omkörning | **PENDING** | — | — |
| `8.4` | Fas 8 — efter MVP: integrerade Actions-resultat | AI- och integrationsyta | **PENDING** | — | — |

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

## Required update after every step

1. Change the current `NEXT` row to `DONE`, `BLOCKED` or `SKIPPED`.
2. Add completion date and a link to evidence.
3. Mark exactly one following eligible row as `NEXT`.
4. Update **Current position** and **Revision history**.
5. Add a step report containing changed files, verification commands/results, limitations and follow-ups.
6. Increment the ZIP revision.
