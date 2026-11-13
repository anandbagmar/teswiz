package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProxySettings}: finds the proxy the notification senders must use, exactly as
 * {@code Setup} does for the rest of teswiz. {@code PROXY_KEY} names the environment variable (or system property) that
 * holds the proxy; {@code PROXY_URL} is that value. {@code PROXY_KEY} comes from the environment first, then from the
 * config file, and falls back to the literal {@code PROXY_KEY}.
 * <p>
 * Two sources are modelled: the configuration {@code Setup} has already resolved (empty when {@code Setup} has not
 * run, as in a stand-alone notifier), and the environment / system properties.
 */
class ProxySettingsTest {

    private static final Map<String, String> NOTHING = Map.of();
    private static final String PROXY = "http://proxy.corp:3128";

    private static ProxySettings resolve(Map<String, String> teswizConfig, Map<String, String> environment) {
        return ProxySettings.resolve(teswizConfig::get, environment::get);
    }

    @Test
    void noProxyConfiguredMeansDirectConnection() {
        ProxySettings proxy = resolve(NOTHING, NOTHING);

        assertThat(proxy.isConfigured()).isFalse();
        assertThat(proxy.describe()).isEqualTo("none");
        assertThat(proxy.selector()).isEmpty();
    }

    @Test
    void theProxyUrlSetupAlreadyResolvedIsUsed() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", PROXY), NOTHING);

        assertThat(proxy.address()).contains(InetSocketAddress.createUnresolved("proxy.corp", 3128));
        assertThat(proxy.describe()).isEqualTo("proxy.corp:3128");
    }

    @Test
    void aBareHostAndPortIsAccepted() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", "proxy.corp:8080"), NOTHING);

        assertThat(proxy.address()).contains(InetSocketAddress.createUnresolved("proxy.corp", 8080));
    }

    @Test
    void theConfigFileProxyKeyNamesTheVariableThatHoldsTheProxy() {
        ProxySettings proxy = resolve(Map.of("PROXY_KEY", "HTTP_PROXY"), Map.of("HTTP_PROXY", PROXY));

        assertThat(proxy.describe()).isEqualTo("proxy.corp:3128");
    }

    @Test
    void theEnvironmentProxyKeyNamesTheVariableThatHoldsTheProxy() {
        Map<String, String> environment = Map.of("PROXY_KEY", "CI_PROXY", "CI_PROXY", "http://ci-proxy:3128");

        ProxySettings proxy = resolve(NOTHING, environment);

        assertThat(proxy.describe()).isEqualTo("ci-proxy:3128");
    }

    @Test
    void theEnvironmentProxyKeyWinsOverTheConfigFileProxyKey() {
        Map<String, String> environment = Map.of("PROXY_KEY", "ENV_PROXY", "ENV_PROXY", "http://from-env:1",
                "FILE_PROXY", "http://from-file:2");

        ProxySettings proxy = resolve(Map.of("PROXY_KEY", "FILE_PROXY"), environment);

        assertThat(proxy.describe()).isEqualTo("from-env:1");
    }

    @Test
    void aPlainHttpProxyVariableIsNotUsedUnlessProxyKeyNamesIt() {
        ProxySettings proxy = resolve(NOTHING, Map.of("HTTP_PROXY", PROXY));

        assertThat(proxy.isConfigured()).isFalse();
    }

    @Test
    void aProxyUrlEnvironmentVariableIsNotReadDirectlyBecauseSetupDerivesProxyUrl() {
        ProxySettings proxy = resolve(NOTHING, Map.of("PROXY_URL", PROXY));

        assertThat(proxy.isConfigured()).isFalse();
    }

    @Test
    void theProxyUrlSetupResolvedWinsOverTheEnvironment() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", "http://direct:1", "PROXY_KEY", "HTTP_PROXY"),
                Map.of("HTTP_PROXY", "http://other:2"));

        assertThat(proxy.describe()).isEqualTo("direct:1");
    }

    @Test
    void credentialsInTheProxyUrlAreNeverDescribed() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", "http://user:topsecret@proxy.corp:3128"), NOTHING);

        assertThat(proxy.describe()).isEqualTo("proxy.corp:3128").doesNotContain("topsecret");
    }

    @Test
    void aMalformedProxyMeansNoProxyAndNeverThrows() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", "http://:notaport"), NOTHING);

        assertThat(proxy.isConfigured()).isFalse();
    }

    @Test
    void theSelectorSendsEveryRequestThroughTheProxy() {
        ProxySettings proxy = resolve(Map.of("PROXY_URL", PROXY), NOTHING);

        Proxy selected = proxy.selector().orElseThrow().select(URI.create("https://hooks.slack.com/x")).get(0);

        InetSocketAddress selectedAddress = (InetSocketAddress) selected.address();
        assertThat(selected.type()).isEqualTo(Proxy.Type.HTTP);
        assertThat(selectedAddress.getHostString()).isEqualTo("proxy.corp");
        assertThat(selectedAddress.getPort()).isEqualTo(3128);
    }
}
