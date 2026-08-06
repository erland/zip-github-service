import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import App from './App';

function renderAt(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <App />
    </MemoryRouter>,
  );
}

describe('App routing', () => {
  it('redirects the root route to the project list', async () => {
    renderAt('/');
    expect(await screen.findByRole('heading', { name: 'Dina projekt' })).toBeInTheDocument();
  });

  it('navigates from the project list to project details and a new import', async () => {
    const user = userEvent.setup();
    renderAt('/projects');

    await user.click(screen.getByRole('link', { name: 'Öppna projekt' }));
    expect(screen.getByRole('heading', { name: 'Bokprojekt' })).toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Ny import' }));
    expect(screen.getByRole('heading', { name: 'Ladda upp projekt-ZIP' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown routes', () => {
    renderAt('/saknas');
    expect(screen.getByRole('heading', { name: 'Sidan hittades inte' })).toBeInTheDocument();
  });
});
