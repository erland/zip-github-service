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

## Steg 7.11 - Generalisera ZIP-ingestion och lagring

- Bryt ut återanvändbar streaming-, storleks-, filnamns-, SHA-256- och lagringslogik från den användar-/importbundna uppladdningsorkestreringen.
- Inför ett neutralt lagringsresultat, exempelvis `StoredUpload`, som kan ägas av en vanlig import eller av en framtida staging-import utan att fejka användaridentitet.
- Behåll samma absoluta ZIP-gränser, checksummeberäkning och säkra filhantering oavsett ingestion-källa.
- Säkerställ att befintlig webbuppladdning fortsätter fungera oförändrat efter refaktoreringen.

## Steg 7.12 - Skapa vanlig Import från redan lagrad ZIP

- Separera skapandet av en vanlig import från själva HTTP-uppladdningen.
- Gör det möjligt att registrera/promovera en redan säkert lagrad ZIP som en vanlig användarägd `ImportSession` utan ny nätverksuppladdning.
- Återanvänd samma inventory-, snapshot-, comparison-, policy-, plan-, selection- och delivery-pipeline som för webbuppladdning.
- Säkerställ idempotens så retry inte skapar dubbla importer eller duplicerar fysisk ZIP-data.

## Steg 7.13 - Formalisera importkälla och auditmetadata

- Inför en liten, explicit källmodell, exempelvis `ImportSource`, med minst `WEB_UPLOAD` och förberett `STORED_UPLOAD`/`STAGING_IMPORT`.
- Spara källtyp och en icke-hemlig, valfri källreferens i auditmetadata utan att blanda in capability-/claim-tokenvärden.
- Visa källan där den hjälper felsökning och historik men låt den inte påverka policy-, selection- eller Git-semantik.
- Dokumentera att framtida ingestion-kanaler måste konvergera till samma vanliga Import före jämförelse och delivery.

## Steg 7.14 - Regression för alternativ ZIP-ingestion

- Testa att vanlig browser-upload och en redan lagrad ZIP når samma importpipeline och producerar ekvivalent inventering/plan vid samma bytes och base SHA.
- Verifiera att en lagrad ZIP kan överlämnas utan andra upload och utan att säkerhetsgränser kringgås.
- Testa retry/idempotens, cleanup/ägarskap och att källmetadata inte påverkar plan- eller selection-digest.
- Dokumentera integrationspunkten som framtida `StagingImport`/iOS Shortcut ska använda.

## Steg 7.15 - Korrigera policy för oförändrade skyddade sökvägar

- Tillämpa `.github/**`-override endast när importen faktiskt skulle ändra repositoryt: `ADDED`, `MODIFIED` eller `WOULD_DELETE`.
- `UNCHANGED` under `.github/**` ska visas som oförändrad vid behov men får inte klassas som en förändring som kräver override.
- Samma princip ska gälla andra pathbaserade change-blockerare: ingen write-risk finns när bytes/path inte ändras.
- Behåll arkiv- och innehållssäkerhetsregler som gäller själva ZIP-filen oberoende av diffstatus.
- Lägg regression för oförändrat workflow, ändrat workflow, nytt workflow och borttaget workflow.

## Steg 7.16 - Automatisera upload till granskningsplan

- När ZIP-uppladdningen lyckats ska backend/frontend automatiskt fortsätta med inventory, snapshot, comparison, policy och immutable plan utan ett separat användarklick på “Skapa granskningsplan”.
- Navigera automatiskt till granskningsvyn när planen är klar.
- Visa tydlig bearbetningsstatus under långsamma steg och behåll en explicit retry-åtgärd endast vid återhämtningsbart fel.
- Gör orkestreringen idempotent så refresh/retry inte skapar flera planer eller använder en annan base SHA än den användaren ska granska.

## Steg 7.17 - Gör godkännande och commit till en användaråtgärd

- Knappen “Godkänn valda förändringar” ska vara det enda normala användarklicket mellan granskning och commit.
- Vid klick: skapa/lås immutable selection, registrera explicit approval och fortsätt direkt med workspace, exakt diffverifiering, commit och push.
- Behåll den interna säkerhetsgränsen: ingen GitHub-skrivning får ske förrän selection + overrides är validerade och approval är beständigt registrerat.
- Visa progress/resultat och säkra idempotent retry om commit/push misslyckas efter att approval skapats.
- Separat “skapa commit”-knapp ska endast finnas som återhämtningsåtgärd om ett tidigare godkännande finns men delivery inte slutfördes.

## Steg 7.18 - E2E-regression för det förenklade importflödet

- Testa normal happy path som `välj ZIP -> bearbetning -> granska -> godkänn -> commit/resultat` utan mellanliggande manuella steg.
- Testa långsam planbyggnad, refresh, retry och fel mellan approval och push utan dubbla planer eller commits.
- Verifiera att oförändrade `.github/**`-filer aldrig kräver override medan nya/ändrade/borttagna workflows fortfarande gör det.
- Testa delurval, overrides, author-val och aktiv Work-branch i det förenklade flödet på desktop och mobil.

## Steg 7.19 - Gör pågående import fullt återupptagningsbar — DONE

- Säkerställ att en import som har laddats upp och nått granskningsvyn kan återupptas efter logout/login utan ny ZIP-uppladdning.
- Säkerställ att samma import även kan återupptas efter backend-restart/deploy; kvarvarande import-, upload-, plan-, selection- och approval-state som behövs för återupptagning får därför inte vara beroende av JVM-minne.
- Låt projekt-/Work-vyn tydligt identifiera högst en pågående import och ge en direkt åtgärd för att fortsätta från rätt steg.
- Återöppna granskningen mot exakt samma låsta plan/base SHA; om selection eller approval redan finns ska de återläsas i stället för att skapas om.
- Behåll befintliga retention- och cleanupregler men förhindra att en aktiv, återupptagningsbar import gallras som om den vore övergiven.
- Lägg regression för logout/login, ny webbsession, backend-restart och direkt återöppning från projektet utan ny upload.

## Steg 7.20 - Förenkla Work-vyn till Git-historik och pågående import

- Gör Work-branchen och dess Git-commits till den primära användarsynliga historiken för ett pågående arbete.
- Visa högst en aktiv/pågående import som en separat arbetsuppgift med tydlig fortsättningsåtgärd.
- Tona ned eller ta bort den generella listan över tidigare importer från huvudvyn när de redan motsvaras av commits på Work-branchen.
- Behåll full importhistorik och import-ID:n i backend för audit, felsökning, idempotens och teknisk återöppning; förändringen gäller primärt användargränssnittet.
- Visa commit-SHA, commitmeddelande, author och relevanta GitHub-länkar i Work-historiken utan att duplicera GitHubs fullständiga commitvy.
- Säkerställ att Work-vyn fortfarande fungerar när GitHub-status/historikläsning är tillfälligt otillgänglig genom att visa beständig lokal Work-/resultatmetadata där det behövs.

## Steg 7.21 - Slutregression för resume och Work-vy

- E2E-testa `upload -> review -> logout -> login -> fortsätt review` utan ny upload eller ny plan.
- Testa backend-restart mellan upload/review, efter selection och efter approval och verifiera korrekt återupptagning utan dubbla selectioner, approvals eller commits.
- Testa att projekt-/Work-vyn visar aktiv branch, relevant commit-historik och högst en pågående import med rätt återupptagningslänk.
- Verifiera att historiska importer fortfarande finns åtkomliga som audit-/felsökningsdata även om de inte dominerar huvud-UX.
- Verifiera ägarskapsisolering: en annan användare får aldrig återuppta, läsa eller leverera någon annans pågående import.
- Uppdatera releasechecklista, operations-/arkitekturdokumentation och fas-7-kvalitetsgrind efter regressionen.

## Steg 7.22 - Avbryt och stäng pågående import

- Lägg en tydlig åtgärd `Avbryt import` i gransknings-/återupptagningsflödet så användaren kan lämna en import som inte längre ska committas.
- Tillåt cancel före approval och efter immutable selection/approval så länge Git-delivery ännu inte har skapat/pushat committen.
- Gör cancel owner-scoped, idempotent och beständigt; en redan avbruten import ska kunna få samma svar vid retry utan nya sidoeffekter.
- Markera importen terminalt som avbruten och frigör Work så en ny ZIP kan startas.
- Radera temporär workspace-data och gör den uppladdade ZIP-filen eligible för säker cleanup enligt retentionreglerna; behåll den auditmetadata som behövs för felsökning och spårbarhet.
- Förbjud cancel efter lyckad Git-delivery; då är korrekt åtgärd att hantera committen/Work-flödet i stället för att låtsas att importen aldrig levererades.

## Steg 7.23 - Gör Work-actions state-baserade och ta bort redundanta vägar

