CREATE TABLE verification_session (
    id UUID PRIMARY KEY,
    label VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),
    retention_policy VARCHAR(64) NOT NULL
);

CREATE TABLE source_package (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES verification_session(id) ON DELETE CASCADE,
    original_filename VARCHAR(512),
    checksum_sha256 CHAR(64) NOT NULL,
    compressed_size_bytes BIGINT NOT NULL,
    extracted_size_bytes BIGINT,
    file_count INTEGER,
    top_level_entries TEXT,
    storage_reference VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_source_package_session_id ON source_package(session_id);

CREATE TABLE verification_run (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES verification_session(id) ON DELETE CASCADE,
    source_package_id UUID NOT NULL REFERENCES source_package(id),
    status VARCHAR(32) NOT NULL,
    plan_id VARCHAR(128),
    requested_plan_id VARCHAR(128),
    network_mode VARCHAR(32) NOT NULL,
    summary TEXT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_millis BIGINT
);

CREATE INDEX idx_verification_run_session_id ON verification_run(session_id);
CREATE INDEX idx_verification_run_source_package_id ON verification_run(source_package_id);

CREATE TABLE verification_command_result (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES verification_run(id) ON DELETE CASCADE,
    command_label VARCHAR(128) NOT NULL,
    working_directory VARCHAR(1024) NOT NULL,
    command_display VARCHAR(1024) NOT NULL,
    status VARCHAR(32) NOT NULL,
    exit_code INTEGER,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_millis BIGINT,
    log_excerpt TEXT,
    failure_category VARCHAR(128),
    failure_message TEXT,
    stdout_artifact_ref UUID,
    stderr_artifact_ref UUID
);

CREATE INDEX idx_verification_command_result_run_id ON verification_command_result(run_id);

CREATE TABLE artifact_reference (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES verification_run(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    storage_reference VARCHAR(1024) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_artifact_reference_run_id ON artifact_reference(run_id);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    actor VARCHAR(255),
    resource_type VARCHAR(128),
    resource_id UUID,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_event_resource ON audit_event(resource_type, resource_id);
