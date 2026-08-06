import type { ReactElement } from 'react';
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ErrorCard, LoadingCard, MissingResourceCard, PageCard, PageLayout } from './PageState';

function renderWithRouter(ui: ReactElement) {
  render(<MemoryRouter>{ui}</MemoryRouter>);
}

afterEach(() => {
  cleanup();
});

describe('PageState components', () => {
  it('renders a loading card message', () => {
    renderWithRouter(<LoadingCard message="Loading session…" />);

    expect(screen.getByText('Loading session…')).toBeInTheDocument();
  });

  it('renders an error card with an optional recovery link', () => {
    renderWithRouter(
      <ErrorCard
        title="Could not load session"
        message="Check that the session exists and that the backend is running."
        linkTo="/"
        linkLabel="Create a new session"
      />,
    );

    expect(screen.getByRole('heading', { name: 'Could not load session' })).toBeInTheDocument();
    expect(screen.getByText('Check that the session exists and that the backend is running.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create a new session' })).toHaveAttribute('href', '/');
  });

  it('renders a missing-resource card', () => {
    renderWithRouter(<MissingResourceCard resourceName="run" linkTo="/" linkLabel="Create a new session" />);

    expect(screen.getByRole('heading', { name: 'Missing run' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Create a new session' })).toHaveAttribute('href', '/');
  });

  it('renders page layout content', () => {
    renderWithRouter(
      <PageLayout>
        <PageCard>Page content</PageCard>
      </PageLayout>,
    );

    expect(screen.getByText('Page content')).toBeInTheDocument();
  });
});
