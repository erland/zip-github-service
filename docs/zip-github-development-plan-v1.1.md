**Version 1.0 · 6 augusti 2026**

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Syfte<br />
</strong>Detta dokument är avsett att bifogas i en ny ChatGPT-konversation tillsammans med zip-buildserver-main.zip och den funktionella specifikationen. Planen ska ge tillräcklig teknisk och processmässig vägledning för att inventera den äldre lösningen, skapa en ny målarkitektur och implementera tjänsten stegvis utan att behöva upprepa den tidigare analysen.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 1. Dokumentets roll och arbetssätt

Planen är både en teknisk utvecklingsplan och en arbetsinstruktion för en ny chat. Den nya chatten ska behandla den äldre zip-buildserver-koden som ett inspirations- och återanvändningsunderlag, inte som en arkitektur som måste bevaras. Funktionell specifikation är styrande för produktens beteende. Denna plan är styrande för implementeringsordning, kvalitetsgrindar och leverabler.

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Viktig instruktion till den nya chatten<br />
</strong>Börja inte med att skriva stora mängder ny kod. Packa först upp båda underlagen, inventera repositoryt, jämför befintlig kod mot funktionskraven och skapa en konkret migreringskarta. Gör sedan små, verifierbara leveranser. Efter varje fas ska projektet byggas, testas, dokumenteras och paketeras som en uppdaterad ZIP.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 2. Produktmål

Tjänsten ska göra det möjligt för en GitHub-användare att konfigurera ett repository som projekt, ladda upp ett komplett eller avgränsat projektarkiv, granska förändringarna mot en vald branch och därefter leverera godkända förändringar till GitHub. GitHub Actions i repositoryt ska sköta kompilering, tester, PDF/EPUB-generering och andra projektspecifika byggsteg.

- GitHub ska vara den beständiga och auktoritativa projektkällan.

- ZIP-filen ska vara ett transport- och arbetsformat, inte en parallell permanent master.

- Användaren ska kunna utföra hela importflödet från en telefon.

- Inga förändringar får skrivas till GitHub innan användaren har sett och godkänt en importplan.

- MVP ska skapa en separat importbranch och en pull request.

- Tjänsten ska inte köra godtycklig uppladdad projektkod lokalt i MVP.

- GitHub Actions ska vara den primära bygg-, test- och publiceringsmotorn.

# 3. Beslut som betraktas som låsta

| **Område**       | **Låst beslut**                                                                                                            |
|------------------|----------------------------------------------------------------------------------------------------------------------------|
| Backend          | Behåll Java och Quarkus som utgångspunkt om inventeringen inte visar ett blockerande problem.                              |
| Frontend         | Behåll React, TypeScript och Vite som utgångspunkt.                                                                        |
| Databas          | Behåll PostgreSQL och Flyway.                                                                                              |
| Autentisering    | GitHub används för användarinloggning. Repositoryåtkomst ges med GitHub App eller motsvarande minst privilegierade modell. |
| GitHub-skrivning | MVP skapar alltid ny branch och pull request; ingen direkt skrivning till main.                                            |
| Byggning         | GitHub Actions kör projektets bygg- och teststeg.                                                                          |
| ZIP-säkerhet     | ZIP valideras och packas upp i isolerad temporär arbetsyta med skydd mot traversal, länkar och resursmissbruk.             |
| Filborttagningar | Blockeras i MVP eller kräver särskild uttrycklig funktion efter MVP.                                                       |
| Workflowfiler    | Ändringar under .github/\*\* blockeras i MVP.                                                                              |
| Gammal worker    | Docker-baserad exekvering av uppladdad kod tas bort från den kritiska vägen.                                               |

# 4. Omfattning

## 4.1 Ingår i MVP

- GitHub-login och utloggning.

- Lista användarens tillgängliga GitHub App-installationer och repositoryn.

- Skapa och redigera en projektkonfiguration som pekar på repository och standardbranch.

- Ladda upp ZIP via webbläsare på dator och iPhone.

- Säker validering, uppackning och normalisering av ZIP.

- Jämföra ZIP-innehållet mot ett fast commit-SHA från vald branch.

- Visa filer som ska läggas till, ändras, ignoreras eller blockeras.

- Blockera filborttagningar och .github/\*\*.

- Godkänna importplan.

- Skapa importbranch, en atomisk commit och en pull request.

- Visa länkar till commit, pull request och GitHub Actions/checks.

- Spara auditinformation och importstatus.

- Retention och automatisk städning av uppladdningar och temporära arbetsytor.

- Backend- och frontendtester samt GitHub Actions för tjänstens egen CI.

## 4.2 Efter MVP

- Textdiff i appen för mindre textfiler.

