package com.znsio.teswiz.visual;

/**
 * Default {@link GcpAccessTokenProvider} backed by Google Application Default Credentials (ADC).
 *
 * <p>The {@code google-auth-library-oauth2-http} dependency is optional ({@code compileOnly}); it is
 * referenced reflectively and only when Vertex AI is actually used, so neither its absence nor
 * missing credentials break class-loading of {@link LlmVisionService} or the other providers. A
 * clear, actionable error is thrown only at the point a token is genuinely required.
 */
public class GoogleAdcAccessTokenProvider implements GcpAccessTokenProvider {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String CREDENTIALS_CLASS = "com.google.auth.oauth2.GoogleCredentials";

    @Override
    public String getAccessToken() {
        Object credentials = loadApplicationDefaultCredentials();
        refreshIfExpired(credentials);
        return readTokenValue(credentials);
    }

    private Object loadApplicationDefaultCredentials() {
        try {
            Class<?> credentialsClass = Class.forName(CREDENTIALS_CLASS);
            Object credentials = credentialsClass.getMethod("getApplicationDefault").invoke(null);
            return credentialsClass.getMethod("createScoped", Iterable.class)
                    .invoke(credentials, java.util.List.of(CLOUD_PLATFORM_SCOPE));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "LLM_VISION_PROVIDER=vertex-gemini requires the optional dependency "
                    + "'com.google.auth:google-auth-library-oauth2-http' on the classpath, but it was not found. "
                    + "Add it to your project's dependencies to use Vertex AI.", e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Unable to obtain Google Application Default Credentials for Vertex AI. "
                    + "Ensure ADC is configured (e.g. 'gcloud auth application-default login' or a service-account "
                    + "key via GOOGLE_APPLICATION_CREDENTIALS): " + rootMessage(e), e);
        }
    }

    private void refreshIfExpired(Object credentials) {
        try {
            credentials.getClass().getMethod("refreshIfExpired").invoke(credentials);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to refresh Google credentials for Vertex AI: " + rootMessage(e), e);
        }
    }

    private String readTokenValue(Object credentials) {
        try {
            Object accessToken = credentials.getClass().getMethod("getAccessToken").invoke(credentials);
            if (accessToken == null) {
                throw new IllegalStateException("Google credentials returned no access token for Vertex AI.");
            }
            return (String) accessToken.getClass().getMethod("getTokenValue").invoke(accessToken);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to read Google access token value for Vertex AI: " + rootMessage(e), e);
        }
    }

    private String rootMessage(ReflectiveOperationException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return cause.getMessage();
    }
}
