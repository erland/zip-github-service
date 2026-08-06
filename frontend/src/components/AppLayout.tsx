import { NavLink, Outlet } from 'react-router-dom';

const navClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link nav-link--active' : 'nav-link';

export default function AppLayout() {
  return (
    <div className="app-shell">
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
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