- Kontrollerade filborttagningar.

- Projektmanifest med managed, protected och generated paths.

- base_commit och revisionskontroll i ZIP-manifest.

- Integrerad visning av Actions-status, checks och artifacts.

- Kondenserade kompilator- och byggfel.

- Manuell omkörning av workflow från appen.

- Export av en aktuell GitHub-branch som ett AI-anpassat ZIP-arbetspaket.

- Custom GPT Action eller MCP-adapter för små status- och styranrop.

- Stöd för flera användare och organisationspolicyer.

## 4.3 Ingår inte i MVP

- Exekvering av godtycklig kod i tjänstens egna Docker-containrar.

- Automatisk merge av pull requests.

- Direkt commit till skyddade huvudbrancher.

- Generell Git-hosting utöver GitHub.

- Långtidslagring av byggartifacts i tjänsten.

- Komplett webbaserad kodredigerare.

- Automatisk konfliktlösning när målbranchen har ändrats.

- Skrivning till repositoryn där GitHub App inte uttryckligen installerats.

# 5. Målarkitektur

> Mobil/dator
>
> \|
>
> v
>
> React-webbapp
>
> \|
>
> v
>
> Quarkus API
>
> \|-- GitHub OAuth / webbsession
>
> \|-- GitHub App-installationstoken
>
> \|-- ZIP-lagring och säker uppackning
>
> \|-- Importplan och filklassificering
>
> \|-- Temporär Git-arbetskopia
>
> \|-- Commit, push och pull request
>
> \|-- Audit, status och retention
>
> \|
>
> +--\> PostgreSQL
>
> +--\> Temporär objekt-/fillagring
>
> +--\> GitHub API och Git-protokoll
>
> \|
>
> v
>
> GitHub repository
>
> \|
>
> v
>
> GitHub Actions

## 5.1 Rekommenderade backendmoduler

| **Modul**  | **Ansvar**                                                                                |
|------------|-------------------------------------------------------------------------------------------|
| auth       | GitHub OAuth, webb-sessioner, CSRF/state, logout och aktuell användare.                   |
| github     | GitHub App-installationer, repositoryn, brancher, commits, pull requests och checkstatus. |
| projects   | Sparade projektkopplingar och standardinställningar.                                      |
| uploads    | Streaminguppladdning, checksumma, metadata och retention.                                 |
| archives   | ZIP-inspektion, säker uppackning, filtrering och rotidentifiering.                        |
| importplan | Jämförelse, filklassificering, varningar, blockerare och godkännande.                     |
| workspace  | Temporär Git-klon, filapplicering, staging och cleanup.                                   |
| delivery   | Branch, commit, push, pull request och idempotens.                                        |
| workflows  | Länkar och senare status från Actions/checks.                                             |
| audit      | Spårbarhet för säkerhets- och supportändamål.                                             |

## 5.2 Rekommenderade frontendområden

- Login/callback och inloggad användarmeny.

- Projektlista och projektkonfiguration.

- Importguide i tydliga steg.

- ZIP-uppladdning med progress och mobil filväljare.

- Importöversikt med summering, filter och varningar.

- Godkännandesida med tydliga blockerare.

- Resultatsida med commit-, PR- och Actions-länkar.

- Importhistorik.

- Responsiv design med iPhone som obligatorisk testplattform.

# 6. Migreringsstrategi från zip-buildserver

Den äldre koden ska inventeras komponent för komponent. Återanvändning avgörs av beteende och testbarhet, inte av att behålla gamla klassnamn eller domänbegrepp.

| **Befintligt område**           | **Rekommenderad behandling**                                             |
|---------------------------------|--------------------------------------------------------------------------|
| Quarkus-applikationsskal        | Återanvänd och förenkla.                                                 |
| React/Vite-skal                 | Återanvänd; ersätt nuvarande session/run-flöde med projekt/import-flöde. |
| PostgreSQL/Flyway               | Återanvänd; lägg nya migreringar och behåll endast relevant historik.    |
| ArchiveValidationService        | Återanvänd som bas; komplettera säkerhetskontroller.                     |
| PackageStorageService           | Återanvänd som bas; byt terminologi till upload/source package.          |
| Retention och cleanup           | Återanvänd efter anpassning.                                             |
| Audit events                    | Återanvänd mönstret.                                                     |
| OpenAPI och REST-mönster        | Återanvänd.                                                              |
| Artifact/statuskomponenter      | Återanvänd visuella och tekniska mönster.                                |
| Docker worker och worker-images | Arkivera; använd inte i MVP.                                             |
| Verification plans              | Arkivera; GitHub Actions ersätter kommandoplanerna.                      |
| Run/command-result-domän        | Ersätt med import delivery och workflow status.                          |
| Statiskt bearer-token           | Behåll endast för framtida maskin-API; ersätt för webb med GitHub-login. |

