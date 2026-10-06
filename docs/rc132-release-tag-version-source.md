# rc.132 — GitHub Release-taggen som releaseversion

Date: 2026-10-06  
Repository revision: r0180  
Product version: 1.0.0-rc.132

## Problem

Releaseartefakterna hade två möjliga versionskällor:

- container-images använde repositoryfilen `VERSION`;
- ChatGPT-pluginpaketet använde GitHub Release-taggen.

Det kunde skapa olika versionsnummer för backend/frontend och plugin om en release-tag och `VERSION` skilde sig.

## Correction

En publicerad GitHub Release använder nu release-taggen som versionskälla för samtliga releaseartefakter:

- backend container image;
- frontend container image;
- `zip-github-plugin-<version>.zip`.

Ett valfritt inledande `v` normaliseras bort, så både `v1.2.3` och `1.2.3` ger version `1.2.3`.

`VERSION` är inte versionskälla i releasebygget och används inte heller som releasegrind. GitHub Release-taggen är ensam sanningskälla för releaseversionen.

Vanlig CI på `main` får fortsatt använda `VERSION` för kontinuerligt byggda images. CI publicerar inte längre images på Git-taggar; releasepublicering ägs endast av release-workflowen. Workflowen kan dessutom köras manuellt för en befintlig release-tag för att återhämta ett misslyckat releasebygge.

## Release output

En publicerad release `v1.0.0-rc.132` ger:

```text
ghcr.io/erland/zip-github-service-backend:1.0.0-rc.132
ghcr.io/erland/zip-github-service-frontend:1.0.0-rc.132
zip-github-plugin-1.0.0-rc.132.zip
```

## Step status

Steg 10.2 är fortsatt BLOCKED endast på den separata live-acceptansen av deployad MCP upload/claim. Den här releasekorrigeringen ändrar inte den grinden.
