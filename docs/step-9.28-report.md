# Steg 9.28 — CI trigger optimization

## Bakgrund

Huvudworkflowet `.github/workflows/ci.yml` lyssnade på både `push` och `pull_request`. När ett zip-GitHub Work redan hade en öppen PR och en senare ZIP skapade ytterligare en commit på samma Work-branch kunde GitHub därför starta två fulla CI-körningar för samma ändring: en för branch-pushen och en för PR-aktiviteten `synchronize`.

## Beslut

Huvud-CI:t använder från rc.100 automatiskt endast `push` samt fortsatt `workflow_dispatch` för manuella körningar. Ingen branchbegränsning läggs på `push`, så Work-branches, default branch och taggar behåller nuvarande verifiering.

Detta passar zip-GitHubs leveransmodell: varje ZIP-leverans pushar en konkret commit till den persistenta Work-branchen. Samma commit behöver inte därefter köra samma fulla pipeline en andra gång bara för att Work-branchen råkar ha en öppen PR. En senare korrigerings-ZIP skapar en ny push och verifieras därmed normalt. Merge/push till `main` verifieras igen.

## Container images

Befintlig publiceringsregel är oförändrad. `container-images` får bygga på alla verifierade commits men publicerar endast när eventet inte är `pull_request` och ref är `main` eller en tagg. Efter triggerförenklingen betyder det fortfarande att vanliga Work-branches aldrig publicerar images.

## Required checks och tradeoff

Vi använder inte ett branch/path-filter som skapar ett förväntat men skippat PR-workflow. Repository-regler bör kräva de stabila CI-jobb/check-namnen för committen.

Tradeoffen är medveten: `push` verifierar PR-headens faktiska commit, medan `pull_request` normalt kan verifiera GitHubs syntetiska merge-ref. GitHub rapporterar merge conflicts separat. Om projektet senare använder merge queue eller behöver obligatorisk test av syntetisk merge-commit bör det införas som ett separat steg/workflow som inte återintroducerar dubbla fulla körningar för varje Work-push.

## Verifiering

Releasegrinden verifierar att huvud-CI:t har `push` och `workflow_dispatch`, saknar `pull_request` och fortfarande begränsar image-publicering till `main` eller tagg.
