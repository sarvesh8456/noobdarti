/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 */
package io.verifyx;

import io.verifyx.Main;
import io.verifyx.VerificationManager;
import io.verifyx.tab.VerifyTab;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener
implements Listener {
    private final Main plugin;
    private final VerificationManager verificationManager;
    private VerifyTab verifyTab;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
        this.verificationManager = plugin.getVerificationManager();
        if (plugin.getVerifyTab() != null) {
            this.verifyTab = plugin.getVerifyTab();
        } else {
            plugin.getLogger().warning("VerifyTab not available - tab features disabled");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        if (this.verificationManager == null) {
            this.plugin.getLogger().warning("VerificationManager not ready for player join: " + event.getPlayer().getName());
            return;
        }
        if (this.verifyTab != null) {
            this.verifyTab.onPlayerJoin(event.getPlayer());
        }
        // Run legacy verification join handling
        this.verificationManager.handlePlayerJoin(event.getPlayer());
        // New login security enforcement
        try {
            if (this.plugin.getLoginSecurityManager() != null) {
                this.plugin.getLoginSecurityManager().checkOnJoin(event.getPlayer());
            }
        } catch (Exception ignored) {}
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        if (this.verifyTab != null) {
            this.verifyTab.onPlayerQuit(event.getPlayer());
        }
        try {
            org.bukkit.entity.Player p = event.getPlayer();
            if (this.plugin.getIPTracker() != null) {
                this.plugin.getIPTracker().recordPlayerLocation(p.getName(), p.getUniqueId().toString(), p.getLocation());
                String norm = p.getName() != null ? p.getName().replaceAll("^[\\.\\s]+", "") : null;
                if (norm != null && !norm.equals(p.getName())) {
                    this.plugin.getIPTracker().recordPlayerLocation(norm, p.getUniqueId().toString(), p.getLocation());
                }
            }
            try {
                if (this.plugin.getVerificationManager() != null) {
                    this.plugin.getVerificationManager().cancelPendingForUUID(p.getUniqueId().toString());
                }
            } catch (Exception ignored) {}            try {
                if (this.plugin.getLoginSecurityManager() != null) {
                    this.plugin.getLoginSecurityManager().onPlayerQuit(p);
                }
            } catch (Exception ignored) {}        } catch (Exception e) {
            this.plugin.getLogger().warning("Error recording player location on quit: " + e.toString());
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        org.bukkit.entity.Player p = event.getPlayer();
        try {
            boolean frozen = this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(p.getUniqueId().toString());
            boolean loginBlocked = this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(p.getUniqueId().toString());
            if (frozen || loginBlocked) {
                event.setTo(event.getFrom());
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        org.bukkit.entity.Player p = event.getPlayer();
        try {
            boolean frozen = this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(p.getUniqueId().toString());
            boolean loginBlocked = this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(p.getUniqueId().toString());
            if (frozen || loginBlocked) {
                event.setCancelled(true);
                if (loginBlocked) p.sendMessage("\u00a7ePlease login using \u00a7a/login <password> \u00a7eor register with \u00a7a/register <password> <password>\u00a7e. This message is only visible to you.");
                else p.sendMessage("\u00a7eYour session is pending confirmation. Please check your DMs.");
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        org.bukkit.entity.Player p = event.getPlayer();
        try {
            boolean frozen = this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(p.getUniqueId().toString());
            boolean loginBlocked = this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(p.getUniqueId().toString());
            if (frozen || loginBlocked) {
                String cmd = event.getMessage().toLowerCase();
                // allow login/register commands when blocked
                if (cmd.startsWith("/login") || cmd.startsWith("/register") || cmd.startsWith("/verify") || (p.hasPermission("verifyx.admin") && cmd.startsWith("/verifyadmin")) || (p.hasPermission("verifyx.admin") && cmd.startsWith("/loginsecurity"))) {
                    return;
                }
                event.setCancelled(true);
                if (loginBlocked) p.sendMessage("\u00a7eYou must login or register first. Use /login <password> or /register <password> <password>.");
                else p.sendMessage("\u00a7eYour session is pending confirmation. Commands are disabled until confirmed.");
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        org.bukkit.entity.Player p = event.getPlayer();
        try {
            boolean frozen = this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(p.getUniqueId().toString());
            boolean loginBlocked = this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(p.getUniqueId().toString());
            if (frozen || loginBlocked) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        try {
            if (event.getEntity() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player p = (org.bukkit.entity.Player)event.getEntity();
                if (this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(p.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
                if (this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(p.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        try {
            if (event.getDamager() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player damager = (org.bukkit.entity.Player)event.getDamager();
                if (this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(damager.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
                if (this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(damager.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
            }
            if (event.getEntity() instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player target = (org.bukkit.entity.Player)event.getEntity();
                if (this.plugin.getLoginSecurityManager() != null && this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && !this.plugin.getLoginSecurityManager().isAuthenticated(target.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
                if (this.plugin.getVerificationManager() != null && this.plugin.getVerificationManager().isPlayerFrozen(target.getUniqueId().toString())) {
                    event.setCancelled(true);
                }
            }
        } catch (Exception ignored) {}
    }
}

