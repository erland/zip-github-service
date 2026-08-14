# Steg 9.34 - Attention-first review

## Mål

Minska den visuella och mentala belastningen i review utan att förenkla bort säkerhetsbeslut.

## Implementerat

- Ett nytt attention-lager visas först när planen innehåller blockerade poster, externa Work-konflikter eller varningar.
- Lagret visar separata antal och direkta knappar till de befintliga filtren `Blockerade`, `Externa ändringar` och `Varningar`.
- Planer utan sådana avvikelser visar i stället `Inga särskilda risker hittades`.
- `Vanliga ändringar` summeras separat med antal tillagda/ändrade/ignorerade och en direkt väg tillbaka till förändringsfiltret.
- Den fullständiga plansammanfattningen finns kvar men ligger i en expanderbar disclosure.
- Filträdet, explicit selection, blocker decisions, override-godkännanden, external-change acknowledgement och commit/delivery-flödet är oförändrade.

## Säkerhetsavgränsning

Steg 9.34 ändrar endast presentation och navigering mellan redan existerande filter. Ingen backendklassificering, default selection eller blockerregel har ändrats.

## Regression

Frontendregression verifierar attention-lagret, direkt navigation till blockerade beslut och låg-risk-presentation för en plan utan blockers/external conflicts/warnings. Befintliga review-tester fortsätter täcka selection, overrides, hard blockers och delivery.
