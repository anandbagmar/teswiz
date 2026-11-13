package com.znsio.teswiz.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.znsio.teswiz.config.TeswizConfig;
import com.znsio.teswiz.tools.OverriddenVariable;

/**
 * The proxy teswiz's own outgoing connections (notifications, telemetry, heartbeat, downloads) must go through, found
 * exactly the way {@code Setup} finds it for the browsers and drivers: {@code PROXY_KEY} names the environment variable
 * (or system property) that holds the proxy, and {@code PROXY_URL} is that value. {@code PROXY_KEY} comes from the
 * environment first, then from the config file, and falls back to the literal {@code PROXY_KEY}. When {@code Setup}
 * has already run, its resolved {@code PROXY_URL} is used as is; a stand-alone caller (no {@code Setup}) derives it the
 * same way. No second proxy setting is introduced.
 * <p>
 * Never throws: a missing or malformed proxy means a direct connection. Credentials in the proxy URL are never logged:
 * {@link #describe()} shows only {@code host:port}.
 */
public final class ProxySettings {

    // LogManager/Logger are fully-qualified ON PURPOSE (same as JUnitResults): an auto-import-organizer in this
    // environment rewrites a plain log4j import to java.util.logging.LogManager and drops Logger, breaking the build.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(ProxySettings.class.getName());

    private static final String PROXY_URL_KEY = "PROXY_URL";
    private static final String PROXY_KEY_KEY = "PROXY_KEY";
    private static final String NO_PROXY_DESCRIPTION = "none";
    private static final String SCHEME_SEPARATOR = "://";

    private final InetSocketAddress address;

    private ProxySettings(InetSocketAddress address) {
        this.address = address;
    }

    /** @return the proxy teswiz is configured with (through {@code Setup}, or the environment when it has not run) */
    public static ProxySettings fromEnvironment() {
        return resolve(ProxySettings::teswizConfigValue, ProxySettings::environmentValue);
    }

    /** @return the "no proxy" value: connect directly */
    public static ProxySettings none() {
        return new ProxySettings(null);
    }

    /**
     * @param teswizConfig the configuration {@code Setup} has resolved ({@code null} for every key when it has not run)
     * @param environment  environment variables and system properties
     * @return the proxy {@code Setup} resolved, else the one held by the variable {@code PROXY_KEY} names, else none
     */
    public static ProxySettings resolve(Function<String, String> teswizConfig, Function<String, String> environment) {
        String proxyUrl = valueOrNull(teswizConfig.apply(PROXY_URL_KEY));
        if (proxyUrl == null) {
            proxyUrl = valueOrNull(environment.apply(proxyVariableName(teswizConfig, environment)));
        }
        return proxyUrl == null ? none() : parse(proxyUrl);
    }

    /** @return true when a proxy is configured */
    public boolean isConfigured() {
        return address != null;
    }

    /** @return the proxy's (unresolved) address, when configured */
    public Optional<InetSocketAddress> address() {
        return Optional.ofNullable(address);
    }

    /** @return the proxy as a {@link Proxy}, resolved when a connection is made; {@link Proxy#NO_PROXY} when none */
    public Proxy javaProxy() {
        if (address == null) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(address.getHostString(), address.getPort()));
    }

    /** @return a selector that sends every request through the proxy, resolving its host when the request is made */
    public Optional<ProxySelector> selector() {
        if (address == null) {
            return Optional.empty();
        }
        return Optional.of(new FixedProxySelector(address));
    }

    /** @return {@code host:port} of the proxy, or {@code none} — never any credentials */
    public String describe() {
        return address == null ? NO_PROXY_DESCRIPTION : address.getHostString() + ":" + address.getPort();
    }

    private static String proxyVariableName(Function<String, String> teswizConfig,
            Function<String, String> environment) {
        String fromEnvironment = valueOrNull(environment.apply(PROXY_KEY_KEY));
        if (fromEnvironment != null) {
            return fromEnvironment;
        }
        String fromConfigFile = valueOrNull(teswizConfig.apply(PROXY_KEY_KEY));
        return fromConfigFile == null ? PROXY_KEY_KEY : fromConfigFile;
    }

    private static ProxySettings parse(String proxyUrl) {
        try {
            URI uri = URI.create(proxyUrl.contains(SCHEME_SEPARATOR) ? proxyUrl : "http" + SCHEME_SEPARATOR + proxyUrl);
            if (uri.getHost() == null || uri.getPort() < 0) {
                LOGGER.warn("Ignoring the proxy setting: it is not in host:port form.");
                return none();
            }
            return new ProxySettings(InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort()));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Ignoring the proxy setting: it is not a valid URL.");
            return none();
        }
    }

    private static String teswizConfigValue(String key) {
        return TeswizConfig.reader().getString(key);
    }

    private static String environmentValue(String key) {
        return OverriddenVariable.getOverriddenStringValue(key, null);
    }

    private static String valueOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Sends every request through one proxy; the proxy host is resolved when a request is made, not at startup. */
    private static final class FixedProxySelector extends ProxySelector {

        private final InetSocketAddress unresolvedProxy;

        private FixedProxySelector(InetSocketAddress unresolvedProxy) {
            this.unresolvedProxy = unresolvedProxy;
        }

        @Override
        public List<Proxy> select(URI uri) {
            return List.of(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(unresolvedProxy.getHostString(), unresolvedProxy.getPort())));
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            LOGGER.warn("Could not connect to the proxy {}:{}.", unresolvedProxy.getHostString(),
                    unresolvedProxy.getPort());
        }
    }
}
