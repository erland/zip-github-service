import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getImportChecks, getPullRequest, ImportCheckStatusResponse, PullRequestResponse } from '../api/imports';

export default function ImportResultPage() {
  const { projectId, importId } = useParams();
  const [result, setResult] = useState<PullRequestResponse | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [checks, setChecks] = useState<ImportCheckStatusResponse | null>(null);
  const [checksError, setChecksError] = useState('');

  useEffect(() => {
    if (!importId) {
      setError('Import-ID saknas.');
      setLoading(false);
      return;
    }
    let active = true;
    getPullRequest(importId)
      .then((loaded) => { if (active) setResult(loaded); })
      .catch((reason) => { if (active) setError(reason instanceof Error ? reason.message : 'Importresultatet kunde inte hämtas.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [importId]);

  useEffect(() => {
    if (!importId || !result) return;
    let active = true;
    let attempts = 0;
    let timer: number | undefined;
    const poll = async () => {
      attempts += 1;
      try {
        const loaded = await getImportChecks(importId);
        if (!active) return;
        setChecks(loaded);
        setChecksError('');
        if (!loaded.terminal && attempts < 12) timer = window.setTimeout(poll, 10_000);
      } catch (reason) {
        if (!active) return;
        setChecksError(reason instanceof Error ? reason.message : 'Checkstatus kunde inte hämtas.');
      }
    };
    void poll();
    return () => { active = false; if (timer) window.clearTimeout(timer); };
  }, [importId, result]);

  const links = useMemo(() => result ? githubLinks(result) : null, [result]);

  return (
    <section className="page-card result-page" aria-labelledby="result-heading">
      <p><Link className="back-link" to={projectId ? `/projects/${projectId}` : '/projects'}>← Till projektet</Link></p>
      <p className="eyebrow">Importresultat</p>
      <h1 id="result-heading">Pull request skapad</h1>
      <p className="lead">Importen finns nu i GitHub. Länkarna nedan bygger på sparad resultatmetadata och kan öppnas även om statusintegrationen är otillgänglig.</p>

      <ol className="step-list" aria-label="Importflöde">
        <li><span>1</span><strong>Välj ZIP</strong></li>
        <li><span>2</span><strong>Granska förändringar</strong></li>
        <li className="step-list__current"><span>3</span><strong>Öppna resultatet</strong></li>
      </ol>

      {loading && <p className="status-message" role="status">Hämtar sparad resultatmetadata…</p>}
      {error && <p className="status-message status-message--error" role="alert">{error}</p>}
      {result && links && <ResultContent result={result} links={links} checks={checks} checksError={checksError} />}
    </section>
  );
}

function ResultContent({ result, links, checks, checksError }: { result: PullRequestResponse; links: ReturnType<typeof githubLinks>; checks: ImportCheckStatusResponse | null; checksError: string }) {
  return (
    <>
      <div className="result-success" role="status">
        <div><strong>Leveransen är klar</strong><p>Draft-PR #{result.pullRequestNumber} är skapad från den godkända importplanen.</p></div>
        <span className="status-badge">{result.state}</span>
      </div>

      <CheckStatusCard checks={checks} error={checksError} fallbackUrl={links.checks} />

      <div className="result-primary-action">
        <a className="button" href={result.pullRequestUrl} target="_blank" rel="noreferrer">Öppna pull request</a>
        <p>Pull requesten öppnas direkt i GitHub.</p>
      </div>

      <dl className="result-link-grid" aria-label="GitHub-länkar">
        <ResultLink label="Repository" value={result.repositoryFullName} href={links.repository} />
        <ResultLink label="Importbranch" value={result.branchName} href={links.branch} />
        <ResultLink label="Commit" value={shortSha(result.commitSha)} href={links.commit} />
        <ResultLink label="Checks för commit" value="Öppna checks" href={links.checks} />
        <ResultLink label="GitHub Actions" value="Öppna Actions" href={links.actions} />
        <ResultLink label="Målbranch" value={result.baseBranch} href={links.baseBranch} />
      </dl>

      <details className="plan-identity">
        <summary>Leveransidentitet</summary>
        <dl>
          <div><dt>Plan-digest</dt><dd><code>{result.planDigestSha256}</code></dd></div>
          <div><dt>Commit-SHA</dt><dd><code>{result.commitSha}</code></dd></div>
          <div><dt>PR</dt><dd>#{result.pullRequestNumber} · {result.draft ? 'Draft' : 'Öppen'}</dd></div>
          <div><dt>Skapad</dt><dd>{formatDate(result.createdAt)}</dd></div>
        </dl>
      </details>

      <p className="result-status-note">Checkstatus hämtas högst tolv gånger med tio sekunders mellanrum och polling stoppas vid terminal status. GitHub-länkarna fungerar även när statusen är otillgänglig.</p>
    </>
  );
}


function CheckStatusCard({ checks, error, fallbackUrl }: { checks: ImportCheckStatusResponse | null; error: string; fallbackUrl: string }) {
  const state = checks?.state ?? (error ? 'unavailable' : 'pending');
  const labels: Record<string, string> = {
    pending: 'Kontroller pågår',
    success: 'Alla kontroller lyckades',
    failure: 'En eller flera kontroller misslyckades',
    cancelled: 'Kontroller avbröts',
    unavailable: 'Checkstatus är otillgänglig',
  };
  return (
    <section className={`check-status check-status--${state}`} aria-labelledby="check-status-heading" aria-live="polite">
      <div>
        <p className="eyebrow">GitHub checks</p>
        <h2 id="check-status-heading">{labels[state]}</h2>
        {checks && <p>{checks.total} kontroller · {checks.successful} lyckade · {checks.pending} pågående · {checks.failed} misslyckade · {checks.cancelled} avbrutna</p>}
        {!checks && !error && <p>Hämtar aktuell status från GitHub…</p>}
        {error && <p>{error}</p>}
      </div>
      <a className="button button--secondary" href={checks?.detailsUrl || fallbackUrl} target="_blank" rel="noreferrer">Öppna checks</a>
    </section>
  );
}

function ResultLink({ label, value, href }: { label: string; value: string; href: string }) {
  return <div><dt>{label}</dt><dd><a href={href} target="_blank" rel="noreferrer">{value}</a></dd></div>;
}

function githubLinks(result: PullRequestResponse) {
  const repository = `https://github.com/${result.repositoryFullName}`;
  return {
    repository,
    branch: `${repository}/tree/${encodeURIComponent(result.branchName)}`,
    baseBranch: `${repository}/tree/${encodeURIComponent(result.baseBranch)}`,
    commit: `${repository}/commit/${result.commitSha}`,
    checks: `${repository}/commit/${result.commitSha}/checks`,
    actions: `${repository}/actions?query=${encodeURIComponent(`branch:${result.branchName}`)}`,
  };
}

function shortSha(sha: string) { return sha.slice(0, 12); }
function formatDate(value: string) { return new Intl.DateTimeFormat('sv-SE', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)); }
