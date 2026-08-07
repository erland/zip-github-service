# Zip GitHub - stegvis implementeringsplan

Version 1.0  
Datum: 6 augusti 2026

## Syfte

Detta dokument bryter ned utvecklingsplanens faser till mindre, verifierbara arbetssteg. Ett steg är avsett att normalt kunna genomföras i en separat ChatGPT-prompt.

Faserna används som milstolpar. Stegen används som faktisk genomförandeordning.

## Statusstyrning

`docs/implementation-status.md` är den auktoritativa checklistan. Varje levererad ZIP ska innehålla exakt ett steg med status `NEXT`. När användaren skriver **”kör nästa steg”** genomförs endast detta steg. Efter genomförandet uppdateras status, evidens, revisionshistorik och nästa steg innan en ny ZIP skapas.

Tillåtna statusvärden är `PENDING`, `NEXT`, `IN_PROGRESS`, `DONE`, `BLOCKED` och `SKIPPED`. `IN_PROGRESS` ska normalt inte finnas kvar i en levererad ZIP.

## Arbetsregel för varje steg

Varje steg ska som huvudregel avslutas med att:

1. berörd kod är implementerad,
2. backend och frontend byggs där de påverkas,
3. relevanta tester körs och resultatet dokumenteras,
4. dokumentation och status uppdateras,
5. projektet paketeras i en ny revisionsnumrerad ZIP,
6. nästa steg anges tydligt.

Ett steg får delas ytterligare om oväntad teknisk komplexitet uppstår. Flera steg bör inte slås ihop om det gör leveransen svår att granska eller verifiera.

# Fas 0 - inventering och verifierbar baslinje

## Steg 0.1 - Packa upp och inventera legacyprojektet

- Packa upp `zip-buildserver-main.zip`.
- Ta bort `__MACOSX` och andra transportartefakter ur arbetskopian.
- Inventera katalogstruktur, backendmoduler, frontend, databas, tester, Dockerberoenden och dokumentation.
- Skapa en kort nulägesrapport.

**Leverabel:** `docs/legacy-inventory.md`.

## Steg 0.2 - Bygg och testa legacybaslinjen

- Identifiera Java-, Maven-, Node- och package manager-versioner.
- Lägg till wrappers eller versionsdokumentation där det saknas.
- Kör backendtester, frontendtester och produktionsbyggen.
- Dokumentera alla befintliga fel utan att blanda in produktombyggnad.

**Leverabel:** reproducerbar baslinje och `docs/baseline-verification.md`.

## Steg 0.3 - Skapa återanvändnings- och migreringskarta

- Klassificera paket och komponenter som `reuse`, `adapt`, `replace` eller `archive`.
- Peka ut vilka delar som flyttas till den nya kodbasen.
- Dokumentera vilka gamla delar som inte får ligga kvar i den kritiska vägen.

**Leverabel:** `docs/reuse-assessment.md`.

## Steg 0.4 - Skapa ren zip-github-bas

- Skapa den nya projektstrukturen och byt produktnamn till `zip-github`.
- Behåll endast beslutad grundteknik och selektivt återanvändbara komponenter.
- Skapa eller verifiera CI för backend och frontend.
- Dokumentera legacybaslinjens referens.

**Kvalitetsgrind för fas 0:** den nya basen kan byggas och testas i ren miljö, och återanvändningen är dokumenterad på komponentnivå.

# Fas 1 - ny domän och applikationsskal

## Steg 1.1 - Definiera domänmodell och statusmaskiner

- Inför `Project`, `ImportSession`, `SourceUpload`, `ImportPlan`, `ImportPlanEntry` och `GitHubDelivery`.
- Definiera statusar och tillåtna övergångar.
- Skriv enhetstester för tillåtna, förbjudna och idempotenta övergångar.

## Steg 1.2 - Skapa databasmodell och Flyway-migreringar

- Skapa nya tabeller med tydliga namn.
- Lägg constraints, index och auditfält.
- Skriv integrationstester mot PostgreSQL/Testcontainers.

