import { assertSessionActive } from './session';
export type ShortcutRelease = {
  available: boolean;
  version: string;
  generation: string;
  filename: string | null;
  sizeBytes: number | null;
  sha256: string | null;
  downloadUrl: string | null;
};

export async function getShortcutRelease(): Promise<ShortcutRelease> {
  const response = await fetch('/api/shortcut-release', { credentials: 'include' });
  assertSessionActive(response);
  if (!response.ok) throw new Error(`Shortcut-information kunde inte hämtas (${response.status}).`);
  return response.json();
}
