import { FormEvent, useState } from 'react';
import { ApiError } from '../api/client';
import { useUploadPackage } from '../api/packages';
import styles from './PackageUploadDropzone.module.css';

interface PackageUploadDropzoneProps {
  sessionId: string;
  onUploadSuccess?: (sourcePackageId: string) => void;
}

function formatBytes(value: number): string {
  if (value < 1024) {
    return `${value} B`;
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }

  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

export function PackageUploadDropzone({ sessionId, onUploadSuccess }: PackageUploadDropzoneProps) {
  const [file, setFile] = useState<File | null>(null);
  const uploadPackage = useUploadPackage();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!file) {
      return;
    }

    const uploaded = await uploadPackage.mutateAsync({ sessionId, file });
    onUploadSuccess?.(uploaded.id);
  }

  const uploadedPackage = uploadPackage.data;
  const errorMessage =
    uploadPackage.error instanceof ApiError
      ? uploadPackage.error.message
      : uploadPackage.error
        ? 'Could not upload package.'
        : null;

  return (
    <form className={styles.dropzone} onSubmit={handleSubmit}>
      <label htmlFor="package-file">
        <strong>Upload source-code zip</strong>
      </label>
      <input
        id="package-file"
        name="file"
        type="file"
        accept=".zip,application/zip"
        onChange={(event) => {
          setFile(event.target.files?.[0] ?? null);
          uploadPackage.reset();
        }}
      />
      {file ? <p className={styles.meta}>Selected {file.name} ({formatBytes(file.size)})</p> : null}

      <button className={styles.button} type="submit" disabled={!file || uploadPackage.isPending}>
        {uploadPackage.isPending ? 'Uploading…' : 'Upload package'}
      </button>

      {uploadedPackage ? (
        <div className={styles.result} role="status">
          Uploaded {uploadedPackage.originalFilename} as {uploadedPackage.status}.
          {uploadedPackage.projectDetection?.summary ? ` ${uploadedPackage.projectDetection.summary}` : ''}
        </div>
      ) : null}

      {errorMessage ? <div className={styles.error}>{errorMessage}</div> : null}
    </form>
  );
}
