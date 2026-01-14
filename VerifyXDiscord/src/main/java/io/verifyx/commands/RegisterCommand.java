package io.verifyx.commands;

import io.verifyx.Main;
import io.verifyx.security.LoginSecurityManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {
    private final Main plugin;
    public RegisterCommand(Main plugin) { this.plugin = plugin; }

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
        // Bedrock players authenticate via Microsoft; disallow password registration
        try {
            String xuid = this.plugin.getVerificationManager() != null ? this.plugin.getVerificationManager().getFloodgateXuid(p) : null;
            if (xuid != null) {
                p.sendMessage("\u00a7eBedrock accounts use Microsoft authentication and do not support /register.");
                return true;
            }
        } catch (Exception ignored) {}
        if (args.length < 2) {
            p.sendMessage("\u00a7eUsage: /register <password> <password>");
            return true;
        }
        String a = args[0];
        String b = args[1];
        if (!a.equals(b)) {
            p.sendMessage("\u00a7cPasswords do not match.");
            return true;
        }
        LoginSecurityManager mgr = this.plugin.getLoginSecurityManager();
        if (mgr == null) {
            p.sendMessage("\u00a7cLogin manager not available.");
            return true;
        }
        mgr.register(p, a).thenAccept(ok -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (ok) {
                    p.sendMessage("\u00a7aRegistration successful. You are now logged in.");
                    try { mgr.unfreezePlayer(p); } catch (Exception ignored) {}
                } else {
                    p.sendMessage("\u00a7cRegistration failed (already registered or internal error).");
                }
            });
        });
        return true;
    }
}