## Steg 1.3 - Skapa API-skelett och felkontrakt

- Skapa resurser och tjänstegränser för projekt och importer.
- Inför konsekvent `problem+json` eller motsvarande felmodell.
- Uppdatera OpenAPI och API-tester.

## Steg 1.4 - Skapa frontendskal och routing

- Skapa projektlista, projektdetalj och ny import som tomma men navigerbara flöden.
- Lägg auth guards som kan kopplas in i nästa fas.
- Skapa komponent- och routingtester.

**Kvalitetsgrind för fas 1:** lokala projekt och tomma importsessioner kan skapas, läsas och visas utan GitHub-anslutning.

# Fas 2 - GitHub-login och GitHub App

## Steg 2.1 - Genomför GitHub-teknikspike

- Verifiera GitHub App-behörigheter mot ett separat testrepository.
- Skapa kortlivad installationstoken.
- Läs repository och branch-SHA.
- Skapa testbranch, commit och pull request.
- Läs grundläggande checkstatus.
- Dokumentera resultat och slutligt behörighetsbehov.

**Leverabel:** `docs/github-integration-spike.md`.

## Steg 2.2 - Implementera GitHub-login och webbsession

- Implementera redirect, callback och engångs-state.
- Skapa säker server-side session.
- Implementera aktuell användare och logout.
- Testa återanvänt eller ogiltigt state, session fixation och cookieinställningar.

## Steg 2.3 - Implementera GitHub App-installationer och repositorylista

- Skapa installationstoken vid behov.
- Lista installationer och repositoryn som både användaren och appen får använda.
- Säkerställ att token aldrig exponeras till frontend eller sparas permanent.

## Steg 2.4 - Koppla projektkonfiguration till GitHub

- Låt användaren välja installation, repository och standardbranch.
- Verifiera åtkomst när projektet sparas och öppnas.
- Lägg tester för ägarskap och obehörig åtkomst.

**Kvalitetsgrind för fas 2:** användaren kan logga in, välja ett uttryckligen auktoriserat repository och återöppna en sparad projektkonfiguration.

# Fas 3 - uppladdning och säker ZIP-inspektion

## Steg 3.1 - Implementera streaminguppladdning och metadata

- Strömma ZIP till temporär lagring.
- Beräkna SHA-256 under uppladdning.
- Inför komprimerad storleksgräns och faktisk streaminggräns.
- Spara metadata och retentionstid.

## Steg 3.2 - Implementera path- och filtypssäkerhet

- Blockera traversal, absoluta sökvägar, NUL, symlänkar och specialfiler.
- Blockera dubblettsökvägar och skiftlägeskollisioner.
- Skapa separata namngivna regler med egna fixtures och tester.

## Steg 3.3 - Implementera resursgränser och ZIP-bombskydd

- Begränsa uppackad storlek, antal filer, enskild fil, sökvägslängd och kompressionskvot.
- Säkerställ att samma regler används av både inspektion och uppackning.
- Lägg skadliga och legitima fixturearkiv.

## Steg 3.4 - Implementera normalisering och filinventering

- Ignorera `__MACOSX`, `.DS_Store` och konfigurerat brus.
- Identifiera och ta bort ensam wrapperkatalog.
- Skapa deterministisk inventering med path, storlek, typ och hash.

## Steg 3.5 - Implementera retention och mobil uppladdningsvy

- Städa ZIP och arbetsytor automatiskt.
- Lägg uppladdningsprogress, avbrott och begripliga fel.
- Testa filval från Safari på iPhone och iOS Filer/iCloud Drive.

## Steg 3.6 - Etablera komplett CI-baslinje

- Lägg en lämplig `.gitignore` för backend, frontend, temporära uppladdningar, lokala hemligheter och byggresultat.
- Lägg Maven Wrapper och lås Maven-versionen för reproducerbara backendbyggen.
- Skapa GitHub Actions för struktur-/säkerhetskontroller, backend `verify` samt frontendtest och produktionsbygge.
- Kör workflowen vid push, pull request och manuell dispatch med minsta möjliga GitHub-behörighet.
- Publicera testresultat och frontendbygge som tillfälliga CI-artifacts.

