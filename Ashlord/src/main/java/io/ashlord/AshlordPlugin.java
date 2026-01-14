package io.ashlord;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import io.ashlord.commands.AshlordCommand;

import java.util.logging.Level;

public class AshlordPlugin extends JavaPlugin {
    private Economy econ;
    private AshlordManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().log(Level.WARNING, "Vault (or an economy plugin) not found - Ashlord checks will be disabled until an economy provider is available.");
        }

        manager = new AshlordManager(this);

        int intervalSeconds = getConfig().getInt("interval-seconds", 300);
        long ticks = Math.max(1, intervalSeconds) * 20L; // schedule on main thread

        // schedule repeating task
        Bukkit.getScheduler().runTaskTimer(this, () -> manager.runCheck(econ), 20L, ticks);

        // command
        getCommand("ashlord").setExecutor(new AshlordCommand(this));

        getLogger().info("Ashlord plugin enabled.");
    }

    @Override
    public void onDisable() {
        manager.clear();
        getLogger().info("Ashlord plugin disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() { return econ; }
    public AshlordManager getManager() { return manager; }
}
