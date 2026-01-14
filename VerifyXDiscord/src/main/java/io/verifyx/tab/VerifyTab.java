/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 *  org.bukkit.scoreboard.Team$Option
 *  org.bukkit.scoreboard.Team$OptionStatus
 */
package io.verifyx.tab;

import io.verifyx.Main;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class VerifyTab {
    private final Main plugin;
    private final Map<UUID, String> playerTeams = new HashMap<UUID, String>();
    private Scoreboard scoreboard;

    public VerifyTab(Main plugin) {
        this.plugin = plugin;
        this.initializeScoreboard();
        this.startUpdateTask();
    }

    private void initializeScoreboard() {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (this.scoreboard.getTeam("verify_verified") == null) {
            Team verifiedTeam = this.scoreboard.registerNewTeam("verify_verified");
            verifiedTeam.setPrefix(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a\u2705 "));
            verifiedTeam.setColor(ChatColor.GREEN);
            verifiedTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        if (this.scoreboard.getTeam("verify_unverified") == null) {
            Team unverifiedTeam = this.scoreboard.registerNewTeam("verify_unverified");
            unverifiedTeam.setPrefix(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c\u274c "));
            unverifiedTeam.setColor(ChatColor.RED);
            unverifiedTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        if (this.scoreboard.getTeam("verify_pending") == null) {
            Team pendingTeam = this.scoreboard.registerNewTeam("verify_pending");
            pendingTeam.setPrefix(ChatColor.translateAlternateColorCodes((char)'&', (String)"&e\u23f3 "));
            pendingTeam.setColor(ChatColor.YELLOW);
            pendingTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
    }

    public void updatePlayerTab(Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                boolean isVerified = false;
                String playerUUID = player.getUniqueId().toString();
                String playerName = player.getName();
                
                // First check the verification cache (covers force-verified players)
                if (this.plugin.getVerificationManager() != null) {
                    try {
                        isVerified = this.plugin.getVerificationManager().isCachedVerified(playerUUID, playerName);
                    } catch (Exception e) {
                        isVerified = false;
                    }
                }
                
                // If not in cache, check the database
                if (!isVerified && this.plugin.getDatabaseManager() != null) {
                    try {
                        isVerified = this.plugin.getDatabaseManager().isPlayerVerified(playerUUID).join();
                    } catch (Exception e) {
                        isVerified = false;
                    }
                }
                
                boolean verified = isVerified;
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.updatePlayerTeam(player, verified));
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Error updating tab for " + player.getName() + ": " + e.toString());
            }
        });
    }

    private void updatePlayerTeam(Player player, boolean isVerified) {
        try {
            String teamName;
            Team team;
            String oldTeamName;
            Team oldTeam;
            UUID playerId = player.getUniqueId();
            if (this.playerTeams.containsKey(playerId) && (oldTeam = this.scoreboard.getTeam(oldTeamName = this.playerTeams.get(playerId))) != null) {
                oldTeam.removeEntry(player.getName());
            }
            if ((team = this.scoreboard.getTeam(teamName = isVerified ? "verify_verified" : "verify_unverified")) != null) {
                team.addEntry(player.getName());
                this.playerTeams.put(playerId, teamName);
                String displayName = this.getDisplayNameForTeam(teamName) + player.getName();
                player.setPlayerListName(ChatColor.translateAlternateColorCodes((char)'&', (String)displayName));
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error setting team for " + player.getName() + ": " + e.toString());
        }
    }

    private String getDisplayNameForTeam(String teamName) {
        switch (teamName) {
            case "verify_verified": {
                return "&a\u2705 &f";
            }
            case "verify_unverified": {
                return "&c\u274c &f";
            }
            case "verify_pending": {
                return "&e\u23f3 &f";
            }
        }
        return "";
    }

    public void setPlayerVerified(Player player) {
        this.updatePlayerTeam(player, true);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Iterator iterator2 = Bukkit.getOnlinePlayers().iterator();
            while (iterator2.hasNext()) {
                Player onlinePlayer = (Player)iterator2.next();
                onlinePlayer.setScoreboard(this.scoreboard);
            }
            return;
        });
    }

    public void setPlayerUnverified(Player player) {
        this.updatePlayerTeam(player, false);
    }

    public void updateAllPlayers() {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Iterator iterator2 = Bukkit.getOnlinePlayers().iterator();
            while (iterator2.hasNext()) {
                Player player = (Player)iterator2.next();
                this.updatePlayerTab(player);
            }
            return;
        });
    }

    private void startUpdateTask() {
        new BukkitRunnable(){

            public void run() {
                VerifyTab.this.updateAllPlayers();
            }
        }.runTaskTimer((Plugin)this.plugin, 600L, 1200L);
    }

    public void onPlayerJoin(Player player) {
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            this.updatePlayerTab(player);
            player.setScoreboard(this.scoreboard);
        }, 20L);
    }

    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        if (this.playerTeams.containsKey(playerId)) {
            String teamName = this.playerTeams.get(playerId);
            Team team = this.scoreboard.getTeam(teamName);
            if (team != null) {
                team.removeEntry(player.getName());
            }
            this.playerTeams.remove(playerId);
        }
    }

    public void cleanup() {
        if (this.scoreboard != null) {
            for (String teamName : new String[]{"verify_verified", "verify_unverified", "verify_pending"}) {
                Team team = this.scoreboard.getTeam(teamName);
                if (team == null) continue;
                team.unregister();
            }
        }
        this.playerTeams.clear();
    }

    public void reloadConfig() {
        // Reinitialize scoreboard teams in case prefixes/colors changed in config
        try {
            this.initializeScoreboard();
            this.plugin.getLogger().info("VerifyTab: reloadConfig invoked");
        } catch (Exception e) {
            this.plugin.getLogger().warning("VerifyTab reload failed: " + e.toString());
        }
    }
}

