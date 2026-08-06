import { Link } from 'react-router-dom';
import { demoProjects } from '../data/demoProjects';

export default function ProjectListPage() {
  return (
    <section className="page-card" aria-labelledby="project-list-heading">
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">Projektöversikt</p>
          <h1 id="project-list-heading">Dina projekt</h1>
          <p className="lead">Välj ett projekt för att granska repositorykopplingen eller starta en ny ZIP-import.</p>
        </div>
        <button className="button button--secondary" disabled title="Aktiveras när GitHub-kopplingen är implementerad">
          Skapa projekt
        </button>
      </div>

      <ul className="project-list">
        {demoProjects.map((project) => (
          <li className="project-card" key={project.id}>
            <div>
              <h2><Link to={`/projects/${project.id}`}>{project.name}</Link></h2>
              <p className="repository-name">{project.repository}</p>
            </div>
            <dl className="project-meta">
              <div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div>
              <div><dt>Senaste import</dt><dd>{project.latestImportStatus}</dd></div>
            </dl>
            <Link className="button" to={`/projects/${project.id}`}>Öppna projekt</Link>
          </li>
        ))}
      </ul>

      <p className="prototype-note">Visningen använder tillfälliga exempeldata tills frontend kopplas till API:t och GitHub-inloggningen.</p>
    </section>
  );
}
