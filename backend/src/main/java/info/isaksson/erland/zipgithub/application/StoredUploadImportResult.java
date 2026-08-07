package info.isaksson.erland.zipgithub.application;

import info.isaksson.erland.zipgithub.api.dto.ImportResponse;
import info.isaksson.erland.zipgithub.api.dto.SourceUploadResponse;

/** Result of promoting an already stored ZIP artifact into the normal import model. */
public record StoredUploadImportResult(ImportResponse importSession, SourceUploadResponse sourceUpload) { }
