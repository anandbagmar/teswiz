package com.znsio.teswiz.visual;

import java.util.Map;

/**
 * Maps a position phrase from a test step ("first", "last", "3rd", "2", ...) to a zero-based list index.
 *
 * <ul>
 *     <li>blank or unrecognised text maps to {@code 0} (the first element);</li>
 *     <li>"first"/"1st", "second"/"2nd", "third"/"3rd" map to 0, 1, 2; "last" maps to the final index;</li>
 *     <li>a numeric ordinal ("4th", "21st") is one-based, so "4th" maps to 3;</li>
 *     <li>a plain number ("2") is used as the index as-is.</li>
 * </ul>
 */
public final class PositionIndexParser {
    private static final String LAST = "last";
    private static final Map<String, Integer> NAMED_POSITIONS = Map.of(
            "first", 0, "1st", 0,
            "second", 1, "2nd", 1,
            "third", 2, "3rd", 2);

    private PositionIndexParser() {
    }

    public static int toIndex(String positionText, int listSize) {
        if (positionText == null || positionText.isBlank()) {
            return 0;
        }
        String normalized = positionText.trim().toLowerCase();
        if (LAST.equals(normalized)) {
            return Math.max(0, listSize - 1);
        }
        Integer namedIndex = NAMED_POSITIONS.get(normalized);
        if (null != namedIndex) {
            return namedIndex;
        }
        String ordinalDigits = isOrdinalWord(normalized) ? normalized.replaceAll("[^0-9]", "") : "";
        return ordinalDigits.isEmpty() ? parseIndexOrFirst(normalized) : Integer.parseInt(ordinalDigits) - 1;
    }

    private static boolean isOrdinalWord(String normalized) {
        return normalized.endsWith("st") || normalized.endsWith("nd")
                || normalized.endsWith("rd") || normalized.endsWith("th");
    }

    private static int parseIndexOrFirst(String normalized) {
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
