package info.isaksson.erland.zipgithub.domain.model;

/** Non-secret classification of how a ZIP entered the normal import pipeline. */
public enum ImportSource {
    WEB_UPLOAD,
    STORED_UPLOAD,
    STAGING_IMPORT
}
