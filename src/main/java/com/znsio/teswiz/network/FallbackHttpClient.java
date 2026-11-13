package com.znsio.teswiz.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Dns;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The HTTP client teswiz's own outgoing calls share (notifications, telemetry, heartbeat, downloads). When the host has
 * several addresses and one cannot be reached, it connects to the next one ({@link AddressFallback}: each address
 * once, {@code HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS} each) and records a {@link NetworkNote}. It never tries
 * another address once the request has started to be sent, so a request is never sent twice.
 * <p>
 * With {@code HTTP_ADDRESS_FALLBACK=false} only the first address is tried. When a proxy is configured
 * ({@link ProxySettings}) the proxy looks up the host, so there is nothing to fall back between.
 */
public final class FallbackHttpClient {

    private static final OkHttpClient SHARED = new OkHttpClient.Builder().retryOnConnectionFailure(false).build();

    private final String purpose;
    private final AddressFallbackSettings settings;
    private final ProxySettings proxy;
    private final AddressResolver resolver;
    private final NetworkNotes notes;

    /** @param purpose what the calls are for, shown in network notes (e.g. {@code slack}, {@code telemetry}) */
    public FallbackHttpClient(String purpose) {
        this(purpose, AddressFallbackSettings.fromEnvironment(), ProxySettings.fromEnvironment(),
                AddressResolver.system(), NetworkNotes.global());
    }

    FallbackHttpClient(String purpose, AddressFallbackSettings settings, ProxySettings proxy, AddressResolver resolver,
            NetworkNotes notes) {
        this.purpose = purpose;
        this.settings = settings;
        this.proxy = proxy;
        this.resolver = resolver;
        this.notes = notes;
    }

    /**
     * @param request the request to send
     * @return the response (any status; a 4xx or 5xx is an answer, not a failure)
     * @throws IOException when no address could be connected to, or the call failed after connecting
     */
    public HttpResponse send(HttpRequest request) throws IOException {
        HttpUrl url = HttpUrl.get(request.url());
        Request okRequest = toOkHttp(request);
        if (proxy.isConfigured()) {
            return execute(baseBuilder(request, request.timeout()).proxy(proxy.javaProxy()).build(), okRequest);
        }
        List<InetAddress> addresses = resolver.resolve(url.host());
        if (!settings.enabled()) {
            return execute(pinned(request, url.host(), addresses.get(0), request.timeout()), okRequest);
        }
        return new AddressFallback(notes).run(purpose, url.host(), url.port(), addresses,
                address -> attempt(request, okRequest, url.host(), address));
    }

    private HttpResponse attempt(HttpRequest request, Request okRequest, String host, InetAddress address)
            throws IOException {
        AtomicBoolean sendingStarted = new AtomicBoolean();
        OkHttpClient client = pinned(request, host, address, settings.connectTimeoutPerAddress()).newBuilder()
                .eventListener(new SendingStartedListener(sendingStarted)).build();
        try {
            return execute(client, okRequest);
        } catch (IOException e) {
            if (sendingStarted.get()) {
                throw e;
            }
            throw new AddressFallback.NotConnectedException(e);
        }
    }

    private static OkHttpClient pinned(HttpRequest request, String host, InetAddress address,
            Duration connectTimeout) {
        return baseBuilder(request, connectTimeout).dns(new PinnedDns(host, address)).build();
    }

    private static OkHttpClient.Builder baseBuilder(HttpRequest request, Duration connectTimeout) {
        return SHARED.newBuilder().connectTimeout(connectTimeout).readTimeout(request.timeout())
                .writeTimeout(request.timeout()).callTimeout(request.timeout())
                .followRedirects(request.followRedirects()).followSslRedirects(request.followRedirects());
    }

    private static HttpResponse execute(OkHttpClient client, Request okRequest) throws IOException {
        try (Response response = client.newCall(okRequest).execute()) {
            ResponseBody body = response.body();
            byte[] bytes = body == null ? new byte[0] : body.bytes();
            return new HttpResponse(response.code(), bytes, response.headers().toMultimap());
        }
    }

    private static Request toOkHttp(HttpRequest request) {
        Request.Builder builder = new Request.Builder().url(request.url());
        request.headers().forEach(builder::header);
        RequestBody body = request.body() == null ? null : RequestBody.create(request.body(), mediaType(request));
        return builder.method(request.method(), body).build();
    }

    private static MediaType mediaType(HttpRequest request) {
        return request.contentType() == null ? null : MediaType.parse(request.contentType());
    }

    /** Answers one address for the host being called; any other host (e.g. after a redirect) uses the system. */
    private static final class PinnedDns implements Dns {
        private final String host;
        private final InetAddress address;

        private PinnedDns(String host, InetAddress address) {
            this.host = host;
            this.address = address;
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            return host.equalsIgnoreCase(hostname) ? List.of(address) : Dns.SYSTEM.lookup(hostname);
        }
    }

    /** Remembers that the request started to be sent, after which another address must never be tried. */
    private static final class SendingStartedListener extends EventListener {
        private final AtomicBoolean sendingStarted;

        private SendingStartedListener(AtomicBoolean sendingStarted) {
            this.sendingStarted = sendingStarted;
        }

        @Override
        public void requestHeadersStart(Call call) {
            sendingStarted.set(true);
        }
    }
}
