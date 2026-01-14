package io.verifyx.gui;

import io.verifyx.Main;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class VerificationDashboard {
    private final Main plugin;
    private final Player player;
    private Inventory inventory;

    public VerificationDashboard(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.createInventory();
        this.refreshData();
    }

    private void createInventory() {
        // Read live colors from config (ChatColor names)
        ChatColor primary = ChatColor.DARK_PURPLE;
        ChatColor accent = ChatColor.AQUA;
        try {
            String pc = this.plugin.getConfig().getString("gui.primary-color", "DARK_PURPLE");
            String ac = this.plugin.getConfig().getString("gui.accent-color", "AQUA");
            primary = ChatColor.valueOf(pc);
            accent = ChatColor.valueOf(ac);
        } catch (Exception ignored) {}

        this.inventory = Bukkit.createInventory(null, 27, primary + "⟣ VerifyX Dashboard ⟢");
        // Center info panel
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(accent + "≛ Loading... ≛");
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        info.setItemMeta(meta);
        this.inventory.setItem(13, info);

        // Cyberpunk styled DB opener
        ItemStack openDb = new ItemStack(Material.ENDER_CHEST);
        ItemMeta dbMeta = openDb.getItemMeta();
        dbMeta.setDisplayName(accent + "⟁ Database Viewer ⟁");
        dbMeta.setLore(java.util.Arrays.asList(primary + "Cyberpunk UI", ChatColor.GRAY + "Click to open database viewer"));
        dbMeta.addEnchant(Enchantment.LURE, 1, true);
        dbMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        openDb.setItemMeta(dbMeta);
        this.inventory.setItem(22, openDb);
    }

    private void refreshData() {
        CompletableFuture.runAsync(() -> {
            int verified = 0;
            int pending = 0;
            try {
                if (this.plugin.getDatabaseManager() != null) {
                    verified = this.plugin.getDatabaseManager().getVerifiedCount().join();
                    pending = this.plugin.getDatabaseManager().getPendingOTPCount().join();
                }
            } catch (Exception e) {
                // ignore
            }
            final int v = verified;
            final int p = pending;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                ItemStack info = new ItemStack(Material.PAPER);
                ItemMeta meta = info.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + "Verified: " + ChatColor.WHITE + v);
                meta.setLore(java.util.Arrays.asList(ChatColor.GRAY + "Pending OTPs: " + ChatColor.WHITE + p, "", ChatColor.YELLOW + "Click chest to open DB viewer"));
                info.setItemMeta(meta);
                this.inventory.setItem(13, info);
            });
        });
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void handleClick(int slot) {
        if (slot == 22) {
            // Open database GUI
            try {
                if (this.plugin.getGuiManager() != null) {
                    this.plugin.getGuiManager().openDatabaseGUI(this.player);
                }
            } catch (Exception e) {
                // ignore
            }
        } else if (slot == 13) {
            // refresh data
            this.refreshData();
        }
    }
}
