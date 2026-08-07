| **Version** | 1.1                                                                           |
|-------------|-------------------------------------------------------------------------------|
| **Datum**   | 7 augusti 2026                                                                |
| **Status**  | Uppdaterad målbild efter MVP-validering                                              |
| **Syfte**   | Definiera den nya målprodukten oberoende av den äldre prototypens utformning. |

**Rekommenderad målbild: GitHub är beständig källa; appen importerar och granskar ZIP-förändringar; GitHub Actions bygger och verifierar.**

# Dokumentöversikt

- 1\. Syfte och produktvision

- 2\. Omfattning

- 3\. Aktörer och behörigheter

- 4\. Centrala användarflöden

- 5\. Funktionella krav

- 6\. Regler för ZIP-import och jämförelse

- 7\. GitHub-integration

- 8\. GitHub Actions-integration

- 9\. Användargränssnitt

- 10\. Begreppsmässig datamodell

- 11\. API-översikt

- 12\. Säkerhet och icke-funktionella krav

- 13\. Felhantering och spårbarhet

- 14\. MVP och leveransfaser

- 15\. Acceptanskriterier

- 16\. Öppna beslut och rekommendationer

# 1. Syfte och produktvision

Tjänsten ska göra det möjligt att föra över ett komplett projektpaket från en ZIP-fil till ett konfigurerat GitHub-repository på ett kontrollerat, granskningsbart och mobilvänligt sätt. Applikationen analyserar ZIP-filen, jämför innehållet med en vald branch, visar de föreslagna förändringarna och skriver det godkända resultatet till GitHub. Befintliga GitHub Actions bygger, testar, paketerar eller publicerar därefter projektet.

Det primära användningsfallet är ett AI-assisterat arbetsflöde där ChatGPT eller ett annat verktyg skapar en uppdaterad projekt-ZIP. Användaren ska kunna ladda upp ZIP-filen från en telefon utan att manuellt packa upp filer, kopiera innehåll till Git, köra lokala byggen eller transportera byggloggar för hand.

## 1.1 Produktprinciper

- GitHub är den permanenta källan för projektfiler och historik.

- Applikationen skriver aldrig över repositoryinnehåll utan tydlig granskning och godkännande.

- Användaren ser en översikt av förändringarna innan någon commit skapas.

- Bygg- och verifieringslogik ligger i repositoryts GitHub Actions-workflows.

- En enkel version kan länka till GitHub; rikare presentation i appen kan tillkomma senare.

- Hela kärnflödet ska fungera i mobil webbläsare och med iOS Filer/iCloud Drive.

# 2. Omfattning

## 2.1 Ingår

- Inloggning med GitHub och koppling till användarens konto.

- Konfiguration av ett eller flera projekt knutna till GitHub-repositoryn.

- Val av repository, standardbranch och importpolicy per projekt.

- Uppladdning, säker analys och uppackning av ZIP.

- Jämförelse mellan ZIP-innehåll och vald GitHub-branch.

- Presentation av tillagda, ändrade, borttagna, oförändrade, ignorerade och blockerade filer.

- Godkännande innan commit eller pull request skapas.

- Skapande av importbranch, commit och normalt en pull request.

- Länkar till commit, pull request och Actions-resultat.

- Valfri senare presentation av workflowstatus, kontroller, kondenserade fel och artifactlänkar i appen.

- Revisions- och auditspår för importer och GitHub-operationer.

## 2.2 Ingår inte i första versionen

- Körning av godtycklig uppladdad projektkod på applikationsservern.

- Ersättning av GitHub Actions som byggmiljö.

- Automatisk merge till skyddad standardbranch utan användarbeslut.

- Fullständig webbaserad IDE.

- Allmän Git-hosting eller repositoryadministration.

- Permanent lagring av byggartifacts utanför GitHub.

- AI-generering inne i tjänsten.

# 3. Aktörer och behörigheter

| **ID**     | **Krav**       | **Beskrivning**                                                                                             | **Prioritet** |
|------------|----------------|-------------------------------------------------------------------------------------------------------------|---------------|
| **ACT-01** | Användare      | Autentiserad person som konfigurerar projekt, laddar upp ZIP, granskar förändringar och godkänner importer. | Måste         |
| **ACT-02** | GitHub-konto   | Extern identitet för autentisering.                                                                         | Måste         |
| **ACT-03** | GitHub App     | Rekommenderad integration med begränsade repositorybehörigheter.                                            | Måste         |
| **ACT-04** | GitHub Actions | Bygg- och verifieringsplattform som triggas av push, pull request eller manuell körning.                    | Måste         |
| **ACT-05** | Administratör  | Valfri operatör för konfiguration, gränser och support.                                                     | Bör           |

