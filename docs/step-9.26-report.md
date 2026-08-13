# Steg 9.26 — Grupperad presentation av workflow-runs för samma commit

Datum: 2026-08-13  
Revision: `r0145`  
Release candidate: `1.0.0-rc.97`

## Bakgrund

Efter att en pull request skapats kan en senare ZIP-commit på samma Work-branch trigga både `push` och `pull_request`. GitHub returnerar då flera riktiga workflow-runs för samma workflow och commit. Actions-panelen i rc.96 renderade varje run som ett eget toppnivåkort, vilket gjorde att samma workflow såg duplicerat ut.

## Genomförande

- `ActionsPanel` grupperar runs på GitHubs `workflowId` + commit SHA. Om `workflowId` saknas används workflow-path/namn som kompatibilitetsfallback.
- Gruppen visas som ett enda toppnivåkort med antal GitHub-körningar.
- Varje faktiskt run bevaras i en expanderbar lista med event, status, jobs och GitHub-länk.
- Gruppstatus aggregeras konservativt: `failure` prioriteras före `in_progress`, `queued`, `cancelled` och `success`.
- Befintlig 9.18-deduplicering mellan workflow-jobs och sekundära GitHub Actions-checks använder fortfarande jobs från samtliga runs.
- Olika workflow-ID:n slås aldrig ihop bara för att de har samma visningsnamn.

## Regression

`ActionsPanel.test.tsx` täcker:

- `push` + `pull_request` för samma workflow/commit grupperas till en toppnivåpost;
- blandad success/failure ger synligt failure och båda runs finns kvar;
- olika workflow-ID:n med samma namn förblir separata.

## Nästa steg

`9.27` är nästa steg: explicit varning/bekräftelse innan en ny ZIP läggs till i ett Work som redan har en öppen PR.