- Om en aktiv import finns ska projektvyn primärt visa `Fortsätt import`/`Fortsätt granska` och `Avbryt import`; ny import får inte startas parallellt.
- Dölj eller disable `Ladda upp nästa ZIP` medan en aktiv import finns och kontrollera samma invariant server-side så UI-bypass inte kan skapa flera samtidiga aktiva imports i samma Work.
- Ta bort den redundanta generella `Fortsätt arbete`-knappen när den i praktiken leder till samma handling som att ladda upp nästa ZIP.
- Om Work är öppet men ingen import är aktiv ska primär åtgärd vara `Ladda upp nästa ZIP`.
- Efter lyckad commit/push ska resultatsidan direkt erbjuda både `Ladda upp nästa ZIP` och `Arbetet är klart – skapa pull request`.
- PR-skapandet ska fortfarande vara ett separat explicit beslut och använda befintlig Work-head/PR-idempotens; användaren ska bara slippa ett onödigt mellansteg via projektsidan.

## Steg 7.24 - Regression för cancel och state-baserade Work-actions

- Testa cancel från review före selection/approval och verifiera att ingen Git-operation sker och att ny import därefter kan startas.
- Testa cancel efter selection/approval men före delivery och verifiera terminal cancel, cleanup eligibility och bevarad auditmetadata.
- Testa att cancel efter lyckad delivery avvisas och aldrig raderar eller döljer en skapad commit.
- Verifiera både UI och API-invariant att högst en aktiv import kan finnas per Work och att parallell ny ZIP blockeras tills aktuell import committats eller avbrutits.
- Verifiera knappuppsättning per state: aktiv import -> fortsätt/avbryt; öppen Work utan aktiv import -> ladda upp nästa ZIP; lyckad commit -> nästa ZIP eller avsluta Work/PR.
- E2E-testa att `Arbetet är klart – skapa pull request` kan köras direkt från commit-resultatet utan extra navigation och utan dubbla PR:er vid retry.
- Regressionskör logout/login och backend-restart så cancel-/Work-state fortfarande återhämtas korrekt och owner-isoleringen bibehålls.

**Kvalitetsgrind för fas 7:** tjänsten uppfyller tidigare mobil-, säkerhets- och driftkrav, kan skapa en commit från exakt ett användarvalt filträd, har en återanvändbar ZIP-ingestion-kärna och erbjuder normalflödet `välj ZIP -> granska -> godkänn -> commit` utan onödiga mellanliggande klick. Hårt blockerade paths levereras aldrig, överstyrbara förändringar kräver explicit audit-godkännande och oförändrade skyddade paths kräver aldrig change-override. En pågående import kan återupptas efter logout/login och backend-restart utan ny ZIP-upload, kan explicit avbrytas före delivery och blockerar parallell ny ZIP tills den antingen committats eller avbrutits. Work-vyn använder Git-commit-historiken som primär historik med högst en aktiv import, och efter en lyckad commit kan användaren direkt välja mellan nästa ZIP och att avsluta Work genom att skapa pull request.

# Fas 8 - efter MVP: integrerade Actions-resultat

## Steg 8.1 - Workflow runs och jobs

- Läs workflow runs och jobs som hör till aktuell Work-commit eller pull request med kortlivad GitHub App-installationstoken.
- Mappa GitHubs status/conclusion till en liten stabil intern modell och visa status per workflow/job/check.
- Länka alltid till full GitHub-run och låt GitHub förbli källa för fullständig körningsinformation.
- Begränsa polling, backoff och API-användning; terminala körningar ska inte pollas vidare.
- Hantera att Actions saknas, är avstängt, ännu inte har startat eller tillfälligt inte kan läsas utan att övriga Work-resultat försvinner.

## Steg 8.2 - Artifacts och kondenserade fel

- Lista relevanta artifacts från GitHub Actions och visa säkra GitHub-länkar utan permanent artifactlagring i zip-github.
- Hämta endast de begränsade loggdelar som krävs för att identifiera ett kondenserat fel; full logg ska fortsatt öppnas på GitHub.
- Extrahera begränsade, spårbara felutdrag för prioriterade byggverktyg, initialt Maven/Gradle, npm/Vite, Pandoc och xcodebuild där formatet går att känna igen robust.
- Begränsa antal rader/bytes, sanera terminalkontrollsekvenser och undvik att exponera tokens/secrets i appens sammanfattning.
- Visa tydligt vilken workflow/job/step och GitHub-URL som felutdraget kommer från.

## Steg 8.3 - Kontrollerad workflow dispatch och omkörning

- Tillåt manuell `workflow_dispatch` och/eller rerun endast för uttryckligen tillåtna workflows och operationer.
- Kontrollera repository-/installationstillhörighet, användarägarskap och nödvändiga GitHub App-behörigheter server-side.
- Inför audit och idempotens så dubbelklick/retry inte skapar oväntade parallella körningar.
- Kräv explicit användarhandling och visa vilken branch/ref och workflow som kommer att köras.
- Lägg regressionsskydd för obehörig workflow, stale Work, dubbel retry och GitHub-fel.

**Kvalitetsgrind för fas 8:** zip-github ger en mobilvänlig översikt över relevanta Actions-runs/jobs, artifacts och begränsade felutdrag utan att ersätta GitHub som fullständig källa. Kontrollerad dispatch/rerun är explicit, owner-scoped, auditerad och idempotent.

# Fas 9 - Shortcut och kortlivad StagingImport

Fas 9 gör det möjligt att från exempelvis iOS delningsblad/Shortcut skicka en ZIP till zip-github innan användaren öppnar webbappen. Staging är endast en transportbuffert: den får aldrig ge GitHub-åtkomst, skapa en vanlig import under anonym identitet eller införa en separat comparison/policy/delivery-pipeline. Efter autentisering och claim ska den redan lagrade ZIP-filen promoveras genom den befintliga `StoredUploadArtifact`/`createImportFromStoredUpload(...)`-vägen.

Fas 9 ska samtidigt täppa till metadataförlust som kan uppstå i ZIP→GitHub-flödet för Git file modes. Unix executable-bit är en del av projektets Git-semantik och ska, när den finns tillgänglig i ZIP-metadata eller redan finns i basrepositoryt, bevaras genom staging, inventory/plan, granskning och delivery. zip-github får inte gissa körbarhet från filnamn eller filändelse. För vanliga filer stöds initialt de Git-relevanta lägena `100644` och `100755`; övriga/otillåtna modevärden ska normaliseras eller blockeras enligt en explicit säker policy.

## Steg 9.1 - Definiera och persistiera StagingImport-livscykeln

- Inför en separat `StagingImport`-modell/tabell för kortlivad, ännu inte användarägd ZIP med staging-id, storage metadata/SHA-256, skapad tid, expiry, status och claim-token-hash.
- Statusmodellen ska minst skilja på `AVAILABLE`, `CLAIMED`, `PROMOTED`, `EXPIRED` och `CANCELLED`/motsvarande terminalt läge.
- Spara aldrig claim-token i klartext och använd aldrig claim-/upload-token som `sourceReference` eller audittext.
- Staging ska referera till samma neutrala lagringsartefakt som `ZipIngestionService` producerar; inga ZIP-bytes ska kopieras eller streamas om vid promotion.
- Dokumentera transaktions-/låsningskrav så endast en användare kan claima ett stagingobjekt och promotion kan göras idempotent.
- Utöka den neutrala upload-/stagingrepresentationen så känd Unix executable-metadata från ZIP kan bevaras per fil utan att göra staging till en separat importpipeline. Representationen ska kunna bära skillnaden mellan vanlig fil (`100644`) och executable (`100755`) vidare till ordinary Import.
- Definiera säker fallback för saknad mode-metadata: befintliga repositoryfiler ska som utgångspunkt behålla basrepositoryts Git mode, medan helt nya filer utan tillförlitlig mode-information får `100644`; zip-github får inte inferera `100755` från exempelvis `.sh`, `mvnw` eller andra filnamn.

## Steg 9.2 - Lägg capability-skyddad staging-upload

- Lägg ett smalt upload-endpoint avsett för Shortcut/andra enkla klienter som accepterar en ZIP via befintlig `ZipIngestionService`.
- Skydda endpointen med en separat **deployment-scoped, roterbar och lågprivilegierad upload credential/capability** som endast ger rätt att skapa en staging-upload; capabilityn är uttryckligen **inte användarautentisering** och får aldrig ge list/read/claim/project/GitHub-behörighet. Första fas-9-versionen ska inte göra credentialen användarspecifik.
- Capability skickas i en dedikerad header (föredraget `X-ZipGitHub-Upload-Credential`) och aldrig i URL. Den ska hållas separat från webbsession/OAuth/GitHub App-secrets och får inte förekomma i access logs, analytics eller audittext. Lägg hård rate limiting, samma ZIP-/storleksgränser som webbuppladdning och generisk felinformation.
- Credentialens säkerhetsmål är abuse-skydd för stagingkapacitet, inte identitet eller repositoryauktorisation. En läcka ska därför kunna hanteras genom omedelbar revoke/rotation utan att GitHub-credentials eller användarsessioner behöver roteras.
- Returnera ett ogenomskinligt staging-id, expiry och en engångs-claim-URL/token. Claim-token ska ha hög entropi, returneras endast vid skapandet och endast hash lagras server-side.
- Det ska inte finnas något anonymt list-endpoint eller möjlighet att läsa tillbaka ZIP/data med endast staging-id eller upload capability.

