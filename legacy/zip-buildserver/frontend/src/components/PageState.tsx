import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import styles from '../pages/Page.module.css';

interface ChildrenProps {
  children: ReactNode;
}

interface LoadingCardProps {
  message: string;
}

interface ErrorCardProps {
  title: string;
  message: string;
  linkTo?: string;
  linkLabel?: string;
}

interface MissingResourceCardProps {
  resourceName: string;
  linkTo: string;
  linkLabel: string;
}

export function PageLayout({ children }: ChildrenProps) {
  return <section className={styles.page}>{children}</section>;
}

export function PageCard({ children }: ChildrenProps) {
  return <div className={styles.card}>{children}</div>;
}

export function LoadingCard({ message }: LoadingCardProps) {
  return (
    <PageLayout>
      <PageCard>{message}</PageCard>
    </PageLayout>
  );
}

export function ErrorCard({ title, message, linkTo, linkLabel }: ErrorCardProps) {
  return (
    <PageLayout>
      <PageCard>
        <h2>{title}</h2>
        <p>{message}</p>
        {linkTo && linkLabel ? <Link to={linkTo}>{linkLabel}</Link> : null}
      </PageCard>
    </PageLayout>
  );
}

export function MissingResourceCard({ resourceName, linkTo, linkLabel }: MissingResourceCardProps) {
  return (
    <PageLayout>
      <PageCard>
        <h2>Missing {resourceName}</h2>
        <Link to={linkTo}>{linkLabel}</Link>
      </PageCard>
    </PageLayout>
  );
}
