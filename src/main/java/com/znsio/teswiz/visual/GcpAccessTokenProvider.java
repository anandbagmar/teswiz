package com.znsio.teswiz.visual;

/**
 * Supplies a short-lived GCP OAuth2 access token for Vertex AI calls. Abstracted so tests can inject
 * a fake token source and never touch real Google Application Default Credentials.
 */
public interface GcpAccessTokenProvider {

    /**
     * Returns a valid OAuth2 bearer token value for the {@code cloud-platform} scope.
     */
    String getAccessToken();
}