## Steg 9.3 - Implementera autentiserad claim från webbläsaren

- Lägg en mobilvänlig claim-route som öppnas från Shortcut efter lyckad upload.
- Minimera tokenläckage genom att föredra claim-token i URL-fragment/client-side state; token får inte hamna i server access logs, analytics eller referrer till tredje part.
- Om användaren inte är inloggad ska claim-intentionen överleva GitHub-login säkert och återupptas efter callback.
- Efter vanlig GitHub-inloggning ska backend atomiskt binda stagingobjektet till den autentiserade användaren; en redan claimad token får inte kunna tas över av en annan användare.
- Claim ska vara idempotent för samma ägare men ge neutralt 404/410-liknande svar för fel token, utgånget eller redan taget objekt så information inte läcker.

## Steg 9.4 - Välj projekt och promovera staging-ZIP till vanlig Import

- Efter claim visar webbappen ZIP-namn/storlek/SHA/expiry och användarens valbara zip-github-projekt; ingen GitHub-data ska vara tillgänglig före vanlig auth.
- Användaren väljer projekt/Work. Om projektet redan har en aktiv import ska promotion blockeras av samma `ACTIVE_IMPORT_EXISTS`-invariant som webbladdning och användaren får välja annat projekt, avbryta befintlig import eller återkomma.
- Promovera den redan lagrade artefakten via befintlig `createImportFromStoredUpload(...)` med `ImportSource.STAGING_IMPORT` och endast en icke-hemlig staging-korrelation som `sourceReference`.
- Ingen ny upload/copy/restream får ske och identisk promotion/retry ska returnera samma vanliga Import.
- Efter promotion används exakt befintligt flöde: prepare review -> inventory/snapshot/comparison/policy/plan -> selection/approval -> Work-commit/PR.
- Säkerställ att promotion och den vanliga importpipen bevarar känd file-mode-metadata och att repositorysnapshotens mode används som fallback för befintliga paths när ZIP:en saknar tillförlitlig Unix mode-information.
- Gör modeförändringar granskningsbara och approval-bundna på samma sätt som innehåll/path-förändringar: exempelvis `100644 -> 100755` eller `100755 -> 100644` ska vara synligt i plan/review och ingå i den exakta verifieringen före commit. Exkluderade paths får inte få mode ändrat.
- Git delivery ska skapa korrekt index/tree-mode för godkända vanliga filer (`100644`/`100755`) och verifiera den staged Git-diffen inklusive mode före commit.

## Steg 9.5 - Låt användaren ange commitmeddelandet

- Lägg ett tydligt commitmeddelandefält i den ordinarie review/approval-vägen innan commit skapas. Funktionen ska gälla både vanlig browser-upload och en Import som kommer från StagingImport; staging får inte få en separat commitregel.
- Den nuvarande automatiskt genererade texten får användas som ett **redigerbart förslag** för bekvämlighet, men får inte längre vara ett dolt/slutligt val som användaren inte kan ändra. Användaren ska kunna ersätta hela meddelandet före approval/delivery.
- Validera och normalisera commitmeddelandet server-side med dokumenterad maxlängd, normaliserade radslut och avvisning av tomt/whitespace-only meddelande i den interaktiva vägen. Ingen osanitiserad kontrolltext får skickas till Git/GitHub.
- Persistiera det valda commitmeddelandet i den restart-säkra import/approval-state som krävs för att refresh, logout/login, backend-restart och delivery-retry använder exakt samma meddelande och inte genererar ett nytt.
- Bind det slutliga commitmeddelandet till den explicita approval/delivery-intentionen så att ett meddelande inte kan bytas efter godkännande utan ett nytt explicit godkännande. Idempotent retry av samma godkända delivery ska återanvända samma meddelande och får inte skapa en extra commit.
- Visa commitmeddelandet i den sista bekräftelsen tillsammans med branch/ref och valt filurval. Efter delivery ska Work-historiken fortsatt visa det faktiska commitmeddelandet från GitHub.
- Behåll en bakåtkompatibel, deterministisk fallback endast där äldre återupptagningsdata eller icke-interaktiva interna anrop saknar det nya fältet; normal ny UI-flow ska alltid låta användaren se och välja meddelandet. Dokumentera fallbacken tydligt så den inte blir den primära UX:en igen.
- Lägg backend- och frontendregression för eget meddelande, redigerat förslag, tomt/ogiltigt meddelande, restart/resume och retry/idempotens.

## Steg 9.6 - Retention, abuse-skydd och säkerhetsregression för staging

- Städa oclaimade stagingobjekt automatiskt efter kort konfigurerbar TTL, initialt cirka en timme; terminala/utgångna artifacts ska raderas deterministiskt.
- Definiera en kort säker frist för claimade men ännu inte promoverade objekt och säkerställ att promotion/cleanup inte kan race:a.
- Rate-limita per capability och nätverkskälla där möjligt och sätt tak för samtidiga staginguploads/lagringsvolym så en läckt capability inte kan fylla disken.
- Säkerställ **omedelbar revoke och enkel deployment-credential-rotation utan datamigrering** och dokumentera incidentåtgärd vid läckt upload capability. Första versionen behöver inte stödja `current`/`previous` parallellt: efter rotation får en gammal Shortcut misslyckas tydligt och användaren hänvisas till att hämta/installera den senaste signerade Shortcut-versionen.
- Stagingobjekt som redan skapats före credentialrotation fortsätter styras av sina separata claim-token/TTL-regler; rotation av upload credential får inte i sig ge eller ta bort claim-/GitHub-behörighet.
- Lägg tester för revoked/old upload credential, token guessing/reuse, concurrent claim, cross-user isolation, expired claim, cleanup race, överstor ZIP och att staging aldrig kan nå GitHub-operationer före promotion.

## Steg 9.7 - Distribuera en signerad referens-Shortcut för iOS

- Skapa och dokumentera ett minimalt iOS Shortcut-flöde som tar emot en ZIP från Share Sheet/Filer, anropar staging-upload med den dedikerade capability-headern och öppnar returnerad claim-URL i standardwebbläsaren.
- **Första versionen ska distribueras som en statisk, försignerad `.shortcut`-releaseartefakt**, inte genereras/signeras dynamiskt av Java-backend per användare. En inloggad användare ska kunna hämta den aktuella installerbara Shortcut-filen från zip-githubs UI/installationssida.
- Den signerade standard-Shortcuten får bära den aktuella deployment-scoped upload credentialen eftersom denna endast ger staging-create/abuse-skydd. Shortcuten får aldrig innehålla GitHub-token, GitHub App private key, zip-github user-id, Project-id som auktorisationsgenväg eller importpolicylogik.
- Dokumentera build/release-processen för Shortcut-filen: skapa/uppdatera Shortcuten i en betrodd Apple-miljö, signera den för delning (`anyone`) och publicera exakt den signerade artefakten. Det praktiska phase-9-spiket visade att GitHub-hostad macOS-runner har `shortcuts sign` men misslyckar utan en iCloud-inloggad miljö; dynamisk GitHub Actions-signering är därför **inte** ett krav för fas 9.
- Credentialrotation sker i första versionen genom att backend revokar den gamla deployment-credentialen, en ny signerad Shortcut med ny credential publiceras och gamla installationer får ett tydligt `shortcut outdated/credential revoked`-fel som hänvisar användaren till zip-github för att hämta/installera senaste Shortcut. Ingen `current`/`previous` grace-period krävs.
- Versionsmärk referens-Shortcuten så UI/felmeddelanden och operationsdokumentation kan identifiera vilken Shortcut-generation som används utan att logga credentialen.
- Ge tydliga felvägar för offline, 401/403 på gammal/revokad Shortcut, 413/429, utgången claim och serverfel. Claim-token eller upload credential får inte läggas i Shortcut-notiser, persistent loggtext eller annan diagnos som användaren typiskt delar.
- Dokumentera en exakt manuell återbyggnadsinstruktion (actions, header, request body, svar, signering och release) så artefakten kan reproduceras även om den signerade releasefilen behöver ersättas.
- **Verifiera download-identiteten separat från serverfilens tekniska namn:** deploymentartefakten får fortsatt lagras som `shortcut/releases/zip-github.shortcut`, men den autentiserade download-endpointen ska exponera ett användarvänligt filnamn via `Content-Disposition`, initialt **`Skicka till zip-github.shortcut`**, eftersom iOS använder det nedladdade filnamnet som genvägens namn vid import. Test ska bevisa både headern och att bytes/hash fortfarande avser samma signerade artefakt.
- **Verifiera runtime-läsbarhet för den signerade releasefilen:** signing/publiceringsflödet får inte lämna `.shortcut` med rättigheter som endast signing-användaren kan läsa (det observerade `0600`-fallet). Deployment-/releasegrinden ska kontrollera att backendens runtime-användare faktiskt kan läsa den bind-mountade filen före publicering; rekommenderad enkel filmode för den read-only-mountade releaseartefakten är `0644` eller semantiskt motsvarande ACL/ownership. Credentialen skyddas genom att artefakten inte source-trackas och endast distribueras via autentiserad endpoint, inte genom ett mode som gör den oläsbar för backend.

