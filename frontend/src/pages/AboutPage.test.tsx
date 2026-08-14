import { render, screen } from '@testing-library/react';
import { expect, it } from 'vitest';
import AboutPage from './AboutPage';

it('shows the build-injected zip-GitHub version', () => {
  render(<AboutPage />);
  expect(screen.getByText('Version')).toBeInTheDocument();
  expect(screen.getByText(/development|1\.0\.0-rc\./)).toBeInTheDocument();
});
