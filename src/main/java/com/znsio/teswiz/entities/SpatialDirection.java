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

    /**
     * Returns whether a candidate at ({@code candidateCenterX}, {@code candidateCenterY}) lies in
     * this direction relative to an anchor at ({@code anchorCenterX}, {@code anchorCenterY}).
     * {@link #NEAR} matches any position. Encapsulating the predicate here keeps the directional
     * rule with the direction itself instead of a {@code switch} in the finder.
     */
    public boolean matchesRelativePosition(int anchorCenterX, int anchorCenterY,
                                           int candidateCenterX, int candidateCenterY) {
        switch (this) {
            case ABOVE:
                return candidateCenterY < anchorCenterY;
            case BELOW:
                return candidateCenterY > anchorCenterY;
            case LEFT_OF:
                return candidateCenterX < anchorCenterX;
            case RIGHT_OF:
                return candidateCenterX > anchorCenterX;
            case NEAR:
            default:
                return true;
        }
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
