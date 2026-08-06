import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import { useCreateSession } from '../api/sessions';
import styles from './SessionCreateForm.module.css';

export function SessionCreateForm() {
  const [label, setLabel] = useState('');
  const navigate = useNavigate();
  const createSession = useCreateSession();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const session = await createSession.mutateAsync({
      label: label.trim() || undefined,
    });

    navigate(`/sessions/${session.id}`);
  }

  const errorMessage =
    createSession.error instanceof ApiError
      ? createSession.error.message
      : createSession.error
        ? 'Could not create session.'
        : null;

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.field}>
        <label htmlFor="session-label">Session label</label>
        <input
          id="session-label"
          name="label"
          maxLength={255}
          placeholder="Example: zip-buildserver Step 16"
          value={label}
          onChange={(event) => setLabel(event.target.value)}
        />
      </div>

      <div className={styles.actions}>
        <button className={styles.button} type="submit" disabled={createSession.isPending}>
          {createSession.isPending ? 'Creating…' : 'Create session'}
        </button>
        {errorMessage ? <span className={styles.error}>{errorMessage}</span> : null}
      </div>
    </form>
  );
}