# 7. Utvecklingsprinciper

- Gör en enda arkitekturell förändring per fas.

- Bibehåll byggbar huvudgren efter varje leverans.

- Skriv tester samtidigt som funktioner implementeras.

- Låt säkerhetskontroller vara separata, namngivna regler med egna tester.

- Använd idempotenta operationer för GitHub-skrivning.

- Frys jämförelsebasen till commit-SHA när importanalysen börjar.

- Skicka aldrig fullständiga GitHub-token till frontend.

- Spara inte GitHub App-installationstoken permanent.

- Kör aldrig ZIP-innehåll som programkod i backend.

- Logga identifierare och status, men inte OAuth-hemligheter eller ZIP-innehåll.

- Dokumentera alla manuella steg som krävs för lokal utveckling och GitHub App-konfiguration.

# 8. Fasplan

## Fas 0 – Inventering och verifierbar baslinje

Implementation:

- Packa upp zip-buildserver-main.zip i en ny arbetskatalog och ta bort \_\_MACOSX-metadata från arbetskopian.

- Läs README, AGENTS.md, befintlig funktionell specifikation, development plan, security model och operations.

- Inventera backendpaket, frontendrouting, databasversioner, tester och Dockerberoenden.

- Skapa docs/reuse-assessment.md med klassificering: reuse, adapt, replace, archive.

- Kör backend- och frontendtester. Om verktygswrappers saknas, lägg Maven Wrapper och dokumentera Node-version.

- Skapa GitHub Actions för tjänstens egen build/test om det saknas.

- Tagga eller dokumentera en legacy-baslinje innan destruktiv ombyggnad.

Kvalitetsgrind:

- Projektet kan byggas i en ren miljö.

- Befintliga testfel är dokumenterade.

- Återanvändningskartan är konkret på paket- och komponentnivå.

- Ingen produktfunktion har ännu ändrats utan baslinje.

## Fas 1 – Ny domän och applikationsskal

Implementation:

- Inför nya begrepp: Project, ImportSession, SourceUpload, ImportPlan, ImportPlanEntry och GitHubDelivery.

- Skapa nya Flyway-migreringar; undvik att återanvända gamla verifieringstabeller med missvisande namn.

- Skapa statusmaskiner och tydliga övergångar.

- Skapa API-skelett och frontendrouting för projektlista, projekt och ny import.

- Behåll gamla routes tillfälligt bakom feature flag eller ta bort dem efter att tester flyttats.

Kvalitetsgrind:

- En användare kan skapa ett lokalt projektobjekt utan GitHub-anslutning.

- Tom importsession kan skapas och läsas.

- Domänregler och statusövergångar har enhetstester.

- OpenAPI beskriver de nya resurserna.

## Fas 2 – GitHub-login och GitHub App

Implementation:

- Skapa en GitHub OAuth App eller använd GitHub App user authorization för login.

- Implementera authorization redirect, callback, state-validering och sessionscookie.

- Skapa säker server-side webbsession med HttpOnly, Secure och SameSite.

- Implementera GitHub App-autentisering och kortlivade installationstoken.

- Lista installationer och repositoryn som användaren och appen båda får använda.

- Skapa projektsida där repository, standardbranch och importinställningar sparas.

Kvalitetsgrind:

- Användaren kan logga in och ut.

- Ogiltigt eller återanvänt state blockeras.

- Frontend ser aldrig app-private key eller installationstoken.

- Användaren kan endast välja repositoryn som GitHub App har åtkomst till.

- Projektkonfiguration kan sparas och återöppnas.

## Fas 3 – Uppladdning och säker ZIP-inspektion

Implementation:

- Återanvänd streaminglagring och SHA-256 där det är lämpligt.

- Inför explicit content-length- och faktisk streaminggräns.

- Validera ZIP-signatur och ZIP-struktur.

- Blockera traversal, absoluta sökvägar, NUL, symlänkar, specialfiler, dubblettsökvägar och skiftlägeskollisioner.

- Begränsa komprimerad storlek, uppackad storlek, antal filer, filstorlek, sökvägslängd och kompressionskvot.

- Filtrera \_\_MACOSX, .DS_Store och konfigurerade ignorerade sökvägar.

- Identifiera om ZIP har en ensam projektrot och normalisera denna.

- Packa upp med en och samma säkra extractor som använder valideringsreglerna.

Kvalitetsgrind:

- ZIP kan laddas upp från iPhone och dator.

- Skadliga fixture-ZIP-filer avvisas.

