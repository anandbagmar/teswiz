package com.znsio.teswiz.entities;

public enum SpatialDirection {
    ABOVE("above"),
    BELOW("below"),
    LEFT_OF("left of"),
    RIGHT_OF("right of"),
    NEAR("near");

    private final String direction;

    SpatialDirection(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }

    public static SpatialDirection fromString(String text) {
        if (text == null || text.isBlank()) {
            return NEAR;
        }
        String normalized = text.trim().toLowerCase();
        for (SpatialDirection dir : values()) {
            if (dir.direction.equalsIgnoreCase(normalized) || dir.name().equalsIgnoreCase(normalized)) {
                return dir;
            }
        }
        if (normalized.contains("above") || normalized.contains("top")) return ABOVE;
        if (normalized.contains("below") || normalized.contains("bottom")) return BELOW;
        if (normalized.contains("left")) return LEFT_OF;
        if (normalized.contains("right")) return RIGHT_OF;
        return NEAR;
    }
}
