/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Difficulty
 *  org.bukkit.GameMode
 *  org.bukkit.GameRule
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.World$Environment
 *  org.bukkit.WorldCreator
 *  org.bukkit.WorldType
 *  org.bukkit.entity.Player
 *  org.bukkit.generator.ChunkGenerator
 *  org.bukkit.generator.ChunkGenerator$ChunkData
 *  org.bukkit.generator.WorldInfo
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package io.verifyx;

import io.verifyx.Main;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldManager {
    private final Main plugin;
    private World verificationWorld;
    private boolean worldReady = false;

    public WorldManager(Main plugin) {
        this.plugin = plugin;
        new BukkitRunnable(){

            public void run() {
                WorldManager.this.createVerificationWorld();
            }
        }.runTask((Plugin)plugin);
    }

    private void createVerificationWorld() {
        String worldName = "verifyx_world";
        this.plugin.getLogger().info("\ud83c\udf0d Creating verification world: " + worldName);
        World existingWorld = Bukkit.getWorld((String)worldName);
        if (existingWorld != null) {
            this.verificationWorld = existingWorld;
            this.plugin.getLogger().info("\u2705 Found existing verification world");
            this.worldReady = true;
            return;
        }
        try {
            WorldCreator wc = new WorldCreator(worldName);
            wc.generator((ChunkGenerator)new EmptyWorldGenerator());
            wc.type(WorldType.FLAT);
            wc.generateStructures(false);
            wc.environment(World.Environment.NORMAL);
            this.verificationWorld = Bukkit.createWorld((WorldCreator)wc);
            if (this.verificationWorld != null) {
                this.setupWorldRules();
                this.plugin.getLogger().info("\u2705 Successfully created verification world");
                this.worldReady = true;
            } else {
                this.plugin.getLogger().severe("\u274c Failed to create verification world");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().severe("\u274c Error creating verification world: " + e.toString());
            e.printStackTrace();
        }
    }

    private void setupWorldRules() {
        if (this.verificationWorld == null) {
            return;
        }
        this.verificationWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        this.verificationWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.verificationWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        this.verificationWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        this.verificationWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
        this.verificationWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        this.verificationWorld.setGameRule(GameRule.DO_INSOMNIA, false);
        this.verificationWorld.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        this.verificationWorld.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        this.verificationWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        this.verificationWorld.setDifficulty(Difficulty.PEACEFUL);
        new BukkitRunnable(){

            public void run() {
                WorldManager.this.createWorldBoundary();
            }
        }.runTaskAsynchronously((Plugin)this.plugin);
    }

    private void createWorldBoundary() {
        if (this.verificationWorld == null) {
            return;
        }
        int size = 80;
        this.plugin.getLogger().info("\ud83e\uddf1 Creating world boundary...");
        int x = -size;
        while (true) {
            if (x > size) {
                new BukkitRunnable(){

                    public void run() {
                        int x = -10;
                        while (true) {
                            if (x > 10) {
                                WorldManager.this.verificationWorld.setSpawnLocation(0, 64, 0);
                                WorldManager.this.plugin.getLogger().info("\u2705 World boundary created");
                                return;
                            }
                            for (int z = -10; z <= 10; ++z) {
                                WorldManager.this.verificationWorld.getBlockAt(x, 63, z).setType(Material.GRASS_BLOCK, false);
                                WorldManager.this.verificationWorld.getBlockAt(x, 62, z).setType(Material.DIRT, false);
                                WorldManager.this.verificationWorld.getBlockAt(x, 61, z).setType(Material.DIRT, false);
                            }
                            ++x;
                        }
                    }
                }.runTask((Plugin)this.plugin);
                return;
            }
            for (int z = -size; z <= size; ++z) {
                if (Math.abs(x) != size && Math.abs(z) != size) continue;
                final int finalX = x;
                final int finalZ = z;
                new BukkitRunnable(){

                    public void run() {
                        int y = 0;
                        while (y <= 10) {
                            WorldManager.this.verificationWorld.getBlockAt(finalX, y, finalZ).setType(Material.BEDROCK, false);
                            ++y;
                        }
                        return;
                    }
                }.runTask((Plugin)this.plugin);
            }
            ++x;
        }
    }

    public void teleportToVerificationWorld(Player player) {
        if (this.verificationWorld != null && this.worldReady) {
            Location spawnLoc = new Location(this.verificationWorld, 0.5, 64.5, 0.5);
            spawnLoc.setPitch(0.0f);
            spawnLoc.setYaw(180.0f);
            player.teleport(spawnLoc);
            player.getInventory().clear();
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0.0f);
            player.setFireTicks(0);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            this.plugin.getLogger().info("Teleported " + player.getName() + " to verification world");
            return;
        }
        this.plugin.getLogger().warning("Verification world not ready, cannot teleport player");
    }

    public void teleportToMainWorld(Player player) {
        try {
            org.bukkit.Location lastLoc = null;
            if (this.plugin.getIPTracker() != null) {
                lastLoc = this.plugin.getIPTracker().getLastLocationByUUID(player.getUniqueId().toString());
                if (lastLoc == null) lastLoc = this.plugin.getIPTracker().getLastLocation(player.getName());
            }
            if (lastLoc != null && lastLoc.getWorld() != null) {
                player.teleport(lastLoc);
                player.setGameMode(GameMode.SURVIVAL);
                this.plugin.getLogger().info("Teleported " + player.getName() + " to their last known location");
                return;
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error locating last known position for " + player.getName() + ": " + e.toString());
        }

        World mainWorld = (World)Bukkit.getWorlds().get(0);
        if (mainWorld == null) {
            this.plugin.getLogger().warning("Main world not found!");
            return;
        }
        Location spawnLoc = mainWorld.getSpawnLocation();
        if (spawnLoc == null) {
            spawnLoc = new Location(mainWorld, 0.0, 64.0, 0.0);
        }
        player.teleport(spawnLoc);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public boolean isInVerificationWorld(Player player) {
        return this.verificationWorld != null && player.getWorld().equals((Object)this.verificationWorld);
    }

    public World getVerificationWorld() {
        return this.verificationWorld;
    }

    public boolean isWorldReady() {
        return this.worldReady;
    }

    public void unloadWorld() {
        if (this.verificationWorld != null && this.verificationWorld.getPlayers().isEmpty()) {
            Bukkit.unloadWorld((World)this.verificationWorld, (boolean)false);
            this.verificationWorld = null;
            this.worldReady = false;
            this.plugin.getLogger().info("Unloaded verification world");
        }
    }

    private static class EmptyWorldGenerator
    extends ChunkGenerator {
        private EmptyWorldGenerator() {
        }

        public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        }

        public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        }

        public void generateBedrock(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        }

        public void generateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
        }

        public boolean shouldGenerateNoise() {
            return false;
        }

        public boolean shouldGenerateSurface() {
            return false;
        }

        public boolean shouldGenerateBedrock() {
            return false;
        }

        public boolean shouldGenerateCaves() {
            return false;
        }
    }
}

