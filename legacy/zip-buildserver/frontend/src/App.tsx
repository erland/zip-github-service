import { NavLink, Route, Routes } from 'react-router-dom';
import { AboutPage } from './pages/AboutPage';
import { HomePage } from './pages/HomePage';
import { PlansPage } from './pages/PlansPage';
import { RunPage } from './pages/RunPage';
import { SessionPage } from './pages/SessionPage';
import styles from './styles/App.module.css';

const navItems = [
  { to: '/', label: 'Home' },
  { to: '/plans', label: 'Plans' },
  { to: '/about', label: 'About' },
];

export default function App() {
  return (
    <div className={styles.appShell}>
      <header className={styles.header}>
        <div>
          <p className={styles.eyebrow}>zip-buildserver</p>
          <h1>Build and test verification for uploaded source zips</h1>
        </div>
        <nav className={styles.nav} aria-label="Primary navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                isActive ? `${styles.navLink} ${styles.navLinkActive}` : styles.navLink
              }
              end={item.to === '/'}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className={styles.main}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/sessions/:sessionId" element={<SessionPage />} />
          <Route path="/runs/:runId" element={<RunPage />} />
          <Route path="/plans" element={<PlansPage />} />
          <Route path="/about" element={<AboutPage />} />
        </Routes>
      </main>
    </div>
  );
}