Rekommenderad modell: samma GitHub App används både för användarautentisering och repositoryåtkomst. GitHub App user authorization identifierar användaren och ger åtkomst till användarens installationer; App ID och private key används server-side för kortlivade installationstoken vid repositoryoperationer. Token exponeras aldrig till frontend.

# 4. Centrala användarflöden

## 4.1 Första konfigurationen

1.  Användaren väljer ”Logga in med GitHub”.

2.  GitHub autentiserar användaren och återför denne till appen.

3.  Användaren installerar eller väljer tjänstens GitHub App för önskade repositoryn.

4.  Användaren skapar ett projekt och väljer repository och standardbranch.

5.  Appen verifierar åtkomst och sparar nödvändiga identifierare och inställningar.

## 4.2 Importera projekt-ZIP

6.  Användaren öppnar ett konfigurerat projekt.

7.  Om projektet saknar ett aktivt arbete utgår importen från projektets standardbranch. Om ett arbete redan pågår används senaste commit på samma arbetsbranch automatiskt.

8.  Användaren väljer ZIP-fil från enhetens filväljare och kan ange en alternativ Git-author om förändringarna skapats av någon annan. Committer är alltid den inloggade användaren.

9.  Appen laddar upp, validerar och packar säkert upp filen i temporär lagring.

10. Innehållet jämförs med ett oföränderligt commit-SHA för den aktuella basen.

11. Användaren granskar förändringarna i ett hierarkiskt filträd med status för nya, ändrade, borttagna, ignorerade och blockerade sökvägar.

12. Vanliga förändringar är markerade för inkludering som standard. Användaren kan avmarkera en fil eller en hel katalog; avmarkering av en katalog exkluderar alla dess valbara underliggande poster. Policyblockerade poster hanteras enligt sin blockerartyp.

13. Överstyrbara blockerare, exempelvis `.github/**` eller borttagningar enligt policy, är exkluderade som standard och kräver uttryckligt override-godkännande för att kunna inkluderas. Hårt blockerade poster, exempelvis `.git/**`, kan inte inkluderas men hindrar inte att övriga valda förändringar godkänns.

14. Appen skapar en immutable godkänd selection som binder plan-SHA, base commit, valda/exkluderade sökvägar och eventuella overrides.

15. Appen skapar exakt en commit av den godkända selectionen på projektets aktiva arbetsbranch.

16. Resultatsidan visar commit och checks. Användaren kan därefter ladda upp nästa ZIP till samma arbete eller markera arbetet som klart.

17. När arbetet är klart skapar appen en pull request från arbetsbranchen till projektets standardbranch.

## 4.3 Granska byggresultat

16. MVP visar länkar till commit, pull request och relevanta Actions/checks i GitHub.

17. En senare version kan visa köad, pågående, lyckad eller misslyckad status direkt i appen.

18. Appen kan visa kondenserade fel och länkar till fullständiga loggar och artifacts.

19. GitHub förblir den auktoritativa detaljvyn.

# 5. Funktionella krav

## 5.1 Autentisering och projektkonfiguration

| **ID**     | **Krav**          | **Beskrivning**                                                                                   | **Prioritet** |
|------------|-------------------|---------------------------------------------------------------------------------------------------|---------------|
| **FR-A01** | GitHub-inloggning | Användaren ska kunna logga in och ut med GitHub.                                                  | Måste         |
| **FR-A02** | Säker session     | Webbsessionen ska vara säker och GitHub-token får inte exponeras för webbläsarens JavaScript.     | Måste         |
| **FR-A03** | Repositorylista   | Appen ska lista repositoryn som är åtkomliga via GitHub App-installationen.                       | Måste         |
| **FR-A04** | Skapa projekt     | Användaren ska kunna skapa ett namngivet projekt bundet till ett repository.                      | Måste         |
| **FR-A05** | Standardbranch    | Projektet ska lagra standardbranch för jämförelse och mål.                                        | Måste         |
| **FR-A06** | Importläge        | Projektet ska ange om import alltid skapar PR eller får committa direkt.                          | Måste         |
| **FR-A07** | Sökvägspolicy     | Projektet bör kunna definiera hanterade, skyddade, ignorerade och genererade sökvägar.            | Bör           |
| **FR-A08** | Förvaltning       | Projektkonfiguration ska kunna ändras, inaktiveras och tas bort utan att GitHub-innehåll raderas. | Måste         |

