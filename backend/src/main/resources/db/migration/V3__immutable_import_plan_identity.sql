ALTER TABLE import_plan
    ADD COLUMN source_upload_sha256 CHAR(64),
    ADD COLUMN plan_digest_sha256 CHAR(64);

ALTER TABLE import_plan
    ADD CONSTRAINT ck_import_plan_source_upload_sha
        CHECK (source_upload_sha256 IS NULL OR source_upload_sha256 ~ '^[0-9a-f]{64}$'),
    ADD CONSTRAINT ck_import_plan_digest_sha
        CHECK (plan_digest_sha256 IS NULL OR plan_digest_sha256 ~ '^[0-9a-f]{64}$');

CREATE UNIQUE INDEX uq_import_plan_digest
    ON import_plan(owner_user_id, plan_digest_sha256)
    WHERE plan_digest_sha256 IS NOT NULL;
