ALTER TABLE import_plan
    ADD COLUMN approved_by_user_id UUID;

ALTER TABLE import_plan
    ADD CONSTRAINT fk_import_plan_approved_by
        FOREIGN KEY (approved_by_user_id) REFERENCES user_account(id) ON DELETE RESTRICT;

ALTER TABLE import_plan
    ADD CONSTRAINT ck_import_plan_approval_consistency CHECK (
        (approved_at IS NULL AND approved_by_user_id IS NULL)
        OR (approved_at IS NOT NULL AND approved_by_user_id IS NOT NULL AND status = 'APPROVED')
    );
