# Steg 9.37 - New repository flow: directly to first ZIP

## Mål

Ta bort den sista normala UI-övergången där användaren måste förstå och välja `Starta arbete` innan den egentliga intentionen — att ladda upp första ZIP-filen — kan påbörjas.

## Implementerat

`RepositoryDetailPage` använder fortsatt `startRepositoryWork(installationId, repositoryId)`. Endpointen skapar/verifierar internt projekt och Work precis som tidigare.

Skillnaden är presentation och navigation:

- primär knapp: `Ladda upp första ZIP`
- pågående text: `Förbereder repository…`
- lyckad provisionering navigerar direkt till `/projects/{projectId}/imports/new`
- fel beskriver att repositoryt inte kunde förberedas för första ZIP-filen

Om repositorylistan redan returnerar `projectId` omdirigeras användaren fortsatt till befintlig projektsida.

## Säkerhetsavgränsning

Ingen backendlogik har ändrats. Samma Work-start, branchverifiering, empty-repository-bootstrap, GitHub App-auktorisering och fail-closed provisioning återanvänds.

## Regression

App-integrationstestet verifierar att ett nytt repository:

1. öppnas utan internt projekt,
2. visar `Ladda upp första ZIP`,
3. provisionerar projekt + Work genom befintlig endpoint,
4. landar direkt på `Ladda upp projekt-ZIP` med aktiv Work.