**Kvalitetsgrind för fas 3:** legitima arkiv ger deterministisk inventering, skadliga arkiv avvisas, inget ZIP-innehåll exekveras och hela projektet har en reproducerbar CI-väg.

# Fas 4 - repositorysnapshot och importplan

## Steg 4.1 - Välj och implementera repositorysnapshot

- Genomför ett litet val mellan shallow clone och Git tree API.
- Använd shallow clone som förstahandsval om inget blockerande upptäcks.
- Lås vald branch till exakt commit-SHA.
- Skapa inventering av Git-trädet på detta SHA.

## Steg 4.2 - Implementera hashbaserad jämförelse

- Klassificera `ADDED`, `MODIFIED`, `UNCHANGED` och `WOULD_DELETE`.
- Använd stabila content hashes.
- Lägg repository-fixtures och determinismtester.

## Steg 4.3 - Implementera importpolicy och blockerare

- Klassificera `IGNORED` och `BLOCKED`.
- Blockera `.git/**`, `.github/**`, borttagningar, för stora filer och högriskhemligheter i MVP.
- Varna för exempelvis `.env` enligt beslutad policy.

## Steg 4.4 - Spara immutable importplan

- Spara planversion, policyversion, base SHA, summeringar och poster.
- Förhindra ändring efter att planen skapats.
- Kontrollera ägarskap och status i API:t.

## Steg 4.5 - Bygg granskningsvyn

- Visa summering, grupperade filer, filter, varningar och blockerare.
- Anpassa långa fillistor för mobil med kort/listor och sidindelning eller virtualisering.
- Säkerställ att användaren ser exakt vilka filer som kan påverkas.

**Kvalitetsgrind för fas 4:** samma ZIP och base commit ger samma plan, blockerade planer kan inte godkännas och ingen GitHub-skrivning har skett.

# Fas 5 - godkännande och Git-leverans

## Steg 5.1 - Implementera godkännande av exakt plan

- Godkänn endast aktuell immutable plan.
- Kontrollera status, ägarskap, policy och base commit.
- Lås planens godkännandestatus och auditera händelsen.

## Steg 5.2 - Implementera temporär Git-arbetsyta och filapplicering

- Skapa en isolerad shallow clone från exakt base commit.
- Applicera endast godkända `ADDED` och `MODIFIED` filer.
- Verifiera lokal diff mot importplanen före commit.
- Städa credentials och arbetsyta deterministiskt.

## Steg 5.3 - Implementera branch, atomisk commit och push

- Skapa säkert och unikt branchnamn.
- Skapa en commit med import-ID, ZIP-namn, ZIP-SHA och base SHA.
- Pusha med kortlivad installationstoken utan att läcka credentials i loggar.

## Steg 5.4 - Implementera pull request och resultatmetadata

- Öppna pull request mot vald målbranch.
- Lägg sammanfattning av filklasser i PR-texten.
- Spara branch, commit-SHA, PR-nummer och URL:er.

## Steg 5.5 - Implementera idempotens, retry och felåterhämtning

- Använd idempotency key knuten till importsession och plan.
- Förhindra dubbla brancher, commits och PR:er.
- Hantera lyckad push men misslyckad PR separat.
- Testa leverans mot lokalt bare repository och mockad GitHub-klient.

**Kvalitetsgrind för fas 5:** en godkänd plan skapar exakt en branch, en commit och en PR, och committen motsvarar exakt den granskade planen.

# Fas 6 - Actions-länkar och resultatsida

## Steg 6.1 - Bygg resultatsidan med beständiga GitHub-länkar

- Visa branch, commit, pull request och Actions/checks.
- Säkerställ att användaren alltid kan öppna GitHub även om statusintegrationen är otillgänglig.

## Steg 6.2 - Lägg grundläggande checkstatus

- Läs combined status eller check runs för committen.
- Visa `pending`, `success`, `failure`, `cancelled` och `unavailable`.
- Begränsa polling och stoppa vid terminal status.

