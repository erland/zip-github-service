export default function AboutPage() {
  return (
    <section className="page-card">
      <p className="eyebrow">Om tjänsten</p>
      <h1>ZIP till GitHub med granskning</h1>
      <p className="lead">zip-github ska jämföra ett projektarkiv med en vald GitHub-branch och skapa en pull request först efter uttryckligt godkännande.</p>
      <h2>Produktprinciper</h2>
      <ul>
        <li>GitHub är den beständiga projektkällan.</li>
        <li>Varje användares projekt och importer ska vara isolerade.</li>
        <li>Uppladdad projektkod körs inte i tjänstens backend.</li>
        <li>GitHub Actions ansvarar för byggen, tester och publicering.</li>
      </ul>
    </section>
  );
}
