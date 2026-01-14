/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 */
package io.verifyx.gui;

import io.verifyx.Main;
import io.verifyx.gui.DatabaseGUI;
import io.verifyx.gui.VerificationDashboard;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class GUIManager
implements Listener {
    private final Main plugin;
    private final Map<UUID, DatabaseGUI> openGUIs = new HashMap<UUID, DatabaseGUI>();
    private final Map<UUID, VerificationDashboard> openDashboards = new HashMap<UUID, VerificationDashboard>();

    public GUIManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
    }

    public void registerGUI(UUID playerId, DatabaseGUI gui) {
        this.openGUIs.put(playerId, gui);
    }

    public void unregisterGUI(UUID playerId) {
        this.openGUIs.remove(playerId);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals((Object)gui.getInventory())) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    gui.handleClick(event.getSlot());
                }
            } else if (event.getView().getTopInventory().equals((Object)gui.getInventory()) && !event.getClickedInventory().equals((Object)player.getInventory())) {
                event.setCancelled(true);
            }
        }
        VerificationDashboard dash = this.openDashboards.get(player.getUniqueId());
        if (dash != null) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals((Object)dash.getInventory())) {
                event.setCancelled(true);
                if (event.getCurrentItem() != null) {
                    try {
                        dash.handleClick(event.getSlot());
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } else if (event.getView().getTopInventory().equals((Object)dash.getInventory()) && !event.getClickedInventory().equals((Object)player.getInventory())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null && event.getInventory().equals((Object)gui.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player)event.getPlayer();
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null && event.getInventory().equals((Object)gui.getInventory())) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (this.openGUIs.containsKey(player.getUniqueId())) {
                    DatabaseGUI currentGUI = this.openGUIs.get(player.getUniqueId());
                    if (event.getInventory().equals((Object)currentGUI.getInventory())) {
                        this.openGUIs.remove(player.getUniqueId());
                    }
                }
            });
        }
        VerificationDashboard dash = this.openDashboards.get(player.getUniqueId());
        if (dash != null && event.getInventory().equals((Object)dash.getInventory())) {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (this.openDashboards.containsKey(player.getUniqueId())) {
                    VerificationDashboard current = this.openDashboards.get(player.getUniqueId());
                    if (event.getInventory().equals((Object)current.getInventory())) {
                        this.openDashboards.remove(player.getUniqueId());
                    }
                }
            });
        }
    }

    public void openDatabaseGUI(Player player) {
        if (this.openGUIs.containsKey(player.getUniqueId())) {
            DatabaseGUI existingGUI = this.openGUIs.get(player.getUniqueId());
            existingGUI.close();
            this.openGUIs.remove(player.getUniqueId());
        }
        DatabaseGUI gui = new DatabaseGUI(this.plugin, player);
        this.openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }

    public void openVerificationDashboard(Player player) {
        if (this.openDashboards.containsKey(player.getUniqueId())) {
            VerificationDashboard existing = this.openDashboards.get(player.getUniqueId());
            existing = null;
            this.openDashboards.remove(player.getUniqueId());
        }
        VerificationDashboard dash = new VerificationDashboard(this.plugin, player);
        this.openDashboards.put(player.getUniqueId(), dash);
        dash.open();
    }

    public void openDatabaseGUI(Player player, String view) {
        if (this.openGUIs.containsKey(player.getUniqueId())) {
            DatabaseGUI existingGUI = this.openGUIs.get(player.getUniqueId());
            existingGUI.close();
            this.openGUIs.remove(player.getUniqueId());
        }
        DatabaseGUI gui = new DatabaseGUI(this.plugin, player);
        if (view.equalsIgnoreCase("otps")) {
            gui.openOTPsView();
        } else {
            gui.openVerificationsView();
        }
        this.openGUIs.put(player.getUniqueId(), gui);
        gui.open();
    }

    public void closeAllGUIs() {
        Iterator<Map.Entry<UUID, DatabaseGUI>> iterator2 = this.openGUIs.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<UUID, DatabaseGUI> entry = iterator2.next();
            UUID playerId = entry.getKey();
            DatabaseGUI gui = entry.getValue();
            Player player = Bukkit.getPlayer((UUID)playerId);
            if (player != null && player.isOnline() && player.getOpenInventory() != null && gui.getInventory().equals((Object)player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
            gui.close();
            iterator2.remove();
        }
        Iterator<Map.Entry<UUID, VerificationDashboard>> it2 = this.openDashboards.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<UUID, VerificationDashboard> entry = it2.next();
            UUID playerId = entry.getKey();
            VerificationDashboard dash = entry.getValue();
            Player player = Bukkit.getPlayer((UUID)playerId);
            if (player != null && player.isOnline() && player.getOpenInventory() != null && dash.getInventory().equals((Object)player.getOpenInventory().getTopInventory())) {
                player.closeInventory();
            }
            // no explicit close method on dashboard; remove reference
            it2.remove();
        }
        return;
    }

    public void closePlayerGUI(Player player) {
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null) {
            gui.close();
            this.openGUIs.remove(player.getUniqueId());
        }
        VerificationDashboard dash = this.openDashboards.get(player.getUniqueId());
        if (dash != null) {
            this.openDashboards.remove(player.getUniqueId());
        }
    }

    public void refreshPlayerGUI(Player player) {
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null) {
            gui.refresh();
        }
        VerificationDashboard dash = this.openDashboards.get(player.getUniqueId());
        if (dash != null) {
            try {
                dash.open();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public void cleanupInactiveGUIs() {
        Iterator<Map.Entry<UUID, DatabaseGUI>> iterator2 = this.openGUIs.entrySet().iterator();
        int cleaned = 0;
        while (iterator2.hasNext()) {
            Map.Entry<UUID, DatabaseGUI> entry = iterator2.next();
            UUID playerId = entry.getKey();
            DatabaseGUI gui = entry.getValue();
            Player player = Bukkit.getPlayer((UUID)playerId);
            if (player == null || !player.isOnline()) {
                iterator2.remove();
                ++cleaned;
                continue;
            }
            if (gui.isClosed()) {
                iterator2.remove();
                ++cleaned;
                continue;
            }
            if (player.getOpenInventory() != null && gui.getInventory().equals((Object)player.getOpenInventory().getTopInventory())) continue;
            iterator2.remove();
            ++cleaned;
        }
        if (cleaned > 0 && this.plugin.getLogger() != null) {
            this.plugin.getLogger().info("Cleaned up " + cleaned + " inactive GUIs");
        }
        // cleanup dashboards
        Iterator<Map.Entry<UUID, VerificationDashboard>> it3 = this.openDashboards.entrySet().iterator();
        int cleanedDash = 0;
        while (it3.hasNext()) {
            Map.Entry<UUID, VerificationDashboard> entry = it3.next();
            UUID playerId = entry.getKey();
            VerificationDashboard dash = entry.getValue();
            Player player = Bukkit.getPlayer((UUID)playerId);
            if (player == null || !player.isOnline()) {
                it3.remove();
                ++cleanedDash;
                continue;
            }
            if (player.getOpenInventory() != null && dash.getInventory().equals((Object)player.getOpenInventory().getTopInventory())) continue;
            it3.remove();
            ++cleanedDash;
        }
        if (cleanedDash > 0 && this.plugin.getLogger() != null) {
            this.plugin.getLogger().info("Cleaned up " + cleanedDash + " inactive dashboards");
        }
    }

    public DatabaseGUI getGUI(Player player) {
        return this.openGUIs.get(player.getUniqueId());
    }

    public boolean hasGUI(Player player) {
        return this.openGUIs.containsKey(player.getUniqueId());
    }

    public void removeGUI(Player player) {
        this.openGUIs.remove(player.getUniqueId());
    }

    public void startCleanupTask() {
        new BukkitRunnable(){

            public void run() {
                GUIManager.this.cleanupInactiveGUIs();
            }
        }.runTaskTimer((Plugin)this.plugin, 1200L, 6000L);
    }

    public int getOpenGUICount() {
        return this.openGUIs.size();
    }

    public void reloadConfig() {
        // Live reload hook: update open GUIs and dashboards to pick up new colors/icons
        this.plugin.getLogger().info("GUIManager: reloadConfig invoked - updating open GUIs");
        for (Map.Entry<UUID, DatabaseGUI> e : new HashMap<>(this.openGUIs).entrySet()) {
            try {
                DatabaseGUI gui = e.getValue();
                if (gui != null && !gui.isClosed()) {
                    gui.refresh();
                }
            } catch (Exception ignored) {}
        }
        for (Map.Entry<UUID, VerificationDashboard> e : new HashMap<>(this.openDashboards).entrySet()) {
            try {
                VerificationDashboard dash = e.getValue();
                if (dash != null) {
                    // reopen to re-render with new theme
                    try { dash.open(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    public void updatePlayerGUI(Player player) {
        DatabaseGUI gui = this.openGUIs.get(player.getUniqueId());
        if (gui != null && player.getOpenInventory() != null) {
            player.updateInventory();
        }
    }
}