- En legitim projekt-ZIP ger en deterministisk filinventering.

- Upload och arbetsyta städas enligt retention.

- ZIP-innehåll exekveras aldrig.

CI-baslinje före fas 4:

- En `.gitignore` skyddar hemligheter och utesluter genererade bygg-, test- och temporärfiler.
- Maven Wrapper låser backendens Maven-version.
- GitHub Actions kör struktur- och säkerhetskontroller, backendens fulla `verify` samt frontendens tester och produktionsbygge.
- Workflowen använder endast `contents: read` och körs vid push, pull request samt manuell dispatch.

## Fas 4 – Repositorysnapshot och importplan

Implementation:

- Hämta vald branch och lås dess aktuella commit-SHA som comparison base.

- Skapa en temporär shallow clone eller använd Git tree API; välj metod efter ett litet tekniskt spike.

- Jämför normaliserat ZIP-innehåll med bascommit.

- Beräkna added, modified, unchanged, ignored, blocked och would-delete.

- Blockera .github/\*\*, hemlighetsmisstänkta filer, för stora filer och alla borttagningar i MVP.

- Skapa stabila content hashes för att undvika felaktiga skillnader.

- Spara importplanen som immutable snapshot med base commit.

- Presentera summering och filnivå i frontend.

Kvalitetsgrind:

- Samma ZIP och base commit ger samma importplan.

- Repositoryändring efter analys ändrar inte den sparade planens bas.

- Användaren ser alla filer som faktiskt kommer att påverkas.

- Blockerade ändringar kan inte godkännas.

- Ingen GitHub-skrivning sker i denna fas.

## Fas 5 – Godkännande och Git-leverans

Implementation:

- Implementera godkännandekommando som kontrollerar planstatus och base commit.

- Skapa unik branch, exempelvis import/\<project-slug\>/\<timestamp\>-\<short-id\>.

- Applicera endast godkända added/modified-filer i temporär klon.

- Skapa en atomisk commit med ZIP-SHA, base SHA och importsession-ID i meddelandet.

- Pusha med GitHub App-installationstoken.

- Öppna pull request mot den konfigurerade målbranchen.

- Spara commit SHA, branch, PR-nummer och länkar.

- Implementera idempotens så att dubbelklick inte skapar flera brancher eller PR:er.

Kvalitetsgrind:

- En godkänd plan skapar exakt en commit och en PR.

- Main ändras inte direkt.

- Misslyckad push ger begriplig status och kan återupptas säkert.

- Committen motsvarar exakt den granskade planen.

- Alla temporära Git-credentials tas bort.

## Fas 6 – Actions-länkar och resultatsida

Implementation:

- Visa alltid länk till pull request och GitHub Actions/checks.

- Hämta grundläggande combined status eller check runs efter push.

- Pollning ska vara begränsad och stoppas när terminal status nås.

- Visa pending, success, failure, cancelled och unavailable.

- Visa länk till relevant run även om API-status inte kan hämtas.

- Återanvänd generella status- och artifact-komponenter där det är lämpligt.

Kvalitetsgrind:

- Användaren kan från appen hitta byggresultatet med ett tryck.

- Appen fungerar även när Actions-status är fördröjd eller otillgänglig.

- Polling orsakar inte obegränsad API-användning.

- PR-länk och commit-länk sparas permanent.

## Fas 7 – Mobil, säkerhet och driftsättning

Implementation:

- Testa hela flödet i Safari på iPhone med filer från Hämtade filer och iCloud Drive.

- Förbättra uppladdningsprogress, återhämtning och tydliga felmeddelanden.

- Inför CSRF-skydd, CSP, säker cookie-konfiguration och rate limiting.

- Granska loggar för token, filinnehåll och persondata.

- Dokumentera GitHub App permissions och webhook/callback URL:er.

- Skapa Docker Compose eller annan enkel driftmodell utan Docker socket.

- Inför health checks, backupstrategi för PostgreSQL och retentionjobb.

- Genomför hotmodell och säkerhetsregression.

Kvalitetsgrind:

- Flödet kan genomföras från telefon.

- Backendcontainern monterar inte Docker socket.

- GitHub-hemligheter kan roteras utan datamigrering.

- Drift- och återställningsinstruktioner är kompletta.

- Samtliga säkerhetsfixtures passerar.

## Fas 8 – Efter MVP: integrerade Actions-resultat

Implementation:

- Hämta workflow runs, jobs och relevanta loggfragment.

- Presentera status per check och artifactlänkar.

- Extrahera kondenserade fel för Maven, Gradle, npm/Vite, Pandoc och xcodebuild där möjligt.

