package com.znsio.teswiz.mobile.provider;

import com.znsio.teswiz.runner.Runner;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MobileExecutionProviderResolver {

    private static final Supplier<MobileExecutionProvider> DEFAULT_PROVIDER = LocalMobileExecutionProvider::new;

    /**
     * Registry of provider suppliers keyed on the provider's {@link MobileExecutionProvider#name()}
     * (lower-cased). Adding a new provider means adding it to this list - no switch to edit.
     */
    private static final Map<String, Supplier<MobileExecutionProvider>> PROVIDERS_BY_NAME =
            registerProviders(
                    BrowserStackMobileExecutionProvider::new,
                    LambdaTestMobileExecutionProvider::new,
                    HeadSpinMobileExecutionProvider::new,
                    PCloudyMobileExecutionProvider::new,
                    DEFAULT_PROVIDER);

    @SafeVarargs
    private static Map<String, Supplier<MobileExecutionProvider>> registerProviders(
            Supplier<MobileExecutionProvider>... providerSuppliers) {
        return List.of(providerSuppliers).stream()
                .collect(Collectors.toMap(
                        supplier -> supplier.get().name().toLowerCase(Locale.ROOT),
                        supplier -> supplier));
    }

    public MobileExecutionProvider resolve() {
        return resolve(Runner.getCloudName());
    }

    public MobileExecutionProvider resolve(String providerName) {
        if (null == providerName || providerName.isBlank() || Runner.NOT_SET.equalsIgnoreCase(providerName)) {
            return DEFAULT_PROVIDER.get();
        }
        return PROVIDERS_BY_NAME
                .getOrDefault(providerName.toLowerCase(Locale.ROOT), DEFAULT_PROVIDER)
                .get();
    }
}
