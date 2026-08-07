# Web and API security hardening

Step 7.2 adds a browser/API security baseline without changing the ownership checks already enforced by application services.

## CSRF protection

All unsafe `/api/**` requests (`POST`, `PUT`, `PATCH`, `DELETE`) must include both:

- an `Origin` header matching `zipgithub.frontend-url` exactly by scheme, host and effective port;
- `X-Zip-GitHub-Request: 1`.

The marker forces a CORS preflight for cross-origin browser requests and the origin comparison prevents a foreign site from replaying a credentialed request. Test profile disables this filter so existing API tests do not need deployment-specific origins. Production should keep it enabled.

## Cookies and CORS

Session and OAuth-state cookies remain `HttpOnly`, `SameSite=Lax` and `Secure` outside dev/test. CORS is restricted to the configured frontend origin, explicit methods and explicit headers, with credentials enabled.

## Response headers

Every response receives `nosniff`, frame denial, a restrictive API CSP, no-referrer policy, permissions policy and same-origin resource policy. API responses additionally receive `Cache-Control: no-store` and `Pragma: no-cache`.

## Error handling and logs

Unexpected errors return a generic problem response. The server logs the exception together with the same correlation ID returned to the client. Tokens, private keys and internal paths must not be added to API error details.

## Deployment requirements

- Use HTTPS and leave `ZIP_GITHUB_SECURE_COOKIES=true`.
- Set one exact `ZIP_GITHUB_FRONTEND_URL`; do not use wildcard CORS origins.
- Keep `ZIP_GITHUB_CSRF_ENABLED=true`.
- Terminate TLS at a trusted reverse proxy and add HSTS there, where the proxy can guarantee HTTPS.
