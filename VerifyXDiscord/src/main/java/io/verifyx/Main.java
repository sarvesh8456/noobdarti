/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package io.verifyx;

import io.verifyx.DatabaseManager;
import io.verifyx.DiscordBot;
import io.verifyx.OTPManager;
import io.verifyx.PlayerListener;
import io.verifyx.VerificationManager;
import io.verifyx.commands.AISecurityCommand;
import io.verifyx.commands.AdminCommands;
import io.verifyx.gui.GUIManager;
import io.verifyx.models.ConfigData;
import io.verifyx.security.AISecurityManager;
import io.verifyx.tab.VerifyTab;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Main
extends JavaPlugin {
    private static Main instance;
    private ConfigData configData;
    private DatabaseManager databaseManager;
    private OTPManager otpManager;
    private VerificationManager verificationManager;
    private DiscordBot discordBot;
    private GUIManager guiManager;
    private VerifyTab verifyTab;
    private AISecurityManager aiSecurityManager;
    private io.verifyx.util.IPTracker ipTracker;
    private io.verifyx.security.LoginSecurityManager loginSecurityManager;
    private boolean managersInitialized = false;

    public io.verifyx.security.LoginSecurityManager getLoginSecurityManager() {
        return this.loginSecurityManager;
    }

    public void onEnable() {
        instance = this;
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }
        this.saveDefaultConfig();
        this.configData = this.loadConfigData();
        if (this.configData == null) {
            this.getLogger().severe("\u274c Failed to load configuration!");
            Bukkit.getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.getLogger().info("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        this.getLogger().info("  \u2705 VerifyXDiscord v1.1.0 ENABLED    ");
        this.getLogger().info("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
        this.getLogger().info("  \ud83d\udcca Database: Initializing...        ");
        this.getLogger().info("  \ud83e\udd16 Discord Bot: Starting...         ");
        this.getLogger().info("  \ud83d\udcf1 GUI Manager: Loading...          ");
        this.getLogger().info("  \ud83d\udc64 Tab Manager: Loading...          ");
        this.getLogger().info("  \ud83d\udee1\ufe0f AI Security: Loading...          ");
        this.getLogger().info("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        this.initializeManagers();
        Objects.requireNonNull(this.getCommand("verifyadmin")).setExecutor((CommandExecutor)new AdminCommands(this));
        if (this.getCommand("register") != null) {
            Objects.requireNonNull(this.getCommand("register")).setExecutor((CommandExecutor)new io.verifyx.commands.RegisterCommand(this));
        }
        if (this.getCommand("login") != null) {
            Objects.requireNonNull(this.getCommand("login")).setExecutor((CommandExecutor)new io.verifyx.commands.LoginCommand(this));
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.registerAISecurityCommand(), 80L);
        this.scheduleDelayedTasks();
    }

    private void registerAISecurityCommand() {
        this.getLogger().info("\ud83d\udd04 Attempting to register /aisecurity command...");
        if (this.aiSecurityManager == null) {
            this.getLogger().warning("\u26a0\ufe0f AI Security Manager is null, trying again in 2 seconds...");
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.registerAISecurityCommand(), 40L);
            return;
        }
        try {
            Objects.requireNonNull(this.getCommand("aisecurity")).setExecutor((CommandExecutor)new AISecurityCommand(this, this.aiSecurityManager));
            this.getLogger().info("\u2705 /aisecurity command registered successfully!");
            if (this.getCommand("security") != null) {
                Objects.requireNonNull(this.getCommand("security")).setExecutor((CommandExecutor)new AISecurityCommand(this, this.aiSecurityManager));
                this.getLogger().info("\u2705 /security alias registered successfully!");
            }
        }
        catch (NullPointerException e) {
            this.getLogger().severe("\u274c Failed to register /aisecurity command - command not found in plugin.yml!");
            this.getLogger().severe("\u274c Make sure 'aisecurity:' is defined in your plugin.yml file!");
        }
        catch (Exception e) {
            this.getLogger().severe("\u274c Error registering /aisecurity command: " + e.toString());
        }
    }

    private void scheduleDelayedTasks() {
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.startDiscordBot(), 40L);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.managersInitialized) {
                Bukkit.getPluginManager().registerEvents((Listener)new PlayerListener(this), (Plugin)this);
                this.getLogger().info("\u2705 Event listeners registered");
            }
        }, 80L);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.managersInitialized) {
                this.initializeVerifyTab();
            }
        }, 60L);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.managersInitialized) {
                this.initializeAISecurityManager();
            }
        }, 80L);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.startMaintenanceTasks(), 100L);
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.printStartupStats(), 200L);
        // Start live config watcher to apply config changes without manual reload
        Bukkit.getScheduler().runTaskLaterAsynchronously((Plugin)this, this::startConfigWatcher, 300L);
    }

    // Watch config.yml in the plugin data folder and apply live reloads when it changes
    // WatchService used by the config watcher so it can be closed on shutdown
    private java.nio.file.WatchService configWatcher = null;

    private void startConfigWatcher() {
        try {
            java.nio.file.Path dir = this.getDataFolder().toPath();
            if (!java.nio.file.Files.exists(dir)) return;
            final java.nio.file.Path configFile = dir.resolve("config.yml");
            this.configWatcher = dir.getFileSystem().newWatchService();
            this.configWatcher = dir.getFileSystem().newWatchService();
            dir.register(this.configWatcher, java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);
            this.getLogger().info("Config watcher started for: " + configFile.toString());
            // Run watcher loop async
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
                while (this.isEnabled()) {
                    try {
                        java.nio.file.WatchKey key = this.configWatcher.take();
                        for (java.nio.file.WatchEvent<?> event : key.pollEvents()) {
                            java.nio.file.Path changed = (java.nio.file.Path)event.context();
                            if (changed != null && changed.getFileName().toString().equalsIgnoreCase("config.yml")) {
                                this.getLogger().info("Detected config.yml change, applying live reload...");
                                try {
                                    // Schedule sync task to reload config and notify managers
                                    Bukkit.getScheduler().runTask((Plugin)this, () -> {
                                        try {
                                            this.reloadConfig();
                                            this.configData = this.loadConfigData();
                                            if (this.getVerificationManager() != null) this.getVerificationManager().reloadConfig();
                                            if (this.getAISecurityManager() != null) this.getAISecurityManager().reloadConfig();
                                            if (this.getOtpManager() != null) {
                                                try {
                                                    this.getOtpManager().reloadConfig();
                                                } catch (Throwable ignored) {}
                                            }
                                            if (this.getDatabaseManager() != null) {
                                                try {
                                                    this.getDatabaseManager().reloadConfig();
                                                } catch (Throwable ignored) {}
                                            }
                                            if (this.getGuiManager() != null) {
                                                try {
                                                    this.getGuiManager().reloadConfig();
                                                } catch (Throwable ignored) {}
                                            }
                                            if (this.getVerifyTab() != null) {
                                                try {
                                                    this.getVerifyTab().reloadConfig();
                                                } catch (Throwable ignored) {}
                                            }
                                            if (this.discordBot != null) {
                                                try {
                                                    this.discordBot.reloadConfig();
                                                } catch (Throwable ignored) {}
                                            }
                                            this.getLogger().info("Live reload applied");
                                        } catch (Exception e) {
                                            this.getLogger().warning("Error applying live reload: " + e.toString());
                                        }
                                    });
                                } catch (Exception e) {
                                    this.getLogger().warning("Failed to schedule live reload task: " + e.toString());
                                }
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) break;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (java.nio.file.ClosedWatchServiceException cwse) {
                        // Watcher closed during shutdown - exit cleanly
                        break;
                    } catch (Exception ex) {
                        this.getLogger().warning("Config watcher loop error: " + ex.toString());
                    }
                }
                try { 
                    if (this.configWatcher != null) {
                        try { this.configWatcher.close(); } catch (Exception ignored) {}
                        this.configWatcher = null;
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            this.getLogger().warning("Failed to start config watcher: " + e.toString());
        }
    }

    private void initializeManagers() {
        try {
            // Initialize IP Tracker first (simple file-based tracking)
            this.ipTracker = new io.verifyx.util.IPTracker(this);
            this.getLogger().info("\u2705 IP Tracker initialized");
            
            this.databaseManager = new DatabaseManager(this, this.configData);
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
                try {
                    boolean connected = this.waitForConnection(10);
                    if (!connected) {
                        this.getLogger().severe("\u274c Database connection failed!");
                        return;
                    }
                    try {
                        this.aiSecurityManager = new AISecurityManager(this);
                        this.getLogger().info("\u2705 AI Security Manager initialized");
                        Bukkit.getScheduler().runTask((Plugin)this, () -> {
                            if (this.getCommand("aisecurity") != null && this.getCommand("aisecurity").getExecutor() == null) {
                                this.registerAISecurityCommand();
                            }
                        });
                    }
                    catch (NoClassDefFoundError e) {
                        this.getLogger().warning("\u26a0\ufe0f ProtocolLib not found - AI Security running in basic mode");
                        this.aiSecurityManager = null;
                    }
                    catch (Exception e) {
                        this.getLogger().warning("\u26a0\ufe0f Error creating AI Security Manager: " + e.toString());
                        this.aiSecurityManager = null;
                    }
                    this.otpManager = new OTPManager(this, this.databaseManager);
                    if (this.aiSecurityManager != null) {
                        this.otpManager.setAISecurityManager(this.aiSecurityManager);
                    } else {
                        this.getLogger().info("\u26a0\ufe0f AI Security disabled - OTP generation will work without AI checks");
                    }
                    this.verificationManager = new VerificationManager(this, this.databaseManager, this.otpManager, this.aiSecurityManager);
                    // Initialize LoginSecurityManager
                    this.loginSecurityManager = new io.verifyx.security.LoginSecurityManager(this);
                    this.guiManager = new GUIManager(this);
                    this.managersInitialized = true;
                    this.getLogger().info("\u2705 All managers initialized successfully!");
                }
                catch (Exception e) {
                    this.getLogger().log(Level.SEVERE, "\u274c Error initializing managers: " + e.toString(), e);
                }
            });
        }
        catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "\u274c Error creating database manager: " + e.toString(), e);
        }
    }

    private void initializeVerifyTab() {
        try {
            this.verifyTab = new VerifyTab(this);
            this.getLogger().info("\u2705 VerifyTab manager initialized!");
            Bukkit.getScheduler().runTaskLater((Plugin)this, () -> this.verifyTab.updateAllPlayers(), 20L);
        }
        catch (Exception e) {
            this.getLogger().warning("\u26a0\ufe0f Failed to initialize VerifyTab: " + e.toString());
        }
    }

    private void initializeAISecurityManager() {
        try {
            if (this.aiSecurityManager != null) {
                this.getLogger().info("\u2705 AI Security Manager ready!");
                Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                    if (this.aiSecurityManager != null) {
                        this.aiSecurityManager.startMonitoring();
                        this.getLogger().info("\u2705 AI Security monitoring started");
                    }
                }, 40L);
            }
        }
        catch (Exception e) {
            this.getLogger().warning("\u26a0\ufe0f Failed to initialize AI Security Manager: " + e.toString());
        }
    }

    private boolean waitForConnection(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeout2 = (long)timeoutSeconds * 1000L;
        while (System.currentTimeMillis() - startTime < timeout2) {
            if (this.databaseManager != null && this.databaseManager.isConnected()) {
                this.getLogger().info("\u2705 Database connection established!");
                return true;
            }
            try {
                Thread.sleep(500L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return this.databaseManager != null && this.databaseManager.isConnected();
    }

    private ConfigData loadConfigData() {
        try {
            ConfigData config = new ConfigData();
            this.reloadConfig();
            FileConfiguration bukkitConfig = this.getConfig();
            config.setSupabaseUrl(bukkitConfig.getString("supabase.url", ""));
            config.setSupabaseServiceKey(bukkitConfig.getString("supabase.service-role-key", ""));
            config.setDiscordBotToken(bukkitConfig.getString("discord.bot-token", ""));
            config.setDiscordGuildId(bukkitConfig.getString("discord.guild-id", ""));
            config.setDiscordVerifyChannelId(bukkitConfig.getString("discord.verify-channel-id", ""));
            config.setDiscordVerifiedRoleId(bukkitConfig.getString("discord.verified-role-id", ""));
            config.setOtpLength(bukkitConfig.getInt("otp.length", 6));
            config.setOtpCharset(bukkitConfig.getString("otp.charset", "0123456789"));
            config.setOtpExpiryMinutes(bukkitConfig.getInt("otp.expiry-minutes", 10));
            config.setMaxOtpAttempts(bukkitConfig.getInt("otp.max-attempts", 3));
            config.setAttemptCooldownSeconds(bukkitConfig.getInt("otp.cooldown-seconds", 60));
            config.setAISecurityEnabled(bukkitConfig.getBoolean("ai-security.enabled", true));
            config.setAITrustThreshold(bukkitConfig.getInt("ai-security.trust-threshold", 85));
            config.setAIBlockThreshold(bukkitConfig.getInt("ai-security.block-threshold", 95));
            config.setAIKickThreshold(bukkitConfig.getInt("ai-security.kick-threshold", 70));
            config.setMaxAltsPerIP(bukkitConfig.getInt("ai-security.max-alts-per-ip", 2));
            config.setPreventAltOTP(bukkitConfig.getBoolean("ai-security.prevent-alt-otp", true));
            this.getLogger().info("\u2705 Configuration loaded from YAML");
            this.getLogger().info("\ud83d\udd10 OTP Length: " + config.getOtpLength());
            this.getLogger().info("\ud83d\udd10 OTP Charset: " + config.getOtpCharset());
            this.getLogger().info("\u23f1\ufe0f OTP Expiry: " + config.getOtpExpiryMinutes() + " minutes");
            return config;
        }
        catch (Exception e) {
            this.getLogger().severe("\u274c Error loading configuration: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private void startDiscordBot() {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
            try {
                if (this.configData.getDiscordBotToken() == null || this.configData.getDiscordBotToken().isEmpty()) {
                    this.getLogger().warning("\u26a0\ufe0f Discord bot token not set, skipping bot initialization");
                    return;
                }
                this.discordBot = new DiscordBot(this);
                this.discordBot.start();
                this.getLogger().info("\u2705 Discord bot started");
            }
            catch (Exception e) {
                this.getLogger().log(Level.SEVERE, "\u274c Failed to start Discord bot: " + e.toString(), e);
            }
        });
    }

    private void startMaintenanceTasks() {
        if (this.databaseManager != null && this.managersInitialized) {
            Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this, () -> {
                try {
                    if (this.databaseManager.isConnected()) {
                        this.databaseManager.cleanupExpiredOTPs();
                    }
                }
                catch (Exception e) {
                    this.getLogger().warning("\u26a0\ufe0f Error during OTP cleanup: " + e.toString());
                }
            }, 1200L, 6000L);
            Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
                if (this.guiManager != null) {
                    this.guiManager.cleanupInactiveGUIs();
                }
            }, 1200L, 12000L);
            Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this, () -> {
                if (this.aiSecurityManager != null) {
                    this.aiSecurityManager.cleanup();
                }
            }, 1200L, 18000L);
            this.getLogger().info("\u2705 Maintenance tasks scheduled");
            return;
        }
        this.getLogger().warning("\u26a0\ufe0f Managers not ready for maintenance tasks");
    }

    private void printStartupStats() {
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
            try {
                int verifiedCount = 0;
                int pendingOTPs = 0;
                boolean dbConnected = false;
                boolean discordConnected = false;
                int flaggedAlts = 0;
                boolean aiSecurityRegistered = false;
                if (this.databaseManager != null && (dbConnected = this.databaseManager.isConnected())) {
                    verifiedCount = this.databaseManager.getVerifiedCount().join();
                    pendingOTPs = this.databaseManager.getPendingOTPCount().join();
                }
                if (this.discordBot != null) {
                    discordConnected = this.discordBot.isConnected();
                }
                if (this.aiSecurityManager != null) {
                    flaggedAlts = this.aiSecurityManager.getFlaggedPlayers().size();
                }
                if (this.getCommand("aisecurity") != null && this.getCommand("aisecurity").getExecutor() != null) {
                    aiSecurityRegistered = true;
                }
                this.getLogger().info("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
                this.getLogger().info("  \ud83d\udcca SYSTEM STATUS                   ");
                this.getLogger().info("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500");
                this.getLogger().info("  \ud83d\udcca Database: " + (dbConnected ? "\u2705 CONNECTED           " : "\u274c DISCONNECTED        "));
                this.getLogger().info("  \u2705 Verified Players: " + String.format("%-8d", verifiedCount) + "        ");
                this.getLogger().info("  \ud83d\udd10 Active OTPs: " + String.format("%-11d", pendingOTPs) + "        ");
                this.getLogger().info("  \ud83e\udd16 Discord Bot: " + (discordConnected ? "\u2705 CONNECTED         " : "\u274c DISCONNECTED      "));
                this.getLogger().info("  \ud83d\udcf1 GUI Manager: " + (this.guiManager != null ? "\u2705 READY            " : "\u274c NOT READY        "));
                this.getLogger().info("  \ud83d\udc64 Tab Manager: " + (this.verifyTab != null ? "\u2705 READY            " : "\u274c NOT READY        "));
                this.getLogger().info("  \ud83d\udee1\ufe0f AI Security: " + (this.aiSecurityManager != null ? "\u2705 READY         " : "\u274c NOT READY       "));
                this.getLogger().info("  \ud83d\udeab Flagged Alts: " + String.format("%-10d", flaggedAlts) + "        ");
                this.getLogger().info("  \ud83d\udcdd /aisecurity: " + (aiSecurityRegistered ? "\u2705 REGISTERED       " : "\u274c NOT REGISTERED   "));
                this.getLogger().info("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error printing startup stats: " + e.toString());
            }
        });
    }

    public void onDisable() {
        this.getLogger().info("Shutting down VerifyXDiscord...");

        // Cancel any scheduled Bukkit tasks for this plugin (ensures timers stop)
        try {
            org.bukkit.Bukkit.getScheduler().cancelTasks((Plugin)this);
            this.getLogger().info("\u2705 Cancelled scheduled plugin tasks");
        } catch (Exception ignored) {}

        // Close config watcher to stop its blocking thread
        try {
            if (this.configWatcher != null) {
                try { this.configWatcher.close(); } catch (Exception ignored) {}
                this.configWatcher = null;
                this.getLogger().info("\u2705 Config watcher stopped");
            }
        } catch (Exception e) {
            this.getLogger().warning("\u26a0\ufe0f Error closing config watcher: " + e.toString());
        }

        if (this.aiSecurityManager != null) {
            try {
                this.aiSecurityManager.cleanup();
                this.getLogger().info("\u2705 AI Security Manager cleaned up");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error cleaning up AI Security Manager: " + e.toString());
            }
        }
        if (this.verifyTab != null) {
            try {
                this.verifyTab.cleanup();
                this.getLogger().info("\u2705 VerifyTab cleaned up");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error cleaning up VerifyTab: " + e.toString());
            }
        }
        if (this.guiManager != null) {
            try {
                this.guiManager.closeAllGUIs();
                this.getLogger().info("\u2705 All GUIs closed");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error closing GUIs: " + e.toString());
            }
        }
        if (this.discordBot != null) {
            try {
                this.discordBot.shutdown();
                this.getLogger().info("\u2705 Discord bot shutdown");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error shutting down Discord bot: " + e.toString());
            }
        }
        if (this.databaseManager != null) {
            try {
                this.databaseManager.close();
                this.getLogger().info("\u2705 Database connections closed");
            }
            catch (Exception e) {
                this.getLogger().warning("\u26a0\ufe0f Error closing database: " + e.toString());
            }
        }
        this.getLogger().info("\u2705 VerifyXDiscord shutdown complete");
    }

    public ConfigData getConfigData() {
        return this.configData;
    }

    public OTPManager getOtpManager() {
        return this.otpManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public VerificationManager getVerificationManager() {
        return this.verificationManager;
    }

    public DiscordBot getDiscordBot() {
        return this.discordBot;
    }

    public GUIManager getGuiManager() {
        return this.guiManager;
    }

    public VerifyTab getVerifyTab() {
        return this.verifyTab;
    }

    public AISecurityManager getAISecurityManager() {
        return this.aiSecurityManager;
    }

    public io.verifyx.util.IPTracker getIPTracker() {
        return this.ipTracker;
    }

    public boolean areManagersInitialized() {
        return this.managersInitialized;
    }

    public static Main getInstance() {
        return instance;
    }
}

