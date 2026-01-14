/*
 * Decompiled with CFR.
 */
package io.verifyx.models;

public class ConfigData {
    private String supabaseUrl;
    private String supabaseServiceKey;
    private int otpLength = 6;
    private String otpCharset = "0123456789";
    private int maxOtpAttempts = 3;
    private int otpExpiryMinutes = 10;
    private int attemptCooldownSeconds = 60;
    private String discordBotToken;
    private String discordGuildId;
    private String discordVerifyChannelId;
    private String discordVerifiedRoleId;
    private String guiTitle = "&aVerify Your Account";
    private int guiRows = 3;
    private boolean aiSecurityEnabled = true;
    private boolean aiLearningMode = true;
    private boolean aiEnterpriseMode = true;
    private boolean aiRequireMultipleFactors = true;
    private int aiTrustThreshold = 85;
    private int aiBlockThreshold = 95;
    private int aiKickThreshold = 70;
    private int aiWarningThreshold = 50;
    private int aiMinDataPoints = 50;
    private int aiMaxFalsePositiveRate = 2;
    private int maxAltsPerIP = 2;
    private boolean preventAltOTP = true;
    private boolean aiAutoBlockAlts = true;
    private boolean aiDetectAltClusters = true;
    private boolean aiFingerprintDevices = true;
    private boolean aiTrackHardwareChanges = true;
    private boolean aiDetectBotDevices = true;
    private boolean aiAnalyzeMovement = true;
    private boolean aiAnalyzeClicks = true;
    private boolean aiAnalyzeChat = true;
    private boolean aiAnalyzeCommands = true;
    private boolean aiDetectVPNProxy = true;
    private boolean aiCheckIPReputation = true;
    private boolean aiAnalyzeNetworkPatterns = true;
    private boolean aiMonitorSuspicious = true;
    private boolean aiAutoStartMonitoring = true;
    private boolean aiDetailedLogging = false;
    private boolean notifyAdmins = true;
    private boolean aiNotifyDiscord = false;
    private String aiDiscordWebhook = "";
    private boolean aiNotifyOnBlock = true;
    private boolean aiNotifyOnKick = true;
    private boolean aiAutoBlockBotnets = true;
    private boolean aiAutoBlockMalwareIPs = true;
    private boolean aiAutoBlockKnownVPNs = true;
    private boolean aiAutoKickSuspicious = true;
    private boolean aiBypassWhitelisted = true;
    private boolean aiTrustedPlayersBypass = true;
    private boolean aiAdminBypass = true;
    private int aiTrustIncrement = 2;
    private int aiTrustDecrement = 5;
    private int aiAnomalyDecayRate = 1;
    private double aiPatternLearningRate = 0.1;
    private boolean aiAnonymizeIP = false;
    private boolean aiHashIdentifiers = true;
    private boolean aiEncryptStoredData = false;
    private int aiDataRetentionDays = 30;
    private int aiDetectionThreshold = 75;
    private int aiWarningTime = 120;
    private boolean aiAutoBlock = true;
    private int aiMaxAltsPerIP = 2;

    public int getAiMaxAltsPerIP() {
        return this.aiMaxAltsPerIP;
    }

    public String getSupabaseUrl() {
        return this.supabaseUrl;
    }

    public String getSupabaseServiceKey() {
        return this.supabaseServiceKey;
    }

    public int getOtpLength() {
        return this.otpLength;
    }

    public String getOtpCharset() {
        return this.otpCharset;
    }

    public int getMaxOtpAttempts() {
        return this.maxOtpAttempts;
    }

    public String getDiscordBotToken() {
        return this.discordBotToken;
    }

    public String getDiscordGuildId() {
        return this.discordGuildId;
    }

    public String getDiscordVerifyChannelId() {
        return this.discordVerifyChannelId;
    }

    public String getDiscordVerifiedRoleId() {
        return this.discordVerifiedRoleId;
    }

    public int getOtpExpiryMinutes() {
        return this.otpExpiryMinutes;
    }

    public int getAttemptCooldownSeconds() {
        return this.attemptCooldownSeconds;
    }

    public String getGuiTitle() {
        return this.guiTitle;
    }

    public int getGuiRows() {
        return this.guiRows;
    }

    public boolean isAISecurityEnabled() {
        return this.aiSecurityEnabled;
    }

    public boolean isAILearningMode() {
        return this.aiLearningMode;
    }

    public boolean isAIEnterpriseMode() {
        return this.aiEnterpriseMode;
    }

    public boolean isAIRequireMultipleFactors() {
        return this.aiRequireMultipleFactors;
    }