## 5.2 Uppladdning och arkivanalys

| **ID**     | **Krav**           | **Beskrivning**                                                                                 | **Prioritet** |
|------------|--------------------|-------------------------------------------------------------------------------------------------|---------------|
| **FR-U01** | Filval             | ZIP ska kunna väljas från dator och mobil filväljare.                                           | Måste         |
| **FR-U02** | Uppladdningsstatus | Appen bör visa förlopp och tillåta avbrott innan bearbetning.                                   | Bör           |
| **FR-U03** | Arkivgränser       | Gränser ska finnas för komprimerad storlek, uppackad storlek, filantal, sökväg och enskild fil. | Måste         |
| **FR-U04** | Säker uppackning   | Path traversal, absoluta sökvägar, symlink-flykt och specialfiler ska förhindras.               | Måste         |
| **FR-U05** | Brusfilter         | \_\_MACOSX, .DS_Store och liknande bör ignoreras eller flaggas.                                 | Bör           |
| **FR-U06** | Checksumma         | SHA-256 ska beräknas och sparas för ZIP-filen.                                                  | Måste         |
| **FR-U07** | Projektrot         | En ensam omslutande katalog bör upptäckas och normaliseras.                                     | Bör           |
| **FR-U08** | Manifest           | Ett valfritt importmanifest i ZIP bör kunna läsas.                                              | Bör           |

## 5.3 Jämförelse och granskning

| **ID**     | **Krav**           | **Beskrivning**                                                                                       | **Prioritet** |
|------------|--------------------|-------------------------------------------------------------------------------------------------------|---------------|
| **FR-C01** | Branchsnapshot       | Jämförelse ska ske mot ett commit-SHA som lösts från aktuell arbetsbas.                                            | Måste         |
| **FR-C02** | Förändringsklasser   | Filer klassas som tillagda, ändrade, borttagna, oförändrade, ignorerade, blockerade eller i konflikt.             | Måste         |
| **FR-C03** | Sammanfattning       | Antal, storlekar och varningar ska visas före godkännande.                                                        | Måste         |
| **FR-C04** | Filträd              | Påverkade sökvägar ska visas hierarkiskt som kataloger och filer, med status per fil och aggregerad status per katalog. | Måste         |
| **FR-C05** | Textdiff             | Radbaserad diff bör visas för textfiler.                                                                          | Bör           |
| **FR-C06** | Binärfiler           | Binärfiler markeras som tillagda, ersatta, borttagna eller oförändrade utan textdiff.                             | Måste         |
| **FR-C07** | Borttagningsskydd    | Borttagningar ska vara tydligt markerade, exkluderade som standard och kräva uttryckligt override för inkludering. | Måste         |
| **FR-C08** | Skyddade sökvägar    | Policy ska skilja mellan hårt blockerade och överstyrbara blockerade sökvägar.                                    | Måste         |
| **FR-C09** | Hemlighetsvarning    | Troliga credentials och privata nycklar ska tydligt flaggas och klassas enligt beslutad blockerarpolicy.          | Bör           |
| **FR-C10** | Inaktuell bas        | Arbetsbasens SHA ska kontrolleras igen före skrivning.                                                            | Måste         |
| **FR-C11** | Filurval             | Varje valbar förändring ska kunna inkluderas eller exkluderas före godkännande.                                   | Måste         |
| **FR-C12** | Katalogurval         | Val av katalog ska styra alla valbara underliggande poster; delvis valda kataloger ska visas med indeterminate-status. | Måste      |
| **FR-C13** | Standardurval        | Vanliga nya/ändrade filer är valda som standard; blockerade poster följer sin policy och kan vara exkluderade.   | Måste         |
| **FR-C14** | Blockerarnivåer      | `HARD_BLOCKED` kan aldrig inkluderas; `OVERRIDABLE_BLOCKED` kan endast inkluderas efter explicit override.        | Måste         |
| **FR-C15** | Immutable selection  | Godkännandet ska binda exakt valda/exkluderade paths, overrides, plan-digest och base SHA i en immutable selection. | Måste      |

## 5.4 Commit och pull request

