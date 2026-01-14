package io.verifyx.util;

import io.verifyx.Main;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;

public class IPTracker {
    private final Main plugin;
    private final File dataFolder;
    private final File ipFile;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory caches to avoid frequent file scans
    private final Map<String, String> lastIpByPlayer = new ConcurrentHashMap<>();
    private final Map<String, String> lastUuidByPlayer = new ConcurrentHashMap<>();
    // Cache last known locations in-memory for immediate restore on join
    private final Map<String, org.bukkit.Location> lastLocationByUUID = new ConcurrentHashMap<>();
    private final Map<String, org.bukkit.Location> lastLocationByPlayer = new ConcurrentHashMap<>();

    // Simple queue for writes; a background task flushes it periodically off the main-thread
    private final Queue<String> writeQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean writerScheduled = new AtomicBoolean(false);

    public IPTracker(Main plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.ipFile = new File(this.dataFolder, "player_ips.txt");
        this.ensureFileExists();
        // load current state into memory once
        this.loadCacheFromFile();
        // schedule regular flush (in case plugin shuts down unexpectedly we still flush periodically)
        this.schedulePeriodicFlush();
    }

    private void ensureFileExists() {
        try {
            if (!this.dataFolder.exists()) {
                this.dataFolder.mkdirs();
            }
            if (!this.ipFile.exists()) {
                this.ipFile.createNewFile();
                this.plugin.getLogger().info("✓ Created player_ips.txt tracking file");
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error creating IP tracking file: " + e.toString());
        }
    }

    private void loadCacheFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.ipFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String playerName = parts[0].trim();
                    String uuid = null;
                    String ip = null;
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("UUID:")) uuid = part.substring(5).trim();
                        if (part.startsWith("IP:")) ip = part.substring(3).trim();
                    }
                    if (ip != null) lastIpByPlayer.put(playerName, ip);
                    if (uuid != null) lastUuidByPlayer.put(playerName, uuid);
                }
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error loading IP tracker cache: " + e.toString());
        }
    }

    private void schedulePeriodicFlush() {
        // schedule a periodic async task to flush queued writes every 5 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously((org.bukkit.plugin.Plugin)this.plugin, () -> {
            try {
                flushQueueToFile();
            } catch (Exception ignored) {}
        }, 100L, 100L);
    }

    private void flushQueueToFile() {
        if (this.writeQueue.isEmpty()) return;
        java.util.List<String> drained = new java.util.ArrayList<>();
        String next;
        while ((next = this.writeQueue.poll()) != null) drained.add(next);
        boolean containsRemovePlayer = drained.stream().anyMatch(s -> s.startsWith("__REMOVE_PLAYER__|"));
        boolean containsRemoveUUID = drained.stream().anyMatch(s -> s.startsWith("__REMOVE_UUID__|"));
        try {
            if (!containsRemovePlayer && !containsRemoveUUID) {
                try (FileWriter writer = new FileWriter(this.ipFile, true)) {
                    for (String entry : drained) writer.append(entry);
                    writer.flush();
                }
            } else {
                // rewrite file excluding matching entries
                File tempFile = new File(this.dataFolder, "player_ips_tmp.txt");
                try (BufferedReader reader = new BufferedReader(new FileReader(this.ipFile)); java.io.FileWriter writer = new java.io.FileWriter(tempFile, false)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        boolean skip = false;
                        for (String marker : drained) {
                            if (marker.startsWith("__REMOVE_PLAYER__|")) {
                                String name = marker.substring("__REMOVE_PLAYER__|".length()).trim();
                                if (line.startsWith(name + " |")) { skip = true; break; }
                            } else if (marker.startsWith("__REMOVE_UUID__|")) {
                                String uuid = marker.substring("__REMOVE_UUID__|".length()).trim();
                                if (line.contains("UUID: " + uuid)) { skip = true; break; }
                            }
                        }
                        if (!skip) writer.append(line).append(System.lineSeparator());
                    }
                }
                if (this.ipFile.delete()) tempFile.renameTo(this.ipFile);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error flushing IP tracker queue: " + e.toString());
        }
    }

    public void recordPlayerIP(String playerName, String ipAddress, String playerUUID) {
        if (playerName == null || playerName.isEmpty() || ipAddress == null) return;
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String entry = String.format("%s | UUID: %s | IP: %s | Time: %s%n", playerName, playerUUID == null ? "" : playerUUID, ipAddress, timestamp);
            // update in-memory cache for fast reads
            lastIpByPlayer.put(playerName, ipAddress);
            if (playerUUID != null && !playerUUID.isEmpty()) lastUuidByPlayer.put(playerName, playerUUID);
            // enqueue write (fast non-blocking)
            this.writeQueue.add(entry);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error recording player IP: " + e.toString());
        }
    }

    public String getPlayerIP(String playerName) {
        if (playerName == null) return null;
        String ip = this.lastIpByPlayer.get(playerName);
        return ip;
    }

    public String getLastKnownUUID(String playerName) {
        if (playerName == null) return null;
        return this.lastUuidByPlayer.get(playerName);
    }

    public Map<String, String> getAllPlayerIPs() {
        return new HashMap<>(this.lastIpByPlayer);
    }

    public File getIPFile() {
        return this.ipFile;
    }

    public boolean isUUIDTracked(String playerUUID) {
        if (playerUUID == null) return false;
        return this.lastUuidByPlayer.containsValue(playerUUID);
    }

    public void recordPlayerLocation(String playerName, String playerUUID, org.bukkit.Location loc) {
        if (playerName == null || playerName.isEmpty() || loc == null) return;
        try {
            String timestamp = LocalDateTime.now().format(formatter);
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
            String entry = String.format("%s | UUID: %s | LOC: %s,%.6f,%.6f,%.6f,%.2f,%.2f | Time: %s%n", playerName, playerUUID == null ? "" : playerUUID, worldName, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), timestamp);
            // update in-memory UUID cache
            if (playerUUID != null && !playerUUID.isEmpty()) lastUuidByPlayer.put(playerName, playerUUID);
            // update in-memory last location caches for fast restore
            if (playerUUID != null && !playerUUID.isEmpty()) lastLocationByUUID.put(playerUUID, loc);
            lastLocationByPlayer.put(playerName, loc);
            // enqueue write
            this.writeQueue.add(entry);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error recording player location: " + e.toString());
        }
    }

    public org.bukkit.Location getLastLocation(String playerName) {
        if (playerName == null) return null;
        org.bukkit.Location loc = this.lastLocationByPlayer.get(playerName);
        if (loc != null) return loc;
        // fallback: try to resolve via cached UUID mapping
        String uuid = this.lastUuidByPlayer.get(playerName);
        if (uuid != null) return this.lastLocationByUUID.get(uuid);
        return null;
    }

    public org.bukkit.Location getLastLocationByUUID(String playerUUID) {
        if (playerUUID == null) return null;
        org.bukkit.Location loc = this.lastLocationByUUID.get(playerUUID);
        if (loc != null) return loc;
        // fallback: try lookup by player name
        for (Map.Entry<String, String> e : this.lastUuidByPlayer.entrySet()) {
            if (playerUUID.equals(e.getValue())) {
                return this.lastLocationByPlayer.get(e.getKey());
            }
        }
        return null;
    }

    public void removeEntriesByPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) return;
        // enqueue a removal marker to be processed by flush task to avoid blocking the main thread
        try {
            String marker = "__REMOVE_PLAYER__|" + playerName + System.lineSeparator();
            this.writeQueue.add(marker);
            // update in-memory state immediately
            this.lastIpByPlayer.remove(playerName);
            this.lastUuidByPlayer.remove(playerName);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error scheduling removal of player entries from tracker: " + e.toString());
        }
    }

    public void removeEntriesByUUID(String playerUUID) {
        if (playerUUID == null || playerUUID.isEmpty()) return;
        try {
            String marker = "__REMOVE_UUID__|" + playerUUID + System.lineSeparator();
            this.writeQueue.add(marker);
            // update in-memory state
            this.lastUuidByPlayer.values().removeIf(val -> val != null && val.equals(playerUUID));
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error scheduling removal of UUID entries from tracker: " + e.toString());
        }
    }
}