- Tillåt manuell workflow_dispatch endast för uttryckligen tillåtna workflows.

- Bevara GitHub som källa för full logg och artifact.

Kvalitetsgrind:

- Appen ersätter inte GitHub utan ger en förenklad översikt.

- Felutdrag är begränsade, spårbara och länkar till full logg.

- Artifacts laddas inte genom modellen och behöver inte lagras permanent i appen.

# 9. Detaljerad domänmodell

| **Entitet**         | **Nyckelfält och ansvar**                                                                             |
|---------------------|-------------------------------------------------------------------------------------------------------|
| UserAccount         | GitHub user id, login, avatar URL, created/last-login. Ingen permanent OAuth-token om den inte krävs. |
| GitHubInstallation  | Installation id, account, permissionssnapshot och senaste synkning.                                   |
| Project             | Namn, installation, repository id, owner/name, default target branch, aktiv/inaktiv.                  |
| SourceUpload        | Originalnamn, storlek, SHA-256, storage key, validation status, retention deadline.                   |
| ImportSession       | Project, base branch, base SHA, status, upload, timestamps, initiator.                                |
| ImportPlan          | Immutable plan version, counts, approval status, warnings och policyversion.                          |
| ImportPlanEntry     | Path, change type, source hash, target hash, size, text/binary, policy result.                        |
| GitHubDelivery      | Import branch, commit SHA, PR number/url, delivery status, idempotency key.                           |
| WorkflowObservation | Check state, conclusion, GitHub URL, last refresh; endast cache/sammanfattning.                       |
| AuditEvent          | Actor, event type, target id, timestamp och säker metadata.                                           |

# 10. Statusmodeller

> ImportSession:
>
> CREATED
>
> -\> UPLOADING
>
> -\> INSPECTING
>
> -\> PLAN_READY
>
> -\> APPROVED
>
> -\> DELIVERING
>
> -\> PULL_REQUEST_CREATED
>
> -\> COMPLETED
>
> Terminala felstatusar:
>
> UPLOAD_FAILED
>
> INSPECTION_FAILED
>
> BLOCKED
>
> DELIVERY_FAILED
>
> CANCELLED
>
> EXPIRED

Statusövergångar ska centraliseras i domän- eller applikationslager. REST-resurser får inte direkt sätta godtyckliga statusvärden. Varje övergång ska ha tester för tillåten, förbjuden och idempotent repetition.

# 11. API-förslag

| **Metod och sökväg**                            | **Syfte**                               |
|-------------------------------------------------|-----------------------------------------|
| GET /api/auth/me                                | Aktuell inloggad GitHub-användare.      |
| GET /api/auth/github/login                      | Startar GitHub-login.                   |
| GET /api/auth/github/callback                   | Callback, state-validering och session. |
| POST /api/auth/logout                           | Avslutar webbsession.                   |
| GET /api/github/installations                   | Lista tillgängliga installationer.      |
| GET /api/github/installations/{id}/repositories | Lista valbara repositoryn.              |
| GET /api/github/repositories/{id}/branches      | Lista brancher.                         |
| GET /api/projects                               | Lista sparade projekt.                  |
| POST /api/projects                              | Skapa projektkoppling.                  |
| GET /api/projects/{id}                          | Hämta projekt.                          |
| PATCH /api/projects/{id}                        | Ändra projektinställningar.             |
| POST /api/projects/{id}/imports                 | Skapa importsession.                    |
| PUT /api/imports/{id}/upload                    | Strömma ZIP.                            |
| POST /api/imports/{id}/inspect                  | Validera, packa upp och skapa plan.     |
| GET /api/imports/{id}/plan                      | Hämta summering och filer.              |
| POST /api/imports/{id}/approve                  | Godkänn exakt sparad plan.              |
| POST /api/imports/{id}/deliver                  | Skapa branch, commit och PR.            |
| GET /api/imports/{id}                           | Hämta status och länkar.                |
| GET /api/imports/{id}/checks                    | Grundläggande Actions/checkstatus.      |

API:t ska använda problem+json eller ett konsekvent felkontrakt med maskinläsbar kod, användarvänligt meddelande, correlation id och valfria fältdetaljer.

# 12. GitHub-autentisering och behörigheter

Den slutliga konfigurationen måste verifieras mot aktuell GitHub-dokumentation vid implementation. Målet är minsta möjliga behörighet.

- Metadata: read.

- Contents: read/write för att skapa branch och commit.

- Pull requests: read/write.

- Actions eller Checks: read för statusvisning.

- Workflows: ingen write-behörighet i MVP.

- Appen installeras endast på explicit valda repositoryn.

- Installationstoken skapas vid behov och hålls endast kortlivat server-side.