| **ID**     | **Krav**         | **Beskrivning**                                                          | **Prioritet** |
|------------|------------------|--------------------------------------------------------------------------|---------------|
| **FR-G01** | Godkännandespärr | Ingen GitHub-skrivning får ske före uttryckligt godkännande av exakt selection.                  | Måste         |
| **FR-G02** | Arbetsbranch      | Projektet ska ha högst ett aktivt arbete; första importen skapar arbetsbranch och senare importer fortsätter på samma branch. | Måste |
| **FR-G03** | Atomisk commit    | Varje import ska skapa en commit som exakt motsvarar den godkända selectionen.                    | Måste         |
| **FR-G04** | Metadata          | Commitmeddelandet bör referera projekt, import-ID, ZIP-namn och SHA-256.                          | Bör           |
| **FR-G05** | Push              | Arbetsbranchen ska pushas med GitHub App-behörighet utan force.                                  | Måste         |
| **FR-G06** | Pull request      | När användaren avslutar arbetet ska appen kunna skapa en pull request från arbetsbranch till målbranch. | Måste    |
| **FR-G07** | Author/committer  | Author är normalt inloggad användare men kan anges per import; committer är alltid inloggad användare. | Måste     |
| **FR-G08** | Resultatlänkar    | Work-branch-, commit- och PR-länkar ska sparas och visas.                                         | Måste         |
| **FR-G09** | Idempotens        | Retry får inte oavsiktligt skapa dubbla commits eller PR:er.                                      | Måste         |
| **FR-G10** | Exakt staged diff | Före commit ska staged paths och innehåll verifieras mot den immutable godkända selectionen.      | Måste         |

## 5.5 Actions och byggresultat

| **ID**     | **Krav**           | **Beskrivning**                                                                   | **Prioritet** |
|------------|--------------------|-----------------------------------------------------------------------------------|---------------|
| **FR-W01** | Automatisk trigger | Repositoryworkflows som triggas av push eller PR ska användas där det är möjligt. | Måste         |
| **FR-W02** | GitHub-länk        | Resultatsidan ska alltid länka till relevanta Actions eller checks.               | Måste         |
| **FR-W03** | Status i appen     | Appen bör kunna visa check- och workflowstatus.                                   | Bör           |
| **FR-W04** | Felsammanfattning  | Appen kan visa kondenserade bygg- och testfel.                                    | Kan           |
| **FR-W05** | Artifacts          | Appen bör visa länkar till Actions-artifacts när de är åtkomliga.                 | Bör           |
| **FR-W06** | Manuell dispatch   | Appen kan starta konfigurerad workflow_dispatch efter import.                     | Kan           |
| **FR-W07** | Omkörning          | Appen kan erbjuda omkörning där GitHub-behörighet tillåter.                       | Kan           |

# 6. Regler för ZIP-import och jämförelse

## 6.1 Rekommenderad sökvägs- och blockerarpolicy

Importhanteringen skiljer på arkivfel som gör hela ZIP-filen ogiltig och policyklassificerade poster som kan hanteras i granskningsvyn.

### Arkivfel som avvisar hela ZIP-filen

Path traversal, absoluta sökvägar, NUL, otillåtna symlänkar/specialfiler, resursgränser och ZIP-bombskydd hanteras före importplanen. Dessa kan inte överstyras i granskningsvyn.

### Hårt blockerade poster

`HARD_BLOCKED` visas i filträdet men kan aldrig markeras för commit. Att en sådan post finns ska inte i sig hindra godkännande av övriga valda förändringar.

- `.git/**` är alltid `HARD_BLOCKED`, exkluderad som standard och inte valbar.
- Detta är att föredra framför att tyst ignorera `.git/**`, eftersom användaren då ser varför innehållet inte kommer med.

### Överstyrbara blockerare

`OVERRIDABLE_BLOCKED` är exkluderade som standard men kan tas med efter ett explicit, auditerat godkännande. Typiska exempel är:

- `.github/**`, inklusive workflows,
- borttagningar,
- policydefinierade stora filer inom absoluta säkerhetsgränser,
- vissa secret-fynd beroende på säkerhetsklassning.

Högriskfynd får konfigureras som `HARD_BLOCKED` om de inte ska kunna överstyras.

### Vanliga valbara poster

`ADDED` och `MODIFIED` är markerade som standard. `WOULD_DELETE` är synligt men exkluderat tills användaren explicit tillåter borttagningen. `UNCHANGED` och transportbrus behöver normalt inte vara valbara.

## 6.2 Hierarkiskt filurval och borttagningssemantik

Granskningsvyn representerar förändringarna som ett träd, exempelvis:

