ALTER TABLE import_session
    ADD COLUMN source_type VARCHAR(40) NOT NULL DEFAULT 'WEB_UPLOAD',
    ADD COLUMN source_reference VARCHAR(255);

ALTER TABLE import_session
    ADD CONSTRAINT ck_import_session_source_type
        CHECK (source_type IN ('WEB_UPLOAD', 'STORED_UPLOAD', 'STAGING_IMPORT'));

COMMENT ON COLUMN import_session.source_reference IS
    'Optional non-secret source reference. Capability tokens, claim tokens and credentials must never be stored here.';
