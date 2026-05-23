package com.github.k1ritoz.vote2Sleep.managers;

import com.github.k1ritoz.vote2Sleep.Vote2Sleep;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class HooksManager {

    private static final String[] AFK_METHOD_NAMES = {"isAfk", "isAFK", "getAfk", "getAFK"};
    private static final String[] AFK_METADATA_KEYS = {"afk", "AFK", "isAfk", "is_afk", "essentials_afk", "cmi_afk"};
    private static final String[] GENERIC_AFK_METHOD_NAMES = {
            "isAfk", "isAFK", "isPlayerAfk", "isPlayerAFK",
            "getAfk", "getAFK", "getPlayerAfk", "getPlayerAFK"
    };
    private static final String[] GENERIC_TARGET_METHOD_NAMES = {
            "getAPI", "getApi", "getAfkManager", "getAFKManager", "getPlayerManager", "getUserManager"
    };
    private static final String[] GENERIC_USER_METHOD_NAMES = {"getUser", "getPlayerData", "getAfkPlayer", "getAFKPlayer"};

    private final Vote2Sleep plugin;
    private final List<AfkHook> afkHooks = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<UUID, Long> lastActivityByPlayer = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> resolvingPlaceholderAfk = ThreadLocal.withInitial(() -> false);

    private volatile boolean placeholderAPIHooked = false;
    private volatile Vote2SleepPlaceholders placeholderExpansion = null;

    public HooksManager(Vote2Sleep plugin) {
        this.plugin = plugin;
    }

    public void initializeHooks() {
        initializePlaceholderAPI();
        initializeAfkHooks();

        plugin.getLogger().info("Hooks initialized - PlaceholderAPI: " + placeholderAPIHooked +
                ", AFK hooks: " + getAfkHookNames());
    }

    private void initializePlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found");
            return;
        }

        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                plugin.getLogger().info("PlaceholderAPI found but not enabled");
                return;
            }

            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");

            placeholderExpansion = new Vote2SleepPlaceholders(plugin);
            if (placeholderExpansion.register()) {
                placeholderAPIHooked = true;
                plugin.getLogger().info("Successfully hooked into PlaceholderAPI");
            } else {
                plugin.getLogger().warning("Failed to register PlaceholderAPI expansion");
            }
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("PlaceholderAPI classes not found: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook into PlaceholderAPI", e);
        }
    }

    private void initializeAfkHooks() {
        afkHooks.clear();

        if (!plugin.getConfigManager().isAfkDetectionEnabled()) {
            plugin.getLogger().info("AFK detection disabled in configuration");
            return;
        }

        if (plugin.getConfigManager().areAfkHooksEnabled()) {
            registerExternalAfkHooks();
        }

        if (plugin.getConfigManager().isInternalAfkDetectionEnabled()) {
            Bukkit.getOnlinePlayers().forEach(this::recordPlayerActivity);
            plugin.getLogger().info("Internal AFK detector enabled (fallback after " +
                    plugin.getConfigManager().getInternalAfkTimeoutSeconds() + " seconds)");
        }

        if (afkHooks.isEmpty() && !plugin.getConfigManager().isInternalAfkDetectionEnabled()) {
            plugin.getLogger().warning("AFK detection is enabled, but no external hooks or internal fallback are active");
        }
    }

    private void registerExternalAfkHooks() {
        if (plugin.getConfigManager().isEssentialsAfkHookEnabled()) {
            Plugin essentials = getEnabledPlugin("Essentials");
            if (essentials != null) {
                afkHooks.add(new EssentialsAfkHook(essentials));
                plugin.getLogger().info("Essentials AFK hook enabled");
            }
        }

        if (plugin.getConfigManager().isCmiAfkHookEnabled()) {
            Plugin cmi = getEnabledPlugin("CMI");
            if (cmi != null) {
                afkHooks.add(new CmiAfkHook(cmi));
                plugin.getLogger().info("CMI AFK hook enabled");
            }
        }

        if (plugin.getConfigManager().isPlaceholderApiAfkHookEnabled() && placeholderAPIHooked) {
            afkHooks.add(new PlaceholderApiAfkHook());
            plugin.getLogger().info("PlaceholderAPI AFK hook enabled");
        }

        for (String pluginName : plugin.getConfigManager().getGenericAfkPluginNames()) {
            Plugin genericPlugin = getEnabledPlugin(pluginName);
            if (genericPlugin != null) {
                afkHooks.add(new GenericPluginAfkHook(genericPlugin));
                plugin.getLogger().info(pluginName + " generic AFK hook enabled");
            }
        }

        if (plugin.getConfigManager().isMetadataAfkHookEnabled()) {
            afkHooks.add(new MetadataAfkHook());
            plugin.getLogger().info("Generic metadata AFK hook enabled");
        }

        if (plugin.getConfigManager().isSleepingIgnoredAfkHookEnabled()) {
            afkHooks.add(new SleepingIgnoredAfkHook());
            plugin.getLogger().info("Bukkit sleeping-ignored AFK hook enabled");
        }
    }

    private Plugin getEnabledPlugin(String pluginName) {
        Plugin hookedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (hookedPlugin != null && Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            return hookedPlugin;
        }
        return null;
    }

    /**
     * Re-initialize hooks (useful for plugin reloads)
     */
    public void reinitializeHooks() {
        unregisterPlaceholderExpansion();

        placeholderAPIHooked = false;
        placeholderExpansion = null;
        afkHooks.clear();

        initializeHooks();
    }

    public boolean isPlayerAfk(Player player) {
        if (!plugin.getConfigManager().isAfkDetectionEnabled()) {
            return false;
        }

        boolean externalAnswered = false;
        for (AfkHook afkHook : afkHooks) {
            try {
                Optional<Boolean> status = afkHook.isAfk(player);
                if (status.isPresent()) {
                    externalAnswered = true;
                    if (status.get()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("AFK hook " + afkHook.getName() + " failed for " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        if (!externalAnswered && plugin.getConfigManager().isInternalAfkDetectionEnabled()) {
            return isInternallyAfk(player);
        }

        return false;
    }

    public void recordPlayerActivity(Player player) {
        lastActivityByPlayer.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void forgetPlayerActivity(Player player) {
        lastActivityByPlayer.remove(player.getUniqueId());
    }

    public int getAfkPlayerCount(Iterable<Player> players) {
        int count = 0;
        for (Player player : players) {
            if (isPlayerAfk(player)) {
                count++;
            }
        }
        return count;
    }

    private boolean isInternallyAfk(Player player) {
        long now = System.currentTimeMillis();
        long lastActivity = lastActivityByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> now);
        long timeoutMillis = plugin.getConfigManager().getInternalAfkTimeoutSeconds() * 1000L;

        return timeoutMillis > 0 && now - lastActivity >= timeoutMillis;
    }

    private Optional<Boolean> readAfkFlag(Object target) {
        for (String methodName : AFK_METHOD_NAMES) {
            Optional<Boolean> value = invokeBooleanMethod(target, methodName);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private Optional<Boolean> invokeBooleanMethod(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Boolean) {
                return Optional.of((Boolean) result);
            }
        } catch (ReflectiveOperationException ignored) {
            // Try the next known method name.
        }

        return Optional.empty();
    }

    private Optional<Boolean> invokeAfkMethod(Object target, String methodName, Player player) {
        Object result = invokeMethod(target, methodName, Player.class, player);
        if (result == null) {
            result = invokeMethod(target, methodName, UUID.class, player.getUniqueId());
        }
        if (result == null) {
            result = invokeMethod(target, methodName, String.class, player.getName());
        }

        if (result instanceof Boolean) {
            return Optional.of((Boolean) result);
        }

        return readAfkFlag(result);
    }

    private Object invokeMethod(Object target, String methodName, Class<?> parameterType, Object argument) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            return method.invoke(target, argument);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Object invokeNoArgMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Optional<Boolean> parseTextAfkValue(String rawValue, List<String> trueValues, List<String> falseValues) {
        String normalizedValue = normalizeAfkText(rawValue);
        if (normalizedValue.isEmpty()) {
            return Optional.empty();
        }

        if (matchesAfkValue(normalizedValue, trueValues)) {
            return Optional.of(true);
        }
        if (matchesAfkValue(normalizedValue, falseValues)) {
            return Optional.of(false);
        }

        return Optional.empty();
    }

    private boolean matchesAfkValue(String normalizedValue, List<String> expectedValues) {
        for (String expectedValue : expectedValues) {
            if (normalizedValue.equals(normalizeAfkText(expectedValue))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private String normalizeAfkText(String value) {
        if (value == null) {
            return "";
        }

        return ChatColor.stripColor(value.replaceAll("(?i)&[0-9A-FK-ORX]", ""))
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private List<String> getAfkHookNames() {
        if (afkHooks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        for (AfkHook afkHook : afkHooks) {
            names.add(afkHook.getName());
        }
        return names;
    }

    // Getters
    public boolean isPlaceholderAPIHooked() {
        return placeholderAPIHooked;
    }

    /**
     * Cleanup method for plugin shutdown
     */
    public void cleanup() {
        unregisterPlaceholderExpansion();
        afkHooks.clear();
        lastActivityByPlayer.clear();
    }

    private void unregisterPlaceholderExpansion() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
                plugin.getLogger().info("PlaceholderAPI expansion unregistered");
            } catch (Exception e) {
                plugin.getLogger().warning("Error unregistering PlaceholderAPI expansion: " + e.getMessage());
            }
        }
    }

    private interface AfkHook {
        String getName();

        Optional<Boolean> isAfk(Player player);
    }

    private class EssentialsAfkHook implements AfkHook {
        private final Plugin essentialsPlugin;

        private EssentialsAfkHook(Plugin essentialsPlugin) {
            this.essentialsPlugin = essentialsPlugin;
        }

        @Override
        public String getName() {
            return "Essentials";
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            Object user = invokeMethod(essentialsPlugin, "getUser", Player.class, player);
            if (user == null) {
                user = invokeMethod(essentialsPlugin, "getUser", String.class, player.getName());
            }
            return readAfkFlag(user);
        }
    }

    private class CmiAfkHook implements AfkHook {
        private final Plugin cmiPlugin;

        private CmiAfkHook(Plugin cmiPlugin) {
            this.cmiPlugin = cmiPlugin;
        }

        @Override
        public String getName() {
            return "CMI";
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            Object cmiInstance = getCmiInstance();
            Object playerManager = invokeNoArgMethod(cmiInstance, "getPlayerManager");
            if (playerManager == null) {
                playerManager = invokeNoArgMethod(cmiPlugin, "getPlayerManager");
            }

            Object user = invokeMethod(playerManager, "getUser", UUID.class, player.getUniqueId());
            if (user == null) {
                user = invokeMethod(playerManager, "getUser", Player.class, player);
            }
            if (user == null) {
                user = invokeMethod(playerManager, "getUser", String.class, player.getName());
            }

            return readAfkFlag(user);
        }

        private Object getCmiInstance() {
            try {
                Class<?> cmiClass = Class.forName("com.Zrips.CMI.CMI");
                Method getInstance = cmiClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                return instance != null ? instance : cmiPlugin;
            } catch (ReflectiveOperationException ignored) {
                return cmiPlugin;
            }
        }
    }

    private class PlaceholderApiAfkHook implements AfkHook {
        @Override
        public String getName() {
            return "PlaceholderAPI";
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            if (!placeholderAPIHooked || resolvingPlaceholderAfk.get()) {
                return Optional.empty();
            }

            List<String> placeholders = plugin.getConfigManager().getPlaceholderApiAfkPlaceholders();
            if (placeholders.isEmpty()) {
                return Optional.empty();
            }

            resolvingPlaceholderAfk.set(true);
            try {
                for (String placeholder : placeholders) {
                    if (placeholder == null || placeholder.isBlank() || isVote2SleepPlaceholder(placeholder)) {
                        continue;
                    }

                    String result = setPlaceholderApiPlaceholders(player, placeholder);
                    if (result == null || placeholder.equals(result)) {
                        continue;
                    }

                    Optional<Boolean> status = parseTextAfkValue(
                            result,
                            plugin.getConfigManager().getPlaceholderApiAfkTrueValues(),
                            plugin.getConfigManager().getPlaceholderApiAfkFalseValues()
                    );
                    if (status.isPresent()) {
                        return status;
                    }
                }
            } finally {
                resolvingPlaceholderAfk.remove();
            }

            return Optional.empty();
        }

        private boolean isVote2SleepPlaceholder(String placeholder) {
            return placeholder.toLowerCase(Locale.ROOT).contains("%vote2sleep_");
        }

        private String setPlaceholderApiPlaceholders(Player player, String placeholder) {
            try {
                Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Method setPlaceholders = placeholderAPIClass.getMethod("setPlaceholders", Player.class, String.class);
                return (String) setPlaceholders.invoke(null, player, placeholder);
            } catch (ReflectiveOperationException e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("PlaceholderAPI AFK hook failed for " + player.getName() + ": " + e.getMessage());
                }
                return null;
            }
        }
    }

    private class GenericPluginAfkHook implements AfkHook {
        private final Plugin hookedPlugin;

        private GenericPluginAfkHook(Plugin hookedPlugin) {
            this.hookedPlugin = hookedPlugin;
        }

        @Override
        public String getName() {
            return hookedPlugin.getName();
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            for (Object target : getTargets()) {
                Optional<Boolean> directValue = readDirectAfkValue(target, player);
                if (directValue.isPresent()) {
                    return directValue;
                }

                Optional<Boolean> userValue = readAfkUserValue(target, player);
                if (userValue.isPresent()) {
                    return userValue;
                }
            }

            return Optional.empty();
        }

        private List<Object> getTargets() {
            List<Object> targets = new ArrayList<>();
            targets.add(hookedPlugin);

            for (String methodName : GENERIC_TARGET_METHOD_NAMES) {
                Object target = invokeNoArgMethod(hookedPlugin, methodName);
                if (target != null) {
                    targets.add(target);
                }
            }

            return targets;
        }

        private Optional<Boolean> readDirectAfkValue(Object target, Player player) {
            for (String methodName : GENERIC_AFK_METHOD_NAMES) {
                Optional<Boolean> value = invokeAfkMethod(target, methodName, player);
                if (value.isPresent()) {
                    return value;
                }
            }

            return Optional.empty();
        }

        private Optional<Boolean> readAfkUserValue(Object target, Player player) {
            for (String methodName : GENERIC_USER_METHOD_NAMES) {
                Object user = invokeMethod(target, methodName, UUID.class, player.getUniqueId());
                if (user == null) {
                    user = invokeMethod(target, methodName, Player.class, player);
                }
                if (user == null) {
                    user = invokeMethod(target, methodName, String.class, player.getName());
                }

                Optional<Boolean> value = readAfkFlag(user);
                if (value.isPresent()) {
                    return value;
                }
            }

            return Optional.empty();
        }
    }

    @SuppressWarnings("deprecation")
    private static class MetadataAfkHook implements AfkHook {
        @Override
        public String getName() {
            return "PlayerMetadata";
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            for (String metadataKey : AFK_METADATA_KEYS) {
                if (!player.hasMetadata(metadataKey)) {
                    continue;
                }

                for (MetadataValue metadataValue : player.getMetadata(metadataKey)) {
                    Optional<Boolean> value = parseMetadataValue(metadataValue);
                    if (value.isPresent()) {
                        return value;
                    }
                }
            }

            return Optional.empty();
        }

        private Optional<Boolean> parseMetadataValue(MetadataValue metadataValue) {
            Object rawValue = metadataValue.value();
            if (rawValue instanceof Boolean) {
                return Optional.of((Boolean) rawValue);
            }
            if (rawValue instanceof Number) {
                return Optional.of(((Number) rawValue).intValue() != 0);
            }

            String textValue = String.valueOf(rawValue);
            if ("true".equalsIgnoreCase(textValue) || "yes".equalsIgnoreCase(textValue) || "afk".equalsIgnoreCase(textValue)) {
                return Optional.of(true);
            }
            if ("false".equalsIgnoreCase(textValue) || "no".equalsIgnoreCase(textValue) || "active".equalsIgnoreCase(textValue)) {
                return Optional.of(false);
            }

            return Optional.empty();
        }
    }

    private static class SleepingIgnoredAfkHook implements AfkHook {
        @Override
        public String getName() {
            return "BukkitSleepingIgnored";
        }

        @Override
        public Optional<Boolean> isAfk(Player player) {
            try {
                Method method = player.getClass().getMethod("isSleepingIgnored");
                Object result = method.invoke(player);
                if (Boolean.TRUE.equals(result)) {
                    return Optional.of(true);
                }
            } catch (ReflectiveOperationException ignored) {
                return Optional.empty();
            }

            return Optional.empty();
        }
    }
}