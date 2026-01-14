/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package io.verifyx;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.verifyx.DatabaseManager;
import io.verifyx.Main;
import io.verifyx.models.OTPData;
import io.verifyx.models.VerificationResult;
import io.verifyx.security.AISecurityManager;
import io.verifyx.utils.RateLimiter;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class OTPManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Cache<String, OTPData> otpCache;
    private final RateLimiter rateLimiter;
    private final Random random;
    private AISecurityManager aiSecurity;
    private int otpLength;
    private String otpCharset;
    private int otpExpiryMinutes;

    public OTPManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.random = new Random();
        this.otpLength = plugin.getConfigData().getOtpLength();
        this.otpCharset = plugin.getConfigData().getOtpCharset();
        this.otpExpiryMinutes = plugin.getConfigData().getOtpExpiryMinutes();
        if (this.otpCharset == null || this.otpCharset.isEmpty()) {
            this.otpCharset = "0123456789";
        }
        // Allow flexible OTP lengths (1-10) so server owners can choose short or long codes
        if (this.otpLength < 1 || this.otpLength > 10) {
            this.otpLength = 6;
        }
        this.otpCache = Caffeine.newBuilder().expireAfterWrite(this.otpExpiryMinutes, TimeUnit.MINUTES).maximumSize(1000L).build();
        this.rateLimiter = new RateLimiter();
        this.plugin.getLogger().info("\ud83d\udd10 OTP Manager initialized:");
        this.plugin.getLogger().info("   Length: " + this.otpLength + " characters");
        this.plugin.getLogger().info("   Charset: " + this.otpCharset);
        this.plugin.getLogger().info("   Expiry: " + this.otpExpiryMinutes + " minutes");
    }

    public void setAISecurityManager(AISecurityManager aiSecurity) {
        this.aiSecurity = aiSecurity;
    }

    public String generateOTP(String playerName, String playerUUID, String ipAddress) {
        String otp;
        boolean exists;
        if (this.aiSecurity != null && !this.aiSecurity.canGenerateOTP(playerName, playerUUID, ipAddress)) {
            this.plugin.getLogger().warning("\ud83d\udeab OTP generation blocked for " + playerName + " - Alt account detected");
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                final Player player = Bukkit.getPlayerExact((String)playerName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
                    player.sendMessage(String.valueOf(ChatColor.RED) + "\u26a0\ufe0f  ALT ACCOUNT DETECTED!");
                    player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Our security system has detected this as an alt account.");
                    player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Please join with your main account.");
                    player.sendMessage(String.valueOf(ChatColor.RED) + "OTP verification is not available for alt accounts.");
                    player.sendMessage(String.valueOf(ChatColor.RED) + "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
                    new BukkitRunnable(){

                        public void run() {
                            if (player.isOnline()) {
                                player.kickPlayer(String.valueOf(ChatColor.RED) + "Alt accounts are not allowed. Please join with your main account.");
                            }
                        }
                    }.runTaskLater((Plugin)this.plugin, 60L);
                }
            });
            return null;
        }
        do {
            otp = this.generateRandomOTP();
            exists = this.otpCache.asMap().containsKey(otp);
            if (exists) continue;
            exists = this.databaseManager.otpExists(otp);
        } while (exists);
        Instant expiresAt = Instant.now().plusSeconds((long)this.otpExpiryMinutes * 60L);
        OTPData otpData = new OTPData(otp, playerName, playerUUID, ipAddress, expiresAt);
        this.otpCache.put(otp, otpData);
        this.databaseManager.saveOTPAsync(otpData);
        this.plugin.getLogger().info("\ud83d\udd10 Generated OTP " + otp + " for " + playerName);
        return otp;
    }

    private String generateRandomOTP() {
        StringBuilder otp = new StringBuilder();
        int i = 0;
        while (i < this.otpLength) {
            int index = this.random.nextInt(this.otpCharset.length());
            otp.append(this.otpCharset.charAt(index));
            ++i;
        }
        return otp.toString();
    }

    public VerificationResult verifyOTP(String otp, String discordId, String discordName) {
        String playerName;
        if ((otp = otp.trim()).length() != this.otpLength) {
            return new VerificationResult(false, "\u274c Invalid OTP format. Must be " + this.otpLength + " characters.");
        }
        String pattern = "^[" + Pattern.quote(this.otpCharset) + "]{" + this.otpLength + "}$";
        if (!otp.matches(pattern)) {
            return new VerificationResult(false, "\u274c Invalid OTP characters. Must contain only: " + this.otpCharset);
        }
        String attemptKey = discordId + ":" + otp;
        if (!this.rateLimiter.canAttempt(attemptKey)) {
            long waitTime = this.rateLimiter.getWaitTime(attemptKey);
            return new VerificationResult(false, "\u274c Too many attempts. Please wait " + this.formatTime(waitTime) + " before trying again.");
        }
        String globalBlockKey = "global:" + discordId;
        if (!this.rateLimiter.canAttempt(globalBlockKey)) {
            long waitTime = this.rateLimiter.getWaitTime(globalBlockKey);
            return new VerificationResult(false, "\ud83d\udd12 Account temporarily locked. Please wait " + this.formatTime(waitTime) + " before trying again.");
        }
        OTPData otpData = this.otpCache.getIfPresent(otp);
        boolean fromCache = true;
        if (otpData == null) {
            otpData = this.databaseManager.getOTP(otp);
            fromCache = false;
        }
        if (otpData == null) {
            this.rateLimiter.recordAttempt(attemptKey, false);
            this.rateLimiter.recordAttempt(globalBlockKey, false);
            this.databaseManager.recordAttempt(attemptKey, false);
            return new VerificationResult(false, "\u274c Invalid OTP code.");
        }
        if (otpData.isExpired()) {
            if (fromCache) {
                this.otpCache.invalidate(otp);
            }
            this.databaseManager.markOTPAsUsed(otp, "EXPIRED");
            return new VerificationResult(false, "\u23f0 OTP has expired.");
        }
        if (otpData.isUsed()) {
            return new VerificationResult(false, "\u26a0\ufe0f This OTP has already been used.");
        }
        try {
            // Check if this Discord account is linked to OTHER Minecraft accounts
            // If max-alts > 1, allow same Discord account to link to multiple accounts on the same IP
            CompletableFuture<Boolean> linkedCheck = this.databaseManager.isDiscordAlreadyLinked(discordId, otpData.getPlayerUUID());
            Boolean isLinked = (Boolean)linkedCheck.get();
            if (isLinked != null && isLinked.booleanValue()) {
                // Discord is linked to another account - check if it's an alt on the same IP with max-alts support
                String playerIP = otpData.getIpAddress();
                int maxAlts = this.plugin.getConfig().getInt("ai-security.max-alts-per-ip", 2);
                
                // If max-alts is 1, reject any other account with same Discord
                if (maxAlts <= 1) {
                    return new VerificationResult(false, "\u274c This Discord account is already linked to another Minecraft account.");
                }
                
                // If max-alts > 1, check if there are already max-alts accounts linked to this Discord on the same IP
                int accountsWithDiscordOnIP = this.databaseManager.countVerifiedAccountsWithDiscordOnIP(discordId, playerIP).get().intValue();
                if (accountsWithDiscordOnIP >= maxAlts) {
                    return new VerificationResult(false, "\u274c Maximum alt accounts (" + maxAlts + ") already linked to this Discord on this IP.");
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("\u26a0\ufe0f Error checking Discord link: " + e.toString());
        }
        if (this.aiSecurity != null && this.aiSecurity.isPlayerAlt(playerName = otpData.getPlayerName())) {
            // Check if alt_otp is allowed (false = allow alts without OTP enforcement)
            boolean altOtpEnabled = (this.aiSecurity != null) ? this.aiSecurity.isAltOtpEnforced() : this.plugin.getConfig().getBoolean("ai-security.alt_otp", true);

            if (altOtpEnabled) {
                // alt_otp: true means REJECT alts at OTP verification stage
                otpData.markAsUsed();
                this.otpCache.invalidate(otp);
                this.databaseManager.markOTPAsUsed(otp, "ALT_ACCOUNT_BLOCKED");
                return new VerificationResult(false, "\u274c This account has been flagged as an alt account. Please join with your main account.");
            }
            // alt_otp: false means ALLOW alts to verify via OTP (proceed normally)
        }
        otpData.markAsUsed();
        this.otpCache.invalidate(otp);
        String finalOtp = otp;
        String finalDiscordId = discordId;
        OTPData finalOtpData = otpData;
        this.databaseManager.saveVerificationAsync(finalOtpData.getPlayerName(), finalOtpData.getPlayerUUID(), finalDiscordId, discordName, finalOtpData.getIpAddress());
        this.databaseManager.markOTPAsUsed(finalOtp, finalDiscordId);
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            if (this.plugin.getVerificationManager() != null) {
                this.plugin.getVerificationManager().whitelistPlayer(finalOtpData.getPlayerName());
            } else {
                this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), "whitelist add " + finalOtpData.getPlayerName());
                this.plugin.getServer().dispatchCommand((CommandSender)this.plugin.getServer().getConsoleSender(), "whitelist reload");
            }
        });
        this.rateLimiter.clearAttempts(attemptKey);
        this.rateLimiter.clearAttempts(globalBlockKey);
        this.databaseManager.recordAttempt(attemptKey, true);
        this.plugin.getLogger().info("\u2705 Verified OTP " + otp + " for " + otpData.getPlayerName());
        return new VerificationResult(true, "\u2705 Verification successful! You have been whitelisted.", otpData.getPlayerName(), otpData.getPlayerUUID());
    }

    public boolean verifyOTP(String otp, String discordId) {
        try {
            this.plugin.getLogger().info("\ud83d\udd10 OTPManager.verifyOTP called for OTP: " + otp + ", Discord: " + discordId);
            if (otp.trim().length() != this.otpLength) {
                this.plugin.getLogger().warning("\u274c OTP length mismatch. Expected: " + this.otpLength + ", Got: " + otp.length());
                return false;
            }
            boolean otpExists = this.databaseManager.checkOTP(otp).join();
            if (!otpExists) {
                this.plugin.getLogger().warning("\u274c OTP does not exist in database: " + otp);
                return false;
            }
            VerificationResult result = this.verifyOTP(otp, discordId, "DiscordUser");
            if (result.isSuccess()) {
                this.plugin.getLogger().info("\u2705 OTP verification successful for: " + result.getPlayerName());
            } else {
                this.plugin.getLogger().warning("\u274c OTP verification failed: " + result.getMessage());
            }
            return result.isSuccess();
        }
        catch (Exception e) {
            this.plugin.getLogger().severe("\u274c ERROR in verifyOTP: " + e.toString());
            e.printStackTrace();
            return false;
        }
    }

    public void removeOTP(String otp) {
        this.otpCache.invalidate(otp);
        this.plugin.getLogger().info("\ud83d\uddd1\ufe0f Removed OTP from cache: " + otp);
    }

    public String getPlayerNameFromOTP(String otp) {
        OTPData otpData = this.otpCache.getIfPresent(otp);
        if (otpData != null) {
            return otpData.getPlayerName();
        }
        OTPData dbOtpData = this.databaseManager.getOTP(otp);
        if (dbOtpData != null) {
            return dbOtpData.getPlayerName();
        }
        return null;
    }

    private String formatTime(long seconds) {
        if (seconds < 60L) {
            return seconds + " second" + (seconds != 1L ? "s" : "");
        }
        if (seconds < 3600L) {
            long minutes = seconds / 60L;
            return minutes + " minute" + (minutes != 1L ? "s" : "");
        }
        long hours = seconds / 3600L;
        return hours + " hour" + (hours != 1L ? "s" : "");
    }

    public void cleanupExpiredOTPs() {
        this.otpCache.cleanUp();
        this.rateLimiter.cleanup();
        this.databaseManager.cleanupExpiredData().thenAccept(count -> {
            if (count > 0) {
                this.plugin.getLogger().info("\ud83e\uddf9 Cleaned up " + count + " expired OTPs");
            }
        });
    }

    public int getPendingCount() {
        return (int)this.otpCache.estimatedSize();
    }

    public void clearRateLimit(String identifier) {
        this.rateLimiter.clearAttempts(identifier);
        this.databaseManager.clearRateLimit(identifier);
    }

    public boolean canAttempt(String identifier) {
        return this.rateLimiter.canAttempt(identifier);
    }

    public CompletableFuture<Void> cleanupAllData() {
        return CompletableFuture.runAsync(() -> {
            this.otpCache.invalidateAll();
            this.otpCache.cleanUp();
            this.rateLimiter.cleanup();
        });
    }

    public long getCacheSize() {
        return this.otpCache.estimatedSize();
    }

    public void invalidateOTP(String otp) {
        this.otpCache.invalidate(otp);
    }

    public boolean canPlayerGetOTP(String playerName, String playerUUID, String ip) {
        if (this.aiSecurity == null) {
            return true;
        }
        return this.aiSecurity.canGenerateOTP(playerName, playerUUID, ip);
    }

    public String getOTPWithAICheck(String playerName, String playerUUID, String ip) {
        return this.generateOTP(playerName, playerUUID, ip);
    }

    public int getOtpLength() {
        return this.otpLength;
    }

    public String getOtpCharset() {
        return this.otpCharset;
    }

    public int getOtpExpiryMinutes() {
        return this.otpExpiryMinutes;
    }

    public void reloadConfig() {
        try {
            this.otpLength = plugin.getConfigData().getOtpLength();
            this.otpCharset = plugin.getConfigData().getOtpCharset();
            this.otpExpiryMinutes = plugin.getConfigData().getOtpExpiryMinutes();
            if (this.otpCharset == null || this.otpCharset.isEmpty()) this.otpCharset = "0123456789";
            if (this.otpLength < 1 || this.otpLength > 10) this.otpLength = 6;
            // Rebuild cache with new expiry
            try {
                com.github.benmanes.caffeine.cache.Cache<String, OTPData> newCache = Caffeine.newBuilder().expireAfterWrite(this.otpExpiryMinutes, TimeUnit.MINUTES).maximumSize(1000L).build();
                this.otpCache.asMap().forEach(newCache::put);
                // replace underlying cache by reflection fallback
                java.lang.reflect.Field f = this.getClass().getDeclaredField("otpCache");
                f.setAccessible(true);
                f.set(this, newCache);
            } catch (Throwable ignored) {}
            this.plugin.getLogger().info("OTP Manager reloaded: length=" + this.otpLength + " expiry=" + this.otpExpiryMinutes + " charset=" + this.otpCharset);
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to reload OTPManager config: " + e.toString());
        }
    }
}

