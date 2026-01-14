package io.verifyx.security;

import com.google.gson.*;
import io.verifyx.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.Base64;

public class LoginSecurityManager {
    private final Main plugin;
    private final File dataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, Account> accounts = new HashMap<>(); // key = uuid
    private final Set<String> authenticated = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Integer> failedAttempts = new HashMap<>();
    private final Map<String, Long> lastLoginMillis = new java.util.concurrent.ConcurrentHashMap<>();

    private static class FailedInfo {
        public int attempts = 0;
        public int stage = 0; // 0 -> before first cooldown, 1 -> after first, 2 -> after second
        public long cooldownEndsAt = 0L;
    }
    // account uuid -> (ip -> FailedInfo)
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, FailedInfo>> failedAttemptsByIp = new java.util.concurrent.ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    public static class Account {
        public String name;
        public String uuid;
        public String salt; // base64
        public String hash; // base64
        public int iterations;
        public String createdAt;
        public String lastKnownIp; // last IP that was used to authenticate
        public String discordUserId; // linked discord account
        public java.util.List<String> blockedIps = new java.util.ArrayList<>(); // account-specific blocked IPs
        public java.util.List<String> bedrockXuids = new java.util.ArrayList<>(); // linked Bedrock XUIDs
    }

    public LoginSecurityManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(this.plugin.getDataFolder(), "loginsecurity.json");
        load();
    }

    private void load() {
        try {
            if (!this.dataFile.exists()) {
                save();
                return;
            }
            try (FileReader fr = new FileReader(this.dataFile)) {
                JsonObject root = JsonParser.parseReader(fr).getAsJsonObject();
                if (root == null) return;
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    try {
                        Account a = this.gson.fromJson(e.getValue(), Account.class);
                        if (a != null && a.uuid != null) this.accounts.put(a.uuid, a);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Error loading loginsecurity data: " + e.toString(), e);
        }
    }

    private synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            for (Account a : this.accounts.values()) {
                root.add(a.uuid, this.gson.toJsonTree(a));
            }
            try (FileWriter fw = new FileWriter(this.dataFile)) {
                fw.write(this.gson.toJson(root));
            }
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.WARNING, "Error saving loginsecurity data: " + e.toString(), e);
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLen) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLen);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    public boolean isRegistered(String uuid) {
        if (uuid == null) return false;
        return this.accounts.containsKey(uuid);
    }

    public boolean isAuthenticated(String uuid) {
        if (uuid == null) return false;
        return this.authenticated.contains(uuid);
    }

    public void logout(String uuid) {
        if (uuid == null) return;
        this.authenticated.remove(uuid);
        this.failedAttempts.remove(uuid);
        int expiry = this.plugin.getConfig().getInt("loginsecurity.session-expiry-minutes", 10);
        if (expiry > 0) {
            this.lastLoginMillis.put(uuid, System.currentTimeMillis());
        } else {
            this.lastLoginMillis.remove(uuid);
        }
    }

    public synchronized CompletableFuture<Boolean> register(Player p, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuid = p.getUniqueId().toString();
                if (this.accounts.containsKey(uuid)) return false;
                byte[] salt = new byte[16];
                this.random.nextBytes(salt);
                int iterations = Math.max(50000, this.plugin.getConfig().getInt("loginsecurity.pbkdf2-iterations", 100000));
                byte[] hash = pbkdf2(password.toCharArray(), salt, iterations, 256);
                Account a = new Account();
                a.name = p.getName();
                a.uuid = uuid;
                a.salt = Base64.getEncoder().encodeToString(salt);
                a.hash = Base64.getEncoder().encodeToString(hash);
                a.iterations = iterations;
                a.createdAt = Instant.now().toString();
                // record current ip as last known ip (if available)
                try { a.lastKnownIp = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null; } catch (Exception ignored) { a.lastKnownIp = null; }
                this.accounts.put(uuid, a);
                // persist async
                CompletableFuture.runAsync(this::save);
                // mark authenticated
                this.authenticated.add(uuid);
                this.lastLoginMillis.put(uuid, System.currentTimeMillis());
                // ensure player is unfrozen and informed on main thread
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    try { unfreezePlayer(p); p.sendMessage("\u00a7aRegistration successful. You are now logged in."); } catch (Exception ignored) {}
                });
                this.plugin.getLogger().info("Registered login security for " + p.getName());
                return true;
            } catch (Exception e) {
                this.plugin.getLogger().warning("Error registering: " + e.toString());
                return false;
            }
        });
    }

    public synchronized CompletableFuture<Boolean> login(Player p, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String uuid = p.getUniqueId().toString();
                Account a = this.accounts.get(uuid);
                if (a == null) return false;
                String ip = null;
                try { ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null; } catch (Exception ignored) {}
                // Check account-specific IP block
                if (ip != null && a.blockedIps != null && a.blockedIps.contains(ip)) {
                    // Denied by account owner
                    this.plugin.getLogger().warning("Login denied for " + p.getName() + " from blocked IP: " + ip);
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try { p.kickPlayer("Access denied by account owner."); } catch (Exception ignored) {}
                    });
                    return false;
                }
                // Check per-IP cooldown
                java.util.concurrent.ConcurrentHashMap<String, FailedInfo> map = this.failedAttemptsByIp.computeIfAbsent(uuid, k -> new java.util.concurrent.ConcurrentHashMap<>());
                FailedInfo fi = map.computeIfAbsent(ip != null ? ip : "_unknown", k -> new FailedInfo());
                long now = System.currentTimeMillis();
                if (fi.cooldownEndsAt > now) {
                    long remain = fi.cooldownEndsAt - now;
                    long sec = Math.max(1, remain / 1000L);
                    this.plugin.getLogger().info("Login attempt during cooldown for " + p.getName() + " from IP " + ip + " (wait " + sec + "s)");
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try { p.sendMessage("\u00a7cYou are in cooldown. Please wait " + sec + " seconds before trying again."); } catch (Exception ignored) {}
                    });
                    return false;
                }

                byte[] salt = Base64.getDecoder().decode(a.salt);
                int iters = Math.max(1, a.iterations);
                byte[] computed = pbkdf2(password.toCharArray(), salt, iters, 256);
                String computedB64 = Base64.getEncoder().encodeToString(computed);
                if (computedB64.equals(a.hash)) {
                    this.authenticated.add(uuid);
                    // clear failed info for this IP
                    map.remove(ip != null ? ip : "_unknown");
                    this.lastLoginMillis.put(uuid, System.currentTimeMillis());
                    // If IP changed, update stored lastKnownIp and persist
                    if (ip != null && (a.lastKnownIp == null || !a.lastKnownIp.equals(ip))) {
                        a.lastKnownIp = ip;
                        CompletableFuture.runAsync(this::save);
                    }
                    this.plugin.getLogger().info("Player logged in: " + p.getName());
                    // ensure player is unfrozen on main thread (no duplicate message here)
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try { unfreezePlayer(p); } catch (Exception ignored) {}
                    });
                    return true;
                } else {
                    // Track failed attempt for this (account, ip)
                    fi.attempts++;
                    int threshold = Math.max(1, this.plugin.getConfig().getInt("loginsecurity.failed-attempts-before-cooldown", 5));
                    if (fi.attempts >= threshold) {
                        // escalate stage
                        fi.attempts = 0;
                        fi.stage = Math.min(3, fi.stage + 1);
                        if (fi.stage == 1) {
                            fi.cooldownEndsAt = now + this.plugin.getConfig().getInt("loginsecurity.cooldown-first-minutes", 1) * 60L * 1000L;
                            long mins = this.plugin.getConfig().getInt("loginsecurity.cooldown-first-minutes", 1);
                            Bukkit.getScheduler().runTask(this.plugin, () -> {
                                try { p.kickPlayer("Too many failed login attempts. Cooldown: " + mins + " minutes."); } catch (Exception ignored) {}
                            });
                        } else if (fi.stage == 2) {
                            fi.cooldownEndsAt = now + this.plugin.getConfig().getInt("loginsecurity.cooldown-second-minutes", 10) * 60L * 1000L;
                            long mins = this.plugin.getConfig().getInt("loginsecurity.cooldown-second-minutes", 10);
                            Bukkit.getScheduler().runTask(this.plugin, () -> {
                                try { p.kickPlayer("Too many failed login attempts. Cooldown: " + mins + " minutes."); } catch (Exception ignored) {}
                            });
                        } else {
                            // Stage 3: escalate to Discord verification if linked
                            if (a.discordUserId != null && !a.discordUserId.isEmpty()) {
                                // send security alert to discord
                                try {
                                    if (this.plugin.getDiscordBot() != null) this.plugin.getDiscordBot().sendSecurityAlert(a.discordUserId, a.uuid, p.getName(), ip);
                                } catch (Exception ignored) {}
                                Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    try { p.kickPlayer("Too many failed login attempts. Account owner has been notified via Discord."); } catch (Exception ignored) {}
                                });
                            } else {
                                // No discord linked; block this IP for the account
                                if (ip != null) {
                                    if (a.blockedIps == null) a.blockedIps = new java.util.ArrayList<>();
                                    if (!a.blockedIps.contains(ip)) a.blockedIps.add(ip);
                                    CompletableFuture.runAsync(this::save);
                                }
                                Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    try { p.kickPlayer("Too many failed login attempts. This IP has been blocked for the account."); } catch (Exception ignored) {}
                                });
                            }
                        }
                    } else {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            try { p.sendMessage("\u00a7cLogin failed. Check your password."); } catch (Exception ignored) {}
                        });
                    }
                    // Put back updated fi
                    map.put(ip != null ? ip : "_unknown", fi);
                    return false;
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Error validating login: " + e.toString());
                return false;
            }
        });
    }

    public synchronized boolean resetAccountByName(String name) {
        if (name == null) return false;
        String target = null;
        for (Account a : this.accounts.values()) {
            if (a.name != null && a.name.equalsIgnoreCase(name)) {
                target = a.uuid;
                break;
            }
        }
        if (target == null) return false;
        this.accounts.remove(target);
        CompletableFuture.runAsync(this::save);
        this.authenticated.remove(target);
        this.failedAttempts.remove(target);
        this.lastLoginMillis.remove(target);
        this.failedAttemptsByIp.remove(target);
        this.plugin.getLogger().info("Reset loginsecurity account for " + name);
        return true;
    }

    public synchronized boolean linkDiscordAccount(String playerUUID, String discordUserId) {
        if (playerUUID == null || discordUserId == null) return false;
        Account a = this.accounts.get(playerUUID);
        if (a == null) return false;
        a.discordUserId = discordUserId;
        CompletableFuture.runAsync(this::save);
        this.plugin.getLogger().info("Linked Discord " + discordUserId + " to account " + playerUUID);
        return true;
    }

    public boolean verifyPasswordForUuid(String uuid, String password) {
        try {
            if (uuid == null || password == null) return false;
            Account a = this.accounts.get(uuid);
            if (a == null) return false;
            byte[] salt = Base64.getDecoder().decode(a.salt);
            int iters = Math.max(1, a.iterations);
            byte[] computed = pbkdf2(password.toCharArray(), salt, iters, 256);
            String computedB64 = Base64.getEncoder().encodeToString(computed);
            return computedB64.equals(a.hash);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error verifying password for uuid " + uuid + ": " + e.toString());
            return false;
        }
    }

    public synchronized void allowIpForAccount(String uuid, String ip) {
        if (uuid == null || ip == null) return;
        Account a = this.accounts.get(uuid);
        if (a == null) return;
        a.lastKnownIp = ip;
        if (a.blockedIps != null) a.blockedIps.remove(ip);
        this.failedAttemptsByIp.remove(uuid);
        CompletableFuture.runAsync(this::save);
        this.plugin.getLogger().info("Allowed IP " + ip + " for account " + uuid);
    }

    public synchronized void denyIpForAccount(String uuid, String ip) {
        if (uuid == null || ip == null) return;
        Account a = this.accounts.get(uuid);
        if (a == null) return;
        if (a.blockedIps == null) a.blockedIps = new java.util.ArrayList<>();
        if (!a.blockedIps.contains(ip)) a.blockedIps.add(ip);
        java.util.concurrent.ConcurrentHashMap<String, FailedInfo> map = this.failedAttemptsByIp.get(uuid);
        if (map != null) map.remove(ip);
        CompletableFuture.runAsync(this::save);
        this.plugin.getLogger().info("Denied IP " + ip + " for account " + uuid);
    }

    // Bedrock XUID persistence helpers
    public synchronized void addBedrockXuidForAccount(String uuid, String xuid) {
        if (uuid == null || xuid == null) return;
        Account a = this.accounts.get(uuid);
        if (a == null) return;
        if (a.bedrockXuids == null) a.bedrockXuids = new java.util.ArrayList<>();
        if (!a.bedrockXuids.contains(xuid)) {
            a.bedrockXuids.add(xuid);
            CompletableFuture.runAsync(this::save);
            this.plugin.getLogger().info("Linked Bedrock XUID " + xuid + " to account " + uuid);
        }
    }

    public synchronized void removeBedrockXuidForAccount(String uuid, String xuid) {
        if (uuid == null || xuid == null) return;
        Account a = this.accounts.get(uuid);
        if (a == null || a.bedrockXuids == null) return;
        if (a.bedrockXuids.remove(xuid)) {
            CompletableFuture.runAsync(this::save);
            this.plugin.getLogger().info("Removed Bedrock XUID " + xuid + " from account " + uuid);
        }
    }

    public synchronized java.util.List<String> getBedrockXuidsForAccount(String uuid) {
        if (uuid == null) return java.util.Collections.emptyList();
        Account a = this.accounts.get(uuid);
        if (a == null || a.bedrockXuids == null) return java.util.Collections.emptyList();
        return new java.util.ArrayList<>(a.bedrockXuids);
    }

    public void checkOnJoin(Player p) {
        if (!this.plugin.getConfig().getBoolean("loginsecurity.enabled", false)) return;
        // If player is a Bedrock/Floodgate player, skip password-based login prompts (handled via OTP flow)
        try {
            String bedrockXuid = this.plugin.getVerificationManager() != null ? this.plugin.getVerificationManager().getFloodgateXuid(p) : null;
            if (bedrockXuid != null) return;
        } catch (Exception ignored) {}
        String uuid = p.getUniqueId().toString();
        if (!isRegistered(uuid)) {
            // not registered -> freeze and instruct to /register
            p.sendMessage("\u00a7eYou must register to use this server. Use: \u00a7a/register <password> <password>\u00a7e (messages only visible to you)");
            freezePlayer(p);
            return;
        }
        if (!isAuthenticated(uuid)) {
            // Auto-login when joining from last known IP only when session is still valid
            try {
                Account a = this.accounts.get(uuid);
                String currentIp = null;
                try { currentIp = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null; } catch (Exception ignored) {}
                int expiry = this.plugin.getConfig().getInt("loginsecurity.session-expiry-minutes", 10);
                Long last = this.lastLoginMillis.get(uuid);
                boolean sessionValid = expiry > 0 && last != null && (System.currentTimeMillis() - last.longValue()) <= expiry * 60L * 1000L;
                if (a != null && a.lastKnownIp != null && currentIp != null && a.lastKnownIp.equals(currentIp) && sessionValid) {
                    // Mark as authenticated and restore session
                    this.authenticated.add(uuid);
                    this.lastLoginMillis.put(uuid, System.currentTimeMillis());
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        try { unfreezePlayer(p); p.sendMessage("\u00a7aAuto-logged in from recognized IP."); } catch (Exception ignored) {}
                    });
                    return;
                }
            } catch (Exception ignored) {}

            int expiry = this.plugin.getConfig().getInt("loginsecurity.session-expiry-minutes", 10);
            Long last = this.lastLoginMillis.get(uuid);
            if (expiry > 0 && last != null) {
                long age = System.currentTimeMillis() - last.longValue();
                if (age <= expiry * 60L * 1000L) {
                    // restore session only when IP hasn't changed since last login
                    Account a = this.accounts.get(uuid);
                    String currentIp = null;
                    try { currentIp = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : null; } catch (Exception ignored) {}
                    if (a == null || a.lastKnownIp == null || currentIp == null || a.lastKnownIp.equals(currentIp)) {
                        this.authenticated.add(uuid);
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            try { unfreezePlayer(p); p.sendMessage("\u00a7aSession restored (within " + expiry + "m). You are logged in."); } catch (Exception ignored) {}
                        });
                        return;
                    } else {
                        // IP has changed - require fresh login
                        this.lastLoginMillis.remove(uuid);
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            try { p.sendMessage("\u00a7ePlease login with \u00a7a/login <password>\u00a7e to continue. Messages only visible to you."); freezePlayer(p); } catch (Exception ignored) {}
                        });
                        return;
                    }
                } else {
                    // expired
                    this.lastLoginMillis.remove(uuid);
                }
            }
            p.sendMessage("\u00a7ePlease login with \u00a7a/login <password>\u00a7e to continue. Messages only visible to you.");
            freezePlayer(p);
        }
    }

    private void freezePlayer(Player p) {
        try {
            p.setWalkSpeed(0.0f);
            p.setFlySpeed(0.0f);
            p.setInvulnerable(true);
        } catch (Exception ignored) {}
    }

    public void unfreezePlayer(Player p) {
        try {
            p.setWalkSpeed(0.2f);
            p.setFlySpeed(0.1f);
            p.setInvulnerable(false);
        } catch (Exception ignored) {}
    }

    public void onPlayerQuit(Player p) {
        if (p == null) return;
        String uuid = p.getUniqueId().toString();
        int expiry = this.plugin.getConfig().getInt("loginsecurity.session-expiry-minutes", 10);
        if (expiry > 0) {
            this.lastLoginMillis.put(uuid, System.currentTimeMillis());
        } else {
            this.lastLoginMillis.remove(uuid);
        }
        this.authenticated.remove(uuid);
        this.failedAttempts.remove(uuid);
    }
}
