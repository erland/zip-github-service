# Steg 9.36 - Central session-expiry handling

## Mål

När en etablerad session löper ut ska användaren inte mötas av `API-fel 401` på den sida som råkar göra nästa API-anrop. Frontend ska i stället återgå till login och kunna fortsätta på samma route efter GitHub OAuth.

## Implementerat

- Ny `frontend/src/api/session.ts` med:
  - `SessionExpiredError`
  - `assertSessionActive(response)`
  - global session-expiry-signal
  - subscription för layouten
  - sticky expiry-state så ett mycket tidigt 401 inte kan tappas innan listenern är installerad
- Authenticated API-klienterna för projects, imports, repositories, staging, maintenance, GitHub installationer och Shortcut kontrollerar `401` centralt.
- `uploadZip()` signalerar session timeout även för XHR-status 401.
- `AppLayout` växlar till `anonymous`, tar bort den gamla användaren och visar:
  `Din session har gått ut. Logga in igen för att fortsätta där du var.`
- Login-länken återanvänder befintligt `returnTo` från aktuell pathname + query.
- `/api/auth/me` behåller 401 => `null`, så första besök utan session fungerar som tidigare.
- Logout med 401 betraktas som redan utloggad.
- 403 och andra fel klassificeras inte som timeout.

## Regression

- Enhetstest verifierar 401-signal, att 403/500 inte signalerar timeout och att en sen subscriber fortfarande notifieras.
- App-integrationstest verifierar authenticated -> API 401 -> login samt korrekt `returnTo` och att `API-fel 401` inte visas.
- Releasegrinden verifierar att både fetch-API och XHR-upload använder det gemensamma sessionkontraktet.
