package com.znsio.teswiz.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HostAddressDescriberTest {

    @Test
    void listsResolvedAddressesForResolvableHost() {
        String description = HostAddressDescriber.describe("http://localhost:8080/path?x=1");

        assertThat(description).startsWith("localhost -> [").contains("127.0.0.1");
    }

    @Test
    void reportsUnresolvedHostWithoutThrowing() {
        String description = HostAddressDescriber.describe("https://no-such-host.invalid/v1");

        assertThat(description).startsWith("no-such-host.invalid -> unresolved");
    }

    @Test
    void reportsInvalidUrlWithoutThrowing() {
        String description = HostAddressDescriber.describe("not a url");

        assertThat(description).startsWith("unknown host");
    }
}
