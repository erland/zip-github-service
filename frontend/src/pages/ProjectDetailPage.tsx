import { Link, useParams } from 'react-router-dom';
import { demoProjects } from '../data/demoProjects';

export default function ProjectDetailPage() {
  const { projectId } = useParams();
  const project = demoProjects.find((candidate) => candidate.id === projectId);

  if (!project) {
    return (
      <section className="page-card">
        <p className="eyebrow">Projekt</p>
        <h1>Projektet hittades inte</h1>
        <p>Projektet finns inte i den tillfälliga frontendskalets exempeldata.</p>
        <Link className="button" to="/projects">Till projektlistan</Link>
      </section>
    );
  }

  return (
    <section className="page-card" aria-labelledby="project-heading">
      <p><Link className="back-link" to="/projects">← Alla projekt</Link></p>
      <div className="page-heading-row">
        <div>
          <p className="eyebrow">Projekt</p>
          <h1 id="project-heading">{project.name}</h1>
          <p className="lead">{project.repository}</p>
        </div>
        <Link className="button" to={`/projects/${project.id}/imports/new`}>Ny import</Link>
      </div>

      <dl className="detail-grid">
        <div><dt>Repository</dt><dd>{project.repository}</dd></div>
        <div><dt>Standardbranch</dt><dd>{project.defaultBranch}</dd></div>
        <div><dt>Leveransläge</dt><dd>Importbranch och pull request</dd></div>
        <div><dt>Workflowskydd</dt><dd><code>.github/**</code> blockeras i MVP</dd></div>
      </dl>

      <div className="empty-state">
        <h2>Importhistorik</h2>
        <p>Det finns ännu inga importer för projektet.</p>
      </div>
    </section>
  );
}
