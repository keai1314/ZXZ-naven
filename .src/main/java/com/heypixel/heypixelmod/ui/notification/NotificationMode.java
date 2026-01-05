package com.heypixel.heypixelmod.ui.notification;

public enum NotificationMode {
    NAVEN("Naven"),
    SOUTHSIDE("SouthSide"),
    CAPSULE("Capsule"),
    ISLAND("isLand");

    private final String displayName;
    private static NotificationMode currentMode = SOUTHSIDE;

    NotificationMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static NotificationMode getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(NotificationMode mode) {
        currentMode = mode;
    }

    public static boolean isNavenMode() {
        return currentMode == NAVEN;
    }

    public static boolean isSouthSideMode() {
        return currentMode == SOUTHSIDE;
    }

    public static boolean isCapsuleMode() {
        return currentMode == CAPSULE;
    }

    public static boolean isIsLandMode() {
        return currentMode == ISLAND;
    }
}
