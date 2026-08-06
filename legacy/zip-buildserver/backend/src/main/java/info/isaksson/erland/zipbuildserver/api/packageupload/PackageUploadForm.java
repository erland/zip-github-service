package info.isaksson.erland.zipbuildserver.api.packageupload;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class PackageUploadForm {
    @RestForm("file")
    public FileUpload file;
}
