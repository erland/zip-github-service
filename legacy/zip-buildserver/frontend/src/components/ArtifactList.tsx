import { useState } from 'react';
import { useArtifact, useRunArtifacts } from '../api/artifacts';
import styles from './ArtifactList.module.css';

interface ArtifactListProps {
  runId: string;
}

function formatBytes(value: number): string {
  if (value < 1024) {
    return `${value} B`;
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }

  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

export function ArtifactList({ runId }: ArtifactListProps) {
  const [selectedArtifactId, setSelectedArtifactId] = useState<string | null>(null);
  const artifactsQuery = useRunArtifacts(runId);
  const artifactQuery = useArtifact(selectedArtifactId);

  if (artifactsQuery.isLoading) {
    return <p className={styles.empty}>Loading artifacts…</p>;
  }

  if (artifactsQuery.isError) {
    return <p className={styles.empty}>Could not load artifacts.</p>;
  }

  const artifacts = artifactsQuery.data?.artifacts ?? [];

  if (artifacts.length === 0) {
    return <p className={styles.empty}>No artifacts are available for this run.</p>;
  }

  return (
    <div>
      <ul className={styles.list}>
        {artifacts.map((artifact) => (
          <li className={styles.item} key={artifact.id}>
            <span>
              <strong>{artifact.type}</strong> · {formatBytes(artifact.sizeBytes)}
            </span>
            <button
              className={styles.button}
              type="button"
              onClick={() => setSelectedArtifactId(artifact.id)}
            >
              View log
            </button>
          </li>
        ))}
      </ul>

      {selectedArtifactId ? (
        <section aria-label="Artifact content">
          <h3>Artifact content</h3>
          {artifactQuery.isLoading ? <p>Loading artifact…</p> : null}
          {artifactQuery.isError ? <p>Could not load artifact content.</p> : null}
          {artifactQuery.data ? <pre className={styles.content}>{artifactQuery.data.content}</pre> : null}
        </section>
      ) : null}
    </div>
  );
}
