package com.znsio.teswiz.network;

/**
 * Turns a delivery failure into one log-friendly line: the exception type and message, plus the root cause when it
 * differs. A bare message such as "HTTP connect timed out" does not say what failed, and the exception text can carry a
 * secret (a malformed Slack webhook URL is echoed back in the exception), so every description can be redacted.
 */
public final class FailureDetail {

    private static final String REDACTED = "<redacted>";

    private FailureDetail() {
    }

    /**
     * @param failure the exception to describe
     * @return {@code Type: message}, followed by {@code (root cause: Type: message)} when the root cause differs
     */
    public static String describe(Throwable failure) {
        return describe(failure, "");
    }

    /**
     * @param failure the exception to describe
     * @param secret  a value that must never appear in the output (for example a webhook URL); blank means none
     * @return the description with every occurrence of the secret replaced by {@code <redacted>}
     */
    public static String describe(Throwable failure, String secret) {
        Throwable root = rootCause(failure);
        String description = typeAndMessage(failure);
        if (root != failure) {
            description += " (root cause: " + typeAndMessage(root) + ")";
        }
        return redact(description, secret);
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    private static String typeAndMessage(Throwable failure) {
        String type = failure.getClass().getSimpleName();
        String message = failure.getMessage();
        return message == null || message.isBlank() ? type : type + ": " + message;
    }

    private static String redact(String text, String secret) {
        if (secret == null || secret.isBlank()) {
            return text;
        }
        return text.replace(secret, REDACTED);
    }
}
