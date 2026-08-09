import { useEffect, useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import type { ImportPlanEntry } from '../api/imports';

type MutableDirectory = {
  kind: 'directory';
  name: string;
  path: string;
  directories: Map<string, MutableDirectory>;
  files: ReviewFileNode[];
};

type ReviewDirectoryNode = {
  kind: 'directory';
  name: string;
  path: string;
  children: ReviewTreeNode[];
  entries: ImportPlanEntry[];
};

type ReviewFileNode = {
  kind: 'file';
  name: string;
  path: string;
  entry: ImportPlanEntry;
};

type ReviewTreeNode = ReviewDirectoryNode | ReviewFileNode;

export function defaultSelectedPaths(entries: ImportPlanEntry[]): Set<string> {
  return new Set(entries.filter(isSelectableEntry).map((entry) => entry.path));
}

export function samePathSelection(left: ReadonlySet<string>, right: ReadonlySet<string>): boolean {
  if (left.size !== right.size) return false;
  for (const path of left) if (!right.has(path)) return false;
  return true;
}

export function ReviewFileTree({ entries, selectedPaths, onSelectedPathsChange, overridePaths, onOverridePathsChange, externalChangedPaths = new Set(), locked = false }: {
  entries: ImportPlanEntry[];
  selectedPaths: ReadonlySet<string>;
  onSelectedPathsChange: (selectedPaths: Set<string>) => void;
  overridePaths?: ReadonlySet<string>;
  onOverridePathsChange?: (overridePaths: Set<string>) => void;
  externalChangedPaths?: ReadonlySet<string>;
  locked?: boolean;
}) {
  const nodes = useMemo(() => buildTree(entries), [entries]);
  return (
    <ul className="review-tree" aria-label="Filträd">
      {nodes.map((node) => (
        <TreeNode key={`${node.kind}-${node.path}`} node={node} selectedPaths={selectedPaths}
          onSelectedPathsChange={onSelectedPathsChange} overridePaths={overridePaths ?? new Set()}
          onOverridePathsChange={onOverridePathsChange} externalChangedPaths={externalChangedPaths} locked={locked} level={1} />
      ))}
    </ul>
  );
}

function TreeNode({ node, selectedPaths, onSelectedPathsChange, overridePaths, onOverridePathsChange, externalChangedPaths, locked, level }: {
  node: ReviewTreeNode;
  selectedPaths: ReadonlySet<string>;
  onSelectedPathsChange: (selectedPaths: Set<string>) => void;
  overridePaths: ReadonlySet<string>;
  onOverridePathsChange?: (overridePaths: Set<string>) => void;
  externalChangedPaths: ReadonlySet<string>;
  locked: boolean;
  level: number;
}) {
  if (node.kind === 'file') {
    return <FileTreeNode node={node} selectedPaths={selectedPaths} onSelectedPathsChange={onSelectedPathsChange}
      overridePaths={overridePaths} onOverridePathsChange={onOverridePathsChange} externalChangedPaths={externalChangedPaths} locked={locked} level={level} />;
  }
  return <DirectoryTreeNode node={node} selectedPaths={selectedPaths} onSelectedPathsChange={onSelectedPathsChange}
    overridePaths={overridePaths} onOverridePathsChange={onOverridePathsChange} externalChangedPaths={externalChangedPaths} locked={locked} level={level} />;
}

function DirectoryTreeNode({ node, selectedPaths, onSelectedPathsChange, overridePaths, onOverridePathsChange, externalChangedPaths, locked, level }: {
  node: ReviewDirectoryNode;
  selectedPaths: ReadonlySet<string>;
  onSelectedPathsChange: (selectedPaths: Set<string>) => void;
  overridePaths: ReadonlySet<string>;
  onOverridePathsChange?: (overridePaths: Set<string>) => void;
  externalChangedPaths: ReadonlySet<string>;
  locked: boolean;
  level: number;
}) {
  const [expanded, setExpanded] = useState(true);
  const selectablePaths = node.entries
    .filter((entry) => isSelectableEntry(entry) || (entry.blockerType === 'OVERRIDABLE_BLOCKED' && overridePaths.has(entry.path)))
    .map((entry) => entry.path);
  const selectedCount = selectablePaths.filter((path) => selectedPaths.has(path)).length;
  const checked = selectablePaths.length > 0 && selectedCount === selectablePaths.length;
  const indeterminate = selectedCount > 0 && selectedCount < selectablePaths.length;
  const checkboxRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (checkboxRef.current) checkboxRef.current.indeterminate = indeterminate;
  }, [indeterminate]);

  function toggleDirectory() {
    if (locked || selectablePaths.length === 0) return;
    const next = new Set(selectedPaths);
    if (checked) selectablePaths.forEach((path) => next.delete(path));
    else selectablePaths.forEach((path) => next.add(path));
    onSelectedPathsChange(next);
  }

  const counts = summarizeEntries(node.entries);
  const summary = formatDirectorySummary(counts);
  const checkboxLabel = `${checked ? 'Exkludera' : 'Inkludera'} valbara förändringar i ${node.path}`;

  return (
    <li className="review-tree__item review-tree__item--directory">
      <div className="review-tree__row review-tree__row--directory" style={{ '--tree-level': level } as CSSProperties}>
        <button type="button" className="review-tree__toggle" aria-expanded={expanded}
          aria-label={`${expanded ? 'Fäll ihop' : 'Fäll ut'} ${node.path}`} onClick={() => setExpanded((value) => !value)}>
          <span aria-hidden="true">{expanded ? '▾' : '▸'}</span>
        </button>
        <input ref={checkboxRef} type="checkbox" checked={checked} disabled={locked || selectablePaths.length === 0}
          aria-label={checkboxLabel} onChange={toggleDirectory} />
        <button type="button" className="review-tree__directory-name" aria-expanded={expanded}
          onClick={() => setExpanded((value) => !value)}>{node.name}/</button>
        {summary && <span className="review-tree__summary">{summary}</span>}
      </div>
      {expanded && (
        <ul className="review-tree__children">
          {node.children.map((child) => (
            <TreeNode key={`${child.kind}-${child.path}`} node={child} selectedPaths={selectedPaths}
              onSelectedPathsChange={onSelectedPathsChange} overridePaths={overridePaths}
              onOverridePathsChange={onOverridePathsChange} externalChangedPaths={externalChangedPaths} locked={locked} level={level + 1} />
          ))}
        </ul>
      )}
    </li>
  );
}

