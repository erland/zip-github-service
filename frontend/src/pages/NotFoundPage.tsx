import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <section className="page-card">
      <p className="eyebrow">404</p>
      <h1>Sidan hittades inte</h1>
      <p>Kontrollera adressen eller återgå till projektlistan.</p>
      <Link className="button" to="/projects">Till projektlistan</Link>
    </section>
  );
}