## Steg 6.3 - Lägg importhistorik och återöppning

- Visa tidigare importer, slutstatus och permanenta länkar.
- Återöppna pågående eller misslyckade importer med rätt återhämtningsalternativ.

**Kvalitetsgrind för fas 6:** användaren hittar PR och byggresultat med ett tryck, utan obegränsad API-användning.

# Fas 7 - mobil, säkerhet, driftsättning och flexibel granskning

## Steg 7.1 - Genomför komplett mobil- och tillgänglighetsgenomgång

- Testa hela flödet i Safari på iPhone.
- Förbättra touchytor, felmeddelanden, långa sökvägar och återgång efter skärmlås.
- Kontrollera tangentbord, kontrast och semantisk HTML.

## Steg 7.2 - Härda webb- och API-säkerheten

- Inför eller verifiera CSRF, CSP, säkra cookies och rate limiting.
- Kontrollera ägarskap på alla resurser och statusändringar.
- Granska loggar för token, filinnehåll och persondata.

## Steg 7.3 - Skapa driftmodell och operationsdokumentation

- Skapa Docker Compose eller motsvarande utan Docker socket.
- Lägg health checks, konfiguration, hemlighetsrotation, PostgreSQL-backup och retentionjobb.
- Dokumentera GitHub App callback- och installationskonfiguration.

## Steg 7.4 - Genomför hotmodell och slutlig säkerhetsregression

- Dokumentera tillgångar, trust boundaries, hot och motåtgärder.
- Kör alla säkerhetsfixtures och E2E-test mot testrepository.
- Verifiera återställnings- och cleanupscenarier.

## Steg 7.5 - MVP-release och Definition of Done

- Kontrollera samtliga acceptanskriterier.
- Uppdatera README, arkitektur, drift, säkerhet och användarflöde.
- Skapa releasekandidat och revisionslåst projekt-ZIP.

## Steg 7.6 - Inför blockerarnivåer och icke-fatala policyblockeringar

- Skilj arkivfel som avvisar hela ZIP-filen från policyklassificerade förändringar i importplanen.
- Inför minst `HARD_BLOCKED` och `OVERRIDABLE_BLOCKED` med namngivna orsaker.
- Klassificera `.git/**` som `HARD_BLOCKED`: synlig i planen, exkluderad som standard och aldrig valbar.
- Klassificera `.github/**` och `WOULD_DELETE` som överstyrbara blockerare som standard.
- Säkerställ att en plan med blockerade poster kan gå vidare när alla hårt blockerade poster är exkluderade och inga överstyrbara poster är valda utan explicit override.
- Lägg policytester för blandade ZIP-filer där blockerade och vanliga förändringar förekommer samtidigt.

## Steg 7.7 - Skapa immutable selection-modell och API

- Behåll `ImportPlan` immutable som fullständig ZIP-vs-base-jämförelse.
- Inför en separat selection-modell med valda paths, exkluderade paths, overrides, plan-digest, base SHA, användare och tidsstämpel.
- Skapa deterministisk selection-digest/version och validera att selection endast refererar till aktuell importplan.
- Förbjud tom selection och val av `HARD_BLOCKED` poster.
- Lagra selection/override-audit server-side med ägarskapskontroll.
- Lägg API- och domäntester för manipulation, stale plan och cross-user access.

## Steg 7.8 - Bygg hierarkiskt fil- och katalogurval i granskningsvyn

- Visa förändringar som ett hopfällbart träd av kataloger och filer.
- Visa filstatus `ADDED`, `MODIFIED`, `WOULD_DELETE`, `IGNORED` och blockerarstatus med tydliga symboler/etiketter.
- Markera vanliga valbara förändringar som standard.
- Låt avmarkering av en katalog exkludera alla valbara poster i dess subtree.
- Låt markering/avmarkering av barn uppdatera katalogens tri-state (`checked`, `unchecked`, `indeterminate`).
- Visa aggregerade antal per katalog när underträdet innehåller blandade förändringsklasser.
- Anpassa trädet för mobil, långa paths, tangentbord och skärmläsare.

