package com.github.k1ritoz.vote2Sleep.utils;

import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PluginMetadata {

    private PluginMetadata() {
    }

    public static String getVersion(Plugin plugin) {
        Object value = invokeMetadataMethod(plugin, "getVersion");
        return value instanceof String ? (String) value : "unknown";
    }

    public static List<String> getAuthors(Plugin plugin) {
        Object value = invokeMetadataMethod(plugin, "getAuthors");
        if (!(value instanceof Iterable<?> authors)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (Object author : authors) {
            result.add(String.valueOf(author));
        }
        return result;
    }

    private static Object invokeMetadataMethod(Plugin plugin, String methodName) {
        Object metadata = getMetadata(plugin);
        if (metadata == null) {
            return null;
        }

        try {
            Method method = metadata.getClass().getMethod(methodName);
            return method.invoke(metadata);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    private static Object getMetadata(Plugin plugin) {
        Object pluginMeta = invokePluginMethod(plugin, "getPluginMeta");
        return pluginMeta != null ? pluginMeta : invokePluginMethod(plugin, "getDescription");
    }

    private static Object invokePluginMethod(Plugin plugin, String methodName) {
        try {
            Method method = plugin.getClass().getMethod(methodName);
            return method.invoke(plugin);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }
}
