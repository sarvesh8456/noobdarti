/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package io.verifyx.commands;

import io.verifyx.Main;
import io.verifyx.security.AISecurityManager;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AISecurityCommand
implements CommandExecutor {
    private final Main plugin;
    private final AISecurityManager security;

    public AISecurityCommand(Main plugin, AISecurityManager security) {
        this.plugin = plugin;
        this.security = security;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args2) {
        this.plugin.getLogger().info("DEBUG: Command '/" + label + "' executed by " + sender.getName() + " with args: " + Arrays.toString(args2));
        if (!sender.hasPermission("verifyx.admin.security")) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "No permission!");
            this.plugin.getLogger().warning("DEBUG: Permission check failed for " + sender.getName());
            return true;
        }
        if (args2.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        switch (args2[0].toLowerCase()) {
            case "block": {
                this.handleBlock(sender, args2);
                break;
            }
            case "ipinfo": {
                this.handleIPInfo(sender, args2);
                break;
            }
            case "firstplayer": {
                this.handleFirstPlayer(sender, args2);
                break;
            }
            case "altcheck": {
                this.handleAltCheck(sender, args2);
                break;
            }
            case "kick": {
                this.handleKick(sender, args2);
                break;
            }
            case "report": {
                this.handleReport(sender, args2);
                break;
            }
            case "mode": {
                this.handleMode(sender, args2);
                break;
            }
            case "scan": {
                this.handleScan(sender, args2);
                break;
            }
            case "list": {
                this.handleList(sender);
                break;
            }
            case "preview-purge": {
                this.handlePreviewPurge(sender);
                break;
            }
            default: {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Unknown command. Use /aisecurity for help.");
                this.plugin.getLogger().warning("DEBUG: Unknown command argument: " + args2[0]);
            }
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== AI Security Commands ===");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity block <player> <reason>");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity kick <player> <reason>");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity report <player>");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity mode <on/off>");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity scan <player>");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/aisecurity list");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Alias: /security (works the same)");
    }

    private void handleBlock(CommandSender sender, String[] args2) {
        if (args2.length < 3) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity block <player> <reason>");
            return;
        }
        String playerName = args2[1];
        String reason = String.join((CharSequence)" ", Arrays.copyOfRange(args2, 2, args2.length));
        this.plugin.getLogger().info("DEBUG: Blocking player " + playerName + " for reason: " + reason);
        try {
            this.security.permanentlyBlockPlayer(playerName, reason);
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Player " + playerName + " permanently blocked!");
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error: " + e.getMessage());
            this.plugin.getLogger().severe("Error blocking player: " + e.getMessage());
        }
    }

    private void handleKick(CommandSender sender, String[] args2) {
        if (args2.length < 3) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity kick <player> <reason>");
            return;
        }
        String playerName = args2[1];
        String reason = String.join((CharSequence)" ", Arrays.copyOfRange(args2, 2, args2.length));
        try {
            this.security.instantlyKickPlayer(playerName, reason);
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Player " + playerName + " instantly kicked!");
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error: " + e.getMessage());
        }
    }

    private void handleReport(CommandSender sender, String[] args2) {
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity report <player>");
            return;
        }
        String playerName = args2[1];
        this.plugin.getLogger().info("DEBUG: Getting report for " + playerName);
        try {
            Map<String, Object> report = this.security.getPlayerSecurityReport(playerName);
            if (report == null || report.containsKey("error")) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found or no data!");
                return;
            }
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Security Report: " + playerName + " ===");
            for (Map.Entry<String, Object> entry : report.entrySet()) {
                ChatColor color = ChatColor.WHITE;
                Object value = entry.getValue();
                if (value instanceof Number) {
                    double num = ((Number)value).doubleValue();
                    color = num > 50.0 ? ChatColor.RED : (num > 30.0 ? ChatColor.YELLOW : ChatColor.GREEN);
                }
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + entry.getKey() + ": " + String.valueOf(color) + String.valueOf(value));
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error generating report: " + e.getMessage());
            this.plugin.getLogger().severe("Error generating report: " + e.getMessage());
        }
    }

    private void handleMode(CommandSender sender, String[] args2) {
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity mode <on/off>");
            return;
        }
        String mode = args2[1].toLowerCase();
        if (mode.equals("on")) {
            try {
                this.security.enableAbsoluteMode();
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\ud83d\udd25 ABSOLUTE SECURITY MODE ENABLED!");
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error: " + e.getMessage());
            }
        } else if (mode.equals("off")) {
            try {
                this.security.disableAbsoluteMode();
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Absolute security mode disabled");
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error: " + e.getMessage());
            }
        } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity mode <on/off>");
        }
    }

    private void handleScan(CommandSender sender, String[] args2) {
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity scan <player>");
            return;
        }
        String playerName = args2[1];
        this.plugin.getLogger().info("DEBUG: Scanning player " + playerName);
        Player target = Bukkit.getPlayerExact((String)playerName);
        if (target == null) {
            target = Bukkit.getPlayer((String)playerName);
        }
        if (target == null) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Player not found! They must be online.");
            return;
        }
        try {
            Map<String, Integer> scanResult = this.security.analyzePlayerNow(playerName);
            if (scanResult == null || scanResult.containsKey("error")) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Failed to scan player!");
                return;
            }
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Security Scan: " + playerName + " ===");
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "UUID: " + String.valueOf(ChatColor.WHITE) + String.valueOf(target.getUniqueId()));
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "IP: " + String.valueOf(ChatColor.WHITE) + target.getAddress().getAddress().getHostAddress());
            for (Map.Entry<String, Integer> entry : scanResult.entrySet()) {
                String key = entry.getKey();
                int value = entry.getValue();
                ChatColor color = ChatColor.WHITE;
                String prefix = "";
                if (value > 70) {
                    color = ChatColor.RED;
                    prefix = "\ud83d\udd34 ";
                } else if (value > 40) {
                    color = ChatColor.YELLOW;
                    prefix = "\ud83d\udfe1 ";
                } else {
                    color = ChatColor.GREEN;
                    prefix = "\ud83d\udfe2 ";
                }
                Object formattedKey = key.replace("_", " ");
                formattedKey = ((String)formattedKey).substring(0, 1).toUpperCase() + ((String)formattedKey).substring(1);
                sender.sendMessage(prefix + String.valueOf(ChatColor.GRAY) + (String)formattedKey + ": " + String.valueOf(color) + value + "%");
            }
            Integer riskScore = scanResult.get("risk_score");
            if (riskScore != null) {
                if (riskScore > 70) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "\ud83d\udea8 Recommendation: BLOCK - High risk detected!");
                } else if (riskScore > 40) {
                    sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0\ufe0f Recommendation: MONITOR - Elevated risk");
                } else {
                    sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2705 Recommendation: ALLOW - Low risk");
                }
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error scanning player: " + e.getMessage());
            this.plugin.getLogger().severe("Error scanning player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleList(CommandSender sender) {
        try {
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Blocked Players ===");
            for (String uuid : this.security.getFlaggedPlayers()) {
                String playerName = "Unknown";
                try {
                    playerName = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid)).getName();
                    if (playerName == null) {
                        playerName = "Unknown";
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\u2022 " + String.valueOf(ChatColor.WHITE) + playerName + String.valueOf(ChatColor.GRAY) + " (" + uuid.substring(0, 8) + "...)");
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error listing blocked players: " + e.getMessage());
        }
    }

    private void handlePreviewPurge(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                java.util.List<String> preview = this.security.previewPurgeForceVerifiedIfStrict();
                Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin)this.plugin, () -> {
                    try {
                        if (preview.isEmpty()) {
                            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "No admin-forced verifications would be purged under current strict policy.");
                            return;
                        }
                        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Preview: Admin-Forced Verifications To Purge ===");
                        for (String name : preview) {
                            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "- " + String.valueOf(ChatColor.WHITE) + name);
                        }
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Total: " + preview.size());
                    } catch (Exception e) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Error generating preview: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask((org.bukkit.plugin.Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Error generating preview: " + e.getMessage()));
            }
        });
    }

    private void handleIPInfo(CommandSender sender, String[] args2) {
        String input;
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity ipinfo <IP|player>");
            return;
        }
        String ipToCheck = input = args2[1];
        Player player = Bukkit.getPlayer((String)input);
        if (player != null) {
            ipToCheck = player.getAddress().getAddress().getHostAddress();
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Player " + player.getName() + " IP: " + ipToCheck);
        }
        try {
            List accounts;
            Map<String, Object> ipInfo = this.security.getIPDetailsForCommand(ipToCheck);
            if (ipInfo == null || ipInfo.isEmpty()) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "No information found for: " + ipToCheck);
                return;
            }
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== IP Information: " + String.valueOf(ipInfo.get("ip")) + " ===");
            int totalAccounts = (Integer)ipInfo.get("total_accounts");
            int maxAllowed = (Integer)ipInfo.get("max_allowed");
            ChatColor statusColor = totalAccounts <= maxAllowed ? ChatColor.GREEN : ChatColor.RED;
            String statusIcon = totalAccounts <= maxAllowed ? "\u2705" : "\u274c";
            sender.sendMessage(statusIcon + String.valueOf(ChatColor.GRAY) + " Accounts: " + String.valueOf(ChatColor.WHITE) + totalAccounts + "/" + maxAllowed + " " + String.valueOf(statusColor) + "(" + (totalAccounts <= maxAllowed ? "OK" : "OVER LIMIT") + ")");
            if (ipInfo.get("first_player_name") != null) {
                String firstPlayerName = (String)ipInfo.get("first_player_name");
                boolean firstPlayerOnline = (Boolean)ipInfo.get("first_player_online");
                ChatColor onlineColor = firstPlayerOnline ? ChatColor.GREEN : ChatColor.GRAY;
                String onlineStatus = firstPlayerOnline ? " (online)" : " (offline)";
                sender.sendMessage("\ud83d\udc51 " + String.valueOf(ChatColor.GRAY) + "First Player: " + String.valueOf(ChatColor.GREEN) + firstPlayerName + String.valueOf(onlineColor) + onlineStatus);
            }
            if ((accounts = (List)ipInfo.get("accounts")) != null && !accounts.isEmpty()) {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "=== All Accounts on this IP ===");
                for (Map account : (List<Map>)accounts) {
                    String name = (String)account.get("name");
                    boolean isFirst = (Boolean)account.get("is_first_player");
                    boolean online = (Boolean)account.get("online");
                    ChatColor color = isFirst ? ChatColor.GREEN : ChatColor.WHITE;
                    Object status = isFirst ? " [FIRST]" : " [ALT]";
                    status = (String)status + (online ? " [ONLINE]" : " [OFFLINE]");
                    sender.sendMessage(String.valueOf(color) + "\u2022 " + name + String.valueOf(ChatColor.GRAY) + (String)status);
                }
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error getting IP info: " + e.getMessage());
            this.plugin.getLogger().severe("Error getting IP info: " + e.getMessage());
        }
    }

    private void handleFirstPlayer(CommandSender sender, String[] args2) {
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity firstplayer <player>");
            return;
        }
        String playerName = args2[1];
        try {
            List<String> firstIPs = this.security.getPlayerFirstIPsForCommand(playerName);
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== First Player Status: " + playerName + " ===");
            if (firstIPs.isEmpty()) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\u274c NOT a first player on any IP");
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "This player is considered an alt account.");
            } else {
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2705 FIRST PLAYER on " + firstIPs.size() + " IP" + (firstIPs.size() > 1 ? "s" : ""));
                for (String ip : firstIPs) {
                    Set<String> accounts = this.security.getAccountsOnIP(ip);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "  \u2022 " + ip + " (" + accounts.size() + " accounts)");
                }
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error checking first player status: " + e.getMessage());
        }
    }

    private void handleAltCheck(CommandSender sender, String[] args2) {
        if (args2.length < 2) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /aisecurity altcheck <player>");
            return;
        }
        String playerName = args2[1];
        try {
            Map<String, Object> status = this.security.getPlayerIPStatusForCommand(playerName);
            if (status.containsKey("error")) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Error: " + String.valueOf(status.get("error")));
                return;
            }
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Alt Check: " + playerName + " ===");
            boolean isFirstPlayer = (Boolean)status.get("is_first_player");
            if (isFirstPlayer) {
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2705 Status: PRIMARY ACCOUNT");
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "This player is the first/primary account on their IP.");
            } else {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\u274c Status: ALT ACCOUNT");
                if (status.get("first_player_name") != null) {
                    String firstPlayerName = (String)status.get("first_player_name");
                    boolean firstPlayerOnline = (Boolean)status.get("first_player_online");
                    ChatColor onlineColor = firstPlayerOnline ? ChatColor.GREEN : ChatColor.GRAY;
                    String onlineStatus = firstPlayerOnline ? " (online)" : " (offline)";
                    sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Primary account on this IP: " + String.valueOf(ChatColor.GREEN) + firstPlayerName + String.valueOf(onlineColor) + onlineStatus);
                }
            }
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "IP: " + String.valueOf(ChatColor.WHITE) + String.valueOf(status.get("current_ip")));
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Accounts on IP: " + String.valueOf(ChatColor.WHITE) + String.valueOf(status.get("total_accounts_on_ip")));
            if (status.get("trust_score") != null) {
                double trustScore = (Double)status.get("trust_score");
                ChatColor trustColor = trustScore > 70.0 ? ChatColor.GREEN : (trustScore > 40.0 ? ChatColor.YELLOW : ChatColor.RED);
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Trust Score: " + String.valueOf(trustColor) + String.format("%.1f%%", trustScore));
            }
            if (status.get("risk_level") != null) {
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Risk Level: " + String.valueOf(ChatColor.WHITE) + String.valueOf(status.get("risk_level")));
            }
        }
        catch (Exception e) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Error checking alt status: " + e.getMessage());
        }
    }
}

