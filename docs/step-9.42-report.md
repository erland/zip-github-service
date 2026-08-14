# Steg 9.42 - Compact successful Actions, prominent failures

## Mål

Actions-panelen ska använda minst visuell uppmärksamhet när allt är grönt och mest när användaren faktiskt behöver agera. Presentationen ska inte ändra Actions-, Work- eller PR-semantik.

## Implementerat

### Success

- Rubrik, commit och statusbadge visas direkt.
- En kompakt status säger att alla observerade Actions-kontroller är godkända.
- Workflows, jobs, övriga checks, artifacts och ActionsControls ligger kvar bakom `Visa Actions-detaljer`.
- Detaljerna är kollapsade som standard.

### Pending / queued / in_progress

- En kompakt status visar att GitHub Actions pågår.
- Workflow/check-detaljer ligger bakom `Visa pågående Actions-detaljer`.
- Ingen blockering eller automatisk väntan införs.

### Failure / cancelled

- Felstatus visas prominent.
- Workflows/jobs/checks, failure diagnostics och controls visas direkt utan extra expansion.
- Befintliga kondenserade fel, sanerade jobbloggar, kopieringsfunktioner och GitHub-länkar behålls.

### unavailable / not_started

Dessa specialfall behåller tidigare presentation. `not_started` är fortsatt ett normalt icke-blockerande tillstånd för bland annat repositories utan Actions och PR-only workflows innan en PR finns.

## Regression

Tester verifierar att success är kollapsad som standard men kan expanderas, att failure är direkt synlig, samt att befintliga dedupe-, grouping- och resultatsidetester fortsatt kan nå workflow/check/artifact-detaljer efter explicit expansion.