## Steg 9.8 - Work lifecycle, projektlivscykel och robust branch-provisionering

- Gör repositorynamnet på projektsidan klickbart till det konfigurerade repositoryts default branch på GitHub så användaren enkelt kan öppna GitHub-gränssnittet utan att lämna projektkontexten.
- Lägg en explicit **Avsluta arbete utan PR**-väg. Ett avslutat arbete ska inte längre räknas som aktivt och ska inte kräva att en pull request skapas.
- Låt användaren i avslutsdialogen separat välja om Work-branchen också ska tas bort från GitHub. Standard ska vara att **behålla branchen**, så ett övergivet arbete kan återupptas senare. Branch-delete ska vara en separat, tydligt destruktiv handling.
- Lägg stöd för att **Ta bort projekt** från den normala projektlistan utan att förstöra audit/historik. Implementera detta som archive/soft-delete (`archived_at` eller motsvarande), inte fysisk DELETE av projektets imports/work/audit. Ett projekt med aktivt arbete ska först kräva att arbetet avslutas.
- När ett nytt arbete startas ska användaren kunna välja mellan **Skapa ny branch** och **Fortsätt på befintlig branch**. Befintliga val ska hämtas ägarsäkert från det konfigurerade repositoryt och en branch som redan används av ett annat aktivt arbete får inte väljas.
- Ett tidigare avslutat arbete ska kunna återupptas genom att dess kvarvarande branch väljs som bas för ett nytt aktivt Work, utan att tidigare zip-github-internal state återaktiveras.
- Gör branch-provisionering explicit och server-side verifierad. Ett Work får inte bli `ACTIVE` bara för att zip-github har valt ett branchnamn: GitHub-refen ska faktiskt skapas eller verifieras existera och därefter läsas tillbaka innan Work markeras aktivt.
- Inför ett tydligt provisioning-läge (`PROVISIONING -> ACTIVE -> FINISHED/ABANDONED` eller semantiskt motsvarande) så ett misslyckat GitHub branch-create aldrig lämnar ett aktivt Work som pekar på en branch som inte finns.
- Delivery/commit ska dessutom göra en preflight som verifierar att remote Work-branch fortfarande existerar och att dess SHA motsvarar den förväntade Work-baslinjen innan commit/push. Ingen force-push eller implicit branch recreation får ske vid delivery.
- Lägg regression för det observerade Shortcut-fallet: promotion när inget aktivt arbete finns eller föregående arbete är avslutat ska skapa/verifiera en verklig remote Work-branch innan review/approval kan leda till commit.
- Lägg ägarskaps-, retry- och restart-regression för create/resume/finish/abandon/archive så samma användarintention är idempotent och cross-user access fortsatt ger neutral not-found.

## Steg 9.9 - GitHub Actions-status och fel direkt på Work-sidan

- Återanvänd befintlig fas-8 Actions-integration så status inte endast är tillgänglig direkt efter commit utan också kan öppnas senare från det aktiva eller senaste Work på projektsidan.
- Visa senaste relevanta workflow runs för Work-branchen med minst status (`queued`, `in_progress`, `success`, `failure`, `cancelled`), commit-SHA, workflow-/jobnamn och länk till motsvarande GitHub Actions-run.
- Lägg en explicit **Uppdatera status**-åtgärd. UI får försiktigt polla medan en relevant run pågår men backend ska inte införa permanent monitorering eller ny bakgrundsauktorisation.
- Vid fel ska användaren kunna öppna kondenserade fel från den befintliga Actions-detaljvägen även efter att commit-resultatdialogen stängts.
- Lägg **Kopiera fel** som producerar en kompakt, AI-/supportvänlig text med repository, branch, commit, workflow, job/step och relevant kondenserat loggutdrag. Kopierad text ska ha samma sekretessfiltrering och storleksgränser som befintlig Actions-feldiagnostik och får inte innehålla credentials/tokens.
- Knyt Actions-status till rätt Work/commit och låt inte äldre körningar för samma branch felaktigt presenteras som status för en nyare commit.
- Lägg frontend/backend-regression för återbesök efter refresh/logout-login, success/failure/in-progress, kopiera-fel och GitHub API-fel/rate limit.

## Steg 9.10 - E2E-regression, drift och slutlig releasegrind för fas 9

- E2E-testa `Share/Shortcut -> staging upload -> öppna claim -> login -> claim -> välj projekt -> promotion -> robust Work-provisionering -> automatisk review -> approval -> delivery` utan andra ZIP-bytes eller parallell pipeline.
- Testa logout/login, backend-restart och retry kring claim/promotion/Work-provisionering så varken stagingobjekt, Work eller vanlig import dupliceras.
- Testa två användare och samtidiga claimförsök, aktiv-import-/aktiv-Work-konflikt, archive/abandon samt expiry/cleanup.
- Verifiera att efter promotion är plan/selection/delivery byte- och digest-ekvivalent med samma ZIP via vanlig web upload mot samma base SHA.
- Lägg commitmeddelande-regression som bevisar att browser-upload och StagingImport använder samma användarvalda meddelande, att restart/retry inte regenererar eller ändrar det och att samma delivery-intention inte kan skapa dubbla commits.
- Lägg file-mode-regression för minst: executable-bit bevarad från ZIP (`100755`), befintlig executable fil uppdaterad från ZIP utan mode-metadata behåller `100755`, ny fil utan mode-metadata blir `100644`, explicit godkänd `100644 <-> 100755`-ändring levereras korrekt samt att exkluderad/hårt blockerad fil aldrig får mode ändrat.
- Verifiera att samma ZIP via Shortcut/StagingImport och vanlig browser-upload ger ekvivalent innehåll, path-urval **och Git file modes** mot samma base SHA.
- Verifiera Work-lifecyclefallen: ny branch skapas och läses tillbaka innan `ACTIVE`, befintlig branch kan väljas, avslut utan PR fungerar med både bevarad och borttagen branch, arkiverat projekt försvinner från normallistan och delivery vägrar om remote Work-branch saknas eller har oväntad SHA.
- Verifiera att Actions-status och kondenserade/kopierbara fel kan återbesökas från Work-sidan efter refresh och att de är commit-korrekta och sekretessfiltrerade.
- E2E-verifiera även att den publicerade signerade Shortcut-artefakten kan installeras på iOS, att en aktuell credential accepterar upload och att en revokad/gammal Shortcut ger det dokumenterade uppdateringsfelet utan GitHub- eller användardataläckage.
- Verifiera i samma Shortcut-release-E2E att `/shortcut` laddar ned exakt den signerade manifest-hashen med användarfilnamnet **`Skicka till zip-github.shortcut`**, att backendens runtime-användare kan läsa den mountade releasefilen efter deployment och att en nyinstallerad iOS Shortcut därför får det avsedda visningsnamnet istället för serverns tekniska filnamn.
- Uppdatera operations, threat model, API-kontrakt, releasechecklista och Shortcut-installationsguide, inklusive manuell signerad Shortcut-release och credentialrotation/revoke.

**Kvalitetsgrind för fas 9:** en statiskt publicerad och signerad referens-Shortcut kan installeras på iOS och använda en deployment-scoped, lågprivilegierad upload credential som kan revokas/roteras utan GitHub-credentialrotation; gammal Shortcut ger ett tydligt uppdateringsfel och dynamisk server-/GitHub Actions-signering krävs inte. Work-branch måste existera och vara verifierad på GitHub innan Work blir aktivt, delivery måste vägra saknad/stale remote branch och användaren kan avsluta utan PR, återuppta via kvarvarande branch och arkivera projekt utan att audit förstörs. Actions-status och kondenserade fel är återbesökbara från Work-sidan. användaren kan före delivery se och ändra commitmeddelandet i den gemensamma review/approval-vägen; det slutliga meddelandet är restart-säkert, approval-bundet och idempotent vid retry. En ZIP kan skickas från iOS till en kortlivad, icke-GitHub-auktoriserad stagingyta, claimas av exakt en autentiserad användare och promoveras utan reupload till samma vanliga importpipeline. Oclaimade uploads kan inte listas/läsas, tokens lagras inte i klartext, abuse begränsas och staging ger aldrig repositoryåtkomst i sig. Git file modes bevaras deterministiskt utan filename-baserad gissning: känd ZIP-metadata används, befintliga paths faller tillbaka till basrepositoryts mode, nya paths utan mode-metadata blir `100644`, och varje godkänd modeförändring ingår i review/approval/staged-diff-verifieringen före commit.

