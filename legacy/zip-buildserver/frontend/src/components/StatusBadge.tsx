import styles from './StatusBadge.module.css';

interface StatusBadgeProps {
  label: string;
}

export function StatusBadge({ label }: StatusBadgeProps) {
  return <span className={styles.badge}>{label}</span>;
}
