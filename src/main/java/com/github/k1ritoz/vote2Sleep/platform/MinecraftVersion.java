package com.github.k1ritoz.vote2Sleep.platform;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MinecraftVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");
    private static final int REQUIRED_MAJOR = 26;
    private static final int REQUIRED_MINOR = 1;
    private static final int REQUIRED_PATCH = 0;

    private MinecraftVersion() {
    }

    static boolean isSupported() {
        int[] current = getCurrentVersion();

        if (current[0] != REQUIRED_MAJOR) {
            return current[0] > REQUIRED_MAJOR;
        }
        if (current[1] != REQUIRED_MINOR) {
            return current[1] > REQUIRED_MINOR;
        }
        return current[2] >= REQUIRED_PATCH;
    }

    private static int[] getCurrentVersion() {
        String version = getMinecraftVersion();
        if (version == null || version.isBlank()) {
            version = Bukkit.getBukkitVersion().split("-")[0];
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return new int[]{0, 0, 0};
        }

        return new int[]{
                parsePart(matcher.group(1)),
                parsePart(matcher.group(2)),
                parsePart(matcher.group(3))
        };
    }

    private static String getMinecraftVersion() {
        try {
            Method method = Bukkit.class.getMethod("getMinecraftVersion");
            Object value = method.invoke(null);
            return value instanceof String ? (String) value : null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static int parsePart(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
