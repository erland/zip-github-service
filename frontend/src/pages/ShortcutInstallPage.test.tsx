import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ShortcutInstallPage from './ShortcutInstallPage';

function json(body: unknown, status = 200) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));
}

describe('ShortcutInstallPage', () => {
  beforeEach(() => { vi.stubGlobal('fetch', vi.fn()); });
  afterEach(() => { vi.unstubAllGlobals(); });

  it('offers the authenticated signed release when published', async () => {
    vi.mocked(fetch).mockReturnValue(json({ available: true, version: '1', generation: 'g2', filename: 'zip-github.shortcut', sizeBytes: 4096, sha256: 'a'.repeat(64), downloadUrl: '/api/shortcut-release/download' }));
    render(<ShortcutInstallPage />);
    expect(await screen.findByRole('link', { name: 'Ladda ner aktuell Shortcut' })).toHaveAttribute('href', '/api/shortcut-release/download');
    expect(screen.getByText('g2')).toBeInTheDocument();
  });

  it('does not offer an unsigned placeholder when no signed release is configured', async () => {
    vi.mocked(fetch).mockReturnValue(json({ available: false, version: 'unpublished', generation: 'unpublished', filename: null, sizeBytes: null, sha256: null, downloadUrl: null }));
    render(<ShortcutInstallPage />);
    expect(await screen.findByText(/Ingen signerad Shortcut är publicerad/)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ladda ner aktuell Shortcut' })).not.toBeInTheDocument();
  });
});