## Steg 9.11 - Actions-visibilitet, gemensam Work/commit-vy och utökad feldiagnostik

- Gör Actions-hämtningen robust per delkälla: en workflow-run som matchar exakt Work/commit-SHA ska förbli synlig även om jobs/checks/artifacts/loggdetaljer tillfälligt inte kan läsas.
- Skilj tydligt mellan `not_started` och verkligt GitHub API-/behörighetsfel. HTTP 403 från Actions-API ska presenteras som ett explicit GitHub App-permissionsproblem, inte som att committen saknar workflow-runs.
- Dokumentera att GitHub App-installationen måste ha Repository permission **Actions: Read and write** för nuvarande produktfunktioner (read för status/loggar, write för explicit allowlistad dispatch/rerun), och att befintliga installationer kan behöva godkänna en senare permissionändring på GitHub.
- Gör Work-vyn till den primära Actions-upplevelsen och återanvänd samma frontendkomponent i commit/resultatvyn så status, jobs, artifacts, fel, refresh och Actions-kontroller beter sig likadant.
- Exponera Workens senaste import-id i den owner-skyddade Work-responsen så samma befintliga, importbundna dispatch/rerun-policy kan återanvändas utan en parallell kontrollpipeline.
- För misslyckade jobs: behåll kondenserat fel men lägg dessutom ett sanerat sammanhang runt relevant felpunkt (mål cirka 40 rader före och 12 efter) och en expanderbar sanerad jobblogg.
- Begränsa jobbloggen till högst 128 KiB per misslyckat job och högst 1600 visade rader; markera trunkering. Samma token/secret-redaction som tidigare ska gälla innan loggtext skickas till klienten eller kopieras.
- Lägg separata åtgärder för **Kopiera fel med sammanhang** och **Kopiera jobblogg**, med repository, branch, commit, workflow, job och step i kopierad text.
- Lägg regression för den observerade runnen `31258714926` / commit `f3689dfd3b5f011e2ba04d56e3a5f50b4bc97e69`, Actions-403, jobs-endpointfel samt gemensam Work/resultat-rendering.

**Kvalitetsgrind för 9.11:** en exakt matchande push-run får inte döljas av sekundära GitHub API-fel; permissionsproblem är diagnostiserbara; Work- och commit/resultatvyn använder samma Actions-komponent och samma kontroller; felvisningen innehåller både säkert kondenserat fel, användbart föregående sammanhang och bounded/sanerad jobblogg.

## Steg 9.12 - Repository `.gitignore` i importplan och tydligare review-filter

- Läs de `.gitignore`-filer som är spårade i den låsta repository-snapshoten och använd deras regler när nya ZIP-paths klassificeras.
- En path som inte redan är spårad och som matchar repositoryts `.gitignore` ska få status `IGNORED`, visas som en informations-/varningspost och aldrig ingå i default selection eller kunna väljas för commit. Ingen override eller särskild acknowledgement ska krävas.
- Redan spårade Git-paths ska fortfarande jämföras normalt även om en `.gitignore`-regel matchar dem; `.gitignore` får inte göra en tracked modification osynlig.
- `.git/**` ska fortsätta vara hårt blockerad oberoende av `.gitignore`. Övriga säkerhets-/policyregler ska inte innehålla projektspecifika filnamn för zip-githubs egen signerade Shortcut.
- Ta bort den exakta `shortcut/releases/zip-github.shortcut`-specialregeln. Skyddet för den deploymentartefakten ska i stället följa repositoryts generella `/shortcut/releases/*.shortcut`-regel i `.gitignore`.
- Förenkla review-UI:t så sammanfattningen högst upp är neutral information, inte en andra uppsättning kort/knappar som liknar filtren. Behåll ett enda tydligt filterområde nära fillistan och visa kategoriernas antal där.
- Lägg regression för root/nested `.gitignore`, negation (`!`), tracked-file-semantik, signerad Shortcut som vanlig ignored path samt review-UI utan duplicerade knapp-liknande sammanfattningskontroller.

**Kvalitetsgrind för 9.12:** en ny ZIP-fil som Git skulle ignorera får inte bli blockerande eller valbar; tracked paths försvinner inte på grund av ignore; `.git/**` förblir hard-blocked; ingen produktionspolicy specialbehandlar zip-githubs Shortcut-filnamn; review-sidan har endast ett klickbart filterområde.

## Steg 9.13 - Repository-first UX och lazy intern Project

- Gör repositories som GitHub App-installationen ger användaren åtkomst till till den primära startsidan. Project ska fortsatt finnas som intern owner-bunden resurs för Work/import/audit men inte behöva skapas eller namnges manuellt av användaren.
- Lägg ett owner-skyddat repository-API som slår ihop användarens synliga GitHub App-installationer/repositories med eventuell befintlig intern Project-identitet utan att skapa nya Projects vid listning.
- Visa endast repositoryts korta namn i normallistan. Om flera synliga repositories har samma korta namn får `owner/repo` visas sekundärt för disambiguering; branch/status/övrig Project-metadata ska inte visas i listan.
- Lägg enkel klient-side sökning/filter som matchar case-insensitive på både kort repositorynamn och `owner/repo` medan användaren skriver.
- Ta bort den manuella "Skapa projekt"-vägen från normal routing/startsida. Gamla `/projects/new` ska säkert redirecta till repositorylistan i stället för att bli en trasig länk.
- För ett repository utan Project ska Project skapas först när användaren faktiskt startar Work. `ensureProject` ska verifiera installation/repository/default branch via samma GitHub-katalog som tidigare, återanvända befintlig Project när den finns och generera ett internt kollisionssäkert namn utan att exponera namnhantering i UI.
- Efter lazy creation ska befintliga Project/Work/import-API:er återanvändas så etablerade ägarskaps-, branch-, approval- och delivery-invarianter inte dupliceras.
- Uppdatera Shortcut claim/promotion så användaren väljer repository i stället för Project. Befintlig Project återanvänds; annars får promotion skapa Project lazy innan den vanliga `StoredUpload`-promotionen fortsätter.
- Uppdatera repository-/Work-vyer och navigationstexter så "Project" inte presenteras som det primära användarbegreppet, utan att ta bort den interna domänmodellen eller historiken.
- Lägg regression som bevisar: repositorylistning skapar inga Projects; första Starta arbete skapar exakt en Project och Work; retry återanvänder samma Project; Shortcut-promotion kan välja ett repo utan tidigare Project; sökning filtrerar på kort namn/full name; befintliga repos med Project öppnar befintlig Work-vy.

**Kvalitetsgrind för 9.13:** användaren börjar i en sökbar repositorylista som motsvarar GitHub App-åtkomsten och behöver aldrig ange ett projektnamn. Ingen Project skapas bara för att listan öppnas. Första verkliga Work/Shortcut-promotion skapar eller återanvänder exakt en owner-bunden Project och fortsätter därefter genom den befintliga säkra Work/import-pipelinen.

## Steg 9.14 - Manuell produktionsdeploy från GitHub Actions

- Lägg en explicit `workflow_dispatch`-baserad GitHub Actions-workflow för produktion där operatören anger en immutable `ZIP_GITHUB_VERSION`; en push till repositoryt får inte automatiskt deploya produktion.
- Använd GitHub Environment `production`, minimal workflow-permission (`contents: read`) och en concurrency-grupp som förhindrar parallella produktionsdeployments. Workflowen ska vägra deploy från annan ref än repositoryts default branch.
- Anslut till produktion via vanlig OpenSSH från GitHub-hosted runner utan tredjeparts-SSH-action. Serverns host key ska vara explicit pinnad via en verifierad `known_hosts`-variabel; workflowen får inte etablera trust-on-first-use genom ett okontrollerat `ssh-keyscan` vid varje deploy.
- Skapa en dedikerad OS-identitet `zip-github-deploy` för Git checkout. Den ska inte vara medlem i Docker-gruppen och deploy-SSH-nyckeln ska vara begränsad med `restrict` + forced command så nyckeln inte ger generell shell-/tunnelåtkomst.
- Lägg ett root-ägt deploy-script under `/opt/zip-github/bin` och en smal sudoers-regel som endast tillåter deploy-identiteten att köra detta script. Scriptet ska strikt validera versionsargument och aldrig `eval`-a användardata.
- Flytta ägarskapet för `/opt/zip-github/app` till deploy-identiteten så `git fetch/pull --ff-only` sker utan root. `.env` ska vara `root:zip-github-deploy` mode `0640`; deploy-scriptet får ändra endast `ZIP_GITHUB_VERSION` och ska bevara filens owner/mode.
- Deploy-scriptet ska vägra tracked lokala ändringar, aldrig köra `git clean`, hämta default-branch, dra immutable container images, köra Compose, vänta på backend readiness och kontrollera frontend.
- Ingen automatisk rollback vid misslyckad readiness eftersom databasmigreringar är forward-only. Dokumentera diagnostik och manuell rollback genom att köra samma workflow med tidigare immutable image-version.
- Dokumentera exakt serverbootstrap, SSH-nyckelinstallation, host-key-verifiering, GitHub Environment, variables/secrets och hur workflowen körs/roteras.
- Lägg release-verifiering för workflowens manuella trigger/default-branch guard/concurrency/minimala permissions samt deploy-scriptets versionsvalidering, no-`git clean`, readiness och forced-command/sudoers-modell.

