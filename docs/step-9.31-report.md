# Steg 9.31 — Runtime version and PR-aware import result

## Syfte

Göra den körande releaseversionen synlig utan versionsduplicering samt göra commitresultatsidans PR-åtgärd konsekvent med Work/PR-livscykeln.

## Versionsvisning

`VERSION` i repositoryroten är fortsatt enda manuellt underhållna releaseversionen. GitHub Actions läser samma fil när frontend byggs och sätter `VITE_ZIP_GITHUB_VERSION` för Vite. `AboutPage` visar det injicerade värdet.

Den äldre `frontend/package.json`-versionen används inte som produktreleaseversion och behöver därför inte synkas. Lokala source-builds kan få `ZIP_GITHUB_VERSION` som Docker build-arg; saknas värdet visas `development`.

## PR-medveten commitresultatsida

Efter en levererad commit hämtar `ImportResultPage` aktuell Work via befintlig project Work-endpoint. Endpointen återanvänder serverns PR-reconciliation.

- `ACTIVE`: `Skapa pull request`.
- `PR_OPEN`: ingen create-knapp; sidan anger att befintlig PR uppdaterats och länkar till den.
- `PR_CLOSED`: `Skapa ny pull request`.
- Work-status kan inte hämtas: fail closed, ingen PR-create-knapp.

Det lokala state som används efter att en ny PR skapats från resultatsidan finns kvar för omedelbar feedback, men får inte överstyra en redan öppen PR som hämtats från Work.

## Verifiering

Frontendregressioner täcker buildversionens presentation samt PR_OPEN/PR_CLOSED och befintligt ACTIVE-flöde. Releasegrinden verifierar att CI injicerar versionen från `../VERSION`, att About-sidan använder `VITE_ZIP_GITHUB_VERSION` och att PR_OPEN-resultatet saknar create-åtgärd.