## Steg 7.9 - Implementera explicita overrides och exakt selected delivery

- Låt `OVERRIDABLE_BLOCKED` inkluderas först efter ett explicit per-post eller per-subtree-godkännande med tydlig risktext.
- Säkerställ att katalogmarkering aldrig implicit överstyr blockerare.
- Lås selection och overrides vid godkännande så ändringar kräver nytt godkännande/digest.
- Applicera endast valda förändringar i den temporära Git-arbetsytan, inklusive uttryckligt godkända borttagningar.
- Verifiera staged diff, path-mängd och innehåll exakt mot den godkända selectionen före commit.
- Säkerställ att exkluderade och hårt blockerade paths aldrig ändras av committen.

## Steg 7.10 - Genomför selection-, override- och säkerhetsregression

- E2E-testa blandade träd med nya, ändrade, borttagna, `.github/**` och `.git/**` poster.
- Verifiera katalogurval, partiellt urval, override, borttagning och tom selection på desktop och mobil.
- Testa att `.git/**` aldrig kan levereras men inte blockerar övriga valda filer.
- Testa att `.github/**` och borttagningar kräver explicit override och auditeras.
- Verifiera att committen är byte-/path-ekvivalent med exakt godkänd selection och att stale base fortfarande stoppas.
- Uppdatera hotmodell, säkerhetsregression, API-/arkitekturdokumentation och releasechecklista.

**Kvalitetsgrind för fas 7:** tjänsten uppfyller tidigare mobil-, säkerhets- och driftkrav och kan dessutom skapa en commit från exakt ett användarvalt filträd där hårt blockerade paths aldrig levereras och överstyrbara blockerare endast levereras efter explicit, auditerat godkännande.

# Fas 8 - efter MVP: integrerade Actions-resultat

## Steg 8.1 - Workflow runs och jobs

- Läs workflow runs och jobs som hör till commit eller PR.
- Visa status per workflow och check med länk till full GitHub-vy.

## Steg 8.2 - Artifacts och kondenserade fel

- Visa artifactlänkar utan permanent lokal lagring.
- Extrahera begränsade, spårbara felutdrag för prioriterade byggverktyg.

## Steg 8.3 - Kontrollerad workflow dispatch och omkörning

- Tillåt endast uttryckligen konfigurerade workflows.
- Kontrollera behörighet, audit och idempotens.

## Steg 8.4 - AI- och integrationsyta

- Lägg ett litet read-only/status-API.
- Utvärdera Custom GPT Action eller MCP-adapter för små styr- och statusanrop.
- Lägg export av aktuell GitHub-branch som AI-anpassad ZIP.

# Rekommenderad promptmall för varje steg

```text
Fortsätt arbetet med zip-github från bifogad senaste projekt-ZIP.

Genomför endast steg <nummer och namn> i docs/implementation-steps.md.

Följ functional specification och development plan. Börja med att läsa aktuell status och verifiera baslinjen. Ändra inte senare steg annat än när en liten förberedande ändring är nödvändig.

Krav för leveransen:
- implementera stegets avgränsade funktion,
- skriv eller uppdatera relevanta tester,
- kör berörda builds och tester,
- dokumentera beslut, resultat och eventuella kvarstående problem,
- uppdatera projektstatus och nästa steg,
- paketera hela projektet i en ny revisionsnumrerad ZIP.

Rapportera kort vad som ändrats, vilka tester som körts och om kvalitetsgrinden är uppfylld.
```

# Storleksbedömning

- **Lämpliga som en prompt:** de numrerade stegen ovan.
- **Vanligen för stora som en prompt:** fas 2, 3, 4, 5 och 7 i sin helhet.
- **Kan ibland rymmas i en prompt, men bör ändå delas:** fas 0, 1 och 6.
- **Ska inte kombineras med MVP-arbetet:** fas 8.

Den rekommenderade arbetsformen är därför: **en prompt per steg, en eller flera steg per fas, och en kvalitetsgrind efter varje fas**.