> src/  — delvis vald  
> └─ main/  — vald  
> &nbsp;&nbsp;&nbsp;└─ xxx/  — vald  
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├─ Game.java — MODIFIED  
> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;└─ Board.java — ADDED

Regler:

- En fil kan markeras eller avmarkeras individuellt om policyn gör den valbar.
- Avmarkering av en katalog avmarkerar alla valbara poster i dess subtree.
- Markering av en katalog markerar alla valbara vanliga poster; överstyrbara blockerare kräver fortfarande explicit override och hårt blockerade poster förblir exkluderade.
- Om endast en del av en katalog är vald visas katalogen i ett indeterminate/partiellt tillstånd.
- Katalogstatus är aggregerad och bör visa antal nya, ändrade, borttagna och blockerade poster när underträdet innehåller blandade typer.
- En import med tom selection kan inte godkännas.
- Borttagningar genomförs endast när motsvarande `WOULD_DELETE`-post är vald och dess override är godkänd.
- Skyddade sökvägar kan inte raderas utan den blockerarpolicy som gäller för sökvägen.

Den ursprungliga `ImportPlan` förblir immutable och beskriver hela skillnaden mellan ZIP och base commit. Användarens val sparas separat som en immutable `ApprovedSelection` med selected paths, excluded paths, overrides, plan-digest, base SHA, approver och tidpunkt.

## 6.3 Branchkonsistens

- För första importen i ett arbete löses projektets standardbranch till commit-SHA före jämförelse.

- För senare importer löses den aktiva arbetsbranchens senaste commit och används som base SHA.

- Importplan och selection binds till exakt denna commit.

- Före push kontrolleras att arbetsbranchen fortfarande pekar på godkänd base SHA.

- Vid förändring krävs ny jämförelse och ny selection/godkännande.

- Ett eventuellt ZIP-manifests `base_commit` jämförs med aktuell arbetsbas och ett gammalt paket varnas eller blockeras enligt policy.

# 7. GitHub-integration

## 7.1 Rekommenderad autentiseringsmodell

GitHub Appens user authorization används för att fastställa användarens identitet och lista de installationer användaren kan använda. Repositoryoperationer utförs server-side med kortlivade installationstoken för samma GitHub App och begränsas till utvalda repositoryn och minsta nödvändiga behörigheter.

## 7.2 Rekommenderade behörigheter

| **ID**     | **Krav**      | **Beskrivning**                                                    | **Prioritet** |
|------------|---------------|--------------------------------------------------------------------|---------------|
| **GH-P01** | Metadata      | Läs repositoryidentitet, standardbranch och installationsmetadata. | Läs           |
| **GH-P02** | Contents      | Läs branchträd och skriv commits/brancher.                         | Läs/skriv     |
| **GH-P03** | Pull requests | Skapa och läsa pull requests.                                      | Läs/skriv     |
| **GH-P04** | Checks        | Läs checkresultat.                                                 | Läs           |
| **GH-P05** | Actions       | Läs workflowkörningar och artifacts vid behov.                     | Läs           |
| **GH-P06** | Workflows     | Separat workflow-skrivbehörighet aktiveras endast om explicit `.github/workflows/**`-override ska stödjas och GitHub kräver den för leveransvägen. | Villkorad |

## 7.3 Git-strategi

För hela projektimporter bör tjänsten använda en temporär Git-arbetskopia eller Git data API för att skapa ett komplett träd och en atomisk commit. Undvik ett fristående Contents API-anrop per fil. Kortlivade credentials, isolerad temporär lagring och deterministisk städning krävs.

# 8. GitHub Actions-integration

Befintliga repositoryworkflows förväntas reagera på push och pull request. Sökvägen .github/\*\* är skyddad som standard eftersom en uppladdad ZIP annars kan ändra den kod som körs i Actions.

## 8.1 MVP

- Visa skapad commit och pull request.

- Visa ”Öppna GitHub Actions/checks”.

- Duplicera inte fullständiga loggar eller artifacts i appens lagring.

- GitHub är den auktoritativa detaljvyn.

## 8.2 Förbättrad presentation

- Hämta status för checks och workflowkörningar kopplade till committen.

- Visa väntar, körs, lyckad, misslyckad, avbruten eller kräver åtgärd.

- Visa workflow, varaktighet och resultat.

- Visa länkar till loggar och artifacts.

- Extrahera valfritt kondenserade kompilerings- och testfel.

# 9. Användargränssnitt

