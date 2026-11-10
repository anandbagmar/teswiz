package com.znsio.teswiz.config;

/**
 * Access point for the execution configuration from packages that must not depend on
 * {@code com.znsio.teswiz.runner}. {@code TeswizConfig.reader().getString(key)} answers exactly as
 * {@code Setup.getFromConfigs(key)} does, because both read the same configuration instance.
 *
 * <p>{@code Setup} registers its configuration when the {@code Setup} class is initialised. Until
 * then the reader is empty, which matches {@code Setup} itself: its configuration is empty until
 * {@code Setup.load(...)} runs.
 */
public final class TeswizConfig {
    private static final TeswizConfigReader EMPTY_READER = new EmptyConfigReader();
    private static final TeswizConfigReader LIVE_READER = new LiveConfigReader();
    private static volatile TeswizConfigReader registeredReader = EMPTY_READER;

    private TeswizConfig() {
    }

    /**
     * A live view of the registered configuration. It is safe to keep: every call reads whichever
     * configuration is registered at that moment, so a reader obtained before {@code Setup} is
     * initialised does not stay empty.
     */
    public static TeswizConfigReader reader() {
        return LIVE_READER;
    }

    /**
     * Framework-internal: called by {@code Setup} to publish its configuration. Not intended for
     * test or consumer code.
     */
    public static void register(TeswizConfigReader configurationReader) {
        if (null == configurationReader) {
            throw new IllegalArgumentException("configurationReader must not be null");
        }
        registeredReader = configurationReader;
    }

    static TeswizConfigReader emptyReader() {
        return EMPTY_READER;
    }

    /** Forwards every call to the configuration registered at the time of the call. */
    private static final class LiveConfigReader implements TeswizConfigReader {
        @Override
        public String getString(String key) {
            return registeredReader.getString(key);
        }

        @Override
        public String getStringOrDefault(String key, String defaultValue) {
            return registeredReader.getStringOrDefault(key, defaultValue);
        }

        @Override
        public boolean getBoolean(String key) {
            return registeredReader.getBoolean(key);
        }

        @Override
        public String getBooleanAsString(String key) {
            return registeredReader.getBooleanAsString(key);
        }

        @Override
        public int getInteger(String key) {
            return registeredReader.getInteger(key);
        }

        @Override
        public String getIntegerAsString(String key) {
            return registeredReader.getIntegerAsString(key);
        }
    }

    /** Answers like an empty configuration: no key is present. */
    private static final class EmptyConfigReader implements TeswizConfigReader {
        @Override
        public String getString(String key) {
            return null;
        }

        @Override
        public String getStringOrDefault(String key, String defaultValue) {
            return defaultValue;
        }

        @Override
        public boolean getBoolean(String key) {
            return false;
        }

        @Override
        public String getBooleanAsString(String key) {
            return String.valueOf((Boolean) null);
        }

        @Override
        public int getInteger(String key) {
            return 0;
        }

        @Override
        public String getIntegerAsString(String key) {
            return "0";
        }
    }
}
