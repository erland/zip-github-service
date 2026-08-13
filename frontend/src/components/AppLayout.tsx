import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { getCurrentUser, logout, type AuthenticatedUser } from '../api/auth';
import { captureClaimToken } from '../staging/claimToken';

const navClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link nav-link--active' : 'nav-link';

export default function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthenticatedUser | null>(null);
  const [authState, setAuthState] = useState<'loading' | 'authenticated' | 'anonymous' | 'error'>('loading');
  const [authError, setAuthError] = useState<string | null>(null);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    if (location.pathname !== '/staging/claim' || !location.hash) return;
    if (captureClaimToken(location.hash, sessionStorage)) {
      window.history.replaceState(window.history.state, '', `${location.pathname}${location.search}`);
    }
  }, [location.pathname, location.search, location.hash]);

  useEffect(() => {
    let cancelled = false;
    getCurrentUser()
      .then((currentUser) => {
        if (cancelled) return;
        setUser(currentUser);
        setAuthState(currentUser ? 'authenticated' : 'anonymous');
      })
      .catch((reason: unknown) => {
        if (cancelled) return;
        setAuthError(reason instanceof Error ? reason.message : 'Inloggningsstatus kunde inte hämtas.');
        setAuthState('error');
      });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const main = document.getElementById('main-content');
    if (!main) return;

    const focusHeading = () => {
      const heading = main.querySelector<HTMLElement>('h1');
      if (!heading) return false;
      heading.tabIndex = -1;
      heading.focus({ preventScroll: true });
      return true;
    };

    if (focusHeading()) return;

    const observer = new MutationObserver(() => {
      if (focusHeading()) observer.disconnect();
    });
    observer.observe(main, { childList: true, subtree: true });
    return () => observer.disconnect();
  }, [location.pathname, authState]);

  async function handleLogout() {
    setLoggingOut(true);
    setAuthError(null);
    try {
      await logout();
      setUser(null);
      setAuthState('anonymous');
      navigate('/projects', { replace: true });
    } catch (reason) {
      setAuthError(reason instanceof Error ? reason.message : 'Utloggningen misslyckades.');
    } finally {
      setLoggingOut(false);
    }
  }

  const returnTo = `${location.pathname}${location.search}`;
  const loginUrl = `/api/auth/github/login?returnTo=${encodeURIComponent(returnTo)}`;

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Hoppa till huvudinnehållet</a>
      <header className="site-header">
        <NavLink className="brand" to="/projects">zip-github</NavLink>
        {authState === 'authenticated' && user && (
          <div className="header-actions">
            <nav aria-label="Huvudnavigering" className="primary-nav">
              <NavLink className={navClassName} to="/projects">Repositories</NavLink>
              <NavLink className={navClassName} to="/shortcut">Shortcut</NavLink>
              <NavLink className={navClassName} to="/maintenance">Underhåll</NavLink>
              <NavLink className={navClassName} to="/about">Om tjänsten</NavLink>
            </nav>
            <div className="account-menu">
              {user.avatarUrl && <img className="account-avatar" src={user.avatarUrl} alt="" width="32" height="32" />}
              <span className="account-login">{user.login}</span>
              <button className="button button--secondary button--compact" type="button" onClick={handleLogout} disabled={loggingOut}>
                {loggingOut ? 'Loggar ut…' : 'Logga ut'}
              </button>
            </div>
          </div>
        )}
      </header>
      <main className="main-content" id="main-content" tabIndex={-1}>
        {authState === 'loading' && (
          <section className="page-card"><h1>Kontrollerar GitHub-inloggning</h1><p role="status">Hämtar din session…</p></section>
        )}
        {authState === 'error' && (
          <section className="page-card"><h1>Inloggningsstatus kunde inte hämtas</h1><p className="status-message status-message--error" role="alert">{authError}</p></section>
        )}
        {authState === 'anonymous' && (
          <section className="page-card auth-card" aria-labelledby="login-heading">
            <p className="eyebrow">GitHub-inloggning</p>
            <h1 id="login-heading">Logga in för att fortsätta</h1>
            <p className="lead">zip-github använder GitHub OAuth för din identitet och GitHub App-installationer för åtkomst till de repositories du uttryckligen har tillåtit.</p>
            {authError && <p className="status-message status-message--error" role="alert">{authError}</p>}
            <a className="button" href={loginUrl}>Logga in med GitHub</a>
          </section>
        )}
        {authState === 'authenticated' && <Outlet />}
      </main>
    </div>
  );
}