| **ID**    | **Krav**              | **Beskrivning**                                                    | **Prioritet** |
|-----------|-----------------------|--------------------------------------------------------------------|---------------|
| **UI-01** | Inloggning            | GitHub-inloggning, integritetsinformation och utloggning.          | MVP           |
| **UI-02** | Projekt               | Lista, skapa och hantera konfigurerade projekt.                    | MVP           |
| **UI-03** | Projektöversikt       | Repository, standardbranch, policy och senaste importer.           | MVP           |
| **UI-04** | Uppladdning           | Branchval, ZIP-väljare, förlopp och validering.                    | MVP           |
| **UI-05** | Förändringsgranskning | Sammanfattning, varningar och ett hopfällbart filträd med status, tri-state katalogval, filurval och blockeraroverride. | MVP/RC |
| **UI-06** | Godkännande           | Bekräfta exakt selection, eventuella overrides och commitmetadata innan skrivning. | MVP/RC |
| **UI-07** | Importresultat        | Länkar till GitHub, commit, PR och Actions.                        | MVP           |
| **UI-08** | Byggstatus            | Integrerade checks, workflows, fel och artifacts.                  | Senare        |
| **UI-09** | Importhistorik        | Sökbar historik med resultat och länkar.                           | Bör           |

## 9.1 Mobilkrav

- Hela importflödet ska fungera i smal mobilvy.

- ZIP-väljaren ska fungera med iOS Filer och iCloud Drive.

- Stora difflistor ska vara hopfällbara och laddas stegvis.

- Primära åtgärder ska kunna nås utan horisontell scrollning.

- Ingen native-app ska krävas.

- PWA kan tillkomma men krävs inte i MVP.

# 10. Begreppsmässig datamodell

| **ID**    | **Krav**            | **Beskrivning**                                                      | **Prioritet** |
|-----------|---------------------|----------------------------------------------------------------------|---------------|
| **DM-01** | User                | GitHub-identitet, profil och sessioner.                              | Kärna         |
| **DM-02** | GitHubInstallation  | Installations-ID, konto och behörighetsmetadata.                     | Kärna         |
| **DM-03** | Project             | Namn, repository, standardbranch och policy.                         | Kärna         |
| **DM-04** | ImportSession       | ZIP, jämförelsecommit, status och tider.                             | Kärna         |
| **DM-05** | Archive             | Namn, storlek, checksumma, temporär referens och validering.         | Kärna         |
| **DM-06** | ImportPlan          | Immutable fullständig skillnad mellan ZIP och exakt base commit, inklusive policyklassning. | Kärna |
| **DM-07** | GitDelivery         | Branch, commit-SHA, pull request och URL:er.                         | Kärna         |
| **DM-08** | WorkflowObservation | Cache av check-/workflowstatus och länkar.                           | Utökad        |
| **DM-09** | AuditEvent          | Säkerhets- och verksamhetshändelser.                                 | Kärna         |
| **DM-10** | WorkSession         | Aktiv arbetsbranch, basbranch, senaste commit och slutlig PR.         | Kärna         |
| **DM-11** | ApprovedSelection   | Immutable valda/exkluderade paths, overrides, plan-digest, base SHA och godkännandemetadata. | Kärna |

ZIP-filer och uppackade arbetsytor är temporära. Databasen behåller metadata och auditspår längre. GitHub behåller projektinnehåll och detaljerad bygghistorik.

# 11. API-översikt

| **Metod** | **Endpoint**                | **Syfte**                             |
|-----------|-----------------------------|---------------------------------------|
| **POST**  | /auth/github/start          | Starta GitHub-inloggning.             |
| **GET**   | /auth/github/callback       | Slutför OAuth-callback.               |
| **GET**   | /api/projects               | Lista projekt.                        |
| **POST**  | /api/projects               | Skapa projekt.                        |
| **GET**   | /api/projects/{id}/branches | Lista brancher.                       |
| **POST**  | /api/projects/{id}/imports  | Skapa import och ladda upp ZIP.       |
| **GET**   | /api/imports/{id}           | Hämta status och sammanfattning.      |
| **GET**   | /api/imports/{id}/changes   | Hämta sidindelad förändringslista.    |
| **GET**   | /api/imports/{id}/diff      | Hämta textdiff för en fil.            |
| **PUT**   | /api/imports/{id}/selection | Spara/validera aktuellt filurval och overrides före godkännande. |
| **POST**  | /api/imports/{id}/approve   | Lås selection och skapa commit på arbetsbranchen. |
| **POST**  | /api/imports/{id}/cancel    | Avbryt före GitHub-leverans.          |
| **GET**   | /api/imports/{id}/github    | Hämta branch-, commit- och PR-länkar. |
| **GET**   | /api/imports/{id}/checks    | Hämta check-/workflowsammanfattning.  |

