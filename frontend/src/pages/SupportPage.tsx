export default function SupportPage() {
  return (
    <section className="page-card">
      <p className="eyebrow">Support</p>
      <h1>Support för zip-GitHub</h1>
      <p className="lead">zip-GitHub är ett verktyg för att föra över en projekt-ZIP till en granskningsyta och därefter, efter användarens uttryckliga godkännande, fortsätta leveransen till GitHub.</p>
      <h2>Få hjälp</h2>
      <p>För felrapporter, frågor och förbättringsförslag använder du projektets publika GitHub Issues.</p>
      <p><a className="button" href="https://github.com/erland/zip-github-service/issues">Öppna GitHub Issues</a></p>
      <h2>Innan du rapporterar</h2>
      <ul>
        <li>Ta inte med access tokens, sessionscookies, signerade temporära URL:er eller andra hemligheter.</li>
        <li>Beskriv gärna vilket steg som misslyckades: staging, repositoryval, granskning eller GitHub-leverans.</li>
        <li>För känslig information, publicera inte innehållet i ett öppet issue.</li>
      </ul>
      <p>Utvecklare: Erland Lindmark. Källkod och versionshistorik finns i det publika GitHub-repot.</p>
    </section>
  );
}
