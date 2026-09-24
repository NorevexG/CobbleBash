package com.nore.cobblebash.integration;

import java.util.Locale;

public enum GymBattleFormat {
    SINGLES,
    DOUBLES,
    RANDOM;

    public static GymBattleFormat parse(String value) {
        if (value == null || value.isBlank()) {
            return SINGLES;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return SINGLES;
        }
    }

    public static boolean isSupported(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }

        for (GymBattleFormat format : values()) {
            if (format.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }
}