**Kvalitetsgrind för 9.14:** produktion kan deployas manuellt från GitHub med en immutable version utan att använda operatörens personliga SSH-konto. Deployment-nyckeln kan endast starta den validerade deployvägen, servercheckouten uppdateras utan root, Docker/root-behörighet är kapslad bakom root-ägt script, parallella deploys förhindras och ett fel lämnar tydlig diagnostik utan riskabel automatisk rollback.

## Steg 9.15 - Användarattribuerad pull request

- Behåll Git commit author/committer enligt den autentiserade användaridentitet som låses vid Import-skapande; installation token får fortsatt användas som transportcredential för Git push.
- Skapa och återanvänd draft pull requests med den autentiserade GitHub App-användarens user access token i stället för en installation token, så GitHub attribuerar PR:n till den användare som utförde åtgärden.
- Använd samma user access token för idempotent PR-lookup före create och recovery-lookup efter osäkert create-svar.
- Browsern får fortfarande aldrig se GitHub user access token; token hämtas endast från den server-side webbsession som redan krävs av endpointen.
- Övrig repositoryautomation, Actions/status och Git transport ska fortsatt använda kortlivade installation tokens där användarattribution inte är operationens syfte.
- Lägg regression som verifierar att PR lookup/create/retry får exakt den autentiserade user access token och att PullRequestService inte längre skapar en installation token.

**Kvalitetsgrind för 9.15:** en draft PR som initieras av en inloggad användare skapas via GitHub user-to-server-auth och attribueras därför till användaren; commitens author/committer är fortsatt användarens låsta Git-identitet; inga GitHub credentials exponeras för frontend och serverns övriga installation-tokenmodell ändras inte.

## Steg 9.16 - PR-livscykel, fortsatt Work och externa branchändringar

- Ändra Work-livscykeln så skapad PR inte avslutar arbetet. Ett Work med öppen PR ska vara fortsatt aktivt och kunna ta emot fler ZIP-importer/commits på samma Work-branch; GitHub uppdaterar då samma PR automatiskt.
- Modellera minst `ACTIVE`, `PR_OPEN`, `PR_CLOSED`, `MERGED` och `ABANDONED` som tydliga Work-lägen. Migrera den senaste äldre `PULL_REQUEST_CREATED`-Work per repository/projekt till `PR_OPEN` utan att återöppna äldre historiska Work. En mergad PR avslutar Work automatiskt; en stängd PR utan merge ska inte radera arbetet utan ge användaren möjlighet att fortsätta och skapa en ny PR eller avsluta Work.
- Läs aktuell PR-state från GitHub när Work öppnas. zip-github ska observera open/closed/merged men lämna reviewkonversation, approvals, inline-kommentarer och merge-metod till GitHubs gränssnitt.
- När en öppen/stängd PR-Work får en ny ZIP ska repositorysnapshoten fortsatt låsas mot aktuell remote Work-branch HEAD. Nya commits pushas på samma branch och därmed till samma öppna PR.
- Gör remote Work HEAD till källa för aktuell Actions/check-status. Om GitHub-branchen har commits efter zip-githubs senast kända delivery ska Work-vyn tydligt visa att branchen ändrats externt och Actions ska avse remote HEAD, inte den äldre lokalt kända SHA:n.
- För varje ny review efter en extern branchändring, jämför zip-githubs senast kända Work-head med reviewns låsta remote HEAD via GitHub compare och samla berörda sökvägar.
- Markera i reviewn vilka ZIP-förändringar som överlappar dessa externa GitHub-ändringar, inklusive filer som ZIP:en skulle ta bort. Visa ett särskilt `Externa ändringar`-filter och en tydlig varning.
- Extern branchändring före review ska inte vara en policyblockerare. Om användaren väljer en överlappande sökväg ska UI:t kräva ett uttryckligt bekräftande innan godkännande/commit.
- Behåll befintlig SHA-bound stale-plan-kontroll vid delivery: ändras remote branch efter att review/snapshot låsts måste delivery stoppas och en ny review krävas.
- Efter merge ska nästa Work starta från aktuell default branch. Försök inte hålla samma Work levande över en merge eller implementera partial merge/cherry-pick i zip-github; sådana avancerade Git-operationer lämnas till GitHub.
- Lägg regression för fortsatt ZIP-import under `PR_OPEN`, PR-state open/closed/merged, remote Actions HEAD, extern branchvarning/överlappsfilter/bekräftelse och bevarad stale-plan delivery guard.

**Kvalitetsgrind för 9.16:** en skapad PR avslutar inte längre Work. Nya ZIP:ar på en öppen PR granskas mot aktuell remote Work HEAD och uppdaterar samma branch/PR. GitHub-side commits blir synliga i Work/Actions; en gammal ZIP som skulle ersätta sådana ändringar flaggas per sökväg och kräver användarbekräftelse, medan en branchändring efter låst review fortfarande stoppas av SHA-invarianten. Merge avslutar Work och nästa Work utgår från ny default-branch HEAD. Före återanvändning av ett Work med PR-metadata måste aktuell PR-status verifieras strikt mot GitHub; en redan mergad PR ska terminalisera gamla Work och skapa ett nytt Work innan importen binds. Samma PR-status verifieras igen omedelbart före Git delivery så en merge som hunnit ske efter review upptäcks vid den sista preflighten före push.

# Framtida backlog - AI- och integrationsyta

Det tidigare steg 8.4 flyttas uttryckligen utanför den aktiva fasplanen. Det ska omprövas först efter verklig användning av Actions- och Shortcut-flödena.

- Litet read-only/status-API för externa assistenter/integrationer.
- Eventuell Custom GPT Action eller MCP-adapter för små styr- och statusanrop.
- Export av aktuell GitHub-branch som AI-anpassad ZIP.
- Eventuell avancerad trevägsanalys/provenance för att upptäcka ZIP som skapats från äldre repositoryläge utan att kräva zip-github-specifik metadata i vanliga ZIP-filer.

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
- **Ska inte kombineras med MVP-arbetet:** fas 8 och 9.
- **Framtida backlog:** AI-/assistantintegrationer genomförs först när ett konkret behov har validerats efter fas 8–9.

Den rekommenderade arbetsformen är därför: **en prompt per steg, en eller flera steg per fas, och en kvalitetsgrind efter varje fas**.

## Steg 9.17 - Explicit commit- och PR-metadata

- Nya interaktiva importer ska börja med tomt commitmeddelande. Användaren måste skriva ett icke-tomt commitmeddelande innan urvalet kan godkännas; den äldre deterministiska texten får endast finnas kvar som kompatibilitetsfallback för historiska approvals/interna legacy-anrop.
- PR-skapande ska inte längre använda en genererad standardtitel eller standardbeskrivning. Användaren ska ange både titel och beskrivning explicit innan draft-PR:n skapas.
- Validera PR-titel och PR-beskrivning både i frontend och backend, med tydliga längdgränser och `400 PULL_REQUEST_METADATA_INVALID` för ogiltig interaktiv metadata.
- Visa PR-komponeringen innan GitHub-anropet både från Work-vyn och post-commit-resultatet. Fälten ska vara vanliga redigerbara textfält och ska inte innebära något implicit godkännande.
- Lägg till en frivillig snabbåtgärd `Fyll från commitmeddelanden` som hämtar commits som faktiskt ingår i aktuellt Work (efter Workens base SHA) och fyller PR-beskrivningen med dessa i kronologisk ordning. Användaren ska därefter kunna redigera, ta bort och komplettera texten fritt.
- Snabbfyllningen får inte använda fallback-/osäker historik som om den vore faktisk GitHub-historik. Om commitmeddelandena inte kan läsas säkert ska användaren få skriva beskrivningen manuellt.
- Behåll användarattribuerad PR-create från 9.15 och PR-livscykeln från 9.16 oförändrade; detta steg ändrar endast användarkontrollerad metadata och hur den samlas in.
- Lägg regression för tomt commitmeddelande, obligatorisk PR-titel/beskrivning, överförd explicit metadata, snabbfyllning från endast Work-commits och fortsatt redigerbar beskrivning.