- OAuth state ska vara engångsanvänd, kortlivad och knuten till webbsession.

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Säkerhetsbeslut<br />
</strong>Det är frestande att använda användarens OAuth-token för allt. Föredra i stället att separera användaridentitet från repositoryautomation: GitHub-login verifierar användaren och GitHub App-installationen ger uttrycklig repositoryåtkomst.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 13. ZIP- och importpolicy

| **Regel**                  | **MVP-beteende**                                                    |
|----------------------------|---------------------------------------------------------------------|
| Max ZIP                    | Konfigurerbar, initialt 100 MB.                                     |
| Max uppackat               | Konfigurerbar, initialt 500 MB.                                     |
| Max filer                  | Konfigurerbar, exempelvis 20 000.                                   |
| Enskild fil                | Varning/blockering före GitHubs gränser; initialt exempelvis 50 MB. |
| Symlänkar/specialfiler     | Blockeras.                                                          |
| Traversal/absoluta paths   | Blockeras.                                                          |
| Dubblett-/case-kollisioner | Blockeras.                                                          |
| \_\_MACOSX/.DS_Store       | Ignoreras.                                                          |
| .git/\*\*                  | Ignoreras/blockeras; repositorymetadata importeras aldrig.          |
| .github/\*\*               | Blockeras i MVP.                                                    |
| Secrets                    | Blockera kända privata nycklar och hög-risk-filer; varna för .env.  |
| Borttagningar              | Blockeras i MVP.                                                    |
| Binärfiler                 | Tillåts inom storleks- och policygränser; ingen textdiff.           |
| Projektrot                 | En ensam wrapperkatalog tas bort vid normalisering.                 |

# 14. Jämförelsealgoritm

1.  Lås repository, branch och base commit-SHA när inspektionen startar.

2.  Skapa en normaliserad ZIP-inventering med relativ sökväg, storlek, typ och SHA-256.

3.  Skapa en inventering av Git-trädet på exakt base commit.

4.  Applicera ignore- och protected-policy före klassificering.

5.  För varje ZIP-fil: saknas i Git = ADDED; samma hash = UNCHANGED; annan hash = MODIFIED.

6.  För varje Git-fil som inte finns i ZIP: WOULD_DELETE. Blockera i MVP, men skriv inte filen till planen som godkännbar.

7.  Markera skyddade sökvägar som BLOCKED oavsett innehåll.

8.  Spara planen immutable tillsammans med policyversion och base SHA.

9.  Vid leverans, verifiera att planen inte ändrats och att målbranchens policy fortfarande tillåter importen.

MVP behöver inte skapa en full radbaserad diff. Filhashar och storlek räcker för korrekt klassificering. Textdiff kan läggas till efter att importkedjan är stabil.

# 15. Git-strategi

- Använd shallow clone av exakt base branch/commit i första implementationen, eftersom vanlig Git ger en naturlig atomisk commit och lokal diffkontroll.

- Använd en ny temporär arbetskatalog per delivery.

- Konfigurera teknisk commitidentitet, exempelvis Project Importer Bot.

- Använd HTTPS med installationstoken eller annan säker GitHub App-metod.

- Skriv aldrig credentials i remote URL som loggas.

- Skapa endast en commit per import i MVP.

- Branchens namn ska vara unikt, URL-säkert och inte innehålla användarstyrda path-separatorer.

- Commitmeddelandet ska innehålla projekt, originalfilnamn, ZIP-SHA och importsession-ID.

- Öppna pull request med sammanfattning av added/modified/blocked/ignored och länk tillbaka till appen om den är tillgänglig.

- Använd idempotency key knuten till importsession och godkänd plan.

# 16. Frontendflöden

## 16.1 Första användning

10. Användaren väljer Logga in med GitHub.

11. Efter callback visas tillgängliga installationer och repositoryn.

12. Användaren skapar ett projekt och väljer standardbranch.

13. Projektet visas på startsidan.

## 16.2 Ny import

14. Öppna projektet och välj Ny import.

15. Välj branch; standardbranch är förvald.

16. Välj ZIP från Filer/iCloud Drive eller dator.

17. Visa uppladdningsprogress.

18. Inspektera automatiskt efter uppladdning eller via tydlig knapp.

19. Visa summering och grupperade filer.

20. Förklara blockerare innan godkännandeknappen.

21. Godkänn och skapa pull request.

22. Visa resultat, status och GitHub-länkar.

## 16.3 Mobilkrav

- Primära knappar ska vara stora och nåbara.

- Tabeller ska ersättas eller kompletteras med kort/listor på smal skärm.

- Filnamn får brytas men hela sökvägen ska kunna öppnas/kopieras.

