# Steg 9.32 - Guided project actions

## Mål

Minska behovet av att tolka Work-, import- och PR-status på projektsidan. Sidan ska i stället uttryckligen visa vad användaren bör göra härnäst.

## Implementerat

- Ny `Nästa steg`-yta på projektsidan.
- Aktiv import prioriterar `Fortsätt granska` eller `Fortsätt import`.
- Repository utan Work prioriterar `Starta arbete`.
- ACTIVE Work prioriterar `Ladda upp nästa ZIP`; PR-skapande är sekundärt.
- PR_OPEN prioriterar `Ladda upp nästa ZIP` och länkar befintlig PR.
- PR_CLOSED prioriterar `Skapa ny pull request` när Work har en commit och behåller nästa ZIP som sekundärt val.
- Duplicerade upload-/continue-/create-work-handlingar har tagits bort från andra delar av projektsidan.
- Destruktiva handlingar ligger kvar separat och kräver samma explicita beslut som tidigare.

## Avgränsning

Steg 9.32 ändrar inte Work-start, backend-API eller säkerhetsregler. Automatisk Work-start vid första ZIP-upload hör till steg 9.33.

## Regression

Frontendtesterna täcker den guidade handlingen för aktiv import, ingen Work, ACTIVE Work, PR_OPEN och PR_CLOSED. Befintlig PR-/Work-livscykelregression behålls.
