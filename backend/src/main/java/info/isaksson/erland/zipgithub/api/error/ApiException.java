package info.isaksson.erland.zipgithub.api.error;

public final class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    private ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String message) { return new ApiException(400, code, message); }
    public static ApiException unauthorized(String code, String message) { return new ApiException(401, code, message); }
    public static ApiException forbidden(String code, String message) { return new ApiException(403, code, message); }
    public static ApiException notFound(String code, String message) { return new ApiException(404, code, message); }
    public static ApiException tooManyRequests(String code, String message) { return new ApiException(429, code, message); }
    public static ApiException conflict(String code, String message) { return new ApiException(409, code, message); }
    public static ApiException badGateway(String code, String detail) { return new ApiException(502, code, detail); }

    public static ApiException payloadTooLarge(String code, String message) { return new ApiException(413, code, message); }

    public int status() { return status; }
    public String code() { return code; }
}
