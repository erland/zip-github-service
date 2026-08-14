# Steg 9.38 - Project progressive disclosure

## Mål

Projektsidan ska i första hand hjälpa användaren förstå nästa handling och aktuell relevant status. Repositorymetadata, Git-detaljer och avancerade/destruktiva kontroller ska finnas kvar utan att dominera normalvyn.

## Implementerat

- `Nästa steg` ligger före repositorymetadata.
- Repositorymetadata flyttas till expanderbara `Repositoryinformation`.
- `Pågående arbete` visar en kompakt status: Work pågår, PR öppen eller PR stängd utan merge.
- PR-status, extern Work-konflikt, aktiv import och GitHub Actions är fortsatt synliga utan expansion.
- Branch, base branch, head/remote SHA och commit history ligger under `Visa tekniska Work-detaljer`.
- Explicit återupptagning av gammal branch ligger under `Avancerat: återuppta befintlig branch`.
- Avsluta Work/branchradering ligger under `Avancerade Work-åtgärder`.
- Borttagning av repositorykopplingen ligger under `Avancerade repositoryåtgärder`.

## Säkerhetsavgränsning

Progressive disclosure ändrar endast presentation. Attention-status, Work-/PR-livscykel, Actions-kontroller, explicit branch-resume, destructive confirmations och backendsemantik är oförändrade.

## Regression

Frontendtesterna verifierar att `Nästa steg` kommer före repositorymetadata, att tekniska Work-detaljer är kollapsade som standard men tillgängliga efter explicit expansion, och att explicit branch-resume fortfarande fungerar.
