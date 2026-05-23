package com.github.k1ritoz.vote2Sleep.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;
import java.util.Objects;

public final class SoundResolver {

    private SoundResolver() {
    }

    public static Sound resolve(String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return null;
        }

        String trimmed = soundName.trim();
        Sound sound = getByKey(trimmed);
        if (sound != null) {
            return sound;
        }

        return getByLegacyName(trimmed);
    }

    public static Sound resolveKey(String soundKey) {
        if (soundKey == null || soundKey.isBlank()) {
            return null;
        }

        return getByKey(soundKey.trim());
    }

    private static Sound getByKey(String soundName) {
        NamespacedKey key = toNamespacedKey(soundName);
        return key != null ? Registry.SOUNDS.get(key) : null;
    }

    private static Sound getByLegacyName(String soundName) {
        String normalized = normalize(soundName);
        return Registry.SOUNDS.keyStream()
                .filter(key -> normalize(key.getKey()).equals(normalized))
                .map(Registry.SOUNDS::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static NamespacedKey toNamespacedKey(String soundName) {
        String normalized = soundName.toLowerCase(Locale.ROOT);

        try {
            NamespacedKey key = NamespacedKey.fromString(normalized);
            if (key != null) {
                return key;
            }

            return NamespacedKey.minecraft(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