function FileTreeNode({ node, selectedPaths, onSelectedPathsChange, overridePaths, onOverridePathsChange, externalChangedPaths, locked, level }: {
  node: ReviewFileNode;
  selectedPaths: ReadonlySet<string>;
  onSelectedPathsChange: (selectedPaths: Set<string>) => void;
  overridePaths: ReadonlySet<string>;
  onOverridePathsChange?: (overridePaths: Set<string>) => void;
  externalChangedPaths: ReadonlySet<string>;
  locked: boolean;
  level: number;
}) {
  const { entry } = node;
  const ordinarySelectable = isSelectableEntry(entry);
  const overridable = entry.blockerType === 'OVERRIDABLE_BLOCKED';
  const overrideApproved = overridePaths.has(entry.path);
  const selectable = ordinarySelectable || (overridable && overrideApproved);
  const checked = selectable && selectedPaths.has(entry.path);
  const disabledReason = selectionDisabledReason(entry, overrideApproved);
  const externalChanged = externalChangedPaths.has(entry.path) && entry.status !== 'UNCHANGED' && entry.status !== 'IGNORED';

  function toggleFile() {
    if (locked || !selectable) return;
    const next = new Set(selectedPaths);
    if (checked) next.delete(entry.path);
    else next.add(entry.path);
    onSelectedPathsChange(next);
  }

  function toggleOverride() {
    if (locked || !overridable || !onOverridePathsChange) return;
    const nextOverrides = new Set(overridePaths);
    const nextSelected = new Set(selectedPaths);
    if (overrideApproved) {
      nextOverrides.delete(entry.path);
      nextSelected.delete(entry.path);
    } else {
      nextOverrides.add(entry.path);
      nextSelected.add(entry.path);
    }
    onOverridePathsChange(nextOverrides);
    onSelectedPathsChange(nextSelected);
  }

  return (
    <li className={`review-tree__item review-tree__item--file review-tree__item--${entry.status.toLowerCase()}`}>
      <div className="review-tree__row review-tree__row--file" style={{ '--tree-level': level } as CSSProperties}>
        <span className="review-tree__toggle-spacer" aria-hidden="true" />
        {entry.status !== 'IGNORED' && <input type="checkbox" checked={checked} disabled={locked || !selectable}
          aria-label={`${checked ? 'Exkludera' : 'Inkludera'} ${entry.path}`} onChange={toggleFile} />}
        <code className="review-tree__file-name" title={entry.path}>{node.name}</code>
        <div className="review-tree__badges">
          <span className={`file-status file-status--${statusClass(entry)}`}>{statusLabel(entry)}</span>
          {entry.severity === 'WARNING' && <span className="file-status file-status--warning">Varning</span>}
          {externalChanged && <span className="file-status file-status--warning">Ändrad på GitHub</span>}
          {entry.blockerType === 'HARD_BLOCKED' && <span className="file-status file-status--blocked">Hårt blockerad</span>}
          {entry.blockerType === 'OVERRIDABLE_BLOCKED' && <span className="file-status file-status--blocked">Kräver override</span>}
          {entry.modeChanged && <span className="file-status file-status--warning">Mode {entry.repositoryMode} → {entry.effectiveMode}</span>}
        </div>
      </div>
      {overridable && onOverridePathsChange && (
        <label className="review-tree__override" style={{ '--tree-level': level } as CSSProperties}>
          <input type="checkbox" checked={overrideApproved} disabled={locked} onChange={toggleOverride} />
          <span>Jag förstår risken och vill ta med denna blockerade förändring</span>
        </label>
      )}
      {(entry.message || disabledReason || externalChanged) && (
        <p className="review-tree__message" style={{ '--tree-level': level } as CSSProperties}>
          {externalChanged && <>Den här sökvägen ändrades på Work-branchen efter zip-githubs senast kända commit. Den valda ZIP-versionen kommer att ersätta den externa ändringen. {entry.message || disabledReason ? ' ' : ''}</>}
          {entry.message ?? disabledReason}
        </p>
      )}
    </li>
  );
}

