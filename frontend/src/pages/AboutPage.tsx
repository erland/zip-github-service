const runtimeVersion = import.meta.env.VITE_ZIP_GITHUB_VERSION?.trim() || 'development';

export default function AboutPage() {
  return (
    <section className="page-card">
      <p className="eyebrow">Om tjänsten</p>
      <h1>zip-GitHub</h1>
      <p className="lead">zip-GitHub hjälper användaren att föra över en projekt-ZIP till en säker granskningsyta och fortsätta till GitHub först efter uttryckligt godkännande.</p>

      <h2>Så fungerar det</h2>
      <ol>
        <li>En ZIP-fil skickas till en tillfällig staging.</li>
        <li>Användaren väljer repository och granskar planerade ändringar i webbgränssnittet.</li>
        <li>GitHub-leverans sker först när användaren uttryckligen godkänner den.</li>
      </ol>

      <p>Staging i sig ändrar inte GitHub och uppladdad projektkod körs inte av zip-GitHub-backenden.</p>

      <dl className="result-link-grid" aria-label="Tjänsteinformation">
        <div><dt>Utvecklare</dt><dd>Erland Lindmark</dd></div>
        <div><dt>Version</dt><dd>{runtimeVersion}</dd></div>
        <div><dt>Källkod</dt><dd><a href="https://github.com/erland/zip-github-service">GitHub</a></dd></div>
      </dl>

      <h2>Information och support</h2>
      <ul>
        <li><a href="/support">Support</a></li>
        <li><a href="/privacy">Integritetspolicy</a></li>
        <li><a href="/terms">Användarvillkor</a></li>
      </ul>
    </section>
  );
}
