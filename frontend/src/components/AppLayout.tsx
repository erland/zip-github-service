import { useEffect } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';

const navClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link nav-link--active' : 'nav-link';

export default function AppLayout() {
  const { pathname } = useLocation();

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
  }, [pathname]);

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Hoppa till huvudinnehållet</a>
      <header className="site-header">
        <NavLink className="brand" to="/projects">
          zip-github
        </NavLink>
        <nav aria-label="Huvudnavigering" className="primary-nav">
          <NavLink className={navClassName} to="/projects">
            Projekt
          </NavLink>
          <NavLink className={navClassName} to="/about">
            Om tjänsten
          </NavLink>
        </nav>
      </header>
      <main className="main-content" id="main-content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
}
