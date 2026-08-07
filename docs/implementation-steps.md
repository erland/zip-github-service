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

## Steg 9.1 - Definiera och persistiera StagingImport-livscykeln

- Inför en separat `StagingImport`-modell/tabell för kortlivad, ännu inte användarägd ZIP med staging-id, storage metadata/SHA-256, skapad tid, expiry, status och claim-token-hash.
- Statusmodellen ska minst skilja på `AVAILABLE`, `CLAIMED`, `PROMOTED`, `EXPIRED` och `CANCELLED`/motsvarande terminalt läge.
- Spara aldrig claim-token i klartext och använd aldrig claim-/upload-token som `sourceReference` eller audittext.
- Staging ska referera till samma neutrala lagringsartefakt som `ZipIngestionService` producerar; inga ZIP-bytes ska kopieras eller streamas om vid promotion.
- Dokumentera transaktions-/låsningskrav så endast en användare kan claima ett stagingobjekt och promotion kan göras idempotent.

## Steg 9.2 - Lägg capability-skyddad staging-upload

- Lägg ett smalt upload-endpoint avsett för Shortcut/andra enkla klienter som accepterar en ZIP via befintlig `ZipIngestionService`.
- Skydda endpointen med en separat roterbar upload capability som endast ger rätt att skapa en staging-upload; capabilityn är uttryckligen **inte användarautentisering** och får aldrig ge list/read/claim/project/GitHub-behörighet.
- Capability skickas i header, aldrig i URL; lägg hård rate limiting, samma ZIP-/storleksgränser som webbuppladdning och generisk felinformation.
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

## Steg 9.5 - Retention, abuse-skydd och säkerhetsregression för staging

- Städa oclaimade stagingobjekt automatiskt efter kort konfigurerbar TTL, initialt cirka en timme; terminala/utgångna artifacts ska raderas deterministiskt.
- Definiera en kort säker frist för claimade men ännu inte promoverade objekt och säkerställ att promotion/cleanup inte kan race:a.
- Rate-limita per capability och nätverkskälla där möjligt och sätt tak för samtidiga staginguploads/lagringsvolym så en läckt capability inte kan fylla disken.
- Säkerställ tokenrotation utan datamigrering och dokumentera incidentåtgärd vid läckt upload capability.
- Lägg tester för token guessing/reuse, concurrent claim, cross-user isolation, expired claim, cleanup race, överstor ZIP och att staging aldrig kan nå GitHub-operationer före promotion.

## Steg 9.6 - Dokumentera och skapa referens-Shortcut för iOS

- Dokumentera ett minimalt iOS Shortcut-flöde som tar emot en ZIP från Share Sheet/Filer, anropar staging-upload med capability-header och öppnar returnerad claim-URL i standardwebbläsaren.
- Shortcut-klienten ska inte innehålla GitHub-token, project-id eller user-id och ska inte behöva förstå zip-githubs importpolicy.
- Beskriv hur upload capability läggs in/byter värde i Shortcut och att den endast skyddar upload mot allmän abuse, inte identifierar användaren.
- Ge tydliga felvägar för offline, 413/429, utgången claim och serverfel samt undvik att logga claim-token i Shortcut-notiser/loggtext.
- Om en exporterbar Shortcut-fil inte kan genereras reproducerbart, leverera i stället en exakt bygginstruktion med actions, headers, request body och förväntat svar.

## Steg 9.7 - E2E-regression, drift och releasegrind för Shortcut/StagingImport

- E2E-testa `Share/Shortcut -> staging upload -> öppna claim -> login -> claim -> välj projekt -> promotion -> automatisk review` utan andra ZIP-bytes eller parallell pipeline.
- Testa logout/login, backend-restart och retry kring claim/promotion så varken stagingobjekt eller vanlig import dupliceras.
- Testa två användare och samtidiga claimförsök, aktiv-import-konflikt samt expiry/cleanup.
- Verifiera att efter promotion är plan/selection/delivery byte- och digest-ekvivalent med samma ZIP via vanlig web upload mot samma base SHA.
- Uppdatera operations, threat model, API-kontrakt, releasechecklista och Shortcut-installationsguide.

**Kvalitetsgrind för fas 9:** en ZIP kan skickas från iOS till en kortlivad, icke-GitHub-auktoriserad stagingyta, claimas av exakt en autentiserad användare och promoveras utan reupload till samma vanliga importpipeline. Oclaimade uploads kan inte listas/läsas, tokens lagras inte i klartext, abuse begränsas och staging ger aldrig repositoryåtkomst i sig.

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
