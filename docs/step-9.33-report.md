# Steg 9.33 - Upload starts Work automatically

## Mål

Ta bort den manuella `Starta arbete`-övergången ur normalflödet. Användaren ska kunna gå direkt från repository till första ZIP-upload utan att behöva förstå Work-livscykeln i förväg.

## Implementerat

- `NewImportPage` hämtar fortfarande aktuell Work via `/projects/{id}/work`, vilket reconcilar PR-status innan uploadflödet fortsätter.
- Om ingen Work finns när den första nya importen skickas startas Work automatiskt med befintliga `startProjectWork(projectId)`.
- Först efter lyckad Work-start skapas importen och därefter laddas ZIP-filen upp.
- Om Work-start misslyckas stoppas flödet före `createImport`, `uploadZip` och `prepareImportReview`.
- Befintlig `ACTIVE`/`PR_CLOSED` Work återanvänds utan ny Work-start.
- `PR_OPEN` behåller den explicita bekräftelsen att nästa ZIP uppdaterar befintlig PR.
- Återöppnad/resumable import skapar inte ny Work.
- Projektsidans normalhandling utan Work är nu `Ladda upp första ZIP`.
- Explicit återupptagning av en vald befintlig branch ligger kvar i Work-sektionen som avancerad handling.

## Säkerhet

Ingen ny backend-provisioneringsväg introduceras. Automatiken använder samma Work-startendpoint och därmed samma branchverifiering, provisioning-state, tom-repo-bootstrap och fail-closed felhantering som den tidigare manuella starten.

## Regression

Frontendtester täcker:
- Work-start före importskapande och upload,
- återanvändning av befintlig Work,
- fail-closed vid Work-startfel,
- bevarad PR_OPEN-bekräftelse,
- projektsidans nya `Ladda upp första ZIP`,
- explicit återupptagning av vald befintlig branch.
