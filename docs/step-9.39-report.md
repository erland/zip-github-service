# Steg 9.39 - Review completion guidance

## Mål

När review fortfarande kräver blockerbeslut ska användarens uppmärksamhet ligga där. När dessa beslut är lösta ska sidan tydligt hjälpa användaren vidare till commitdelen utan att automatisera något säkerhets- eller godkännandebeslut.

## Implementerat

- Review decision-blocket visar fortsatt antal blockerande förändringar som kräver beslut.
- När minst en förändring är vald och `unresolvedBlockers` är tom:
  - statusen är `Urvalet kan godkännas`
  - antal valda förändringar visas i vägledningstexten
  - `Fortsätt till commit` visas som tydlig primär handling
- `Fortsätt till commit` scrollar till befintligt commitmeddelandefält och fokuserar det.
- Knappen låser inte selection, registrerar inget approval och startar ingen delivery.
- Commitmeddelande, explicit override-audit, external-change acknowledgement och slutliga `Godkänn valda förändringar` behåller tidigare semantik.

## Regression

Frontendtest verifierar att:
1. `Fortsätt till commit` inte visas när blockerbeslut återstår.
2. Knappen visas efter att nödvändiga blockerbeslut lösts.
3. Antalet valda förändringar kommuniceras.
4. Klick flyttar fokus till commitmeddelandet.
5. Slutligt godkännande är fortfarande disabled tills commitmeddelande och övriga krav är uppfyllda.