- Långa fillistor ska filtreras och virtualiseras eller sidindelas.

- Uppladdningen ska inte förlora användarens plats vid skärmlås eller återgång där webbläsaren tillåter det.

- Användaren ska alltid kunna öppna PR och Actions i GitHub-appen eller webbläsaren.

# 17. Teststrategi

## 17.1 Backend

- Enhetstester för pathnormalisering, statusövergångar, policyregler, hashklassificering och branch naming.

- ZIP-fixtures för traversal, absolut path, symlink, dubblett, case collision, zip bomb, för många filer, för stor fil och legitim wrapperkatalog.

- Repository-fixtures med added, modified, unchanged och would-delete.

- GitHub-klienttester med WireMock eller motsvarande.

- Integrationstester med PostgreSQL/Testcontainers.

- Git-leveranstest mot lokal bare repository för branch, commit och idempotens.

- API-tester för authkrav, ägarskap och felkontrakt.

- Retentionstester.

## 17.2 Frontend

- Komponenttester för upload, policyvarningar, status och filgrupper.

- Mock Service Worker eller motsvarande för API-scenarier.

- Routing- och authguardtester.

- E2E med Playwright för loginmock, projekt, upload, plan och delivery.

- Responsiva tester på iPhone-liknande viewport.

- Manuellt verkligt test i Safari på iPhone före MVP-godkännande.

## 17.3 GitHub end-to-end

- Använd ett separat privat testrepository.

- Installera GitHub App endast på testrepositoryt under utveckling.

- Skapa ett testworkflow som bygger en enkel artifact.

- Verifiera att en import skapar rätt branch, commit, PR och Actions-run.

- Verifiera att dubbel leverans inte skapar duplikat.

- Verifiera beteende när branch protection eller permissions stoppar push.

# 18. CI/CD för tjänsten

- Backend compile/test.

- Frontend npm ci, lint, test och build.

- Dependency scanning.

- Container build utan Docker socket.

- Säkerhetstest av ZIP-fixtures.

- OpenAPI-kontrakt exporteras och frontendtyper hålls synkade eller verifieras.

- Databasmigreringar verifieras mot tom och uppgraderad databas.

- En release får endast skapas när båda delarna och integrationstesterna är gröna.

# 19. Dokumentation som ska underhållas

| **Dokument**                     | **Innehåll**                                                        |
|----------------------------------|---------------------------------------------------------------------|
| README.md                        | Produktöversikt, snabbstart och länk till övriga dokument.          |
| docs/functional-specification.md | Markdownversion av styrande funktionsspecifikation.                 |
| docs/development-plan.md         | Denna plan, uppdaterad med genomförda beslut.                       |
| docs/architecture.md             | Komponenter, dataflöden och integrationsgränser.                    |
| docs/security-model.md           | Hotmodell, GitHub permissions, ZIP-policy och secrets.              |
| docs/github-app-setup.md         | Steg för registrering, callback, keys och installation.             |
| docs/operations.md               | Drift, backup, retention, loggning och felsökning.                  |
| docs/api-overview.md             | API-resurser och felkontrakt.                                       |
| docs/agent-progress.md           | Genomförd fas, nästa steg, kända problem och verifieringsresultat.  |
| AGENTS.md                        | Bygg/testkommandon, arkitekturregler och filer som inte ska ändras. |

# 20. Leveransdisciplin i den nya chatten

Den nya chatten ska arbeta revisionsvis. Efter varje fas ska följande göras innan nästa fas påbörjas:

23. Uppdatera implementationen.

24. Kör relevanta backend- och frontendtester.

25. Kör statisk kontroll eller build.

26. Uppdatera dokumentation och agent-progress.

27. Inventera git diff och säkerställ att inga orelaterade filer ändrats.

28. Skapa en ny revisionsmärkt ZIP av hela projektet.

29. Rapportera genomförda förändringar, verifiering, kända begränsningar och rekommenderat nästa steg.

<table>
<colgroup>
<col style="width: 100%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Ingen lång kedja utan kontrollpunkt<br />
</strong>Genomför inte flera stora faser i samma odelade leverans. Det är bättre att leverera en fungerande GitHub-login eller en korrekt ZIP-inspektör än en halvfärdig helprodukt som är svår att verifiera.</th>
</tr>
</thead>
<tbody>
</tbody>
</table>

# 21. Rekommenderad första prompt i den nya chatten

