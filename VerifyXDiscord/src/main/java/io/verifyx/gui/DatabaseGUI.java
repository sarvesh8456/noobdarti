/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package io.verifyx.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.verifyx.Main;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

public class DatabaseGUI {
    private final Main plugin;
    private final Player player;
    private Inventory inventory;
    private JsonArray verifications;
    private JsonArray otps;
    private String currentView = "verifications";
    private int currentPage = 0;
    private boolean closed = false;
    private boolean awaitingClearConfirm = false;
    private boolean awaitingPurgeAllConfirm = false;
    private static final int ITEMS_PER_PAGE = 45;

    public DatabaseGUI(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.createInventory();
        this.loadVerificationsData();
    }

    private void createInventory() {
        String cyberpunkTitle = "█▀█ ░░░ ◆◇◆◇◆ " + (this.currentView.equals("verifications") ? "⚡ VERIFICATIONS ⚡" : "⚡ OTPs ⚡") + " ◆◇◆◇◆ ░░░ █▀█";
        String title = String.valueOf(ChatColor.DARK_PURPLE) + cyberpunkTitle + String.valueOf(ChatColor.RESET) + " " + String.valueOf(ChatColor.LIGHT_PURPLE) + "【" + (this.currentPage + 1) + "/" + this.getTotalPages() + "】";
        this.inventory = Bukkit.createInventory(null, (int)54, (String)title);
    }