**Kvalitetsgrind för 9.17:** inga nya commits eller PR:er får skapas med zip-github-genererad standardtext i det interaktiva flödet. Commitmeddelande, PR-titel och PR-beskrivning kräver användarens explicita innehåll; PR-beskrivningen kan frivilligt fyllas från de faktiska commitmeddelandena i aktuellt Work men förblir helt redigerbar före skapandet.

## Steg 9.18 - Deduplicerad GitHub Actions/check-presentation

- Behåll workflow-runs och deras jobs som primär presentation av GitHub Actions-status; repositoryts workflow-konfiguration ska inte ändras för att lösa UI-dubbleringen.
- Innan den sekundära check-listan renderas ska zip-github identifiera checks från appen `GitHub Actions` som redan motsvaras av ett visat workflow-jobb för samma commit.
- Matchningen ska vara konservativ och baseras på normaliserat jobb/check-namn. En check får endast döljas när appen är GitHub Actions och motsvarande jobb redan visas; osäkra eller omatchade checks ska aldrig tappas.
- Checks från andra appar, till exempel CodeQL eller externa CI-/security-appar, ska fortsätta visas. GitHub Actions-checks utan motsvarande laddat workflow-jobb ska också visas.
- Den sekundära rubriken ska heta `Övriga kontroller` och hela sektionen ska utelämnas när samtliga checks redan representeras av visade workflow-jobs.
- Behåll sammanvägd commitstatus, workflowstatus, jobbstatus, artifacts, feldiagnostik, loggar och rerun-/dispatchfunktioner oförändrade.
- Lägg regression för dels ett normalt CI-jobb som annars skulle visas dubbelt, dels blandade checks där externa/omatchade kontroller måste ligga kvar.

**Kvalitetsgrind för 9.18:** samma GitHub Actions-jobb visas högst en gång i Actions-panelen när dess workflow-jobb finns tillgängligt. Ingen check från annan app eller omatchad GitHub Actions-check får försvinna, och `Övriga kontroller` visas endast när det faktiskt finns ytterligare kontroller.

## Steg 9.19 - Prospektiv `.gitignore` och massval i review

- Behandla varje uppladdad ZIP som den kompletta tänkta repositorybilden även för `.gitignore`: jämförelsen ska använda `.gitignore`-filer från ZIP:en där de finns, ta bort repository-regler vars `.gitignore` saknas i ZIP:en och endast falla tillbaka på befintliga repository-regler för `.gitignore`-filer som fortsatt finns.
- Nya ZIP-filer som matchar den prospektiva `.gitignore` ska klassificeras `IGNORED` redan i jämförelse/importplan, vara icke-valbara och aldrig nå approval/workspace/delivery. Tracked repositoryfiler ska fortsatt behandlas som tracked även om den nya `.gitignore` matchar dem.
- Behåll exakt selection/workspace-invariant. Om den ändå bryts ska felet lista saknade respektive oväntade paths i stället för endast ett generiskt mismatch-meddelande.
- Lägg kategori-/filterbaserat massval i review för vanliga valbara förändringar. Massval ska endast påverka poster i den aktiva kategorin och får aldrig inkludera hårt blockerade paths.
- För överstyrbara poster ska ett enda uttryckligt massgodkännande kunna lägga till eller ta bort både override-audit och selection för samtliga överstyrbara poster i den aktiva kategorin. Detta ska ge samma auditsemantik som individuella overrides, inte kringgå blockerpolicyn.
- Lägg regression för ZIP som både inför `__pycache__/`/`*.pyc` i `.gitignore` och innehåller sådana nya filer, för borttagen repository-`.gitignore`, samt för massgodkännande av många deletions där en hårt blockerad path finns i samma kategori.

**Kvalitetsgrind för 9.19:** review-planen och den slutliga Git-committen använder samma prospektiva ignore-semantik för den kompletta ZIP:en. En fil som ZIP:ens egen `.gitignore` gör ignorerad kan inte hamna i selectionen, workspace-mismatch är diagnostiskt, och användaren kan godkänna stora grupper av överstyrbara exempelvis borttagningar med ett enda uttryckligt kategori-godkännande utan att hårt blockerade paths kan läcka in.


## Steg 9.20 - Förenklad PR-metadata från commitmeddelanden

- Behåll `Fyll från commitmeddelanden` som frivillig hjälp och samma säkra Work-commitkälla från steg 9.17.
- PR-beskrivningen ska fyllas med endast den kronologiska Markdown-listan av Workens commitmeddelanden. Lägg inte till någon genererad rubrik som `Ingående commits`.
- Om PR-titeln är tom när snabbfyllningen används ska den sättas till första commitmeddelandets första rad i samma kronologiska lista, begränsad till PR-titelns befintliga maxlängd.
- Om användaren redan har angett en icke-tom PR-titel får snabbfyllningen aldrig skriva över den.
- Beskrivning och eventuell automatiskt satt titel ska fortsatt vara fullt redigerbara innan PR skapas. Backendvalidering och användarattribuerad PR-create ändras inte.
- Lägg regression för ren beskrivningslista, automatisk titel från första committen och bevarande av redan angiven titel.

**Kvalitetsgrind för 9.20:** snabbfyllningen skapar ingen extra rubrik i PR-beskrivningen, en tom titel får ett användbart defaultvärde från första kronologiska Work-committen och explicit användarinmatad titel bevaras alltid.


## Steg 9.21 - Gemensam repository-picker och senaste repositories

- Återanvänd samma repository-picker på repository-startsidan och i Shortcut claim-flödet så sökning, sortering och framtida ranking inte divergerar mellan vyerna.
- Behåll den fullständiga repositorylistan i sin befintliga alfabetiska ordning för scenariot där användaren arbetar med ett nytt repository.
- Lägg sökning på både repositorynamn och fullständigt `owner/repository` även i Shortcut-flödet.
- Begränsa repositorylistans visuella höjd och gör listan separat scrollbar så sidans fortsätt-/primäråtgärd inte flyttas långt ned när många repositories finns.
- Visa högst fem senast använda repositories ovanför den fullständiga listan. I detta steg får recency vara en klient-side convenience och får inte ändra backendbehörighet eller dölja repositories från den fullständiga listan.
- I Shortcut-flödet ska valt repository alltid sammanfattas direkt ovanför `Fortsätt till granskning`, inklusive fullständigt repositorynamn, så valet är synligt även om den valda raden scrollats ur listan.
- Auto-scrolla inte sidan efter repositoryval; UI:t ska förbli stabilt.
- Lägg regression för Shortcut-sökning, recency-sektion och synlig selected-repository summary.

**Kvalitetsgrind för 9.21:** användaren ska kunna hitta ett repository via samma sökbara picker på båda huvudyta och Shortcut claim, långa listor ska scrolla inuti sin egen yta, nyligen använda repositories ska vara snabbåtkomliga utan att den alfabetiska fullistan ändras, och Shortcut-flödet ska alltid visa aktuellt val intill fortsätt-åtgärden.

## Steg 9.22 - Smart Shortcut-förslag av repository

- Bygg vidare på den gemensamma pickern från 9.21 och beräkna ett explicit repositoryförslag när en Shortcut-ZIP ska kopplas till repository.
- Rankingen ska i första hand använda normaliserad filnamnslikhet mot repositorynamn/fullständigt namn, inklusive vanliga repo-prefix som `roman-`, `bradspel-` och `pwa-` utan att kräva projektspecifik hårdkodning.
- Använd tidigare originella uploadfilnamn för samma project/repository som stark signal när en ny ZIP har samma stabila namn-prefix men annan revision/version/datum-suffix.
- Ge nyligen uppdaterade/använda repositories en mindre recency-bonus, men låt aldrig recency slå en tydligt starkare namn-/historikmatch.
- Normalisera bort vanliga revisions-/release-suffix (`r0042`, `v0.8.8`, `rc.89`, datum, `release`, `repo-cleanup` och liknande) före jämförelse där det kan göras deterministiskt utan att förlora den stabila projektdelen.
- Ett hög-confidence-förslag ska visas som `Föreslaget repository` med en primär bekräftelseåtgärd och en tydlig `Välj ett annat repository`-väg till 9.21-pickern. Ingen heuristik får fortsätta eller promota importen utan användarens uttryckliga bekräftelse.
- Vid låg confidence eller flera nära kandidater ska inget repository förväljas; visa i stället recency + sökbar alfabetisk lista.
- Persistens/serverhistorik som behövs för tidigare uploadfilnamn ska vara användar-/project-isolerad och får inte exponera metadata mellan användare eller otillgängliga repositories.
- Lägg regression för stark exakt/prefixmatch, tidigare-uploadmatch, flera tvetydiga kandidater, recency som tie-breaker och att förslag aldrig innebär implicit promotion.

