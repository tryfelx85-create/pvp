package com.example.simplekits;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

/**
 * Creates and manages a void world that contains nothing but a single
 * flat arena platform, for PvP kit fights.
 */
public final class ArenaManager {

    private static final String WORLD_NAME = "arena";

    /** Platform is a (2*RADIUS+1) x (2*RADIUS+1) square centered on 0,0. */
    private static final int RADIUS = 20;
    private static final int PLATFORM_Y = 100;

    private static final Material FLOOR_MATERIAL = Material.BLACKSTONE;
    private static final Material EDGE_MATERIAL = Material.GLOWSTONE;

    private final SimpleKits plugin;

    public ArenaManager(SimpleKits plugin) {
        this.plugin = plugin;
    }

    /** Returns the arena world, creating it (and the platform) if it doesn't exist yet. */
    public World getOrCreateArenaWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        boolean freshlyCreated = false;
        if (world == null) {
            WorldCreator creator = new WorldCreator(WORLD_NAME);
            creator.generator(new VoidGenerator());
            creator.generateStructures(false);
            world = creator.createWorld();
            freshlyCreated = true;
        }
        if (world == null) {
            throw new IllegalStateException("Failed to create/load the arena world.");
        }

        world.setSpawnFlags(false, false); // no natural monster/animal spawns
        world.setPVP(true);

        if (freshlyCreated) {
            buildPlatform(world);
        }
        return world;
    }

    /** Rebuilds the platform from scratch, clearing anything players broke or placed. */
    public void resetPlatform() {
        World world = getOrCreateArenaWorld();
        buildPlatform(world);
    }

    /** The point players land on top of the platform, centered. */
    public Location getSpawnLocation() {
        World world = getOrCreateArenaWorld();
        return new Location(world, 0.5, PLATFORM_Y + 1, 0.5);
    }

    public void teleport(Player player) {
        player.teleport(getSpawnLocation());
    }

    private void buildPlatform(World world) {
        // Clear a bit above and below the platform in case of leftover blocks from a previous build.
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++) {
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                for (int y = PLATFORM_Y - 2; y <= PLATFORM_Y + 4; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }

        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                boolean edge = Math.abs(x) == RADIUS || Math.abs(z) == RADIUS;
                Material material = edge ? EDGE_MATERIAL : FLOOR_MATERIAL;
                world.getBlockAt(x, PLATFORM_Y, z).setType(material, false);
            }
        }

        Location spawn = new Location(world, 0.5, PLATFORM_Y + 1, 0.5);
        world.setSpawnLocation(spawn);
    }
}
