import { useState } from 'react';
import { cleanup, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { BlockerDecision, ImportPlanEntry } from '../api/imports';
import { defaultSelectedPaths, ReviewFileTree } from './ReviewFileTree';

const entries: ImportPlanEntry[] = [
  entry('src/main/xxx/Game.java', 'MODIFIED'),
  entry('src/main/xxx/Board.java', 'ADDED'),
  entry('src/main/other/Other.java', 'MODIFIED'),
  entry('.github/workflows/build.yml', 'BLOCKED', 'OVERRIDABLE_BLOCKED', 'MODIFIED'),
  entry('.git/config', 'BLOCKED', 'HARD_BLOCKED', 'MODIFIED'),
  entry('old/Removed.java', 'BLOCKED', 'OVERRIDABLE_BLOCKED', 'WOULD_DELETE'),
];

afterEach(cleanup);

describe('ReviewFileTree', () => {
  it('selects ordinary changes by default but never blocked entries', () => {
    render(<Harness />);
    expect(screen.getByRole('checkbox', { name: 'Exkludera src/main/xxx/Game.java' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Exkludera src/main/xxx/Board.java' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Inkludera .github/workflows/build.yml' })).toBeDisabled();
    expect(screen.getByRole('checkbox', { name: 'Inkludera .git/config' })).toBeDisabled();
  });

  it('deselects every selectable descendant when a directory is unchecked', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera valbara förändringar i src/main/xxx' }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera src/main/xxx/Game.java' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Inkludera src/main/xxx/Board.java' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Exkludera src/main/other/Other.java' })).toBeChecked();
  });

  it('shows an indeterminate parent when only some selectable children are checked', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const directory = screen.getByRole('checkbox', { name: 'Exkludera valbara förändringar i src/main/xxx' }) as HTMLInputElement;
    await user.click(screen.getByRole('checkbox', { name: 'Exkludera src/main/xxx/Game.java' }));
    expect(directory.indeterminate).toBe(true);
  });

  it('aggregates change classes and can collapse a subtree', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const collapse = screen.getByRole('button', { name: 'Fäll ihop src/main/xxx' });
    const xxxDirectory = collapse.closest('li');
    expect(xxxDirectory).not.toBeNull();
    expect(within(xxxDirectory as HTMLElement).getByText('1 ny · 1 ändrad')).toBeInTheDocument();
    await user.click(collapse);
    expect(screen.queryByText('Game.java')).not.toBeInTheDocument();
    expect(screen.queryByText('Board.java')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Fäll ut src/main/xxx' })).toHaveAttribute('aria-expanded', 'false');
  });


  it('requires explicit override and never includes it through a directory toggle', async () => {
    const user = userEvent.setup();
    render(<OverrideHarness />);
    const blocked = screen.getByRole('checkbox', { name: 'Inkludera .github/workflows/build.yml' });
    expect(blocked).toBeDisabled();
    const include = screen.getAllByRole('radio', { name: 'Jag förstår risken – godkänn och ta med' })[0];
    await user.click(include);
    expect(screen.getByRole('checkbox', { name: 'Exkludera .github/workflows/build.yml' })).toBeChecked();
  });

  it('handles a mixed review tree without allowing hard blockers to leak through subtree selection', async () => {
    const user = userEvent.setup();
    render(<OverrideHarness />);

    await user.click(screen.getByRole('checkbox', { name: 'Exkludera valbara förändringar i src/main/xxx' }));
    expect(screen.getByRole('checkbox', { name: 'Inkludera src/main/xxx/Game.java' })).not.toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Inkludera src/main/xxx/Board.java' })).not.toBeChecked();

    const gitCheckbox = screen.getByRole('checkbox', { name: 'Inkludera .git/config' });
    expect(gitCheckbox).toBeDisabled();

    const overrideBoxes = screen.getAllByRole('radio', { name: 'Jag förstår risken – godkänn och ta med' });
    await user.click(overrideBoxes[0]);
    expect(screen.getByRole('checkbox', { name: 'Exkludera .github/workflows/build.yml' })).toBeChecked();

    await user.click(overrideBoxes[1]);
    expect(screen.getByRole('checkbox', { name: 'Exkludera old/Removed.java' })).toBeChecked();
    expect(gitCheckbox).not.toBeChecked();
  });

  it('requires an explicit acknowledgement for a hard blocker without ever selecting it', async () => {
    const user = userEvent.setup();
    render(<OverrideHarness />);
    const hard = screen.getByRole('checkbox', { name: 'Inkludera .git/config' });
    expect(hard).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Jag har sett att denna hårt blockerade förändring inte kommer att tas med' }));
    expect(hard).not.toBeChecked();
  });

  it('labels deletion and blocker status in the tree', () => {
    render(<Harness />);
    const oldDirectory = screen.getByRole('button', { name: 'Fäll ihop old' }).closest('li');
    expect(oldDirectory).not.toBeNull();
    expect(within(oldDirectory as HTMLElement).getByText('Borttagen')).toBeInTheDocument();
    expect(within(oldDirectory as HTMLElement).getByText('Kräver beslut')).toBeInTheDocument();
  });
});

function Harness() {
  const [selected, setSelected] = useState(() => defaultSelectedPaths(entries));
  return <ReviewFileTree entries={entries} selectedPaths={selected} onSelectedPathsChange={setSelected} />;
}

function OverrideHarness() {
  const [selected, setSelected] = useState(() => defaultSelectedPaths(entries));
  const [overrides, setOverrides] = useState<Set<string>>(new Set());
  const [decisions, setDecisions] = useState<Map<string, BlockerDecision>>(new Map());
  return <ReviewFileTree entries={entries} selectedPaths={selected} onSelectedPathsChange={setSelected}
    overridePaths={overrides} onOverridePathsChange={setOverrides} blockerDecisions={decisions} onBlockerDecisionsChange={setDecisions} />;
}


function entry(path: string, status: ImportPlanEntry['status'], blockerType: ImportPlanEntry['blockerType'] = 'NONE',
  comparisonStatus: string | null = status): ImportPlanEntry {
  return {
    path, status, comparisonStatus, blockerType,
    severity: blockerType === 'NONE' ? 'NONE' : 'BLOCKING',
    policyCode: blockerType === 'NONE' ? null : 'TEST_BLOCKER',
    message: blockerType === 'NONE' ? null : 'Test blocker.',
    archiveSizeBytes: status === 'BLOCKED' && comparisonStatus === 'WOULD_DELETE' ? null : 10,
    archiveSha256: status === 'BLOCKED' && comparisonStatus === 'WOULD_DELETE' ? null : 'a'.repeat(64),
    repositorySizeBytes: comparisonStatus === 'ADDED' ? null : 10,
    repositorySha256: comparisonStatus === 'ADDED' ? null : 'b'.repeat(64),
    textCandidate: true,
  };
}
