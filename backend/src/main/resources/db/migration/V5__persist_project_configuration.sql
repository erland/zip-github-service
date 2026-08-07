-- Make GitHub App installation ownership tenant-safe: the same GitHub installation can
-- be visible to multiple zip-github users, so identity is installation + owner.
ALTER TABLE github_installation DROP CONSTRAINT github_installation_pkey;
ALTER TABLE github_installation ADD PRIMARY KEY (id, owner_user_id);

ALTER TABLE project
    ADD COLUMN private_repository BOOLEAN NOT NULL DEFAULT TRUE;
