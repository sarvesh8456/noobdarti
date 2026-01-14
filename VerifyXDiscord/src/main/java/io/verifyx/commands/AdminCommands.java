/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package io.verifyx.commands;

import io.verifyx.Main;
import io.verifyx.gui.DatabaseGUI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class AdminCommands
implements CommandExecutor {
    private final Main plugin;

    public AdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args2) {
        String subCommand;
        if (args2.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (subCommand = args2[0].toLowerCase()) {
            case "help": {
                this.sendHelp(sender);
                break;
            }
            case "reload": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.reloadConfig(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "status": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.showStatus(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "forceverify": {
                if (sender.hasPermission("verifyx.admin") && args2.length >= 2) {
                    String playerName = args2[1];
                    String ipAddress = args2.length >= 3 ? args2[2] : null;
                    this.forceVerify(sender, playerName, ipAddress);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /verifyadmin forceverify <player> [IP]");
                break;
            }
            case "unverify": {
                if (sender.hasPermission("verifyx.admin") && args2.length >= 2) {
                    String playerName = args2[1];
                    this.unverify(sender, playerName);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /verifyadmin unverify <player>");
                break;
            }
            case "dumpotps": {
                if (sender.hasPermission("verifyx.admin")) {
                    int limit = 10;
                    try {
                        if (args2.length >= 2) limit = Integer.parseInt(args2[1]);
                    } catch (Exception ignored) {}
                    this.dumpOtps(sender, limit);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "cleardata": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.clearData(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "showdatabase": {
                if (sender.hasPermission("verifyx.admin")) {
                    if (sender instanceof Player) {
                        this.showDatabase((Player)sender);
                        break;
                    }
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "This command can only be used by players!");
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "dashboard": {
                if (sender.hasPermission("verifyx.admin")) {
                    if (sender instanceof Player) {
                        this.openDashboard((Player)sender);
                        break;
                    }
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "This command can only be used by players!");
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "checkdata": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.checkData(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "clearcache": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.clearCache(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "limit": {
                if (!sender.hasPermission("verifyx.admin")) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                    break;
                }
                if (args2.length < 2) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /verifyadmin limit <number>");
                    break;
                }
                try {
                    int limitVal = Integer.parseInt(args2[1]);
                    if (limitVal < 1) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Limit must be at least 1");
                        break;
                    }
                    this.plugin.getConfig().set("ai-security.max-trusted-ips", limitVal);
                    this.plugin.saveConfig();
                    sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Set ai-security.max-trusted-ips = " + limitVal);
                } catch (NumberFormatException nfe) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Invalid number: " + args2[1]);
                }
                break;
            }
            case "debug": {
                if (sender.hasPermission("verifyx.admin")) {
                    this.debug(sender);
                    break;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                break;
            }
            case "loginsecurity": {
                if (!sender.hasPermission("verifyx.admin")) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command!");
                    break;
                }
                if (args2.length < 2) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /verifyadmin loginsecurity reset <player>");
                    break;
                }
                String sub = args2[1].toLowerCase();
                if ("reset".equals(sub)) {
                    if (args2.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /verifyadmin loginsecurity reset <player>");
                        break;
                    }
                    String target = args2[2];
                    boolean res = false;
                    try {
                        if (this.plugin.getLoginSecurityManager() != null) {
                            res = this.plugin.getLoginSecurityManager().resetAccountByName(target);
                        }
                    } catch (Exception ignored) {}
                    if (res) sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Reset loginsecurity for " + target);
                    else sender.sendMessage(String.valueOf(ChatColor.RED) + "Could not find account for " + target);
                } else {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "Unknown subcommand for loginsecurity");
                }
                break;
            }
            default: {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Unknown command. Use /verifyadmin help for help.");
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== VerifyXDiscord Admin Commands ===");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin help" + String.valueOf(ChatColor.GRAY) + " - Show this help");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin reload" + String.valueOf(ChatColor.GRAY) + " - Reload configuration");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin status" + String.valueOf(ChatColor.GRAY) + " - Show system status");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin forceverify <player>" + String.valueOf(ChatColor.GRAY) + " - Force verify a player");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin unverify <player>" + String.valueOf(ChatColor.GRAY) + " - Unverify a player");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin limit <number>" + String.valueOf(ChatColor.GRAY) + " - Set max trusted IPs per account (default 1)");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin cleardata" + String.valueOf(ChatColor.GRAY) + " - Clear expired data");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin showdatabase" + String.valueOf(ChatColor.GRAY) + " - Open database viewer GUI");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin checkdata" + String.valueOf(ChatColor.GRAY) + " - Check database statistics");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin clearcache" + String.valueOf(ChatColor.GRAY) + " - Clear plugin cache");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/verifyadmin debug" + String.valueOf(ChatColor.GRAY) + " - Show debug information");
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== AI Security Commands ===");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity" + String.valueOf(ChatColor.GRAY) + " - AI Security management (separate command)");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "  Use /aisecurity help for AI security commands");
    }

    private void reloadConfig(CommandSender sender) {
        this.plugin.reloadConfig();
        if (this.plugin.getVerificationManager() != null) {
            this.plugin.getVerificationManager().reloadConfig();
        }
        if (this.plugin.getAISecurityManager() != null) {
            try {
                this.plugin.getAISecurityManager().reloadConfig();
            } catch (Throwable ignored) {}
        }
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Configuration reloaded!");
    }

    private void showStatus(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean dbConnected = this.plugin.getDatabaseManager() != null && this.plugin.getDatabaseManager().isConnected();
                boolean discordConnected = this.plugin.getDiscordBot() != null && this.plugin.getDiscordBot().isConnected();
                boolean managersReady = this.plugin.areManagersInitialized();
                int verifiedCount = 0;
                int pendingOTPs = 0;
                int flaggedAlts = 0;
                if (dbConnected && this.plugin.getDatabaseManager() != null) {
                    verifiedCount = this.plugin.getDatabaseManager().getVerifiedCount().join();
                    pendingOTPs = this.plugin.getDatabaseManager().getPendingOTPCount().join();
                }
                if (this.plugin.getAISecurityManager() != null) {
                    flaggedAlts = this.plugin.getAISecurityManager().getFlaggedPlayers().size();
                }
                int finalVerifiedCount = verifiedCount;
                int finalPendingOTPs = pendingOTPs;
                int finalFlaggedAlts = flaggedAlts;
                boolean finalDbConnected = dbConnected;
                boolean finalDiscordConnected = discordConnected;
                boolean finalManagersReady = managersReady;
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== System Status ===");
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database: " + (finalDbConnected ? String.valueOf(ChatColor.GREEN) + "CONNECTED" : String.valueOf(ChatColor.RED) + "DISCONNECTED"));
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Discord Bot: " + (finalDiscordConnected ? String.valueOf(ChatColor.GREEN) + "CONNECTED" : String.valueOf(ChatColor.RED) + "DISCONNECTED"));
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Managers: " + (finalManagersReady ? String.valueOf(ChatColor.GREEN) + "READY" : String.valueOf(ChatColor.YELLOW) + "INITIALIZING"));
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Verified Players: " + String.valueOf(ChatColor.GREEN) + finalVerifiedCount);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Active OTPs: " + String.valueOf(ChatColor.YELLOW) + finalPendingOTPs);
                    if (this.plugin.getAISecurityManager() != null) {
                        boolean aiEnabled = this.plugin.getAISecurityManager().isEnabled();
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI Security: " + (aiEnabled ? String.valueOf(ChatColor.GREEN) + "ENABLED" : String.valueOf(ChatColor.RED) + "DISABLED"));
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Flagged Alt Accounts: " + String.valueOf(ChatColor.RED) + finalFlaggedAlts);
                        // Show current alt-related policy configuration
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI alt_otp enforced: " + (this.plugin.getAISecurityManager().isAltOtpEnforced() ? String.valueOf(ChatColor.GREEN) + "true" : String.valueOf(ChatColor.RED) + "false"));
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI max-alts-per-ip: " + String.valueOf(ChatColor.YELLOW) + this.plugin.getConfig().getInt("ai-security.max-alts-per-ip", 2));
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI detect-alt-clusters: " + (this.plugin.getConfig().getBoolean("ai-security.detect-alt-clusters", true) ? String.valueOf(ChatColor.GREEN) + "true" : String.valueOf(ChatColor.RED) + "false"));
                    }
                    String serverIp = Bukkit.getServer().getIp();
                    int serverPort = Bukkit.getServer().getPort();
                    String ipDisplay = serverIp == null || serverIp.isEmpty() || serverIp.equals("0.0.0.0") ? "localhost:" + serverPort : serverIp + ":" + serverPort;
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Server Address: " + String.valueOf(ChatColor.AQUA) + ipDisplay);
                });
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error getting status: " + e.toString());
            }
        });
    }

    private void forceVerify(CommandSender sender, String playerName, String manualIP) {
        CompletableFuture.runAsync(() -> {
            try {
                final String finalPlayerName = playerName;
                Player onlinePlayer = Bukkit.getPlayer((String)finalPlayerName);
                String playerUUID = null;
                if (this.plugin.getDatabaseManager() != null) {
                    try {
                        playerUUID = this.plugin.getDatabaseManager().fetchUUIDFromName(finalPlayerName);
                    } catch (Exception ex) {
                        playerUUID = null;
                    }
                }
                if (playerUUID == null) {
                    try {
                        playerUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + finalPlayerName).getBytes()).toString();
                    } catch (Exception ex) {
                        playerUUID = "";
                    }
                }
                boolean alreadyVerified = false;
                if (this.plugin.getDatabaseManager() != null) {
                    alreadyVerified = this.plugin.getDatabaseManager().checkPlayerExists(playerName).join();
                }

                // If strict alt policy is enabled (max-alts-per-ip == 1), disallow forceverify entirely
                try {
                    if (this.plugin.getConfigData() != null && this.plugin.getConfigData().getMaxAltsPerIP() <= 1) {
                        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Forceverify is disabled when ai-security.max-alts-per-ip <= 1 to prevent admin bypasses."));
                        return;
                    }
                } catch (Exception ignored) {}
                if (alreadyVerified) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.YELLOW) + finalPlayerName + " is already verified!"));
                    return;
                }
                boolean dbSuccess = false;
                if (this.plugin.getDatabaseManager() != null) {
                    dbSuccess = this.plugin.getDatabaseManager().forceVerifyPlayer(playerName, manualIP).join();
                }
                boolean whitelistSuccess = false;
                // playerUUID already determined earlier
                if (this.plugin.getVerificationManager() != null) {
                    this.plugin.getVerificationManager().addToWhitelist(playerName, playerUUID);
                    whitelistSuccess = true;
                    try {
                        this.plugin.getVerificationManager().cacheVerified(playerUUID, playerName);
                        // If player is online and using Floodgate/Geyser, cache Bedrock XUID mapping as well
                        try {
                            Player online = Bukkit.getPlayer((String)playerName);
                            if (online != null) {
                                try {
                                    // Use VerificationManager helper to query Floodgate XUID safely
                                    String xuid = this.plugin.getVerificationManager().getFloodgateXuid(online);
                                    if (xuid != null && !xuid.isEmpty()) {
                                        this.plugin.getVerificationManager().cacheBedrockXuid(xuid, playerUUID);
                                    }
                                } catch (Throwable ignored) {}
                            }
                        } catch (Throwable ignored) {}
                    } catch (Exception ex) {
                        this.plugin.getLogger().warning("Failed to cache verification after forceverify: " + ex.getMessage());
                    }
                }
                if (this.plugin.getAISecurityManager() != null) {
                    this.plugin.getAISecurityManager().markForceVerified(playerUUID);
                }
                boolean finalDbSuccess = dbSuccess;
                boolean finalWhitelistSuccess = whitelistSuccess;
                final String finalPlayerUUID = playerUUID;
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Force Verify Results ===");
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Player: " + String.valueOf(ChatColor.YELLOW) + finalPlayerName);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database: " + (finalDbSuccess ? String.valueOf(ChatColor.GREEN) + "SUCCESS" : String.valueOf(ChatColor.RED) + "FAILED"));
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Whitelist: " + (finalWhitelistSuccess ? String.valueOf(ChatColor.GREEN) + "SUCCESS" : String.valueOf(ChatColor.RED) + "FAILED"));
                    if (!finalDbSuccess) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "\u26a0 Could not save to database!");
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Manual SQL:");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "INSERT INTO verifications (player_name, player_uuid, discord_id, discord_name)");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "VALUES ('" + finalPlayerName + "', '" + finalPlayerUUID + "', 'ADMIN_FORCE', 'Admin');");
                    }
                    try {
                        Player online = null;
                        try {
                            online = Bukkit.getPlayer(java.util.UUID.fromString(finalPlayerUUID));
                        } catch (Exception ue) {
                            online = Bukkit.getPlayer((String)finalPlayerName);
                        }
                        if (online != null && this.plugin.getVerifyTab() != null) {
                            this.plugin.getVerifyTab().setPlayerVerified(online);
                            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Tab display updated for " + finalPlayerName);
                        } else if (this.plugin.getVerifyTab() != null) {
                            // ensure tab is refreshed for all players as a fallback
                            this.plugin.getVerifyTab().updateAllPlayers();
                            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Tab refresh scheduled (player may be offline or name lookup failed)." );
                        }
                    } catch (Exception ex) {
                        this.plugin.getLogger().warning("Error updating tab after forceverify: " + ex.toString());
                    }
                });
            }
            catch (Exception e) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Error force verifying: " + e.toString()));
            }
        });
    }

    private void unverify(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            try {
                String playerUUID = this.getPlayerUUID(playerName);
                String discordId = null;
                if (this.plugin.getDatabaseManager() != null) {
                    discordId = this.plugin.getDatabaseManager().findDiscordIdByPlayerName(playerName).join();
                }
                boolean dbRemoved = false;
                boolean whitelistRemoved = false;
                if (this.plugin.getDatabaseManager() != null) {
                    if (discordId != null) {
                        dbRemoved = this.plugin.getDatabaseManager().removeVerification(discordId).join();
                    }
                    if (!dbRemoved) {
                        dbRemoved = this.plugin.getDatabaseManager().removeVerificationByUUID(playerUUID).join();
                    }
                    if (!dbRemoved) {
                        dbRemoved = this.plugin.getDatabaseManager().removeVerificationByPlayerName(playerName).join();
                    }
                }
                if (this.plugin.getVerificationManager() != null) {
                    this.plugin.getVerificationManager().removeFromWhitelist(playerName, playerUUID);
                    whitelistRemoved = true;
                }
                boolean finalDbRemoved = dbRemoved;
                boolean finalWhitelistRemoved = whitelistRemoved;
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    if (finalDbRemoved && finalWhitelistRemoved) {
                        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2705 Successfully unverified " + playerName);
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Removed from: Database \u2713, Whitelist \u2713");
                    } else if (finalWhitelistRemoved) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 Removed " + playerName + " from whitelist only");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database entry not found or couldn't be removed");
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Manual SQL (if needed):");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "DELETE FROM verifications WHERE player_name = '" + playerName + "';");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "DELETE FROM verifications WHERE player_uuid = '" + playerUUID + "';");
                    } else {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "\u274c Failed to unverify " + playerName);
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Check console for errors");
                    }
                });
            }
            catch (Exception e) {
                this.plugin.getLogger().severe("Error in unverify command: " + e.toString());
                e.printStackTrace();
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "\u274c Error unverifying: " + e.toString()));
            }
        });
    }

    private String getPlayerUUID(String playerName) {
        try {
            Player player = Bukkit.getPlayerExact((String)playerName);
            if (player != null) {
                return player.getUniqueId().toString();
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((String)playerName);
            if (offlinePlayer.hasPlayedBefore()) {
                return offlinePlayer.getUniqueId().toString();
            }
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error getting UUID for " + playerName + ": " + e.toString());
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        }
    }

    private void clearData(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.plugin.getDatabaseManager() != null) {
                    int cleaned = this.plugin.getDatabaseManager().cleanupExpiredData().join();
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Cleaned up " + cleaned + " expired records!"));
                }
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error clearing data: " + e.toString());
            }
        });
    }

    private void dumpOtps(CommandSender sender, int limit) {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.plugin.getDatabaseManager() == null) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Database manager not available."));
                    return;
                }
                JsonArray rows = this.plugin.getDatabaseManager().getRecentOtps(limit).join();
                if (rows == null || rows.size() == 0) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No recent OTPs found."));
                    return;
                }
                int count = Math.min(rows.size(), limit);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Recent OTPs (most recent first) ===");
                });
                for (int i = 0; i < count; i++) {
                    try {
                        JsonObject row = rows.get(i).getAsJsonObject();
                        String otp = row.has("otp_code") && !row.get("otp_code").isJsonNull() ? row.get("otp_code").getAsString() : "";
                        String masked = "***" + (otp.length() > 2 ? otp.substring(Math.max(0, otp.length() - 2)) : "**");
                        String p = row.has("player_name") && !row.get("player_name").isJsonNull() ? row.get("player_name").getAsString() : "?";
                        String uuid = row.has("player_uuid") && !row.get("player_uuid").isJsonNull() ? row.get("player_uuid").getAsString() : "?";
                        String used = row.has("is_used") && !row.get("is_used").isJsonNull() ? String.valueOf(row.get("is_used").getAsBoolean()) : "null";
                        String expires = row.has("expires_at") && !row.get("expires_at").isJsonNull() ? row.get("expires_at").getAsString() : "?";
                        String ip = row.has("ip_address") && !row.get("ip_address").isJsonNull() ? row.get("ip_address").getAsString() : "?";
                        final String line = String.format("%d) OTP:%s Player:%s UUID:%s Used:%s Expires:%s IP:%s", i+1, masked, p, uuid, used, expires, ip);
                        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.GRAY) + line));
                        // Also log masked to server log for audit
                        this.plugin.getLogger().info("[VerifyX][OTP-DUMP] " + line);
                    } catch (Exception e) {
                        continue;
                    }
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Error fetching OTPs: " + e.toString()));
            }
        });
    }

    private void showDatabase(Player player) {
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Opening database viewer...");
        try {
            DatabaseGUI gui = new DatabaseGUI(this.plugin, player);
            gui.open();
        }
        catch (Exception e) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Error opening database GUI: " + e.toString());
            this.plugin.getLogger().severe("Error opening DatabaseGUI: " + e.toString());
            e.printStackTrace();
        }
    }

    private void openDashboard(Player player) {
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Opening VerifyX dashboard...");
        try {
            io.verifyx.gui.VerificationDashboard dash = new io.verifyx.gui.VerificationDashboard(this.plugin, player);
            dash.open();
        } catch (Exception e) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Error opening dashboard: " + e.toString());
            this.plugin.getLogger().severe("Error opening VerificationDashboard: " + e.toString());
            e.printStackTrace();
        }
    }

    private void checkData(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.plugin.getDatabaseManager() != null) {
                    int verifiedCount = this.plugin.getDatabaseManager().getVerifiedCount().join();
                    int pendingOTPs = this.plugin.getDatabaseManager().getPendingOTPCount().join();
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Database Statistics ===");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Total Verified Players: " + String.valueOf(ChatColor.GREEN) + verifiedCount);
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Active OTPs: " + String.valueOf(ChatColor.YELLOW) + pendingOTPs);
                        int onlinePlayers = Bukkit.getOnlinePlayers().size();
                        int whitelistedPlayers = this.plugin.getVerificationManager() != null ? this.plugin.getVerificationManager().getWhitelistCount() : 0;
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Online Players: " + String.valueOf(ChatColor.AQUA) + onlinePlayers);
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Whitelisted Players: " + String.valueOf(ChatColor.AQUA) + whitelistedPlayers);
                        boolean dbConnected = this.plugin.getDatabaseManager().isConnected();
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database Connection: " + (dbConnected ? String.valueOf(ChatColor.GREEN) + "ACTIVE" : String.valueOf(ChatColor.RED) + "INACTIVE"));
                    });
                }
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error checking data: " + e.toString());
            }
        });
    }

    private void clearCache(CommandSender sender) {
        try {
            if (this.plugin.getOtpManager() != null) {
                this.plugin.getOtpManager().cleanupAllData().join();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "OTP cache cleared successfully!");
            } else {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "OTP Manager not available!");
            }
            if (this.plugin.getAISecurityManager() != null) {
                this.plugin.getAISecurityManager().cleanup();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "AI Security cache cleared!");
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error clearing cache: " + e.toString());
        }
    }

    private void debug(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Debug Information ===");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Plugin Version: 1.1.0");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Bukkit Version: " + Bukkit.getVersion());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Plugin Enabled: " + this.plugin.isEnabled());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Managers Initialized: " + this.plugin.areManagersInitialized());
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database Manager: " + (this.plugin.getDatabaseManager() != null ? "Present" : "Null"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "OTP Manager: " + (this.plugin.getOtpManager() != null ? "Present" : "Null"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Verification Manager: " + (this.plugin.getVerificationManager() != null ? "Present" : "Null"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Discord Bot: " + (this.plugin.getDiscordBot() != null ? "Present" : "Null"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI Security Manager: " + (this.plugin.getAISecurityManager() != null ? "Present" : "Null"));
        if (this.plugin.getDatabaseManager() != null) {
            boolean connected = this.plugin.getDatabaseManager().testConnectionSync();
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database Connection Test: " + (connected ? String.valueOf(ChatColor.GREEN) + "SUCCESS" : String.valueOf(ChatColor.RED) + "FAILED"));
        }
        if (this.plugin.getAISecurityManager() != null) {
            boolean aiEnabled = this.plugin.getAISecurityManager().isEnabled();
            int flaggedAlts = this.plugin.getAISecurityManager().getFlaggedPlayers().size();
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "AI Security: " + (aiEnabled ? String.valueOf(ChatColor.GREEN) + "ENABLED" : String.valueOf(ChatColor.RED) + "DISABLED"));
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Flagged Alt Accounts: " + String.valueOf(ChatColor.RED) + flaggedAlts);
        }
    }
}

