import styles from './Page.module.css';

const initialPlans = [
  'node-default: npm install/test/build checks for package.json projects',
  'maven-default: Maven test/package checks for pom.xml projects',
  'multi-project-default: sequential backend and frontend checks',
];

export function PlansPage() {
  return (
    <section className={styles.page}>
      <div className={styles.card}>
        <h2>Verification plans</h2>
        <p>Plans are selected by the backend from server-side configuration, not uploaded package instructions.</p>
        <ul>
          {initialPlans.map((plan) => (
            <li key={plan}>{plan}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}
