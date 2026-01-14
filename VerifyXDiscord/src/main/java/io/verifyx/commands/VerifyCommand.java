/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package io.verifyx.commands;

import io.verifyx.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VerifyCommand
implements CommandExecutor {
    private final Main plugin;

    public VerifyCommand(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args2) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("\u00a7cThis command can only be used by players!");
            return true;
        }
        Player player = (Player)sender;
        if (args2.length == 0) {
            player.sendMessage("\u00a76\u00a7lVerifyX \u00a78\u00bb \u00a77Usage: /verify <otp-code>");
            player.sendMessage("\u00a77Get your OTP code from the Discord verification channel");
            return true;
        }
        String otp = args2[0];
        player.sendMessage("\u00a76\u00a7lVerifyX \u00a78\u00bb \u00a77Please use the Discord bot to verify your OTP code!");
        player.sendMessage("\u00a77Go to the verification channel on Discord and enter: " + otp);
        return true;
    }
}

