export default function PrivacyPage() {
  return (
    <section className="page-card">
      <p className="eyebrow">Integritet</p>
      <h1>Integritetspolicy för zip-GitHub</h1>
      <p className="lead">Den här sidan beskriver vilka uppgifter zip-GitHub behandlar när du använder webbgränssnittet eller MCP-integrationen.</p>
      <h2>Uppgifter som behandlas</h2>
      <ul>
        <li>GitHub-identitet och uppgifter som behövs för inloggning och åtkomst till de GitHub-resurser du uttryckligen har tillåtit.</li>
        <li>Projektarkiv som du laddar upp eller skickar via MCP, tillsammans med teknisk metadata som filnamn, storlek och checksumma.</li>
        <li>Repository-, branch-, import- och granskningsmetadata som krävs för att visa och genomföra arbetsflödet.</li>
        <li>Tekniska loggar som behövs för drift, felsökning och säkerhet. Hemligheter och signerade temporära fil-URL:er ska inte avsiktligt loggas.</li>
      </ul>
      <h2>Hur uppgifterna används</h2>
      <p>Uppgifterna används för att skapa en tillfällig staging, jämföra innehåll, visa en granskning och, först efter uttryckligt godkännande, utföra vald GitHub-leverans. Uppladdad projektkod körs inte av zip-GitHub-backenden.</p>
      <h2>Delning</h2>
      <p>GitHub används för autentisering och för de repositoryåtgärder du uttryckligen initierar. zip-GitHub säljer inte personuppgifter och använder inte projektinnehåll för annonsering.</p>
      <h2>Lagring</h2>
      <p>Tillfälliga staging-uppladdningar har begränsad livslängd och tas bort automatiskt när de löper ut. Metadata som behövs för projekt- och importhistorik kan lagras längre enligt tjänstens funktionella behov.</p>
      <h2>Dina val</h2>
      <p>Du styr vilka GitHub-resurser som appen får åtkomst till genom GitHub och kan återkalla den åtkomsten där. Du kan också avstå från att godkänna en planerad GitHub-leverans.</p>
      <h2>Kontakt</h2>
      <p>Frågor om integritet eller databehandling kan tas via <a href="/support">support-sidan</a>.</p>
      <p><small>Senast uppdaterad: 6 oktober 2026.</small></p>
    </section>
  );
}
