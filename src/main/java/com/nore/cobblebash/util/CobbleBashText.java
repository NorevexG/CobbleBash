package com.nore.cobblebash.util;

import com.gitlab.srcmc.rctapi.api.util.Text;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Pattern;

public final class CobbleBashText {
    private static final Pattern TRANSLATION_KEY = Pattern.compile("[a-z0-9_.-]+");

    private CobbleBashText() {
    }

    public static MutableComponent component(String value) {
        return isTranslationKey(value) ? Component.translatable(value) : Component.literal(value);
    }

    public static Text rctText(String value) {
        return isTranslationKey(value) ? Text.translatable(value) : Text.literal(value);
    }

    public static boolean isTranslationKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmed = value.trim();
        return trimmed.indexOf('.') >= 0 && TRANSLATION_KEY.matcher(trimmed).matches();
    }
}
