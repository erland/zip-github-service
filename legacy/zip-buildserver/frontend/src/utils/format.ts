export function formatDuration(durationMillis: number | null | undefined): string {
  if (durationMillis == null || Number.isNaN(durationMillis)) {
    return '—';
  }

  if (durationMillis < 1000) {
    return `${durationMillis} ms`;
  }

  return `${(durationMillis / 1000).toFixed(1)} s`;
}