function buildTree(entries: ImportPlanEntry[]): ReviewTreeNode[] {
  const root: MutableDirectory = { kind: 'directory', name: '', path: '', directories: new Map(), files: [] };
  for (const entry of entries) {
    const parts = entry.path.split('/').filter(Boolean);
    if (parts.length === 0) continue;
    let directory = root;
    for (let index = 0; index < parts.length - 1; index += 1) {
      const name = parts[index];
      const path = directory.path ? `${directory.path}/${name}` : name;
      let child = directory.directories.get(name);
      if (!child) {
        child = { kind: 'directory', name, path, directories: new Map(), files: [] };
        directory.directories.set(name, child);
      }
      directory = child;
    }
    const name = parts[parts.length - 1];
    directory.files.push({ kind: 'file', name, path: entry.path, entry });
  }
  return finalizeDirectory(root).children;
}

function finalizeDirectory(directory: MutableDirectory): ReviewDirectoryNode {
  const directories = Array.from(directory.directories.values())
    .map(finalizeDirectory)
    .sort((left, right) => left.name.localeCompare(right.name));
  const files = [...directory.files].sort((left, right) => left.name.localeCompare(right.name));
  const entries = [...directories.flatMap((child) => child.entries), ...files.map((file) => file.entry)];
  return { kind: 'directory', name: directory.name, path: directory.path, children: [...directories, ...files], entries };
}

function isSelectableEntry(entry: ImportPlanEntry): boolean {
  return entry.blockerType === 'NONE' && (entry.status === 'ADDED' || entry.status === 'MODIFIED');
}

function selectionDisabledReason(entry: ImportPlanEntry, overrideApproved = false): string | null {
  if (entry.blockerType === 'HARD_BLOCKED') return 'Kan inte väljas.';
  if (entry.blockerType === 'OVERRIDABLE_BLOCKED' && !overrideApproved) return 'Kräver ett uttryckligt riskgodkännande innan den kan väljas.';
  if (entry.status === 'UNCHANGED') return 'Oförändrad fil ingår inte i committen.';
  if (entry.status === 'IGNORED') return 'Ignorerad fil ingår inte i committen.';
  return null;
}

function statusClass(entry: ImportPlanEntry): string {
  if (entry.comparisonStatus === 'WOULD_DELETE') return 'deleted';
  return entry.status.toLowerCase();
}

function statusLabel(entry: ImportPlanEntry): string {
  if (entry.comparisonStatus === 'WOULD_DELETE') return 'Borttagen';
  const labels: Record<ImportPlanEntry['status'], string> = {
    ADDED: 'Ny', MODIFIED: 'Ändrad', UNCHANGED: 'Oförändrad', IGNORED: 'Ignorerad', BLOCKED: 'Blockerad',
  };
  return labels[entry.status];
}

type DirectoryCounts = { added: number; modified: number; deleted: number; blocked: number; warnings: number };

function summarizeEntries(entries: ImportPlanEntry[]): DirectoryCounts {
  return entries.reduce<DirectoryCounts>((counts, entry) => ({
    added: counts.added + (entry.comparisonStatus === 'ADDED' || entry.status === 'ADDED' ? 1 : 0),
    modified: counts.modified + (entry.comparisonStatus === 'MODIFIED' || entry.status === 'MODIFIED' ? 1 : 0),
    deleted: counts.deleted + (entry.comparisonStatus === 'WOULD_DELETE' ? 1 : 0),
    blocked: counts.blocked + (entry.blockerType !== 'NONE' ? 1 : 0),
    warnings: counts.warnings + (entry.severity === 'WARNING' ? 1 : 0),
  }), { added: 0, modified: 0, deleted: 0, blocked: 0, warnings: 0 });
}

function formatDirectorySummary(counts: DirectoryCounts): string {
  const parts = [
    countPart(counts.added, 'ny', 'nya'),
    countPart(counts.modified, 'ändrad', 'ändrade'),
    countPart(counts.deleted, 'borttagen', 'borttagna'),
    countPart(counts.blocked, 'blockerad', 'blockerade'),
    countPart(counts.warnings, 'varning', 'varningar'),
  ].filter((part): part is string => Boolean(part));
  return parts.join(' · ');
}

function countPart(count: number, singular: string, plural: string): string | null {
  if (count === 0) return null;
  return `${count} ${count === 1 ? singular : plural}`;
}
