package io.ashlord.commands;

import io.ashlord.AshlordManager;
import io.ashlord.AshlordPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AshlordCommand implements CommandExecutor {
    private final AshlordPlugin plugin;

    public AshlordCommand(AshlordPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /ashlord check|reload");
            return true;
        }
        String sub = args[0].toLowerCase();
        AshlordManager manager = plugin.getManager();
        switch (sub) {
            case "check":
                manager.runCheck(plugin.getEconomy());
                sender.sendMessage("Ashlord check executed.");
                return true;
            case "reload":
                manager.reloadConfig();
                sender.sendMessage("Ashlord config reloaded.");
                return true;
            default:
                sender.sendMessage("Usage: /ashlord check|reload");
                return true;
        }
    }
}
