package io.verifyx.commands;

import io.verifyx.Main;
import io.verifyx.security.LoginSecurityManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {
    private final Main plugin;

    public LoginCommand(Main plugin) { this.plugin = plugin; }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cThis command can only be used by players.");
            return true;
        }
        Player p = (Player)sender;
        if (!this.plugin.getConfig().getBoolean("loginsecurity.enabled", false)) {
            p.sendMessage("\u00a7cLogin security is disabled.");
            return true;
        }
        // Bedrock players authenticate via Microsoft; disallow /login
        try {
            String xuid = this.plugin.getVerificationManager() != null ? this.plugin.getVerificationManager().getFloodgateXuid(p) : null;
            if (xuid != null) {
                p.sendMessage("\u00a7eBedrock accounts use Microsoft authentication and do not support /login.");
                return true;
            }
        } catch (Exception ignored) {}
        if (args.length < 1) {
            p.sendMessage("\u00a7eUsage: /login <password>");
            return true;
        }
        String pass = args[0];
        LoginSecurityManager mgr = this.plugin.getLoginSecurityManager();
        if (mgr == null) {
            p.sendMessage("\u00a7cLogin manager not available.");
            return true;
        }
        if (mgr.isAuthenticated(p.getUniqueId().toString())) {
            p.sendMessage("\u00a7aYou are already logged in.");
            return true;
        }
        mgr.login(p, pass).thenAccept(ok -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (ok) {
                    p.sendMessage("\u00a7aLogin successful. Welcome back!");
                } else {
                    p.sendMessage("\u00a7cLogin failed. Check your password.");
                }
            });
        });
        return true;
    }
}