    public int getAITrustThreshold() {
        return this.aiTrustThreshold;
    }

    public int getAIBlockThreshold() {
        return this.aiBlockThreshold;
    }

    public int getAIKickThreshold() {
        return this.aiKickThreshold;
    }

    public int getAIWarningThreshold() {
        return this.aiWarningThreshold;
    }

    public int getAIMinDataPoints() {
        return this.aiMinDataPoints;
    }

    public int getAIMaxFalsePositiveRate() {
        return this.aiMaxFalsePositiveRate;
    }

    public int getMaxAltsPerIP() {
        return this.maxAltsPerIP;
    }

    public boolean isPreventAltOTP() {
        return this.preventAltOTP;
    }

    public boolean isAIAutoBlockAlts() {
        return this.aiAutoBlockAlts;
    }

    public boolean isAIDetectAltClusters() {
        return this.aiDetectAltClusters;
    }

    public boolean isAIFingerprintDevices() {
        return this.aiFingerprintDevices;
    }

    public boolean isAITrackHardwareChanges() {
        return this.aiTrackHardwareChanges;
    }

    public boolean isAIDetectBotDevices() {
        return this.aiDetectBotDevices;
    }

    public boolean isAIAnalyzeMovement() {
        return this.aiAnalyzeMovement;
    }

    public boolean isAIAnalyzeClicks() {
        return this.aiAnalyzeClicks;
    }

    public boolean isAIAnalyzeChat() {
        return this.aiAnalyzeChat;
    }

    public boolean isAIAnalyzeCommands() {
        return this.aiAnalyzeCommands;
    }

    public boolean isAIDetectVPNProxy() {
        return this.aiDetectVPNProxy;
    }

    public boolean isAICheckIPReputation() {
        return this.aiCheckIPReputation;
    }

    public boolean isAIAnalyzeNetworkPatterns() {
        return this.aiAnalyzeNetworkPatterns;
    }

    public boolean isAIMonitorSuspicious() {
        return this.aiMonitorSuspicious;
    }

    public boolean isAIAutoStartMonitoring() {
        return this.aiAutoStartMonitoring;
    }

    public boolean isAIDetailedLogging() {
        return this.aiDetailedLogging;
    }

    public boolean isNotifyAdmins() {
        return this.notifyAdmins;
    }

    public boolean isAINotifyDiscord() {
        return this.aiNotifyDiscord;
    }

    public String getAIDiscordWebhook() {
        return this.aiDiscordWebhook;
    }

    public boolean isAINotifyOnBlock() {
        return this.aiNotifyOnBlock;
    }

    public boolean isAINotifyOnKick() {
        return this.aiNotifyOnKick;
    }

    public boolean isAIAutoBlockBotnets() {
        return this.aiAutoBlockBotnets;
    }

    public boolean isAIAutoBlockMalwareIPs() {
        return this.aiAutoBlockMalwareIPs;
    }

    public boolean isAIAutoBlockKnownVPNs() {
        return this.aiAutoBlockKnownVPNs;
    }

    public boolean isAIAutoKickSuspicious() {
        return this.aiAutoKickSuspicious;
    }

    public boolean isAIBypassWhitelisted() {
        return this.aiBypassWhitelisted;
    }

    public boolean isAITrustedPlayersBypass() {
        return this.aiTrustedPlayersBypass;
    }

    public boolean isAIAdminBypass() {
        return this.aiAdminBypass;
    }

    public int getAITrustIncrement() {
        return this.aiTrustIncrement;
    }

    public int getAITrustDecrement() {
        return this.aiTrustDecrement;
    }

    public int getAIAnomalyDecayRate() {
        return this.aiAnomalyDecayRate;
    }

    public double getAIPatternLearningRate() {
        return this.aiPatternLearningRate;
    }

    public boolean isAIAnonymizeIP() {
        return this.aiAnonymizeIP;
    }

    public boolean isAIHashIdentifiers() {
        return this.aiHashIdentifiers;
    }

    public boolean isAIEncryptStoredData() {
        return this.aiEncryptStoredData;
    }

    public int getAIDataRetentionDays() {
        return this.aiDataRetentionDays;
    }

    public int getAIDetectionThreshold() {
        return this.aiDetectionThreshold;
    }

    public int getAIWarningTime() {
        return this.aiWarningTime;
    }

    public boolean isAIAutoBlock() {
        return this.aiAutoBlock;
    }

    public void setAiMaxAltsPerIP(int aiMaxAltsPerIP) {
        this.aiMaxAltsPerIP = aiMaxAltsPerIP;
    }

