/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package io.verifyx;

import io.verifyx.DatabaseManager;
import io.verifyx.Main;
import io.verifyx.OTPManager;
import io.verifyx.WorldManager;
import io.verifyx.security.AISecurityManager;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import java.lang.reflect.Method;

public class VerificationManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final OTPManager otpManager;
    private final AISecurityManager aiSecurityManager;
    private WorldManager worldManager;
    private boolean worldManagerInitializing = false;
    private boolean worldManagerReady = false;

    public VerificationManager(Main plugin, DatabaseManager databaseManager, OTPManager otpManager, AISecurityManager aiSecurityManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.otpManager = otpManager;
        this.aiSecurityManager = aiSecurityManager;
        // load cache configuration
        try {
            this.cacheTtlSeconds = Math.max(10, this.plugin.getConfig().getInt("performance.cache-ttl-seconds", 300));
            this.cacheCleanupIntervalMinutes = Math.max(1, this.plugin.getConfig().getInt("performance.cleanup-interval-minutes", 5));
        } catch (Exception e) {
            this.cacheTtlSeconds = 300;
            this.cacheCleanupIntervalMinutes = 5;
        }
        // detect optional integration plugins (can be disabled via config)
        try {
            boolean enableIntegrations = true;
            try {
                enableIntegrations = this.plugin.getConfig().getBoolean("integrations.enable-via-geyser", true);
            } catch (Exception cfgEx) {
                enableIntegrations = true;
            }
            if (enableIntegrations) {
                this.viaPresent = Bukkit.getPluginManager().getPlugin("ViaVersion") != null || Bukkit.getPluginManager().getPlugin("viaversion") != null;
                this.geyserPresent = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null || Bukkit.getPluginManager().getPlugin("Geyser") != null;
            } else {
                this.viaPresent = false;
                this.geyserPresent = false;
            }
            if (this.plugin.getConfig().getBoolean("logging.log-verifications", true)) {
                this.plugin.getLogger().info("[VerifyX] ViaVersion present: " + this.viaPresent + ", Geyser present: " + this.geyserPresent + ", integrations.enabled:" + enableIntegrations);
            }
        } catch (Exception ex) {
            this.viaPresent = false;
            this.geyserPresent = false;
        }

        // init caffeine cache
        int maxSize = Math.max(1000, this.plugin.getConfig().getInt("ai-security.max-cache-size", 10000));
        this.verifiedCache = Caffeine.newBuilder().expireAfterWrite(this.cacheTtlSeconds, TimeUnit.SECONDS).maximumSize(maxSize).build();


    }

    private Cache<String, Boolean> verifiedCache;
    private int cacheTtlSeconds = 300;
    private int cacheCleanupIntervalMinutes = 5;
    private boolean viaPresent = false;
    private boolean geyserPresent = false;
    private final Map<java.util.UUID, Integer> playerProtocolVersion = new ConcurrentHashMap<>();
    private final Map<String, String> bedrockXuidToJavaUuid = new ConcurrentHashMap<>();

    // Normalize player names: removes leading dots/spaces commonly added by Floodgate/Geyser for Bedrock users
    private static final java.util.regex.Pattern LEADING_DOTS_SPACES = java.util.regex.Pattern.compile("^[\\.\\s]+");
    private String normalizePlayerName(String playerName) {
        if (playerName == null) return null;
        return LEADING_DOTS_SPACES.matcher(playerName).replaceFirst("");
    }

    public void handlePlayerJoin(Player player) {
        if (!this.plugin.areManagersInitialized()) {
            String startupMessage = "\u00a7c==================================\n\u00a7c\u26a0 Verification System Starting\n\u00a7cPlease reconnect in 30 seconds\n\u00a7c==================================";
            player.kickPlayer(startupMessage);
            return;
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                String playerIP = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
                boolean dbVerifiedByUUID = false;
                boolean dbVerifiedByName = false;
                boolean isWhitelisted = false;
                boolean isIPVerified = false;
                boolean isUUIDTracked = false;
                boolean cacheVerified = false;

                // Check whitelist - prefer checking the online Player (by UUID), fallback to name lookup
                try {
                    isWhitelisted = player.isWhitelisted();
                    if (!isWhitelisted) {
                        isWhitelisted = this.isWhitelisted(player.getName());
                    }
                } catch (Exception e) {
                    isWhitelisted = false;
                }

                // Check database by UUID and by player name
                // Check in-memory verification cache first
                try {
                    cacheVerified = this.isCachedVerified(player.getUniqueId().toString(), player.getName());
                } catch (Exception e) {
                    cacheVerified = false;
                }
                if (cacheVerified) {
                    // skip DB/IP checks when cached verified
                }
                
                if (this.databaseManager != null) {
                    try {
                        dbVerifiedByUUID = cacheVerified || this.databaseManager.isPlayerVerified(player.getUniqueId().toString()).join();
                    } catch (Exception e) {
                        dbVerifiedByUUID = false;
                    }
                    try {
                        dbVerifiedByName = cacheVerified || this.databaseManager.checkPlayerExists(player.getName()).join();
                    } catch (Exception e) {
                        dbVerifiedByName = false;
                    }
                }

                // Check IP verification (optional)
                if (playerIP != null && this.databaseManager != null) {
                    try {
                        isIPVerified = this.databaseManager.isIPFromVerifiedAccount(playerIP).join();
                    } catch (Exception e) {
                        this.plugin.getLogger().warning("Error checking IP verification: " + e.toString());
                    }
                }

                // Check if UUID is in tracked file (player has tried to join before)
                if (this.plugin.getIPTracker() != null) {
                    isUUIDTracked = this.plugin.getIPTracker().isUUIDTracked(player.getUniqueId().toString());
                }

                // If Geyser/Floodgate is present, try to fetch Bedrock XUID and protocol version
                try {
                    if (this.geyserPresent) {
                        try {
                            String bedrockXuid = getFloodgateXuid(player);
                            if (bedrockXuid != null) {
                                // Concurrent login protection: if another java UUID is mapped to this XUID and still online, reject
                                String existing = this.bedrockXuidToJavaUuid.get(bedrockXuid);
                                if (existing != null && !existing.equals(player.getUniqueId().toString())) {
                                    try {
                                        java.util.UUID ex = java.util.UUID.fromString(existing);
                                        org.bukkit.entity.Player online = org.bukkit.Bukkit.getPlayer(ex);
                                        if (online != null && online.isOnline()) {
                                            player.kickPlayer("This account is already online.");
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                }
                                this.bedrockXuidToJavaUuid.put(bedrockXuid, player.getUniqueId().toString());
                                // also put a cached verified entry if applicable
                                this.verifiedCache.put("x:" + bedrockXuid, Boolean.TRUE);
                            }
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    if (this.viaPresent) {
                        try {
                            Integer proto = getViaProtocolVersion(player);
                            if (proto != null) this.playerProtocolVersion.put(player.getUniqueId(), proto);
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                } catch (Throwable ignored) {}

                final boolean finalDbVerified = dbVerifiedByUUID || dbVerifiedByName;
                final boolean finalCacheVerified = cacheVerified;
                final boolean finalIsNameVerified = dbVerifiedByName;
                final boolean finalIsWhitelisted = isWhitelisted;
                final boolean finalIsIPVerified = isIPVerified;
                final boolean finalIsUUIDTracked = isUUIDTracked;
                
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                    // Debug: log computed verification flags
                    if (this.plugin.getConfig().getBoolean("logging.log-verifications", true)) {
                        this.plugin.getLogger().info("[VerifyX] Verification flags - DB:" + finalDbVerified + " Cache:" + finalCacheVerified + " NameVerified:" + finalIsNameVerified + " Whitelist:" + finalIsWhitelisted + " IPVerified:" + finalIsIPVerified + " UUIDTracked:" + finalIsUUIDTracked);
                    }
                    // Require BOTH a database verification (by UUID or name) AND whitelist to bypass OTP.
                    if (!((finalDbVerified || finalCacheVerified) && finalIsWhitelisted) && !finalIsIPVerified) {
                        // Record this join attempt with IP for later forceverify
                        if (playerIP != null && this.plugin.getIPTracker() != null) {
                            String name = player.getName();
                            String norm = this.normalizePlayerName(name);
                            // Record both the raw and normalized variants so Bedrock/Java name differences are captured
                            this.plugin.getIPTracker().recordPlayerIP(name, playerIP, player.getUniqueId().toString());
                            if (norm != null && !norm.equals(name)) {
                                this.plugin.getIPTracker().recordPlayerIP(norm, playerIP, player.getUniqueId().toString());
                            }
                        }
                        
                                // If strict 1-account-per-IP policy is enabled, block immediately when another account exists on the IP
                                try {
                                    int maxAlts = this.plugin.getConfig().getInt("ai-security.max-alts-per-ip", 2);
                                    if (maxAlts <= 1 && playerIP != null && this.aiSecurityManager != null) {
                                        java.util.Set<String> accountsOnIP = this.aiSecurityManager.getAccountsOnIP(playerIP);
                                        if (accountsOnIP != null && !accountsOnIP.isEmpty()) {
                                            // if another UUID is present on this IP (not just this player's own uuid), treat as alt
                                            boolean conflict = !(accountsOnIP.size() == 1 && accountsOnIP.contains(player.getUniqueId().toString()));
                                            if (conflict) {
                                                this.handleAltAccount(player);
                                                return;
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {}

                                // Check explicit alt detection first, then AI OTP allowance as a fallback
                                if (this.aiSecurityManager != null) {
                                    try {
                                        boolean isAlt = this.aiSecurityManager.isPlayerAlt(player.getName());
                                        // Log the alt detection and current alt_otp policy for debugging
                                        this.plugin.getLogger().info("Verification: isPlayerAlt=" + isAlt + " for player=" + player.getName() + ", alt_otp=" + (this.aiSecurityManager != null ? this.aiSecurityManager.isAltOtpEnforced() : this.plugin.getConfig().getBoolean("ai-security.alt_otp", true)));
                                        if (isAlt) {
                                            // If server configured to allow alts without OTP enforcement, let them join (unverified)
                                            boolean altOtpEnabled = (this.aiSecurityManager != null) ? this.aiSecurityManager.isAltOtpEnforced() : this.plugin.getConfig().getBoolean("ai-security.alt_otp", true);
                                            if (!altOtpEnabled) {
                                                // alt_otp: false means ALLOW alts without OTP (they stay unverified but join)
                                                player.sendMessage("\u00a76\u00a7l⚠ NOTICE\u00a7r\u00a7e: Your account is flagged as an alt account. You are allowed to join but remain \u00a7cunverified\u00a7e.");
                                                // Allow join (skip OTP), update tab to unverified and continue
                                                try {
                                                    if (this.plugin.getVerifyTab() != null) this.plugin.getVerifyTab().updatePlayerTab(player);
                                                } catch (Exception ignored) {}
                                                return;
                                            }
                                            // alt_otp: true means ENFORCE OTP for alts (kick with OTP)
                                            this.handleAltAccount(player);
                                            return;
                                        }
                                    } catch (Exception ignored) {}
                                    boolean canOtp = true;
                                    try {
                                        canOtp = this.aiSecurityManager.canGenerateOTP(player.getName(), player.getUniqueId().toString(), playerIP != null ? playerIP : "unknown");
                                    } catch (Exception ignored) {}
                                    if (!canOtp) {
                                        this.handleAltAccount(player);
                                        return;
                                    }
                                    
                                    // Check if this is a returning alt with alt_otp disabled
                                    // Don't rely on isPlayerAlt() as it may have stale state after canGenerateOTP() call
                                    boolean altOtpEnabled = (this.aiSecurityManager != null) ? this.aiSecurityManager.isAltOtpEnforced() : this.plugin.getConfig().getBoolean("ai-security.alt_otp", true);

                                    if (!altOtpEnabled && this.aiSecurityManager != null && playerIP != null) {
                                        java.util.Set<String> accountsOnIP = this.aiSecurityManager.getAccountsOnIP(playerIP);
                                        int maxAlts = this.plugin.getConfig().getInt("ai-security.max-alts-per-ip", 2);
                                        if (accountsOnIP != null && accountsOnIP.size() >= maxAlts) {
                                            // This IP has reached max accounts and alt_otp is disabled, allow alt to join unverified
                                            player.sendMessage("\u00a76\u00a7l⚠ NOTICE\u00a7r\u00a7e: Your account is flagged as an alt account. You are allowed to join but remain \u00a7cunverified\u00a7e.");
                                            try {
                                                if (this.plugin.getVerifyTab() != null) this.plugin.getVerifyTab().updatePlayerTab(player);
                                            } catch (Exception ignored) {}
                                            return;
                                        }
                                    }
                                }
                        int freezeSecs = Math.max(0, this.plugin.getConfig().getInt("ai-security.otp-wait-freeze-seconds", 2));
                        if (freezeSecs > 0) {
                            try {
                                this.freezePlayer(player, "Waiting for OTP delivery");
                            } catch (Exception ignored) {}
                            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                                try {
                                    String otp = this.otpManager.generateOTP(player.getName(), player.getUniqueId().toString(), playerIP != null ? playerIP : "unknown");
                                    if (otp == null) {
                                        this.handleAltAccount(player);
                                        return;
                                    }
                                    String kickMessage = this.createKickMessage(otp);
                                    player.kickPlayer(kickMessage);
                                    this.plugin.getLogger().info("Kicked " + player.getName() + " with OTP: " + otp);
                                } catch (Exception ex) {
                                    this.plugin.getLogger().warning("Error generating OTP after freeze: " + ex.toString());
                                } finally {
                                    try { this.unfreezePlayer(player.getUniqueId().toString()); } catch (Exception ignored) {}
                                }
                            }, (long)freezeSecs * 20L);
                        } else {
                            String otp = this.otpManager.generateOTP(player.getName(), player.getUniqueId().toString(), playerIP != null ? playerIP : "unknown");
                            if (otp == null) {
                                this.handleAltAccount(player);
                                return;
                            }
                            String kickMessage = this.createKickMessage(otp);
                            player.kickPlayer(kickMessage);
                            this.plugin.getLogger().info("Kicked " + player.getName() + " with OTP: " + otp);
                        }
                    } else {
                        // If player is database-verified, check for IP change and use Discord DM confirmation flow on cracked servers
                        String reason;
                        if (finalDbVerified) {
                            if (finalIsWhitelisted) reason = "Whitelisted";
                            else if (finalIsNameVerified) reason = "Name verified";
                            else reason = "UUID verified";
                        } else if (finalIsIPVerified) {
                            reason = "IP verified (alt account)";
                        } else {
                            reason = "Unknown";
                        }
                        player.sendMessage("\u00a7a\u2705 Welcome back, " + player.getName() + "!");
                        this.plugin.getLogger().info("\u2705 Verified player joined: " + player.getName() + " (" + reason + ")");
                        // Update last_seen immediately so Discord /status shows correct online state
                        try {
                            if (this.databaseManager != null) this.databaseManager.updateLastSeen(player.getUniqueId().toString());
                        } catch (Exception ignored) {}
                        WorldManager wm = this.getWorldManager();
                        if (wm != null && wm.isWorldReady()) {
                            wm.teleportToMainWorld(player);
                        }
                        // Update tab display for verified player
                        try {
                            if (this.plugin.getVerifyTab() != null) {
                                this.plugin.getVerifyTab().updatePlayerTab(player);
                            }
                        } catch (Exception e) {
                            this.plugin.getLogger().warning("Error updating verify tab for " + player.getName() + ": " + e.toString());
                        }
                        // If login security is enabled, ensure verified players are also prompted to register/login
                        try {
                            if (this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && this.plugin.getLoginSecurityManager() != null) {
                                this.plugin.getLoginSecurityManager().checkOnJoin(player);
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Error handling player join for " + player.getName() + ": " + e.toString());
                String errorMessage = "\u00a7c==================================\n\u00a7c\u274c Verification Error\n\u00a7cPlease try again later\n\u00a7c==================================";
                player.kickPlayer(errorMessage);
            }
        });
    }

    // Keep frozenPlayers set for OTP freeze usage and login security enforcement
    private final java.util.Set<String> frozenPlayers = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    private void freezePlayer(org.bukkit.entity.Player player, String reason) {
        if (player == null) return;
        // Ensure main-thread execution when touching Bukkit APIs
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            org.bukkit.entity.Player p = player;
            org.bukkit.plugin.Plugin pl = (org.bukkit.plugin.Plugin)this.plugin;
            org.bukkit.Bukkit.getScheduler().runTask(pl, () -> freezePlayer(p, reason));
            return;
        }

        String uuid = player.getUniqueId().toString();
        this.frozenPlayers.add(uuid);
        try {
            player.sendMessage(org.bukkit.ChatColor.YELLOW + "Your session is pending verification or login. Please follow the instructions.");
        } catch (Exception ignored) {}
        try {
            // Restrict movement
            player.setWalkSpeed(0.0f);
            player.setFlySpeed(0.0f);
        } catch (Exception ignored) {}
        this.plugin.getLogger().info("Frozen player " + player.getName() + " pending verification/login: " + reason);
    }

    private void unfreezePlayer(String playerUUID) {
        if (playerUUID == null) return;
        // Ensure main-thread execution when touching Bukkit APIs
        if (!org.bukkit.Bukkit.isPrimaryThread()) {
            String uuid = playerUUID;
            org.bukkit.plugin.Plugin pl = (org.bukkit.plugin.Plugin)this.plugin;
            org.bukkit.Bukkit.getScheduler().runTask(pl, () -> unfreezePlayer(uuid));
            return;
        }

        this.frozenPlayers.remove(playerUUID);
        try {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(playerUUID));
            if (player != null && player.isOnline()) {
                try { player.setWalkSpeed(0.2f); } catch (Exception ignored) {}
                try { player.setFlySpeed(0.1f); } catch (Exception ignored) {}
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Verification/login complete. You may now play.");
            }
        } catch (Exception ignored) {}
        this.plugin.getLogger().info("Unfrozen player UUID: " + playerUUID);
    }

    public boolean isPlayerFrozen(String playerUUID) {
        if (playerUUID == null) return false;
        return this.frozenPlayers.contains(playerUUID);
    }

    public void cancelPendingForUUID(String playerUUID) {
        if (playerUUID == null) return;
        this.frozenPlayers.remove(playerUUID);
    }

    private void handleAltAccount(Player player) {
        String altMessage = String.valueOf(ChatColor.RED) + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n" + String.valueOf(ChatColor.RED) + "\u26a0\ufe0f  ALT ACCOUNT DETECTED!\n" + String.valueOf(ChatColor.YELLOW) + "Our security system has flagged this as an alt account.\n" + String.valueOf(ChatColor.YELLOW) + "Please join with your main account.\n" + String.valueOf(ChatColor.RED) + "OTP verification is not available for alt accounts.\n" + String.valueOf(ChatColor.RED) + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550";
        player.kickPlayer(altMessage);
        this.plugin.getLogger().warning("\ud83d\udeab Alt account blocked: " + player.getName());
    }
    

    private String createKickMessage(String otp) {
        // Compact first line (always visible on narrow clients) + a wrapped full message for Java clients
        int expiry = this.plugin.getConfigData() != null ? this.plugin.getConfigData().getOtpExpiryMinutes() : 10;
        int maxAttempts = this.plugin.getConfigData() != null ? this.plugin.getConfigData().getMaxOtpAttempts() : 3;
        String compact = "\u00a7c[VERIFICATION REQUIRED] \u00a7e" + otp + " \u00a7c(exp " + expiry + "m)";

        // Build longer message paragraphs and wrap them to a safe width for Bedrock
        final int maxWidth = 48;
        String separator = new String(new char[maxWidth]).replace('\0', '=');
        java.util.List<String> paragraphs = new java.util.ArrayList<>();
        paragraphs.add("\u00a7c" + separator);
        paragraphs.add("\u00a7c\u26a0 VERIFICATION REQUIRED");
        paragraphs.add("\u00a7cJoin our Discord server - #verification");
        paragraphs.add("\u00a7cClick 'Start Verification' button");
        paragraphs.add("\u00a7cEnter this OTP code:");
        paragraphs.add("\u00a7e" + otp);
        paragraphs.add("\u00a7c");
        paragraphs.add("\u00a7cCode expires in " + expiry + " minutes");
        paragraphs.add("\u00a7cMax " + maxAttempts + " attempts allowed");
        paragraphs.add("\u00a7c" + separator);

        StringBuilder full = new StringBuilder();
        for (String para : paragraphs) {
            java.util.List<String> wrapped = wrapPreservingColor(para, maxWidth);
            for (String w : wrapped) {
                if (full.length() > 0) full.append('\n');
                full.append(w);
            }
        }

        return compact + "\n" + full.toString();
    }

    // Wrap a string into lines <= maxWidth, preserving Minecraft color codes (\u00a7x)
    private static java.util.List<String> wrapPreservingColor(String text, int maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (text == null) return out;
        text = text.replaceAll("\t", "    ");
        String[] tokens = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            int tokenLen = visibleLength(token);
            int lineLen = visibleLength(line.toString());
            if (lineLen + (lineLen > 0 ? 1 : 0) + tokenLen > maxWidth && lineLen > 0) {
                out.add(line.toString());
                String carry = lastColorCodes(line.toString());
                line = new StringBuilder(carry + token);
            } else {
                if (lineLen > 0) line.append(' ');
                line.append(token);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.isEmpty()) out.add(text);
        return out;
    }

    private static int visibleLength(String s) {
        if (s == null) return 0;
        int len = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00a7') {
                i++; // skip color code
                continue;
            }
            len++;
        }
        return len;
    }

    private static String lastColorCodes(String s) {
        if (s == null) return "";
        StringBuilder codes = new StringBuilder();
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '\u00a7') {
                char code = s.charAt(i + 1);
                codes.setLength(0);
                codes.append('\u00a7').append(code);
                i++;
            }
        }
        return codes.toString();
    }

    public WorldManager getWorldManager() {
        if (this.worldManager == null && !this.worldManagerInitializing) {
            this.worldManagerInitializing = true;
            this.plugin.getLogger().info("\ud83d\ude80 Initializing WorldManager...");
            new BukkitRunnable(){

                public void run() {
                    try {
                        VerificationManager.this.worldManager = new WorldManager(VerificationManager.this.plugin);
                        VerificationManager.this.worldManagerReady = true;
                        VerificationManager.this.plugin.getLogger().info("\u2705 WorldManager initialized successfully");
                    }
                    catch (Exception e) {
                        VerificationManager.this.plugin.getLogger().severe("\u274c Failed to initialize WorldManager: " + e.toString());
                        e.printStackTrace();
                    }
                    finally {
                        VerificationManager.this.worldManagerInitializing = false;
                    }
                }
            }.runTask((Plugin)this.plugin);
        }
        return this.worldManager;
    }

    public boolean isWorldManagerReady() {
        return this.worldManager != null && this.worldManagerReady;
    }


    public boolean isPlayerVerified(Player player) {
        try {
            if (this.databaseManager == null) {
                this.plugin.getLogger().warning("DatabaseManager is null, cannot check verification for: " + player.getName());
                return false;
            }
            return this.databaseManager.isPlayerVerified(player.getUniqueId().toString()).join();
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error checking verification for " + player.getName() + ": " + e.toString());
            return false;
        }
    }

    public CompletableFuture<Boolean> verifyDiscordOTP(String otp, String discordId, String discordName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.otpManager.verifyOTP(otp, discordId);
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Error in Discord OTP verification: " + e.toString());
                return false;
            }
        });
    }

    public void addToWhitelist(String playerName, String playerUUID) {
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            this.plugin.getLogger().info("\u2699\ufe0f Adding " + playerName + " to whitelist...");
                OfflinePlayer offlinePlayer = null;
                try {
                    try {
                        offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(playerUUID));
                    } catch (IllegalArgumentException e) {
                        offlinePlayer = Bukkit.getOfflinePlayer((String)playerName);
                    }
                    this.plugin.getLogger().info("\u2699 Adding to whitelist -> resolved offline player UUID: " + (offlinePlayer != null ? offlinePlayer.getUniqueId().toString() : "null") );
                    if (offlinePlayer != null) {
                        offlinePlayer.setWhitelisted(true);
                    }
                    try {
                        Bukkit.reloadWhitelist();
                        this.plugin.getLogger().info("\u2705 Added " + playerName + " to whitelist (reload done)");
                    } catch (Exception ex) {
                        this.plugin.getLogger().warning("Failed to reload whitelist synchronously: " + ex.getMessage());
                    }
                    // Ensure server-level whitelist mapping by dispatching console command
                    try {
                        Bukkit.dispatchCommand((org.bukkit.command.CommandSender)Bukkit.getConsoleSender(), (String)("whitelist add " + playerName));
                        Bukkit.dispatchCommand((org.bukkit.command.CommandSender)Bukkit.getConsoleSender(), (String)"whitelist reload");
                        this.plugin.getLogger().info("\u2705 Ensured server-level whitelist entry for " + playerName);
                    } catch (Exception ex2) {
                        this.plugin.getLogger().warning("Failed to dispatch whitelist command fallback: " + ex2.getMessage());
                    }
                    // Diagnostic: list whitelisted players count and whether contains UUID
                    try {
                        int count = Bukkit.getWhitelistedPlayers().size();
                        boolean contains = false;
                        if (offlinePlayer != null) {
                            for (org.bukkit.OfflinePlayer op : Bukkit.getWhitelistedPlayers()) {
                                if (op != null && op.getUniqueId() != null && op.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                                    contains = true;
                                    break;
                                }
                            }
                        }
                        this.plugin.getLogger().info("[VerifyX] Whitelist state -> total:" + count + " containsPlayerUUID:" + contains);
                        // cache verification immediately to avoid DB race on join
                        try {
                            VerificationManager.this.cacheVerified(playerUUID, playerName);
                            // also cache Bedrock XUID mapping if available
                            try {
                                String xuid = getFloodgateXuid(offlinePlayer != null ? Bukkit.getPlayer(offlinePlayer.getUniqueId()) : null);
                                if (xuid != null) {
                                    VerificationManager.this.verifiedCache.put("x:" + xuid, Boolean.TRUE);
                                    VerificationManager.this.bedrockXuidToJavaUuid.put(xuid, playerUUID);
                                }
                            } catch (Throwable ignored) {
                            }
                        } catch (Exception ex4) {
                            this.plugin.getLogger().warning("Failed to cache verification for " + playerName + ": " + ex4.getMessage());
                        }
                            // Update the tab display for the player if online
                            try {
                                Player online = Bukkit.getPlayer((String)playerName);
                                if (online != null && online.isOnline() && this.plugin.getVerifyTab() != null) {
                                    try { this.plugin.getVerifyTab().setPlayerVerified(online); } catch (Throwable ignored) {}
                                }
                            } catch (Exception ignored) {}
                    } catch (Exception ex3) {
                        this.plugin.getLogger().warning("Error checking whitelist state: " + ex3.getMessage());
                    }
            }
            catch (Exception e) {
                this.plugin.getLogger().severe("\u274c Error adding to whitelist: " + e.toString());
                try {
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("whitelist add " + playerName));
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"whitelist reload");
                    this.plugin.getLogger().info("\u2705 Used fallback method to whitelist " + playerName);
                }
                catch (Exception ex) {
                    this.plugin.getLogger().severe("\u274c Fallback whitelist method also failed: " + ex.getMessage());
                }
            }
        });
    }

    public void removeFromWhitelist(String playerName) {
        removeFromWhitelist(playerName, null);
    }

    public void removeFromWhitelist(String playerName, String playerUUID) {
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            this.plugin.getLogger().info("\u2699\ufe0f Removing " + playerName + " from whitelist...");
            try {
                String norm = this.normalizePlayerName(playerName);
                java.util.List<String> namesToRemove = new java.util.ArrayList<>();
                namesToRemove.add(playerName);
                if (norm != null && !norm.equals(playerName)) namesToRemove.add(norm);

                for (String name : namesToRemove) {
                    try {
                        OfflinePlayer player = Bukkit.getOfflinePlayer((String)name);
                        if (player != null) {
                            player.setWhitelisted(false);
                            try {
                                Bukkit.reloadWhitelist();
                                this.plugin.getLogger().info("\u2705 Removed " + name + " from whitelist");
                            } catch (Exception ex) {
                                this.plugin.getLogger().warning("Failed to reload whitelist synchronously: " + ex.getMessage());
                            }
                            // Ensure server-level whitelist file is updated by dispatching console commands
                            try {
                                Bukkit.dispatchCommand((org.bukkit.command.CommandSender)Bukkit.getConsoleSender(), (String)("whitelist remove " + name));
                                Bukkit.dispatchCommand((org.bukkit.command.CommandSender)Bukkit.getConsoleSender(), (String)"whitelist reload");
                                this.plugin.getLogger().info("\u2705 Dispatched whitelist remove for " + name);
                            } catch (Exception ex2) {
                                this.plugin.getLogger().warning("Failed to dispatch whitelist remove fallback: " + ex2.getMessage());
                            }
                            Player onlinePlayer = Bukkit.getPlayer((String)name);
                            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                String kickMessage = "\u00a7c==================================\n\u00a7c\u274c You have been unverified\n\u00a7c\n\u00a7cYour verification has been removed\n\u00a7cPlease re-verify in Discord\n\u00a7c\n\u00a7cJoin #verification channel\n\u00a7c==================================";
                                onlinePlayer.kickPlayer(kickMessage);
                                this.plugin.getLogger().info("\u2705 Kicked online player " + name);
                            }
                        } else {
                            this.plugin.getLogger().warning("Player " + name + " not found for whitelist removal");
                            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("whitelist remove " + name));
                            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"whitelist reload");
                        }
                    } catch (Exception inner) {
                        this.plugin.getLogger().warning("Error handling whitelist removal for " + name + ": " + inner.toString());
                    }
                }

                // Remove IP tracker entries for both raw and normalized names
                try {
                    if (this.plugin.getIPTracker() != null) {
                        this.plugin.getIPTracker().removeEntriesByPlayerName(playerName);
                        if (norm != null && !norm.equals(playerName)) this.plugin.getIPTracker().removeEntriesByPlayerName(norm);
                        if (playerUUID != null && !playerUUID.isEmpty()) this.plugin.getIPTracker().removeEntriesByUUID(playerUUID);
                        this.plugin.getLogger().info("\u2705 Cleared IP tracker entries for " + playerName + (playerUUID != null ? (" (UUID: " + playerUUID + ")") : ""));
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to clear IP tracker entries: " + e.toString());
                }

                // Invalidate caches
                try {
                    this.invalidateCachedVerification(playerUUID, playerName);
                } catch (Exception ignored) {}

            }
            catch (Exception e) {
                this.plugin.getLogger().severe("\u274c Error removing from whitelist: " + e.toString());
                try {
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("whitelist remove " + playerName));
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"whitelist reload");
                    this.plugin.getLogger().info("\u2705 Used fallback whitelist removal for " + playerName);
                    Player onlinePlayer = Bukkit.getPlayer((String)playerName);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        String kickMessage = "\u00a7c==================================\n\u00a7c\u274c You have been unverified\n\u00a7c\n\u00a7cYour verification has been removed\n\u00a7cPlease re-verify in Discord\n\u00a7c==================================";
                        onlinePlayer.kickPlayer(kickMessage);
                    }
                }
                catch (Exception ex) {
                    this.plugin.getLogger().severe("\u274c Fallback whitelist removal also failed: " + ex.getMessage());
                }
            }
        });
    }

    public boolean isWhitelisted(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer((String)playerName);
        return player != null && player.isWhitelisted();
    }

    public int getWhitelistCount() {
        return Bukkit.getWhitelistedPlayers().size();
    }

    public void whitelistPlayer(String playerName) {
        this.addToWhitelist(playerName, "");
    }

    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.plugin.getLogger().info("\u2705 Configuration reloaded");
    }

    public void cacheVerified(String playerUUID, String playerName) {
        if (playerUUID != null && !playerUUID.isEmpty()) {
            this.verifiedCache.put("u:" + playerUUID, Boolean.TRUE);
        }
        if (playerName != null && !playerName.isEmpty()) {
            this.verifiedCache.put("n:" + playerName.toLowerCase(), Boolean.TRUE);
        }
        if (this.plugin.getConfig().getBoolean("logging.log-verifications", true)) {
            this.plugin.getLogger().info("[VerifyX] Cached verification for " + playerName + " (ttl=" + this.cacheTtlSeconds + "s)");
        }
        try {
            if (playerUUID != null && !playerUUID.isEmpty()) {
                for (Map.Entry<String, String> e : this.bedrockXuidToJavaUuid.entrySet()) {
                    if (playerUUID.equals(e.getValue())) {
                        this.verifiedCache.put("x:" + e.getKey(), Boolean.TRUE);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void invalidateCachedVerification(String playerUUID, String playerName) {
        try {
            if (playerUUID != null && !playerUUID.isEmpty()) {
                this.verifiedCache.invalidate("u:" + playerUUID);
            }
            if (playerName != null && !playerName.isEmpty()) {
                this.verifiedCache.invalidate("n:" + playerName.toLowerCase());
            }
            // remove any bedrock mapping entries for this UUID
            try {
                java.util.Iterator<java.util.Map.Entry<String, String>> it = this.bedrockXuidToJavaUuid.entrySet().iterator();
                while (it.hasNext()) {
                    java.util.Map.Entry<String, String> e = it.next();
                    if (playerUUID != null && playerUUID.equals(e.getValue())) {
                        this.verifiedCache.invalidate("x:" + e.getKey());
                        it.remove();
                    }
                }
            } catch (Throwable ignored) {}
            if (this.plugin.getConfig().getBoolean("logging.log-verifications", true)) {
                this.plugin.getLogger().info("[VerifyX] Invalidated cached verification for " + playerName + " (" + playerUUID + ")");
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to invalidate cached verification for " + playerName + ": " + e.toString());
        }
    }

    // Public helper to cache a Bedrock XUID mapping to a Java UUID
    public void cacheBedrockXuid(String xuid, String playerUUID) {
        if (xuid == null || xuid.isEmpty() || playerUUID == null || playerUUID.isEmpty()) return;
        try {
            this.bedrockXuidToJavaUuid.put(xuid, playerUUID);
            this.verifiedCache.put("x:" + xuid, Boolean.TRUE);
            if (this.plugin.getConfig().getBoolean("logging.log-verifications", true)) {
                this.plugin.getLogger().info("[VerifyX] Cached Bedrock XUID mapping: " + xuid + " -> " + playerUUID);
            }
            // Persist mapping into loginsecurity account so it survives restarts
            try {
                if (this.plugin.getLoginSecurityManager() != null) {
                    this.plugin.getLoginSecurityManager().addBedrockXuidForAccount(playerUUID, xuid);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    // Return list of XUIDs currently mapped to the given Java UUID (may be empty)
    public java.util.List<String> getBedrockXuidsForJavaUuid(String javaUuid) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (javaUuid == null || javaUuid.isEmpty()) return out;
        try {
            for (Map.Entry<String, String> e : this.bedrockXuidToJavaUuid.entrySet()) {
                if (javaUuid.equals(e.getValue())) out.add(e.getKey());
            }
        } catch (Exception ignored) {}
        return out;
    }

    

    public boolean isCachedVerified(String playerUUID, String playerName) {
        if (playerUUID != null && !playerUUID.isEmpty()) {
            String key = "u:" + playerUUID;
            Boolean v = this.verifiedCache.getIfPresent(key);
            if (v != null && v) return true;
        }
        if (playerName != null && !playerName.isEmpty()) {
            String key2 = "n:" + playerName.toLowerCase();
            Boolean v2 = this.verifiedCache.getIfPresent(key2);
            if (v2 != null && v2) return true;
        }
        // also check bedrock xuid key
        try {
            if (playerUUID != null && !playerUUID.isEmpty()) {
                for (Map.Entry<String, String> e : this.bedrockXuidToJavaUuid.entrySet()) {
                    if (playerUUID.equals(e.getValue())) {
                        Boolean xv = this.verifiedCache.getIfPresent("x:" + e.getKey());
                        if (xv != null && xv) return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public String getFloodgateXuid(Player player) {
        if (player == null) return null;
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = floodgateApiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            Method isFg = floodgateApiClass.getMethod("isFloodgatePlayer", java.util.UUID.class);
            Boolean is = (Boolean)isFg.invoke(api, player.getUniqueId());
            if (!is) return null;
            Method getPlayer = floodgateApiClass.getMethod("getPlayer", java.util.UUID.class);
            Object fgPlayer = getPlayer.invoke(api, player.getUniqueId());
            if (fgPlayer == null) return null;
            try {
                Method getXuid = fgPlayer.getClass().getMethod("getXuid");
                Object x = getXuid.invoke(fgPlayer);
                if (x != null) return x.toString();
            } catch (NoSuchMethodException ignored) {}
            try {
                Method getXboxUuid = fgPlayer.getClass().getMethod("getXboxUUID");
                Object x = getXboxUuid.invoke(fgPlayer);
                if (x != null) return x.toString();
            } catch (NoSuchMethodException ignored) {}
            try {
                Method getXboxUniqueId = fgPlayer.getClass().getMethod("getXboxUniqueId");
                Object x = getXboxUniqueId.invoke(fgPlayer);
                if (x != null) return x.toString();
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }

    private Integer getViaProtocolVersion(Player player) {
        if (player == null) return null;
        try {
            Class<?> viaClass = Class.forName("us.myles.ViaVersion.api.Via");
            Method getAPI = viaClass.getMethod("getAPI");
            Object api = getAPI.invoke(null);
            Method getPlayerVersion = api.getClass().getMethod("getPlayerVersion", java.util.UUID.class);
            Object res = getPlayerVersion.invoke(api, player.getUniqueId());
            if (res instanceof Integer) return (Integer)res;
        } catch (Throwable ignored) {}
        try {
            Class<?> viaApiClass = Class.forName("us.myles.ViaVersion.api.ViaAPI");
            Method getInstance = viaApiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            Method getPlayerVersion = api.getClass().getMethod("getPlayerVersion", java.util.UUID.class);
            Object res = getPlayerVersion.invoke(api, player.getUniqueId());
            if (res instanceof Integer) return (Integer)res;
        } catch (Throwable ignored) {}
        return null;
    }
}

