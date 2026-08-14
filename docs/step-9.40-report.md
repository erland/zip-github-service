# Steg 9.40 - Simplified upload

## Mål

Uploadsidan ska i normalfallet handla om användarens primära intention: välj ZIP och fortsätt. Author- och Work-branchdetaljer ska finnas kvar utan att konkurrera visuellt med ZIP-valet.

## Implementerat

- `Projektarkiv` ligger före author-anpassning.
- Standardförfattaren visas kompakt som `Författare: namn <e-post>`.
- `Ändra författare` öppnar befintliga val:
  - `Jag själv`
  - `Någon annan`
  - namn/e-post när annan författare väljs
- Author-detaljer är kollapsade i standardfallet och öppnas automatiskt när `Någon annan` är aktiv.
- Work-branchförklaringen ligger under `Så hanteras arbetsbranchen`.
- Den prominenta `PR_OPEN`-varningen och bekräftelsen ligger kvar före ZIP-valet och ZIP-inputen är fortsatt disabled tills användaren uttryckligen bekräftar.

## Säkerhets- och semantikavgränsning

Ingen API- eller backendlogik är ändrad. Custom author skickas fortfarande endast när `authorMode === 'other'`. Automatic Work start, reuse av befintlig Work, resumable imports, upload, review preparation och open-PR-bekräftelse har samma semantik som före 9.40.

## Regression

- NewImportPage-test verifierar att ZIP-inputen är primär medan Work/author-detaljer är kollapsade.
- Testet verifierar den kompakta standardförfattaren och att `Någon annan` blir tillgänglig efter explicit expansion.
- Simplified import flow öppnar nu author-disclosure innan den väljer annan författare, så den befintliga end-to-end-semantiken fortsätter testas.
