# Steg 9.41 - Result next action follows Actions and Work state

## Mål

Resultatsidan ska lyfta ett verkligt Actions-problem när zip-github vet att aktuell commit har misslyckade eller avbrutna körningar, utan att göra flödet sämre för repositories som saknar Actions eller där workflows endast triggas av pull request.

## Klassificering

Actions får påverka vägledningen endast när statusen avser exakt den visade committen och minst en workflow-körning eller check faktiskt har observerats.

- `failure` / `cancelled` + observerad workflow/check:
  - prominent attention-kort
  - `Granska Actions-felet` blir primär attention-handling
  - befintliga `Ladda upp nästa ZIP` / PR-handlingar finns fortfarande kvar
- `pending` / `queued` / `in_progress` + observerad workflow/check:
  - informativ status
  - normal nästa handling förblir tillgänglig
- `success`:
  - normal Work-/PR-vägledning
- `not_started`:
  - ingen blockering och ingen Actions-baserad omprioritering
  - täcker både repositories utan Actions och PR-only workflows innan PR finns
- `unavailable`:
  - ingen blockering och ingen Actions-baserad omprioritering
  - befintlig diagnostik i Actions-panelen finns kvar

## Säkerhets- och UX-avgränsning

Steget dispatchar eller rerunnar inga workflows automatiskt. Det ändrar inte Work-, PR- eller Actions-backendsemantik. Även vid failure/cancelled behålls normal fortsättning som sekundär handling för att undvika dead-end-lägen.

## Regression

Tester verifierar:
1. verkligt observerat failure prioriteras och länkar till Actions-delen,
2. nästa ZIP och PR är fortfarande möjliga vid failure,
3. `not_started` blockerar inte flödet och representerar bland annat no-Actions/PR-only-fall,
4. in-progress Actions informerar utan att låsa nästa ZIP eller PR.