**Status:** DONE i r0138 / 1.0.0-rc.90. Se `docs/step-9.22-report.md`.

**Kvalitetsgrind för 9.22:** Shortcut-flödet ska i återkommande projekt ofta kunna presentera ett trovärdigt repositoryförslag utan sökning, men ett osäkert förslag får aldrig automatiskt välja/promota repository och den fullständiga 9.21-pickern ska alltid finnas som fallback.


## Steg 9.23 - Produktnamn i aktiv webbklient

- Ändra den aktiva webbklientens dokumenttitel från legacy-namnet `zip-buildserver` till produktnamnet `zip-GitHub`, så webbläsarfliken stämmer med tjänsten.
- Inventera kvarvarande `zip-buildserver`-referenser och ändra endast sådana som hör till aktiv produkt/runtime. Historik, migrationsunderlag och uttryckligt legacy-material ska behålla det historiska namnet.
- Lägg en release-regression som verifierar att `frontend/index.html` använder `zip-GitHub` och inte `zip-buildserver`.

**Status:** DONE i r0139 / 1.0.0-rc.91. Se `docs/step-9.23-report.md`.

**Kvalitetsgrind för 9.23:** den aktiva webbklientens webbläsarflik ska visa `zip-GitHub`, samtidigt som legacy-dokumentation fortfarande får beskriva `zip-buildserver` där det historiskt är korrekt.

## Steg 9.24 - Explicita beslut för blockerande review-poster

- Överstyrbara blockerare ska börja i ett obesvarat läge. Användaren får inte leverera importen förrän varje sådan post uttryckligen har beslutats som antingen `Ta inte med` eller `Godkänn och ta med`.
- Ett uttryckligt `Ta inte med` ska vara ett förstaklassbeslut, inte samma sak som att användaren aldrig interagerat med posten. Ett uttryckligt override ska fortsatt ge befintlig override-audit och selection-semantik.
- Hårt blockerade poster får aldrig väljas, men användaren ska behöva bekräfta att de har sett att respektive post kommer att utelämnas innan leverans kan fortsätta.
- Review-sammanfattningen ska tydligt visa hur många blockerande poster som fortfarande kräver beslut och primär leveransåtgärd ska vara spärrad så länge antalet är större än noll.
- Behåll kategori-massvalet från 9.19 och komplettera det med säkra massbeslut där det minskar klick utan att skapa implicit godkännande: uttryckligt exkludera överstyrbara poster respektive befintligt explicit bulk-override. Hårt blockerade poster ska fortfarande aldrig kunna inkluderas.
- Backend/approval-kontraktet ska validera den explicita beslutsmodellen så att en manipulerad klient inte kan kringgå kravet genom att bara utelämna obesvarade blockerare.
- Lägg regression för obesvarad blockerare som stoppar leverans, explicit exkludering, explicit override+selection, hård blockerare som kräver acknowledgement men inte kan väljas, bulkbeslut samt oförändrad exact-selection-invariant.

**Status:** DONE i r0140 / 1.0.0-rc.92.

**Kvalitetsgrind för 9.24:** ingen blockerande review-post får passera obemärkt. Varje överstyrbar blockerare måste ha ett explicit inkluderings- eller exkluderingsbeslut och varje hård blockerare ett explicit acknowledgement innan leverans, samtidigt som hårt blockerade paths förblir omöjliga att välja och backend upprätthåller samma krav som UI:t.

## Steg 9.25 - Säker global städning av föräldralösa Work-brancher

- Lägg en samlad underhållsvy som inventerar zip-GitHub Work-brancher över samtliga repositories som den autentiserade användaren får se via GitHub App-installationerna, så städning inte kräver repository-för-repository-klick.
- Kandidater ska begränsas strikt till zip-GitHubs egen branch-namespace, exempelvis `zip-github/work-*`. Default branch, protected branches och alla andra branchnamn ska alltid vara icke-raderbara via funktionen.
- Klassificera en branch som säker att radera endast när backend kan verifiera att den saknar koppling till en icke-terminal Work, inte används som head för en öppen pull request och fortfarande ligger i ett repository/an installation som användaren har behörighet till.
- Osäker eller ofullständig GitHub-/databasstatus ska alltid ge `kan inte verifieras säkert` och därmed ingen delete-möjlighet. Hellre falskt negativt än risk för att ta bort aktivt arbete.
- Visa först en read-only förhandsgranskning med totalsiffror och orsak per branch. Radering ska kräva ett separat explicit bulkbeslut och backend ska göra en ny säkerhetskontroll direkt före varje delete för att minimera race mellan preview och mutation.
- Ingen automatisk eller schemalagd branchradering införs i detta steg. Fel vid en branch ska inte få efterföljande brancher att antas säkra; resultatet ska rapporteras per repository/branch.
- Lägg regression för aktiv Work, öppen PR, merged/terminal Work, protected/default branch, främmande branch-prefix, behörighetsbortfall, stale preview/race och blandat bulkresultat.

**Status:** DONE i r0143 / 1.0.0-rc.95. Se `docs/step-9.25-report.md`.

**Kvalitetsgrind för 9.25:** bulkstädning får endast erbjuda och radera brancher som zip-GitHub med hög säkerhet kan bevisa är föräldralösa. Osäkerhet ska alltid stoppa deletion, användaren ska se en preview och radering ska aldrig ske automatiskt.


## Steg 9.26 - Grupperad presentation av workflow-runs för samma commit

- Behåll GitHub workflow-runs som fullständig källa och radera eller filtrera inte bort separata körningar som GitHub faktiskt har skapat.
- Gruppera toppnivåpresentationen när flera runs hör till samma workflow-identitet och samma commit, typiskt när både `push` och `pull_request` triggas efter en ny commit på ett Work som redan har en öppen PR.
- Workflow-identiteten ska i första hand använda GitHubs `workflowId`; endast när det saknas får stabil workflow-path/namn användas som fallback. Två olika workflows med samma visningsnamn får inte slås ihop.
- Gruppens sammanfattade status ska vara konservativ: ett misslyckat run får inte döljas av ett lyckat run, och pågående/köad status ska fortsatt synas när ingen körning har misslyckats.
- Visa workflowet en gång i huvudlistan och ange antalet GitHub-körningar. Användaren ska kunna expandera gruppen och se varje separat event/run, dess status, jobb och GitHub-länk.
- Behåll dedupliceringen mellan workflow-jobs och `Övriga kontroller` från 9.18 över samtliga runs; grupperingen får inte göra att externa eller omatchade checks försvinner.
- Lägg regression för `push` + `pull_request` på samma workflow/commit, blandad success/failure samt två olika workflow-ID:n med samma visningsnamn.

**Status:** DONE i r0145 / 1.0.0-rc.97. Se `docs/step-9.26-report.md`.

**Kvalitetsgrind för 9.26:** samma workflow för samma commit ska bara ta en toppnivåplats i Actions-panelen även när GitHub skapat flera runs, men samtliga runs/statusar/jobs/länkar ska finnas kvar i den expanderbara detaljen och ett failure får aldrig maskeras av ett success-run.

## Steg 9.27 - Bekräftelse innan ett Work med öppen PR utökas

- När användaren försöker starta/ladda upp nästa ZIP till ett Work som redan har en öppen pull request ska zip-GitHub tydligt varna att den nya ZIP:en kommer att läggas på samma Work-branch och därmed uppdatera den befintliga PR:n.
- Flödet ska fortsatt tillåta detta eftersom en ny ZIP kan vara en avsiktlig rättning eller komplettering till PR:n, men användaren måste uttryckligen bekräfta `Ja, fortsätt med nästa ZIP` innan upload/import fortsätter.
- Varningen ska visa aktuell PR-identitet/länk när den finns och erbjuda ett tydligt avbryt-alternativ.
- Kontrollera aktuell PR-status genom befintlig GitHub reconciliation innan beslutet används. Om PR:n hunnit mergas ska befintlig terminal Work-logik vinna och en ny import ska starta från aktuell default branch i stället för att fortsätta gammalt Work.
- Skyddet ska ligga i själva new-import-flödet och inte bara på en enskild navigeringsknapp, så direkt navigation eller andra ingångar inte kringgår varningen.
- Bekräftelsen är avsiktligt per ny import och ska inte bli en permanent inställning som tystar framtida varningar.
- Lägg regression för `PR_OPEN` + avbryt, `PR_OPEN` + explicit fortsätt, ingen varning för Work utan PR samt merged-PR reconciliation.

**Status:** NEXT.

**Kvalitetsgrind för 9.27:** en ny ZIP får aldrig oavsiktligt läggas på ett Work med en redan öppen PR utan att användaren fått en tydlig, aktuell varning och uttryckligen valt att fortsätta; avsiktliga PR-rättningar ska samtidigt förbli möjliga.
