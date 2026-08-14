# Steg 9.35 - Repository attention overview

## Mål

Göra repositoryöversikten till en lista över nästa konkreta uppgifter, inte en generell statistikdashboard.

## Implementerat

- Repositorysidan hämtar först de repositories användarens GitHub App-installationer ger åtkomst till.
- För repositories som redan har ett internt zip-GitHub-projekt hämtas aktuell Work och importhistorik.
- `getProjectWork()` återanvänder befintlig PR-reconciliation, så repositoryöversikten behöver inte vänta på att projektsidan öppnas för att få aktuell PR-status.
- Om Work har en aktuell commit och `lastImportId` hämtas även aktuell Actions-status.
- Resultatet grupperas i:
  - `Behöver din uppmärksamhet`
  - `Pågående`
  - `Övriga repositories`
- Attention omfattar aktiv upload/review, externally changed Work, PR_CLOSED, failed/cancelled/unavailable Actions och status som inte kan verifieras.
- Attention-länken går direkt till review/result där det finns ett tydligt sådant mål; annars till projektet.
- ACTIVE och PR_OPEN klassificeras som pågående.
- Repositories utan internt projekt klassificeras som övriga.
- Sökning går fortsatt över samtliga repositories och visar ett platt sökresultat.

## Fail-closed

Om Work/import-status eller Actions-status för ett existerande projekt inte kan verifieras visas repositoryt i attention-gruppen med `Status kunde inte verifieras`/`Actions-status kunde inte verifieras`; UI:t gissar inte att allt är normalt.

## Avgränsning

Ingen ny backenddatamodell, lagrad attention-status eller KPI-dashboard införs. 9.35 är ett frontend-orienterat sammanställningslager ovanpå befintliga auktoritativa endpoints.

## Regression

Frontendtester verifierar gruppordning, direkt review-navigation, pågående PR, repository utan projekt, fail-closed status och att sökning fortfarande fungerar över alla grupper.