    private void loadVerificationsData() {
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Loading verifications...");
        CompletableFuture.runAsync(() -> {
            try {
                if (this.plugin.getDatabaseManager() != null) {
                    this.verifications = this.plugin.getDatabaseManager().getAllVerifications().join();
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                        if (this.verifications != null && this.verifications.size() > 0) {
                            this.player.sendMessage(String.valueOf(ChatColor.GREEN) + "Loaded " + this.verifications.size() + " verifications!");
                            this.updateDisplay();
                            if (this.player.getOpenInventory() != null && this.player.getOpenInventory().getTopInventory().equals((Object)this.inventory)) {
                                this.player.updateInventory();
                            }
                        } else {
                            this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "No verification data found.");
                            this.createNoDataGUI();
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + "Database manager not available!"));
                }
            }
            catch (Exception e) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + "Error loading data: " + e.toString()));
            }
        });
    }

    private void loadOTPsData() {
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Loading OTPs...");
        CompletableFuture.runAsync(() -> {
            try {
                if (this.plugin.getDatabaseManager() != null) {
                    this.otps = this.plugin.getDatabaseManager().getAllOTPs().join();
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                        if (this.otps != null && this.otps.size() > 0) {
                            this.player.sendMessage(String.valueOf(ChatColor.GREEN) + "Loaded " + this.otps.size() + " OTPs!");
                            this.updateDisplay();
                            if (this.player.getOpenInventory() != null && this.player.getOpenInventory().getTopInventory().equals((Object)this.inventory)) {
                                this.player.updateInventory();
                            }
                        } else {
                            this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "No OTP data found.");
                            this.createNoDataGUI();
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + "Database manager not available!"));
                }
            }
            catch (Exception e) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + "Error loading OTPs: " + e.toString()));
            }
        });
    }

    private void updateDisplay() {
        this.inventory.clear();
        if (this.currentView.equals("verifications")) {
            this.displayVerifications();
        } else {
            this.displayOTPs();
        }
        this.addNavigationButtons();
    }

    private void displayVerifications() {
        if (this.verifications == null) {
            return;
        }
        int startIndex = this.currentPage * 45;
        int endIndex = Math.min(startIndex + 45, this.verifications.size());
        int i = startIndex;
        while (i < endIndex) {
            JsonObject verification = this.verifications.get(i).getAsJsonObject();
            int slot = i - startIndex;
            ItemStack playerHead = this.createVerificationHead(verification, i);
            this.inventory.setItem(slot, playerHead);
            ++i;
        }
        return;
    }

    private void displayOTPs() {
        if (this.otps == null) {
            return;
        }
        int startIndex = this.currentPage * 45;
        int endIndex = Math.min(startIndex + 45, this.otps.size());
        int i = startIndex;
        while (i < endIndex) {
            JsonObject otp = this.otps.get(i).getAsJsonObject();
            int slot = i - startIndex;
            ItemStack otpItem = this.createOTPItem(otp, i);
            this.inventory.setItem(slot, otpItem);
            ++i;
        }
        return;
    }

    private ItemStack createVerificationHead(JsonObject verification, int index) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta)head.getItemMeta();
        String playerName = verification.get("player_name").getAsString();
        String discordName = verification.has("discord_name") ? verification.get("discord_name").getAsString() : "Unknown";
        String verifiedAt = verification.get("verified_at").getAsString();
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer((String)playerName));
        }
        catch (Exception exception) {
            // empty catch block
        }
        meta.setDisplayName(String.valueOf(ChatColor.GREEN) + playerName);
        ArrayList<Object> lore = new ArrayList<Object>();
        lore.add(String.valueOf(ChatColor.GRAY) + "Discord: " + String.valueOf(ChatColor.WHITE) + discordName);
        lore.add(String.valueOf(ChatColor.GRAY) + "Verified: " + String.valueOf(ChatColor.WHITE) + verifiedAt.substring(0, 10));
        if (verification.has("last_seen") && !verification.get("last_seen").isJsonNull()) {
            String lastSeen = verification.get("last_seen").getAsString();
            lore.add(String.valueOf(ChatColor.GRAY) + "Last Seen: " + String.valueOf(ChatColor.WHITE) + lastSeen.substring(0, 10));
        }
        lore.add("");
        lore.add(String.valueOf(ChatColor.YELLOW) + "Click to view details");
        lore.add(String.valueOf(ChatColor.DARK_GRAY) + "Index: " + index);
        meta.setLore((List<String>)(List<?>)lore);
        head.setItemMeta((ItemMeta)meta);
        return head;
    }

    private ItemStack createOTPItem(JsonObject otp, int index) {
        boolean isUsed = otp.has("used") && otp.get("used").getAsBoolean();
        boolean isExpired = otp.has("expired") && otp.get("expired").getAsBoolean();
        boolean bl = isExpired;
        Material material = isUsed ? Material.PAPER : (isExpired ? Material.CLOCK : Material.FEATHER);
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        String otpCode = otp.get("otp_code").getAsString();
        String playerName = otp.get("player_name").getAsString();
        String createdAt = otp.get("created_at").getAsString();
        ChatColor color = isUsed ? ChatColor.RED : (isExpired ? ChatColor.GRAY : ChatColor.GREEN);
        meta.setDisplayName(String.valueOf(color) + "OTP: " + otpCode);
        ArrayList<Object> lore = new ArrayList<Object>();
        lore.add(String.valueOf(ChatColor.GRAY) + "Player: " + String.valueOf(ChatColor.WHITE) + playerName);
        lore.add(String.valueOf(ChatColor.GRAY) + "Created: " + String.valueOf(ChatColor.WHITE) + createdAt.substring(0, 16));
        lore.add(String.valueOf(ChatColor.GRAY) + "Status: " + String.valueOf(this.getStatusColor(isUsed, isExpired)) + this.getStatusText(isUsed, isExpired));
        lore.add("");
        lore.add(String.valueOf(ChatColor.YELLOW) + "Click to view details");
        lore.add(String.valueOf(ChatColor.DARK_GRAY) + "Index: " + index);
        meta.setLore((List<String>)(List<?>)lore);
        item.setItemMeta(meta);
        return item;
    }

    private ChatColor getStatusColor(boolean isUsed, boolean isExpired) {
        if (isUsed) {
            return ChatColor.RED;
        }
        if (isExpired) {
            return ChatColor.GRAY;
        }
        return ChatColor.GREEN;
    }

    private String getStatusText(boolean isUsed, boolean isExpired) {
        if (isUsed) {
            return "Used";
        }
        if (isExpired) {
            return "Expired";
        }
        return "Active";
    }

    private void addNavigationButtons() {
        if (this.currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName(String.valueOf(ChatColor.LIGHT_PURPLE) + "◄════ " + String.valueOf(ChatColor.DARK_PURPLE) + String.valueOf(ChatColor.BOLD) + "PREVIOUS" + String.valueOf(ChatColor.RESET) + String.valueOf(ChatColor.LIGHT_PURPLE) + " ════►");
            prevMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.LIGHT_PURPLE) + "▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼",
                String.valueOf(ChatColor.AQUA) + "Navigate to previous page",
                String.valueOf(ChatColor.DARK_PURPLE) + "[⟲ CYBER-NAV-SYSTEM ⟲]",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲"
            ));
            prevButton.setItemMeta(prevMeta);
            this.inventory.setItem(45, prevButton);
        }
        // Ultra-vibrant Clear expired OTPs button
        ItemStack clearOtps = new ItemStack(this.awaitingClearConfirm ? Material.REDSTONE_BLOCK : Material.BARRIER);
        ItemMeta clearMeta = clearOtps.getItemMeta();
        if (this.awaitingClearConfirm) {
            clearMeta.setDisplayName(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "⚡⚡ PURGE OVERRIDE ⚡⚡");
            clearMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.DARK_RED) + "⟪═══════════════════⟫",
                String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "LEFT CLICK: INCINERATE",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "RIGHT CLICK: ABORT",
                String.valueOf(ChatColor.DARK_RED) + "⟪═══════════════════⟫"
            ));
        } else {
            clearMeta.setDisplayName(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "⊗ ◈ TOXIN PURGE ◈ ⊗");
            clearMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.RED) + "⟪═══════════════════⟫",
                String.valueOf(ChatColor.YELLOW) + String.valueOf(ChatColor.BOLD) + "⚡ ANNIHILATE EXPIRED ⚡",
                String.valueOf(ChatColor.DARK_GRAY) + "Non-expired codes SAFE",
                String.valueOf(ChatColor.RED) + "⟪═══════════════════⟫"
            ));
        }
        clearOtps.setItemMeta(clearMeta);
        this.inventory.setItem(46, clearOtps);

        // PURGE ALL OTPs button (non-expired)
        ItemStack purgeAllOtps = new ItemStack(this.awaitingPurgeAllConfirm ? Material.REDSTONE_BLOCK : Material.TNT);
        ItemMeta purgeAllMeta = purgeAllOtps.getItemMeta();
        if (this.awaitingPurgeAllConfirm) {
            purgeAllMeta.setDisplayName(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "⚡⚡ NUKE ALL OTPS ⚡⚡");
            purgeAllMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.DARK_RED) + "⟪═══════════════════⟫",
                String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "LEFT: OBLITERATE ALL",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "RIGHT: CANCEL",
                String.valueOf(ChatColor.DARK_RED) + "⟪═══════════════════⟫"
            ));
        } else {
            purgeAllMeta.setDisplayName(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "💣 PURGE ALL OTPs 💣");
            purgeAllMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.RED) + "⟪═══════════════════⟫",
                String.valueOf(ChatColor.YELLOW) + String.valueOf(ChatColor.BOLD) + "⚡ DELETE ALL ACTIVE ⚡",
                String.valueOf(ChatColor.DARK_GRAY) + "Non-expired codes ONLY",
                String.valueOf(ChatColor.RED) + "⟪═══════════════════⟫"
            ));
        }
        purgeAllOtps.setItemMeta(purgeAllMeta);
        this.inventory.setItem(51, purgeAllOtps);

        // Switch view with neon gradient
        ItemStack viewButton = new ItemStack(this.currentView.equals("verifications") ? Material.PLAYER_HEAD : Material.PAPER);
        ItemMeta viewMeta = viewButton.getItemMeta();
        if (this.currentView.equals("verifications")) {
            viewMeta.setDisplayName(String.valueOf(ChatColor.DARK_PURPLE) + String.valueOf(ChatColor.BOLD) + "⬡ " + String.valueOf(ChatColor.LIGHT_PURPLE) + "TRANSMIT OTP MODE" + String.valueOf(ChatColor.DARK_PURPLE) + " ⬡");
            viewMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.LIGHT_PURPLE) + "╔═══════════════════╗",
                String.valueOf(ChatColor.AQUA) + "Active: ⊙ VERIFICATION STREAM",
                String.valueOf(ChatColor.DARK_PURPLE) + "Switch ► OTP DATABASE",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "╚═══════════════════╝"
            ));
        } else {
            viewMeta.setDisplayName(String.valueOf(ChatColor.DARK_PURPLE) + String.valueOf(ChatColor.BOLD) + "⬡ " + String.valueOf(ChatColor.LIGHT_PURPLE) + "LOAD VERIF DATA" + String.valueOf(ChatColor.DARK_PURPLE) + " ⬡");
            viewMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.LIGHT_PURPLE) + "╔═══════════════════╗",
                String.valueOf(ChatColor.AQUA) + "Active: ⊙ OTP GENERATOR",
                String.valueOf(ChatColor.DARK_PURPLE) + "Switch ► VERIFICATION DB",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "╚═══════════════════╝"
            ));
        }
        viewButton.setItemMeta(viewMeta);
        this.inventory.setItem(47, viewButton);

        // Refresh with radical styling
        ItemStack refreshButton = new ItemStack(Material.SUNFLOWER);
        ItemMeta refreshMeta = refreshButton.getItemMeta();
        refreshMeta.setDisplayName(String.valueOf(ChatColor.YELLOW) + String.valueOf(ChatColor.BOLD) + "⟳⟲ SYNC MATRIX ⟲⟳");
        refreshMeta.setLore(Arrays.asList(
            String.valueOf(ChatColor.YELLOW) + "╔═══════════════════╗",
            String.valueOf(ChatColor.AQUA) + "LIVE CACHE RELOAD",
            String.valueOf(ChatColor.DARK_GRAY) + "Mode: " + String.valueOf(ChatColor.LIGHT_PURPLE) + this.currentView.toUpperCase(),
            String.valueOf(ChatColor.YELLOW) + "╚═══════════════════╝"
        ));
        refreshButton.setItemMeta(refreshMeta);
        this.inventory.setItem(48, refreshButton);

        // Exit with neon red
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "✕ ◈ DISCONNECT ◈ ✕");
        closeMeta.setLore(Arrays.asList(
            String.valueOf(ChatColor.RED) + "╔═══════════════════╗",
            String.valueOf(ChatColor.YELLOW) + "CLOSE DASHBOARD",
            String.valueOf(ChatColor.DARK_GRAY) + "Logs purged",
            String.valueOf(ChatColor.RED) + "╚═══════════════════╝"
        ));
        closeButton.setItemMeta(closeMeta);
        this.inventory.setItem(49, closeButton);

        // Page indicator with holographic effect
        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName(String.valueOf(ChatColor.LIGHT_PURPLE) + String.valueOf(ChatColor.BOLD) + "❖❖ PAGE " + (this.currentPage + 1) + "/" + this.getTotalPages() + " ❖❖");
        ArrayList<String> pageLore = new ArrayList<String>();
        int totalItems = this.currentView.equals("verifications") ? (this.verifications != null ? this.verifications.size() : 0) : (this.otps != null ? this.otps.size() : 0);
        pageLore.add(String.valueOf(ChatColor.LIGHT_PURPLE) + "┏━━━━━━━━━━━━━━━━━━┓");
        pageLore.add(String.valueOf(ChatColor.AQUA) + "Database Records: " + String.valueOf(ChatColor.YELLOW) + String.valueOf(ChatColor.BOLD) + totalItems);
        pageLore.add(String.valueOf(ChatColor.AQUA) + "Mode: " + String.valueOf(ChatColor.LIGHT_PURPLE) + String.valueOf(ChatColor.BOLD) + (this.currentView.equals("verifications") ? "VERIFICATION" : "OTP-GENERATOR"));
        pageLore.add(String.valueOf(ChatColor.LIGHT_PURPLE) + "┗━━━━━━━━━━━━━━━━━━┛");
        pageMeta.setLore(pageLore);
        pageInfo.setItemMeta(pageMeta);
        this.inventory.setItem(50, pageInfo);

        // Next button with mirror styling
        if (this.hasNextPage()) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName(String.valueOf(ChatColor.LIGHT_PURPLE) + "◄════ " + String.valueOf(ChatColor.DARK_PURPLE) + String.valueOf(ChatColor.BOLD) + "NEXT" + String.valueOf(ChatColor.RESET) + String.valueOf(ChatColor.LIGHT_PURPLE) + " ════►");
            nextMeta.setLore(Arrays.asList(
                String.valueOf(ChatColor.LIGHT_PURPLE) + "▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼▼",
                String.valueOf(ChatColor.AQUA) + "Navigate to next page",
                String.valueOf(ChatColor.DARK_PURPLE) + "[⟲ CYBER-NAV-SYSTEM ⟲]",
                String.valueOf(ChatColor.LIGHT_PURPLE) + "▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲"
            ));
            nextButton.setItemMeta(nextMeta);
            this.inventory.setItem(53, nextButton);
        }
    }

    private void createNoDataGUI() {
        this.inventory.clear();
        ItemStack noData = new ItemStack(Material.BARRIER);
        ItemMeta meta = noData.getItemMeta();
        meta.setDisplayName(String.valueOf(ChatColor.RED) + "No Data Available");
        meta.setLore(Arrays.asList(String.valueOf(ChatColor.GRAY) + "No " + this.currentView + " records found.", String.valueOf(ChatColor.GRAY) + (this.currentView.equals("verifications") ? "Use /verify in Discord to verify players." : "OTPs are generated when players join.")));
        noData.setItemMeta(meta);
        this.inventory.setItem(22, noData);
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(String.valueOf(ChatColor.YELLOW) + "Information");
        ArrayList<String> infoLore = new ArrayList<String>();
        infoLore.add(String.valueOf(ChatColor.GRAY) + "Current View: " + (this.currentView.equals("verifications") ? "Verifications" : "OTPs"));
        infoLore.add(String.valueOf(ChatColor.GRAY) + "Database: " + (this.plugin.getDatabaseManager() != null && this.plugin.getDatabaseManager().isConnected() ? "Connected" : "Disconnected"));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        this.inventory.setItem(31, info);
        this.addNavigationButtons();
    }

    private int getTotalPages() {
        int totalItems;
        if (this.currentView.equals("verifications")) {
            totalItems = this.verifications != null ? this.verifications.size() : 0;
        } else {
            int n = totalItems = this.otps != null ? this.otps.size() : 0;
        }
        if (totalItems == 0) {
            return 1;
        }
        return (int)Math.ceil((double)totalItems / 45.0);
    }

    private boolean hasNextPage() {
        int totalItems = this.currentView.equals("verifications") ? (this.verifications != null ? this.verifications.size() : 0) : (this.otps != null ? this.otps.size() : 0);
        return (this.currentPage + 1) * 45 < totalItems;
    }

    public void previousPage() {
        if (this.currentPage > 0) {
            --this.currentPage;
            this.updateDisplay();
            this.player.updateInventory();
        }
    }

    public void nextPage() {
        if (this.hasNextPage()) {
            ++this.currentPage;
            this.updateDisplay();
            this.player.updateInventory();
        }
    }

    public void openVerificationsView() {
        if (!this.currentView.equals("verifications")) {
            this.currentView = "verifications";
            this.currentPage = 0;
            this.awaitingClearConfirm = false;
            this.awaitingPurgeAllConfirm = false;
            if (this.verifications == null) {
                this.loadVerificationsData();
            } else {
                this.updateDisplay();
                this.player.updateInventory();
            }
        }
    }

    public void openOTPsView() {
        if (!this.currentView.equals("otps")) {
            this.currentView = "otps";
            this.currentPage = 0;
            this.awaitingClearConfirm = false;
            this.awaitingPurgeAllConfirm = false;
            if (this.otps == null) {
                this.loadOTPsData();
            } else {
                this.updateDisplay();
                this.player.updateInventory();
            }
        }
    }

    public void refresh() {
        this.currentPage = 0;
        if (this.currentView.equals("verifications")) {
            this.loadVerificationsData();
        } else {
            this.loadOTPsData();
        }
        this.player.sendMessage(String.valueOf(ChatColor.GREEN) + "Refreshing data...");
    }

    public void open() {
        this.player.openInventory(this.inventory);
        if (this.plugin.getGuiManager() != null) {
            this.plugin.getGuiManager().registerGUI(this.player.getUniqueId(), this);
        }
        this.closed = false;
    }

    public void close() {
        if (this.player.getOpenInventory() != null && this.player.getOpenInventory().getTopInventory().equals((Object)this.inventory)) {
            this.player.closeInventory();
        }
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void handleClick(int slot) {
        if (slot >= 0 && slot < 45) {
            int index = this.currentPage * 45 + slot;
            if (this.currentView.equals("verifications") && this.verifications != null && index < this.verifications.size()) {
                JsonObject verification = this.verifications.get(index).getAsJsonObject();
                this.showVerificationDetails(verification);
            } else if (this.currentView.equals("otps") && this.otps != null && index < this.otps.size()) {
                JsonObject otp = this.otps.get(index).getAsJsonObject();
                this.showOTPDetails(otp);
            }
        } else if (slot == 45 && this.currentPage > 0) {
            this.previousPage();
        } else if (slot == 53 && this.hasNextPage()) {
            this.nextPage();
        } else if (slot == 47) {
            if (this.currentView.equals("verifications")) {
                this.openOTPsView();
            } else {
                this.openVerificationsView();
            }
        } else if (slot == 46) {
            // Clear expired OTPs with confirmation
            if (!this.awaitingClearConfirm) {
                // First click: enter confirmation mode
                this.awaitingClearConfirm = true;
                this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡");
                this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "► TOXIN PURGE PROTOCOL INITIATED ◄");
                this.player.sendMessage(String.valueOf(ChatColor.RED) + "⚡ CLICK AGAIN TO INCINERATE ALL EXPIRED OTPs");
                this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚡ Switch views to ABORT");
                this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡");
                this.updateDisplay();
                this.player.updateInventory();
                // Spawn neon particles
                spawnNeonParticles(this.player, org.bukkit.Particle.REDSTONE, 30);
            } else {
                // Second click: confirm deletion
                this.awaitingClearConfirm = false;
                try {
                    if (this.plugin.getDatabaseManager() != null) {
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "▌▌▌ INITIATING PURGE ▌▌▌");
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + "Incinerating obsolete records...");
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        CompletableFuture.runAsync(() -> {
                            try {
                                int cleaned = this.plugin.getDatabaseManager().cleanupExpiredData().join();
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                    this.player.sendMessage(String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "⚡⚡⚡ PURGE COMPLETE ⚡⚡⚡");
                                    this.player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "✓ " + String.valueOf(ChatColor.GREEN) + cleaned + " EXPIRED RECORDS VAPORIZED");
                                    this.player.sendMessage(String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡⚡");
                                    spawnNeonParticles(this.player, org.bukkit.Particle.CRIT, 40);
                                    this.refresh();
                                });
                            } catch (Exception e) {
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "✗ PURGE PROTOCOL FAILED: " + e.toString()));
                            }
                        });
                    } else {
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + "✗ DATABASE OFFLINE - PURGE ABORTED");
                        this.awaitingClearConfirm = false;
                    }
                } catch (Exception e) {
                    this.player.sendMessage(String.valueOf(ChatColor.RED) + "✗ ERROR: " + e.toString());
                    this.awaitingClearConfirm = false;
                }
            }
        } else if (slot == 51) {
            // Purge ALL non-expired OTPs with confirmation
            if (!this.awaitingPurgeAllConfirm) {
                // First click: enter confirmation mode
                this.awaitingPurgeAllConfirm = true;
                this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "█████████████████████████");
                this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "⚠ CRITICAL: PURGE ALL OTPs ⚠");
                this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "This will DELETE ALL ACTIVE OTPs!");
                this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Players will NOT be able to verify via Discord!");
                this.player.sendMessage(String.valueOf(ChatColor.RED) + "Click again to CONFIRM obliteration");
                this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "█████████████████████████");
                this.updateDisplay();
                this.player.updateInventory();
                // Spawn intense red particles
                spawnNeonParticles(this.player, org.bukkit.Particle.REDSTONE, 50);
            } else {
                // Second click: confirm deletion of ALL OTPs
                this.awaitingPurgeAllConfirm = false;
                try {
                    if (this.plugin.getDatabaseManager() != null) {
                        this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "⚙⚙⚙ NUCLEAR OPTION ACTIVATED ⚙⚙⚙");
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + "Vaporizing all active OTP records...");
                        CompletableFuture.runAsync(() -> {
                            try {
                                int purged = this.plugin.getDatabaseManager().purgeAllOTPs().join();
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                    this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "💣💣💣 TOTAL ANNIHILATION 💣💣💣");
                                    this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "OBLITERATED: " + String.valueOf(ChatColor.YELLOW) + purged + " OTP RECORDS");
                                    this.player.sendMessage(String.valueOf(ChatColor.RED) + "All players' OTPs are now VOID");
                                    this.player.sendMessage(String.valueOf(ChatColor.DARK_RED) + String.valueOf(ChatColor.BOLD) + "💣💣💣💣💣💣💣💣💣💣💣💣");
                                    spawnNeonParticles(this.player, org.bukkit.Particle.CRIT, 60);
                                    this.refresh();
                                });
                            } catch (Exception e) {
                                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.player.sendMessage(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "✗ OBLITERATION FAILED: " + e.toString()));
                            }
                        });
                    } else {
                        this.player.sendMessage(String.valueOf(ChatColor.RED) + "✗ DATABASE OFFLINE - PURGE ABORTED");
                        this.awaitingPurgeAllConfirm = false;
                    }
                } catch (Exception e) {
                    this.player.sendMessage(String.valueOf(ChatColor.RED) + "✗ ERROR: " + e.toString());
                    this.awaitingPurgeAllConfirm = false;
                }
            }
        } else if (slot == 48) {
            this.awaitingClearConfirm = false;
            this.awaitingPurgeAllConfirm = false;
            this.refresh();
        } else if (slot == 49) {
            this.awaitingClearConfirm = false;
            this.awaitingPurgeAllConfirm = false;
            this.close();
        }
    }

    private void showVerificationDetails(JsonObject verification) {
        String playerName = verification.get("player_name").getAsString();
        String playerUUID = verification.get("player_uuid").getAsString();
        String discordName = verification.has("discord_name") ? verification.get("discord_name").getAsString() : "Unknown";
        String discordId = verification.has("discord_id") ? verification.get("discord_id").getAsString() : "Unknown";
        String verifiedAt = verification.get("verified_at").getAsString();
        this.player.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Verification Details ===");
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Player: " + String.valueOf(ChatColor.GREEN) + playerName);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "UUID: " + String.valueOf(ChatColor.AQUA) + playerUUID);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Discord: " + String.valueOf(ChatColor.WHITE) + discordName);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Discord ID: " + String.valueOf(ChatColor.WHITE) + discordId);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Verified: " + String.valueOf(ChatColor.WHITE) + verifiedAt);
        if (verification.has("last_seen") && !verification.get("last_seen").isJsonNull()) {
            String lastSeen = verification.get("last_seen").getAsString();
            this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Last Seen: " + String.valueOf(ChatColor.WHITE) + lastSeen);
        }
        this.player.sendMessage("");
        this.player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Commands:");
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "/verifyadmin unverify " + playerName);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "/verifyadmin check " + playerName);
    }

    private void showOTPDetails(JsonObject otp) {
        String otpCode = otp.get("otp_code").getAsString();
        String playerName = otp.get("player_name").getAsString();
        String createdAt = otp.get("created_at").getAsString();
        boolean isUsed = otp.has("used") && otp.get("used").getAsBoolean();
        boolean isExpired = otp.has("expired") && otp.get("expired").getAsBoolean();
        this.player.sendMessage(String.valueOf(ChatColor.GOLD) + "=== OTP Details ===");
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "OTP Code: " + String.valueOf(ChatColor.GREEN) + otpCode);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Player: " + String.valueOf(ChatColor.WHITE) + playerName);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Created: " + String.valueOf(ChatColor.WHITE) + createdAt);
        this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Status: " + String.valueOf(this.getStatusColor(isUsed, isExpired)) + this.getStatusText(isUsed, isExpired));
        if (otp.has("used_at") && !otp.get("used_at").isJsonNull()) {
            String usedAt = otp.get("used_at").getAsString();
            this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Used At: " + String.valueOf(ChatColor.WHITE) + usedAt);
        }
        if (otp.has("discord_id") && !otp.get("discord_id").isJsonNull()) {
            String discordId = otp.get("discord_id").getAsString();
            this.player.sendMessage(String.valueOf(ChatColor.GRAY) + "Discord ID: " + String.valueOf(ChatColor.WHITE) + discordId);
        }
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    public String getCurrentView() {
        return this.currentView;
    }

    public Player getPlayer() {
        return this.player;
    }

    private void spawnNeonParticles(Player player, org.bukkit.Particle particle, int count) {
        try {
            org.bukkit.Location loc = player.getLocation().add(0, 1, 0);
            if (particle == org.bukkit.Particle.REDSTONE) {
                // Red NEON MAGENTA particles for warning - intense visual
                org.bukkit.Particle.DustOptions redNeon = new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 200), 2.0f);
                // Spiral pattern
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    double distance = 1.2;
                    double x = Math.cos(angle) * distance;
                    double z = Math.sin(angle) * distance;
                    double y = Math.sin(i * 0.5) * 0.5;
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, y + 0.5, z), 2, redNeon);
                }
                // Additional burst effect
                for (int i = 0; i < count / 2; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double x = Math.cos(angle) * Math.random();
                    double z = Math.sin(angle) * Math.random();
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, 1.0, z), 1, redNeon);
                }
            } else if (particle == org.bukkit.Particle.CRIT) {
                // Green SUCCESS particles - vibrant celebration
                for (int i = 0; i < count; i++) {
                    double angle = (Math.PI * 2 * i) / count;
                    double distance = 1.5;
                    double x = Math.cos(angle) * distance;
                    double z = Math.sin(angle) * distance;
                    double y = Math.sin(i * 0.3) * 0.7;
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, y + 1.2, z), 3);
                }
                // Burst outward
                for (int i = 0; i < count / 2; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double distance = 0.5 + Math.random();
                    double x = Math.cos(angle) * distance;
                    double z = Math.sin(angle) * distance;
                    double y = Math.random();
                    player.getWorld().spawnParticle(particle, loc.clone().add(x, y + 1.0, z), 2);
                }
            }
        } catch (Exception ignored) {}
    }
}

