package com.znsio.teswiz.web.provider;

import com.znsio.teswiz.runner.Runner;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class WebExecutionProviderResolver {

    private static final Supplier<WebExecutionProvider> DEFAULT_PROVIDER = LocalWebExecutionProvider::new;

    /**
     * Registry of provider suppliers keyed on the provider's {@link WebExecutionProvider#name()}
     * (lower-cased). Adding a new provider means adding it to this list - no switch to edit.
     */
    private static final Map<String, Supplier<WebExecutionProvider>> PROVIDERS_BY_NAME =
            registerProviders(
                    BrowserStackWebExecutionProvider::new,
                    LambdaTestWebExecutionProvider::new,
                    HeadSpinWebExecutionProvider::new,
                    DEFAULT_PROVIDER);

    @SafeVarargs
    private static Map<String, Supplier<WebExecutionProvider>> registerProviders(
            Supplier<WebExecutionProvider>... providerSuppliers) {
        return List.of(providerSuppliers).stream()
                .collect(Collectors.toMap(
                        supplier -> supplier.get().name().toLowerCase(Locale.ROOT),
                        supplier -> supplier));
    }

    public WebExecutionProvider resolve() {
        return resolve(Runner.getCloudName());
    }

    public WebExecutionProvider resolve(String providerName) {
        if (null == providerName || providerName.isBlank() || Runner.NOT_SET.equalsIgnoreCase(providerName)) {
            return DEFAULT_PROVIDER.get();
        }
        return PROVIDERS_BY_NAME
                .getOrDefault(providerName.toLowerCase(Locale.ROOT), DEFAULT_PROVIDER)
                .get();
    }
}