# 12. Säkerhet och icke-funktionella krav

## 12.1 Säkerhet

| **ID**      | **Krav**          | **Beskrivning**                                                                | **Prioritet** |
|-------------|-------------------|--------------------------------------------------------------------------------|---------------|
| **NFR-S01** | Minsta behörighet | GitHub-åtkomst ska begränsas i rättigheter och repositoryomfattning.           | Måste         |
| **NFR-S02** | Tokenhantering    | Token hålls server-side, skyddas och är kortlivade där möjligt.                | Måste         |
| **NFR-S03** | Sessionsskydd     | CSRF, session fixation och obehöriga statusändringar ska förhindras.           | Måste         |
| **NFR-S04** | Arkivisolering    | Arkiv bearbetas i isolerade temporära kataloger med säker städning.            | Måste         |
| **NFR-S05** | Ingen kodkörning  | Projektkod, skript och workflowinnehåll får inte köras under analys.           | Måste         |
| **NFR-S06** | Secret detection  | Potentiella hemligheter bör blockeras eller tydligt flaggas.                   | Bör           |
| **NFR-S07** | Workflow-skydd    | `.github/**` är exkluderat som standard och får endast inkluderas genom explicit auditerad override. | Måste |
| **NFR-S08** | Audit             | Inloggning, konfiguration, godkännande och GitHub-skrivning ska vara spårbara. | Måste         |
| **NFR-S09** | Kvoter            | Upload- och användningsgränser ska finnas.                                     | Måste         |
| **NFR-S10** | Git-metadata      | `.git/**` får aldrig appliceras från ZIP; posterna visas som hårt blockerade och exkluderas. | Måste |
| **NFR-S11** | Selection-audit   | Exkluderingar och blockeraroverrides ska bindas till godkännandet och vara auditerbara. | Måste |

## 12.2 Tillförlitlighet och prestanda

| **ID**      | **Krav**               | **Beskrivning**                                                               | **Prioritet** |
|-------------|------------------------|-------------------------------------------------------------------------------|---------------|
| **NFR-R01** | Återhämtning           | Avbrutna importer ska ha tydlig status och säker retry.                       | Måste         |
| **NFR-R02** | Atomisk leverans       | Repositoryt får inte lämnas med delvis importerad filuppsättning.             | Måste         |
| **NFR-R03** | Stora jämförelser      | Förändringslistor ska sidindelas eller strömmas.                              | Måste         |
| **NFR-R04** | Konfigurerbara gränser | Uploadgränser ska kunna konfigureras.                                         | Måste         |
| **NFR-R05** | Retention              | ZIP och arbetsytor tas bort automatiskt efter angiven tid.                    | Måste         |
| **NFR-R06** | Observability          | Strukturerade loggar, korrelations-ID och hälsokontroller bör finnas.         | Bör           |
| **NFR-R07** | Tillgänglighet         | Kärnflöden bör följa god praxis för tangentbord, kontrast och semantisk HTML. | Bör           |

# 13. Felhantering och spårbarhet

- Valideringsfel visas åtgärdsinriktat och skapar ingen importplan.

- Behörighetsfel skiljer på inloggning, GitHub App-installation och repositoryrättighet.

- Flyttad branch blockerar godkännande eller kräver ny jämförelse.

- Pushkonflikt bevarar importplanen och erbjuder ny branch/retry.

- Misslyckad PR efter lyckad push behåller branchen och erbjuder ny försöksväg.

- Workflowfel betyder att GitHub-leveransen lyckades men efterföljande verifiering misslyckades.

- Städningsfel larmar drift men ändrar inte GitHub-resultatet.

- Varje kritisk operation har ett importkorrelations-ID.

# 14. MVP och leveransfaser

## 14.1 MVP: säker ZIP-till-PR-brygga

- GitHub-inloggning och GitHub App.

- Repository- och projektkonfiguration.

- Säker ZIP-uppladdning och uppackning.

- Jämförelse av tillagda, ändrade och potentiellt borttagna filer.

- Hierarkiskt fil- och katalogurval före commit.

- `.git/**` visas som hårt blockerad och kan aldrig inkluderas.

- `.github/**` och borttagningar exkluderas som standard och kan inkluderas efter explicit policyoverride.

