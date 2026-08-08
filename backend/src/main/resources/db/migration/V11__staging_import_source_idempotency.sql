-- Step 9.4: make the StagingImport -> ordinary Import correlation restart-safe and unique.
-- source_reference contains only the non-secret staging id correlation.
CREATE UNIQUE INDEX uq_import_session_staging_source_reference
    ON import_session (owner_user_id, source_type, source_reference)
    WHERE source_type = 'STAGING_IMPORT' AND source_reference IS NOT NULL;
