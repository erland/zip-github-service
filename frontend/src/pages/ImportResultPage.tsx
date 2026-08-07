import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getDelivery, getImportChecks, GitDeliveryResponse, ImportCheckStatusResponse } from '../api/imports';
import { createWorkPullRequest } from '../api/projects';

export default function ImportResultPage() {
  const { projectId, importId } = useParams();
  const [result, setResult] = useState<GitDeliveryResponse | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [checks, setChecks] = useState<ImportCheckStatusResponse | null>(null);
  const [finishing, setFinishing] = useState(false);
  const [pullRequestUrl, setPullRequestUrl] = useState('');

  useEffect(() => {
    if (!importId) { setError('Import-ID saknas.'); setLoading(false); return; }
    getDelivery(importId).then(setResult).catch((reason) => setError(reason instanceof Error ? reason.message : 'Commitresultatet kunde inte hämtas.')).finally(() => setLoading(false));
  }, [importId]);

  useEffect(() => {
    if (!importId || !result) return;
    getImportChecks(importId).then(setChecks).catch(() => undefined);
  }, [importId, result]);

  async function finishWork() {
    if (!projectId || finishing || pullRequestUrl) return;
    setFinishing(true); setError('');
    try { const created = await createWorkPullRequest(projectId); setPullRequestUrl(created.pullRequestUrl); }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Pull request kunde inte skapas.'); }
    finally { setFinishing(false); }
  }

  const links = useMemo(() => result ? githubLinks(result) : null, [result]);
  return <section className="page-card result-page" aria-labelledby="result-heading">
    <p><Link className="back-link" to={projectId ? `/projects/${projectId}` : '/projects'}>← Till projektet</Link></p>
    <p className="eyebrow">Importresultat</p>
    <h1 id="result-heading">Commit skapad</h1>
    <p className="lead">ZIP-importen är committad på projektets arbetsbranch. Du kan fortsätta med nästa ZIP eller avsluta arbetet och skapa en pull request direkt här.</p>
    {loading && <p role="status">Hämtar resultat…</p>}
    {error && <p role="alert" className="status-message status-message--error">{error}</p>}
    {result && links && <>
      <div className="result-success" role="status"><div><strong>Importen är committad</strong><p>Arbetsbranchen är uppdaterad med den godkända ZIP-filen.</p></div><span className="status-badge">PUSHED</span></div>
      {pullRequestUrl && <p className="status-message" role="status">Arbetets pull request är skapad. <a href={pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a></p>}
      <div className="result-primary-action">
        {projectId && <Link className="button" to={`/projects/${projectId}/imports/new`}>Ladda upp nästa ZIP</Link>}
        {projectId && <button className="button button--secondary" type="button" disabled={finishing || Boolean(pullRequestUrl)} onClick={finishWork}>{pullRequestUrl?'Pull request skapad':finishing?'Skapar pull request…':'Arbetet är klart – skapa pull request'}</button>}
      </div>
      <dl className="result-link-grid">
        <ResultLink label="Repository" value={result.repositoryFullName} href={links.repository} />
        <ResultLink label="Arbetsbranch" value={result.branchName} href={links.branch} />
        <ResultLink label="Commit" value={result.commitSha.slice(0,12)} href={links.commit} />
        <ResultLink label="GitHub Actions" value="Öppna Actions" href={links.actions} />
      </dl>
      {checks && <p className="result-status-note">Checks: {checks.successful} lyckade · {checks.pending} pågående · {checks.failed} misslyckade.</p>}
    </>}
  </section>;
}
function ResultLink({label,value,href}:{label:string;value:string;href:string}) { return <div><dt>{label}</dt><dd><a href={href} target="_blank" rel="noreferrer">{value}</a></dd></div>; }
function githubLinks(result: GitDeliveryResponse) { const repository=`https://github.com/${result.repositoryFullName}`; return { repository, branch:`${repository}/tree/${encodeURIComponent(result.branchName)}`, commit:`${repository}/commit/${result.commitSha}`, actions:`${repository}/actions?query=${encodeURIComponent(`branch:${result.branchName}`)}` }; }