- Immutable importplan plus separat immutable approved selection.

- Ett aktivt arbete per projekt; en eller flera ZIP-importer skapar sekventiella commits på samma arbetsbranch och en PR skapas när arbetet avslutas.

- Länkar till commit, PR och Actions/checks.

- Auditspår och automatisk städning.

## 14.2 Fas 2: rikare jämförelse och policy

- Textdiff.

- Managed/protected/generated paths.

- Projekt-/repositoryspecifik blockerarpolicy och override-regler.

- Importmanifest och base_commit.

- Secret scanning och stora fil-varningar.

- Importhistorik.

## 14.3 Fas 3: integrerad Actions-upplevelse

- Checks och workflowstatus i appen.

- Artifactlänkar.

- Kondenserade byggfel.

- Omkörning eller workflow dispatch.

- Notifieringar när byggen är klara.

## 14.4 Fas 4: integrations-API

- Read-only API för senaste import, PR och byggstatus.

- Liten Custom GPT Action-yta.

- Valfri MCP-adapter ovanpå samma applikationstjänster.

- Export av aktuell AI-arbets-ZIP från vald branch.

# 15. Acceptanskriterier för MVP

20. AC-01: Användaren kan logga in med GitHub och ser endast auktoriserade repositoryn.

21. AC-02: Ett repositorybaserat projekt och standardbranch kan konfigureras.

22. AC-03: ZIP kan väljas från Safari på iPhone via Filer/iCloud Drive.

23. AC-04: Osäkra eller för stora arkiv avvisas.

24. AC-05: Jämförelsen sker mot en fast Git-commit.

25. AC-06: Granskningssidan visar ett hierarkiskt träd över nya, ändrade, borttagna, ignorerade och blockerade sökvägar med status per fil.

26. AC-07: Användaren kan exkludera en fil eller katalog; katalogval påverkar alla valbara underliggande poster och partiella val visas tydligt.

27. AC-08: `.git/**` kan aldrig inkluderas men hindrar inte en commit av övriga godkända förändringar. `.github/**` och borttagningar kräver explicit override för att inkluderas.

28. AC-09: Ingen GitHub-skrivning sker före att en immutable selection med eventuella overrides har godkänts.

29. AC-10: Committen motsvarar exakt den godkända selectionen och inga exkluderade paths ändras.

30. AC-11: Flera importer i samma arbete skapar sekventiella commits på samma arbetsbranch och en pull request skapas först när arbetet avslutas.

31. AC-12: Resultatsidan länkar till commit, PR och GitHub Actions/checks.

32. AC-13: Temporär ZIP och arbetsyta städas enligt policy.

33. AC-14: Auditdata visar vem som importerade vilken checksumma, selection och overrides till vilket repository och commit.

# 16. Öppna beslut och rekommendationer

| **ID**    | **Krav**          | **Beskrivning**                                                                                 | **Prioritet** |
|-----------|-------------------|-------------------------------------------------------------------------------------------------|---------------|
| **OD-01** | Standardmål       | Skapa alltid importbranch och pull request i MVP.                                               | Rekommenderas |
| **OD-02** | Workflowändringar | Blockera .github/\*\* initialt; lägg till explicit opt-in senare.                               | Rekommenderas |
| **OD-03** | Borttagningar     | Inga borttagningar i MVP; managed-path mirror senare.                                           | Rekommenderas |
| **OD-04** | Actions i appen   | Börja med GitHub-länkar; lägg till direkt status senare.                                        | Rekommenderas |
| **OD-05** | Teknikstack       | Quarkus/Java och React/Vite är lämpliga men specifikationen kräver inte återbruk av gammal kod. | Rekommenderas |
| **OD-06** | GitHub-åtkomst    | GitHub OAuth plus GitHub App, inte långlivad PAT.                                               | Rekommenderas |
| **OD-07** | Lagring           | Temporär lokal/objektlagring; GitHub är beständig källa och byggregister.                       | Rekommenderas |
| **OD-08** | Namn              | Arbetsnamn Projektimporterare; repository project-importer eller zip-github-bridge.             | Öppet         |

## 16.1 Slutrekommendation

**Bygg första versionen som en smal och säker ZIP-till-GitHub-PR-brygga.** Bygg inte en andra exekveringsmiljö. Låt repositoryägda GitHub Actions sköta kompilering, tester, PDF/EPUB och paketering. Börja med länkar till GitHub och lägg till direkt status, felsammanfattningar och artifacts först när importflödet är stabilt.
