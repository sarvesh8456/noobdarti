package io.ashlord;

import net.milkbowl.vault.economy.Economy;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.UUID;

public class AshlordManager {
    private final AshlordPlugin plugin;
    private final Scoreboard board;
    private final Team ashlordTeam;
    private UUID currentHolder = null;

    public AshlordManager(AshlordPlugin plugin) {
        this.plugin = plugin;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        this.board = manager.getMainScoreboard();
        Team t = board.getTeam("ashlord");
        if (t == null) {
            t = board.registerNewTeam("ashlord");
        }
        this.ashlordTeam = t;
        applyPrefixFromConfig();
    }

    public void runCheck(Economy econ) {
        try {
            if (econ == null) {
                plugin.getLogger().warning("Economy provider not available; skipping Ashlord check.");
                return;
            }

            Player top = null;
            double best = Double.NEGATIVE_INFINITY;

            for (Player p : Bukkit.getOnlinePlayers()) {
                double bal = econ.getBalance(p);
                if (bal > best) {
                    best = bal;
                    top = p;
                }
            }

            if (top == null) {
                // no online players
                removeCurrent();
                return;
            }

            if (currentHolder == null || !currentHolder.equals(top.getUniqueId())) {
                // new holder
                assign(top);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error running Ashlord check: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyPrefixFromConfig() {
        String hex = plugin.getConfig().getString("ashlord-color", "FF2D2D");
        String text = plugin.getConfig().getString("prefix-text", "Ashlord");
        String legacyPrefix = ChatColor.of("#" + hex) + "" + ChatColor.BOLD + text + " ";
        ashlordTeam.setDisplayName(legacyPrefix);
        ashlordTeam.setPrefix(legacyPrefix);
        ashlordTeam.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
    }

    private void assign(Player p) {
        // remove old
        removeCurrent();

        // add to team
        ashlordTeam.addEntry(p.getName());
        // set player list name with legacy codes for compatibility
        String hex = plugin.getConfig().getString("ashlord-color", "FF2D2D");
        String colored = ChatColor.of("#" + hex) + "" + ChatColor.BOLD + p.getName();
        p.setPlayerListName(colored);

        currentHolder = p.getUniqueId();

        if (plugin.getConfig().getBoolean("announce", true)) {
            p.sendMessage("You are now the " + plugin.getConfig().getString("prefix-text", "Ashlord") + "!");
        }

        plugin.getLogger().info("Assigned Ashlord to " + p.getName());
    }

    private void removeCurrent() {
        if (currentHolder == null) return;
        Player old = Bukkit.getPlayer(currentHolder);
        if (old != null) {
            ashlordTeam.removeEntry(old.getName());
            old.setPlayerListName(old.getName());
            if (plugin.getConfig().getBoolean("announce", true)) {
                old.sendMessage("You are no longer the " + plugin.getConfig().getString("prefix-text", "Ashlord") + ".");
            }
        } else {
            // offline: remove entry by name if exists
            for (String entry : ashlordTeam.getEntries()) {
                // entries are player names; try to remove if matches uuid? skip
            }
        }
        currentHolder = null;
        plugin.getLogger().info("Removed Ashlord from previous holder.");
    }

    public void clear() {
        // remove team entries but keep prefix
        for (String e : ashlordTeam.getEntries()) {
            ashlordTeam.removeEntry(e);
        }
        currentHolder = null;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        applyPrefixFromConfig();
    }
}