> Jag bifogar:
>
> 1\. zip-buildserver-main.zip
>
> 2\. projektimporterare-funktionell-specifikation-v1.0.docx
>
> 3\. projektimporterare-development-plan-v1.0.docx
>
> Packa upp och inventera projektet. Funktionell specifikation är styrande för vad
>
> produkten ska göra och development plan är styrande för implementeringsordning
>
> och kvalitetsgrindar. Den äldre zip-buildserver-koden är inspirations- och
>
> återanvändningsunderlag; arkitekturen behöver inte bevaras.
>
> Börja med Fas 0:
>
> \- verifiera projektets struktur och byggbarhet,
>
> \- skapa en konkret reuse/adapt/replace/archive-karta på paket- och komponentnivå,
>
> \- identifiera vilka tester och wrappers som saknas,
>
> \- föreslå den exakta första kodrevisionen.
>
> Gör därefter den första rimliga implementationen om den kan genomföras och
>
> verifieras säkert i samma chat. Uppdatera dokumentationen och ge mig en komplett
>
> revisionsmärkt projekt-ZIP. Fråga inte om sådant som redan är beslutat i
>
> specifikationen eller development plan.

# 22. Definition of Done för MVP

- Användaren kan logga in med GitHub.

- Användaren kan skapa ett projekt knutet till ett repository som GitHub App har tillgång till.

- Användaren kan välja målbranch och ladda upp ZIP från iPhone.

- Skadliga och resurskrävande ZIP-filer stoppas.

- Appen visar en korrekt, reproducerbar importplan mot ett fast commit-SHA.

- Added och modified visas; delete och .github/\*\* blockeras.

- Inget skrivs innan uttryckligt godkännande.

- Godkänd plan skapar en unik branch, exakt en commit och en pull request.

- Appen visar fungerande länkar till PR, commit och Actions/checks.

- Projektkod körs inte lokalt i tjänsten.

- Backend och frontend har automatiserad CI och relevanta tester.

- Tjänsten fungerar i en dokumenterad driftmiljö utan Docker socket.

- Säkerhetsmodell, GitHub App-setup, drift och återställning är dokumenterade.

- Hela flödet är manuellt verifierat på iPhone.

# 23. Riskregister

| **Risk**                                       | **Motåtgärd**                                                                                          |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| GitHub OAuth/App blir onödigt komplex          | Implementera ett litet vertikalt spike tidigt och dokumentera exakt authmodell innan övrig GitHub-kod. |
| ZIP skriver över fel version                   | Lås base SHA och spara immutable plan; blockera stale delivery eller skapa ny analys.                  |
| Ofullständig ZIP skulle ta bort filer          | Blockera alla borttagningar i MVP.                                                                     |
| Workflowfiler manipuleras                      | Blockera .github/\*\* i MVP och ge inte Workflows write.                                               |
| Token eller app key läcker                     | Server-side secrets, redigerade loggar, kortlivade token och inga credentials i remote URL/logg.       |
| GitHub rate limits                             | Cachea metadata, begränsa polling och använd conditional requests där lämpligt.                        |
| Stora repositoryn gör clone långsam            | Shallow clone och senare utvärdering av Git tree API; inför storleksgränser.                           |
| Mobil uppladdning avbryts                      | Tydlig retry; senare resumable upload om verkligt behov finns.                                         |
| Gamla buildserver-domänen skapar teknisk skuld | Nya domännamn och migreringskarta; arkivera worker/run-kod tidigt.                                     |
| Actions-resultat blir svårt att normalisera    | MVP länkar till GitHub; integrerad tolkning är separat efter-MVP-fas.                                  |

# 24. Slutlig rekommendation

Implementera tjänsten som en ny produktversion ovanpå de återanvändbara delarna av zip-buildserver. Behåll teknikstacken där den fungerar, men låt funktionell specifikation och den nya domänmodellen styra. Det viktigaste vertikala flödet är: GitHub-login → projekt → ZIP → säker importplan → godkännande → branch/commit/PR → Actions-länk. Allt annat bör prioriteras efter att detta flöde fungerar och är testat.

# 25. Promptvänlig genomförandeordning

Faserna i kapitel 8 är övergripande milstolpar och kvalitetsgrindar. De ska inte generellt tolkas som att en hel fas måste genomföras i en enda prompt.

För det praktiska genomförandet används `implementation-steps.md`. Där är varje fas nedbruten i mindre numrerade steg. Ett sådant steg är normalt avsett att kunna genomföras i en separat prompt med följande leveransdisciplin:

- avgränsad kodförändring,
- relevanta tester,
- körd build och verifiering,
- uppdaterad dokumentation och status,
- ny revisionsnumrerad projekt-ZIP.

Fas 2, 3, 4, 5 och 7 är uttryckligen för stora och riskfyllda för att genomföras som en enda normal prompt. Även övriga faser bör följa den stegvisa planen för att förenkla granskning, felsökning och återstart i en ny chatt.
