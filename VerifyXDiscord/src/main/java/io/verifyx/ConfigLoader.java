/*
 * Decompiled with CFR.
 */
package io.verifyx;

import io.verifyx.models.ConfigData;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigLoader {
    private static final Logger logger = Logger.getLogger("VerifyX");

    /*
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static ConfigData loadConfig(File configFile) {
        ConfigData config = new ConfigData();
        try (FileInputStream input = new FileInputStream(configFile);){
            Properties props = new Properties();
            props.load(input);
            config.setSupabaseUrl(props.getProperty("supabase.url", ""));
            config.setSupabaseServiceKey(props.getProperty("supabase.service-role-key", ""));
            config.setDiscordBotToken(props.getProperty("discord.bot-token", ""));
            config.setDiscordGuildId(props.getProperty("discord.guild-id", ""));
            config.setDiscordVerifyChannelId(props.getProperty("discord.verify-channel-id", ""));
            config.setDiscordVerifiedRoleId(props.getProperty("discord.verified-role-id", ""));
            config.setOtpLength(Integer.parseInt(props.getProperty("otp.length", "6")));
            config.setOtpCharset(props.getProperty("otp.charset", "0123456789"));
            config.setOtpExpiryMinutes(Integer.parseInt(props.getProperty("otp.expiry-minutes", "10")));
            config.setMaxOtpAttempts(Integer.parseInt(props.getProperty("otp.max-attempts", "3")));
            config.setAttemptCooldownSeconds(Integer.parseInt(props.getProperty("otp.cooldown-seconds", "60")));
            config.setGuiTitle(props.getProperty("gui.title", "&aVerify Your Account"));
            config.setGuiRows(Integer.parseInt(props.getProperty("gui.rows", "3")));
            config.setAISecurityEnabled(Boolean.parseBoolean(props.getProperty("ai-security.enabled", "true")));
            config.setAITrustThreshold(Integer.parseInt(props.getProperty("ai-security.trust-threshold", "85")));
            config.setAIBlockThreshold(Integer.parseInt(props.getProperty("ai-security.block-threshold", "95")));
            config.setAIKickThreshold(Integer.parseInt(props.getProperty("ai-security.kick-threshold", "70")));
            config.setAIWarningThreshold(Integer.parseInt(props.getProperty("ai-security.warning-threshold", "50")));
            config.setMaxAltsPerIP(Integer.parseInt(props.getProperty("ai-security.max-alts-per-ip", "2")));
            config.setPreventAltOTP(Boolean.parseBoolean(props.getProperty("ai-security.prevent-alt-otp", "true")));
            config.setAIAutoBlock(Boolean.parseBoolean(props.getProperty("ai-security.auto-block-alts", "true")));
            config.setAIDetectAltClusters(Boolean.parseBoolean(props.getProperty("ai-security.detect-alt-clusters", "true")));
            config.setAILearningMode(Boolean.parseBoolean(props.getProperty("ai-security.learning-mode", "true")));
            config.setAIEnterpriseMode(Boolean.parseBoolean(props.getProperty("ai-security.enterprise-mode", "true")));
            config.setAIRequireMultipleFactors(Boolean.parseBoolean(props.getProperty("ai-security.require-multiple-factors", "true")));
            config.setAIMinDataPoints(Integer.parseInt(props.getProperty("ai-security.min-data-points", "50")));
            config.setAIMaxFalsePositiveRate(Integer.parseInt(props.getProperty("ai-security.max-false-positive-rate", "2")));
            config.setAIFingerprintDevices(Boolean.parseBoolean(props.getProperty("ai-security.fingerprint-devices", "true")));
            config.setAIAnalyzeMovement(Boolean.parseBoolean(props.getProperty("ai-security.analyze-movement", "true")));
            config.setAIAnalyzeClicks(Boolean.parseBoolean(props.getProperty("ai-security.analyze-clicks", "true")));
            config.setAIDetectVPNProxy(Boolean.parseBoolean(props.getProperty("ai-security.detect-vpn-proxy", "true")));
            config.setAiMaxAltsPerIP(Integer.parseInt(props.getProperty("ai-security.max-alts-per-ip", "2")));
            config.setAICheckIPReputation(Boolean.parseBoolean(props.getProperty("ai-security.check-ip-reputation", "true")));
            config.setAIMonitorSuspicious(Boolean.parseBoolean(props.getProperty("ai-security.monitor-suspicious", "true")));
            config.setNotifyAdmins(Boolean.parseBoolean(props.getProperty("ai-security.notify-admins", "true")));
            config.setAIDetectionThreshold(Integer.parseInt(props.getProperty("ai-security.detection-threshold", "75")));
            config.setAIWarningTime(Integer.parseInt(props.getProperty("ai-security.warning-time", "120")));
            if (config.getSupabaseUrl().isEmpty() || config.getSupabaseServiceKey().isEmpty() || config.getDiscordBotToken().isEmpty()) {
                logger.severe("\u274c Missing required configuration values!");
                logger.severe("\u274c Required: supabase.url, supabase.service-role-key, discord.bot-token");
                ConfigData configData = null;
                return configData;
            }
            logger.info("\u2705 Configuration loaded successfully");
            logger.info("\ud83d\udd10 OTP Config:");
            logger.info("  Length: " + config.getOtpLength());
            logger.info("  Charset: " + config.getOtpCharset());
            logger.info("  Expiry: " + config.getOtpExpiryMinutes() + " minutes");
            if (config.isAISecurityEnabled()) {
                logger.info("\u2705 ENTERPRISE AI SECURITY ENABLED");
                logger.info("   Trust Threshold: " + config.getAITrustThreshold() + "%");
                logger.info("   Block Threshold: " + config.getAIBlockThreshold() + "%");
                logger.info("   Learning Mode: " + (config.isAILearningMode() ? "ACTIVE" : "INACTIVE"));
                logger.info("   Max Alts/IP: " + config.getMaxAltsPerIP());
            } else {
                logger.info("\u26a0\ufe0f AI Security: DISABLED");
            }
            ConfigData configData = config;
            return configData;
        }
        catch (NumberFormatException e) {
            logger.severe("\u274c Error parsing numeric configuration value: " + e.toString());
            logger.severe("\u274c Please check all numeric values in config.yml");
            return null;
        }
        catch (Exception e) {
            logger.severe("\u274c Error loading config: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    public static void createDefaultConfig(File configFile, File dataFolder) {
        if (!configFile.exists()) {
            try {
                logger.info("\ud83d\udcc4 Creating default configuration file...");
                String defaultConfig = "# ========================================\n# VerifyX Discord Verification Plugin Config\n# ========================================\n\n# Supabase Configuration\nsupabase.url: \"https://your-project.supabase.co\"\nsupabase.service-role-key: \"your-service-role-key-here\"\n\n# Discord Bot Configuration\ndiscord.bot-token: \"your-bot-token-here\"\ndiscord.guild-id: \"your-discord-server-id\"\ndiscord.verify-channel-id: \"verification-channel-id\"\ndiscord.verified-role-id: \"verified-role-id\"\n\n# ========== OTP SETTINGS ==========\notp.length: 6\notp.charset: \"0123456789\"  # You can customize this\notp.expiry-minutes: 10\notp.max-attempts: 3\notp.cooldown-seconds: 60\n\n# GUI Settings\ngui.title: \"&aVerify Your Account\"\ngui.rows: 3\n\n# ========== ENTERPRISE AI SECURITY ==========\n# Main toggle\nai-security.enabled: true\nai-security.learning-mode: true\nai-security.enterprise-mode: true\nai-security.require-multiple-factors: true\n\n# Thresholds\nai-security.trust-threshold: 85\nai-security.block-threshold: 95\nai-security.kick-threshold: 70\nai-security.warning-threshold: 50\n\n# Data requirements\nai-security.min-data-points: 50\nai-security.max-false-positive-rate: 2\nai-security.session-history-size: 100\nai-security.behavioral-samples: 200\n\n# Alt detection\nai-security.max-alts-per-ip: 2\nai-security.prevent-alt-otp: true\nai-security.auto-block-alts: true\nai-security.detect-alt-clusters: true\n\n# Device fingerprinting\nai-security.fingerprint-devices: true\nai-security.track-hardware-changes: true\nai-security.detect-bot-devices: true\n\n# Behavioral analysis\nai-security.analyze-movement: true\nai-security.analyze-clicks: true\nai-security.analyze-chat: true\nai-security.analyze-commands: true\n\n# Network analysis\nai-security.detect-vpn-proxy: true\nai-security.check-ip-reputation: true\nai-security.analyze-network-patterns: true\n\n# Monitoring\nai-security.monitor-suspicious: true\nai-security.auto-start-monitoring: true\nai-security.monitor-interval-seconds: 30\nai-security.detailed-logging: false\n\n# Notifications\nai-security.notify-admins: true\nai-security.notify-discord: false\nai-security.discord-webhook: \"\"\nai-security.notify-on-block: true\nai-security.notify-on-kick: true\n\n# Auto actions\nai-security.auto-block-botnets: true\nai-security.auto-block-malware-ips: true\nai-security.auto-block-known-vpns: true\nai-security.auto-kick-suspicious: true\n\n# Whitelist integration\nai-security.bypass-whitelisted: true\nai-security.trusted-players-bypass: true\nai-security.admin-bypass: true\n\n# ========== LEGACY SETTINGS ==========\n# Backward compatibility\nai-security.detection-threshold: 75\nai-security.warning-time: 120\nai-security.auto-block: true\nai-security.notify-admins: true\n\n# Notes:\n# 1. trust-threshold: 85%+ confidence required to take action\n# 2. block-threshold: 95%+ for permanent blocking\n# 3. kick-threshold: 70%+ for auto-kick\n# 4. learning-mode: AI learns from patterns over time\n# 5. enterprise-mode: Full enterprise-grade security features\n# 6. require-multiple-factors: Need 2+ risk factors to block\n";
                Files.write(configFile.toPath(), defaultConfig.getBytes(), new OpenOption[0]);
                logger.info("\u2705 Default configuration created at: " + configFile.getPath());
                logger.info("\u26a0\ufe0f Please edit config.yml with your actual values!");
            }
            catch (Exception e) {
                logger.severe("\u274c Failed to create default config: " + e.toString());
            }
        }
    }

    public static ConfigData reloadConfig(File configFile) {
        logger.info("\ud83d\udd04 Reloading configuration...");
        return ConfigLoader.loadConfig(configFile);
    }

    public static boolean validateConfig(ConfigData config) {
        if (config == null) {
            return false;
        }
        ArrayList<String> errors = new ArrayList<String>();
        if (config.getSupabaseUrl().isEmpty()) {
            errors.add("supabase.url is empty");
        }
        if (config.getSupabaseServiceKey().isEmpty()) {
            errors.add("supabase.service-role-key is empty");
        }
        if (config.getDiscordBotToken().isEmpty()) {
            errors.add("discord.bot-token is empty");
        }
        if (config.getOtpLength() < 4 || config.getOtpLength() > 8) {
            errors.add("OTP length must be between 4 and 8 digits");
        }
        if (config.getOtpExpiryMinutes() < 1 || config.getOtpExpiryMinutes() > 60) {
            errors.add("OTP expiry must be between 1 and 60 minutes");
        }
        if (config.getMaxOtpAttempts() < 1 || config.getMaxOtpAttempts() > 10) {
            errors.add("Max OTP attempts must be between 1 and 10");
        }
        if (config.getOtpCharset() == null || config.getOtpCharset().isEmpty()) {
            errors.add("OTP charset cannot be empty");
        }
        if (config.getAITrustThreshold() < 0 || config.getAITrustThreshold() > 100) {
            errors.add("AI trust threshold must be between 0 and 100");
        }
        if (config.getAIBlockThreshold() < 0 || config.getAIBlockThreshold() > 100) {
            errors.add("AI block threshold must be between 0 and 100");
        }
        if (config.getAIKickThreshold() < 0 || config.getAIKickThreshold() > 100) {
            errors.add("AI kick threshold must be between 0 and 100");
        }
        if (config.getMaxAltsPerIP() < 1 || config.getMaxAltsPerIP() > 10) {
            errors.add("Max alts per IP must be between 1 and 10");
        }
        if (errors.isEmpty()) {
            logger.info("\u2705 Configuration validated successfully");
            return true;
        }
        logger.severe("\u274c Configuration validation failed:");
        Iterator iterator2 = errors.iterator();
        while (iterator2.hasNext()) {
            String error = (String)iterator2.next();
            logger.severe("   - " + error);
        }
        return false;
    }
}

