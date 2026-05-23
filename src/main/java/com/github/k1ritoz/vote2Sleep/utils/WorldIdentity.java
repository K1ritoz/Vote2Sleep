package com.github.k1ritoz.vote2Sleep.utils;

import org.bukkit.World;

public final class WorldIdentity {

    public static final char CONFIG_PATH_SEPARATOR = '|';

    private WorldIdentity() {
    }

    public static String key(World world) {
        return world.getKey().asString();
    }

    public static String configPath(World world, String setting) {
        return "worlds" + CONFIG_PATH_SEPARATOR + key(world) + CONFIG_PATH_SEPARATOR + setting;
    }
}