    public void setSupabaseUrl(String supabaseUrl) {
        this.supabaseUrl = supabaseUrl;
    }

    public void setSupabaseServiceKey(String supabaseServiceKey) {
        this.supabaseServiceKey = supabaseServiceKey;
    }

    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }

    public void setOtpCharset(String otpCharset) {
        this.otpCharset = otpCharset;
    }

    public void setMaxOtpAttempts(int maxOtpAttempts) {
        this.maxOtpAttempts = maxOtpAttempts;
    }

    public void setDiscordBotToken(String discordBotToken) {
        this.discordBotToken = discordBotToken;
    }

    public void setDiscordGuildId(String discordGuildId) {
        this.discordGuildId = discordGuildId;
    }

    public void setDiscordVerifyChannelId(String discordVerifyChannelId) {
        this.discordVerifyChannelId = discordVerifyChannelId;
    }

    public void setDiscordVerifiedRoleId(String discordVerifiedRoleId) {
        this.discordVerifiedRoleId = discordVerifiedRoleId;
    }

    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public void setAttemptCooldownSeconds(int attemptCooldownSeconds) {
        this.attemptCooldownSeconds = attemptCooldownSeconds;
    }

    public void setGuiTitle(String guiTitle) {
        this.guiTitle = guiTitle;
    }

    public void setGuiRows(int guiRows) {
        this.guiRows = guiRows;
    }

    public void setAISecurityEnabled(boolean aiSecurityEnabled) {
        this.aiSecurityEnabled = aiSecurityEnabled;
    }

    public void setAILearningMode(boolean aiLearningMode) {
        this.aiLearningMode = aiLearningMode;
    }

    public void setAIEnterpriseMode(boolean aiEnterpriseMode) {
        this.aiEnterpriseMode = aiEnterpriseMode;
    }

    public void setAIRequireMultipleFactors(boolean aiRequireMultipleFactors) {
        this.aiRequireMultipleFactors = aiRequireMultipleFactors;
    }

    public void setAITrustThreshold(int aiTrustThreshold) {
        this.aiTrustThreshold = aiTrustThreshold;
    }

    public void setAIBlockThreshold(int aiBlockThreshold) {
        this.aiBlockThreshold = aiBlockThreshold;
    }

    public void setAIKickThreshold(int aiKickThreshold) {
        this.aiKickThreshold = aiKickThreshold;
    }

    public void setAIWarningThreshold(int aiWarningThreshold) {
        this.aiWarningThreshold = aiWarningThreshold;
    }

    public void setAIMinDataPoints(int aiMinDataPoints) {
        this.aiMinDataPoints = aiMinDataPoints;
    }

    public void setAIMaxFalsePositiveRate(int aiMaxFalsePositiveRate) {
        this.aiMaxFalsePositiveRate = aiMaxFalsePositiveRate;
    }

    public void setMaxAltsPerIP(int maxAltsPerIP) {
        this.maxAltsPerIP = maxAltsPerIP;
    }

    public void setPreventAltOTP(boolean preventAltOTP) {
        this.preventAltOTP = preventAltOTP;
    }

    public void setAIAutoBlockAlts(boolean aiAutoBlockAlts) {
        this.aiAutoBlockAlts = aiAutoBlockAlts;
    }

    public void setAIDetectAltClusters(boolean aiDetectAltClusters) {
        this.aiDetectAltClusters = aiDetectAltClusters;
    }

    public void setAIFingerprintDevices(boolean aiFingerprintDevices) {
        this.aiFingerprintDevices = aiFingerprintDevices;
    }

    public void setAITrackHardwareChanges(boolean aiTrackHardwareChanges) {
        this.aiTrackHardwareChanges = aiTrackHardwareChanges;
    }

    public void setAIDetectBotDevices(boolean aiDetectBotDevices) {
        this.aiDetectBotDevices = aiDetectBotDevices;
    }

    public void setAIAnalyzeMovement(boolean aiAnalyzeMovement) {
        this.aiAnalyzeMovement = aiAnalyzeMovement;
    }

    public void setAIAnalyzeClicks(boolean aiAnalyzeClicks) {
        this.aiAnalyzeClicks = aiAnalyzeClicks;
    }

    public void setAIAnalyzeChat(boolean aiAnalyzeChat) {
        this.aiAnalyzeChat = aiAnalyzeChat;
    }

    public void setAIAnalyzeCommands(boolean aiAnalyzeCommands) {
        this.aiAnalyzeCommands = aiAnalyzeCommands;
    }

    public void setAIDetectVPNProxy(boolean aiDetectVPNProxy) {
        this.aiDetectVPNProxy = aiDetectVPNProxy;
    }

    public void setAICheckIPReputation(boolean aiCheckIPReputation) {
        this.aiCheckIPReputation = aiCheckIPReputation;
    }

    public void setAIAnalyzeNetworkPatterns(boolean aiAnalyzeNetworkPatterns) {
        this.aiAnalyzeNetworkPatterns = aiAnalyzeNetworkPatterns;
    }

    public void setAIMonitorSuspicious(boolean aiMonitorSuspicious) {
        this.aiMonitorSuspicious = aiMonitorSuspicious;
    }

    public void setAIAutoStartMonitoring(boolean aiAutoStartMonitoring) {
        this.aiAutoStartMonitoring = aiAutoStartMonitoring;
    }

    public void setAIDetailedLogging(boolean aiDetailedLogging) {
        this.aiDetailedLogging = aiDetailedLogging;
    }

    public void setNotifyAdmins(boolean notifyAdmins) {
        this.notifyAdmins = notifyAdmins;
    }

    public void setAINotifyDiscord(boolean aiNotifyDiscord) {
        this.aiNotifyDiscord = aiNotifyDiscord;
    }

    public void setAIDiscordWebhook(String aiDiscordWebhook) {
        this.aiDiscordWebhook = aiDiscordWebhook;
    }

    public void setAINotifyOnBlock(boolean aiNotifyOnBlock) {
        this.aiNotifyOnBlock = aiNotifyOnBlock;
    }

    public void setAINotifyOnKick(boolean aiNotifyOnKick) {
        this.aiNotifyOnKick = aiNotifyOnKick;
    }

    public void setAIAutoBlockBotnets(boolean aiAutoBlockBotnets) {
        this.aiAutoBlockBotnets = aiAutoBlockBotnets;
    }

    public void setAIAutoBlockMalwareIPs(boolean aiAutoBlockMalwareIPs) {
        this.aiAutoBlockMalwareIPs = aiAutoBlockMalwareIPs;
    }

    public void setAIAutoBlockKnownVPNs(boolean aiAutoBlockKnownVPNs) {
        this.aiAutoBlockKnownVPNs = aiAutoBlockKnownVPNs;
    }

    public void setAIAutoKickSuspicious(boolean aiAutoKickSuspicious) {
        this.aiAutoKickSuspicious = aiAutoKickSuspicious;
    }

    public void setAIBypassWhitelisted(boolean aiBypassWhitelisted) {
        this.aiBypassWhitelisted = aiBypassWhitelisted;
    }

    public void setAITrustedPlayersBypass(boolean aiTrustedPlayersBypass) {
        this.aiTrustedPlayersBypass = aiTrustedPlayersBypass;
    }

    public void setAIAdminBypass(boolean aiAdminBypass) {
        this.aiAdminBypass = aiAdminBypass;
    }

    public void setAITrustIncrement(int aiTrustIncrement) {
        this.aiTrustIncrement = aiTrustIncrement;
    }

    public void setAITrustDecrement(int aiTrustDecrement) {
        this.aiTrustDecrement = aiTrustDecrement;
    }

    public void setAIAnomalyDecayRate(int aiAnomalyDecayRate) {
        this.aiAnomalyDecayRate = aiAnomalyDecayRate;
    }

    public void setAIPatternLearningRate(double aiPatternLearningRate) {
        this.aiPatternLearningRate = aiPatternLearningRate;
    }

    public void setAIAnonymizeIP(boolean aiAnonymizeIP) {
        this.aiAnonymizeIP = aiAnonymizeIP;
    }

    public void setAIHashIdentifiers(boolean aiHashIdentifiers) {
        this.aiHashIdentifiers = aiHashIdentifiers;
    }

    public void setAIEncryptStoredData(boolean aiEncryptStoredData) {
        this.aiEncryptStoredData = aiEncryptStoredData;
    }

    public void setAIDataRetentionDays(int aiDataRetentionDays) {
        this.aiDataRetentionDays = aiDataRetentionDays;
    }

    public void setAIDetectionThreshold(int aiDetectionThreshold) {
        this.aiDetectionThreshold = aiDetectionThreshold;
    }

    public void setAIWarningTime(int aiWarningTime) {
        this.aiWarningTime = aiWarningTime;
    }

    public void setAIAutoBlock(boolean aiAutoBlock) {
        this.aiAutoBlock = aiAutoBlock;
    }
}

