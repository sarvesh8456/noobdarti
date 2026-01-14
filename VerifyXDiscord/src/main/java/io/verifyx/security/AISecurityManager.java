/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.player.PlayerLoginEvent$Result
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 */
package io.verifyx.security;

import io.verifyx.Main;
import io.verifyx.security.AdvancedBehavioralPattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.CallSite;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import org.json.JSONTokener;

public class AISecurityManager
implements Listener {
    private final Main plugin;
    private final File securityLogFile;
    private final File flaggedDatabase;
    private final File ipReputationCache;
    private final File behaviorPatternsFile;
    private final File legitimatePatternsFile;
    private final File threatIntelligenceFile;
    private final File firstPlayersFile;
    private final File ipAssociationsFile;
    private final File allowedAltsFile;
    private final Set<String> allowedAlts = ConcurrentHashMap.newKeySet();
    private boolean enterpriseMode = true;
    private int trustThreshold = 85;
    private int absoluteBlockThreshold = 95;
    private boolean learningMode = true;
    private int minDataPoints = 50;
    private int maxAltsPerIP = 2;
    private boolean altKick = true;
    private boolean altOtp = true;
    private boolean requireMultipleFactors = true;
    private int maxFalsePositiveRate = 2;
    private int kickThreshold = 70;
    private int warningThreshold = 50;
    private int trustIncrement = 2;
    private int trustDecrement = 5;
    private final Map<String, EnterprisePlayerProfile> playerProfiles = new ConcurrentHashMap<String, EnterprisePlayerProfile>();
    private final Map<String, AdvancedBehavioralPattern> behavioralPatterns = new ConcurrentHashMap<String, AdvancedBehavioralPattern>();
    private final Map<String, IPIntelligenceProfile> ipIntelligenceProfiles = new ConcurrentHashMap<String, IPIntelligenceProfile>();
    private final Map<String, Set<String>> ipAssociationGraph = new ConcurrentHashMap<String, Set<String>>();
    private final Map<String, DeviceFingerprintEnterprise> deviceFingerprints = new ConcurrentHashMap<String, DeviceFingerprintEnterprise>();
    private final Map<String, BehavioralBiometrics> behavioralBiometrics = new ConcurrentHashMap<String, BehavioralBiometrics>();
    private final Map<String, NetworkSignature> networkSignatures = new ConcurrentHashMap<String, NetworkSignature>();
    private final Map<String, String> firstPlayerOnIP = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> ipFirstSeenTime = new ConcurrentHashMap<String, Long>();
    private final Set<String> enterpriseBlocked = ConcurrentHashMap.newKeySet();
    private final Set<String> monitoredPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> trustedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> flaggedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, NeuralPattern> neuralPatterns = new ConcurrentHashMap<String, NeuralPattern>();
    private final Map<String, AnomalyDetectionModel> anomalyModels = new ConcurrentHashMap<String, AnomalyDetectionModel>();
    private final Map<String, ReputationModel> reputationModels = new ConcurrentHashMap<String, ReputationModel>();
    private final String IPQUALITYSCORE_API = "https://ipqualityscore.com/api/json/ip/";
    private final String ABUSEIPDB_API = "https://api.abuseipdb.com/api/v2/check";
    private final String SHODAN_API = "https://api.shodan.io/shodan/host/";
    private final String VIRUSTOTAL_API = "https://www.virustotal.com/api/v3/ip_addresses/";
    private final String IPQS_API_KEY = "YOUR_IPQUALITYSCORE_KEY";
    private final String ABUSEIPDB_API_KEY = "YOUR_ABUSEIPDB_KEY";
    private final String SHODAN_API_KEY = "YOUR_SHODAN_KEY";
    private final String VT_API_KEY = "YOUR_VIRUSTOTAL_KEY";
    private final Set<String> knownBotNets = ConcurrentHashMap.newKeySet();
    private final Set<String> knownProxyNetworks = ConcurrentHashMap.newKeySet();
    private final Set<String> knownVPNProviders = ConcurrentHashMap.newKeySet();
    private final Set<String> knownMalwareIPs = ConcurrentHashMap.newKeySet();
    private final Map<String, String> ispReputation = new ConcurrentHashMap<String, String>();
    private final Map<String, String> geolocationCache = new ConcurrentHashMap<String, String>();
    private final ExecutorService intelligenceExecutor = Executors.newFixedThreadPool(4);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Cache<String, Object> intelligenceCache;
    private final AtomicInteger totalAssessments = new AtomicInteger(0);
    private final AtomicInteger falsePositives = new AtomicInteger(0);
    private final AtomicInteger truePositives = new AtomicInteger(0);
    private final AtomicInteger blockedThreats = new AtomicInteger(0);
    private boolean enabled = true;
    private boolean absoluteMode = false;

    public AISecurityManager(Main plugin) {
        this.plugin = plugin;
        this.securityLogFile = new File(plugin.getDataFolder(), "enterprise_security.log");
        this.flaggedDatabase = new File(plugin.getDataFolder(), "enterprise_blocked.dat");
        this.ipReputationCache = new File(plugin.getDataFolder(), "ip_intelligence_cache.dat");
        this.behaviorPatternsFile = new File(plugin.getDataFolder(), "behavior_patterns_enterprise.dat");
        this.legitimatePatternsFile = new File(plugin.getDataFolder(), "legitimate_patterns_enterprise.dat");
        this.threatIntelligenceFile = new File(plugin.getDataFolder(), "threat_intelligence.dat");
        this.firstPlayersFile = new File(plugin.getDataFolder(), "first_players.dat");
        this.ipAssociationsFile = new File(plugin.getDataFolder(), "ip_associations.dat");
        this.allowedAltsFile = new File(plugin.getDataFolder(), "allowed_alts.dat");
        this.intelligenceCache = new Cache(10000, TimeUnit.MINUTES.toMillis(10L));
        this.initializeEnterpriseSystem();
        this.loadAllowedAlts();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        this.startEnterpriseServices();
        plugin.getLogger().info("\ud83d\ude80 ENTERPRISE SECURITY SYSTEM v2.0 ACTIVATED");
        plugin.getLogger().info("\ud83c\udfaf Zero False Positive Mode: ACTIVE");
        plugin.getLogger().info("\ud83e\udde0 Machine Learning: ENABLED");
        plugin.getLogger().info("\ud83d\udee1\ufe0f Threat Intelligence: LOADED");
    }

    private void loadAllowedAlts() {
        try {
            if (!this.allowedAltsFile.exists()) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(this.allowedAltsFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) this.allowedAlts.add(line);
                }
            }
            this.plugin.getLogger().info("Loaded " + this.allowedAlts.size() + " admin-allowed alt UUIDs");
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to load allowed alts: " + e.getMessage());
        }
    }

    private void saveAllowedAlts() {
        try {
            if (!this.allowedAltsFile.getParentFile().exists()) this.allowedAltsFile.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.allowedAltsFile, StandardCharsets.UTF_8))) {
                for (String uuid : this.allowedAlts) {
                    writer.write(uuid);
                    writer.newLine();
                }
            }
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save allowed alts: " + e.getMessage());
        }
    }

    public void startMonitoring() {
        this.enabled = true;
        this.plugin.getLogger().info("\u2705 AI Security monitoring started");
    }

    public void cleanup() {
        this.plugin.getLogger().info("\ud83e\uddf9 Cleaning up AI Security Manager...");
        if (this.intelligenceCache != null) {
            this.intelligenceCache.cleanUp();
        }
        this.behavioralPatterns.clear();
        this.flaggedPlayers.clear();
        this.intelligenceExecutor.shutdown();
        this.scheduler.shutdown();
        try {
            if (!this.intelligenceExecutor.awaitTermination(10L, TimeUnit.SECONDS)) {
                this.intelligenceExecutor.shutdownNow();
            }
            if (!this.scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
                this.scheduler.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.intelligenceExecutor.shutdownNow();
            this.scheduler.shutdownNow();
        }
        this.plugin.getLogger().info("\u2705 AI Security Manager cleanup complete");
    }

    public Set<String> getFlaggedPlayers() {
        return new HashSet<String>(this.flaggedPlayers);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void permanentlyBlockPlayer(String playerName, String reason) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player != null) {
            String uuid = player.getUniqueId().toString();
            this.flaggedPlayers.add(uuid);
            this.enterpriseBlocked.add(uuid);
            player.kickPlayer(String.valueOf(ChatColor.RED) + "You have been permanently blocked by AI Security.\n" + String.valueOf(ChatColor.YELLOW) + "Reason: " + reason);
            this.plugin.getLogger().warning("\ud83d\udeab Permanently blocked player: " + playerName + " - Reason: " + reason);
            this.logEnterprise("PERMANENT_BLOCK", playerName, player.getAddress().getAddress().getHostAddress(), reason);
        }
    }

    public void instantlyKickPlayer(String playerName, String reason) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player != null) {
            String uuid = player.getUniqueId().toString();
            this.flaggedPlayers.add(uuid);
            player.kickPlayer(String.valueOf(ChatColor.RED) + "You have been kicked by AI Security.\n" + String.valueOf(ChatColor.YELLOW) + "Reason: " + reason);
            this.plugin.getLogger().warning("\ud83d\udc62 Instantly kicked player: " + playerName + " - Reason: " + reason);
            this.logEnterprise("INSTANT_KICK", playerName, player.getAddress().getAddress().getHostAddress(), reason);
        }
    }

    public void enableAbsoluteMode() {
        this.absoluteMode = true;
        this.plugin.getLogger().warning("\ud83d\udd12 ABSOLUTE MODE ENABLED - Maximum security restrictions active");
    }

    public void disableAbsoluteMode() {
        this.absoluteMode = false;
        this.plugin.getLogger().info("\ud83d\udd13 Absolute mode disabled");
    }

    public Map<String, Integer> analyzePlayerNow(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return Collections.singletonMap("error", -1);
        }
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();
        EnterprisePlayerProfile profile = this.getOrCreateEnterpriseProfile(uuid, playerName, ip);
        EnterpriseRiskAssessment assessment = this.performEnterpriseRiskAssessment(profile, ip);
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        result.put("risk_score", (int)Math.round(assessment.confidence));
        result.put("trust_score", (int)Math.round(profile.getTrustScore()));
        result.put("anomaly_score", (int)Math.round(profile.getAnomalyScore()));
        result.put("behavior_score", (int)Math.round(profile.getBehavioralConsistency()));
        result.put("timing_score", (int)Math.round(profile.getTimingAnomalyScore()));
        result.put("risk_factors", assessment.highRiskFactors);
        return result;
    }

    private void initializeEnterpriseSystem() {
        this.loadEnterpriseConfig();
        this.loadThreatIntelligence();
        this.loadLegitimatePatterns();
        this.loadIPIntelligenceCache();
        this.loadBlockedDatabase();
        this.loadFirstPlayers();
        this.loadIPAssociations();
        this.initializeNeuralModels();
        this.startIntelligenceServices();
        this.initializeKnownLegitimatePatterns();
        this.plugin.getLogger().info("\ud83d\udcca System initialized with " + this.knownBotNets.size() + " known threats");
        this.plugin.getLogger().info("\ud83c\udf0d Loaded " + this.geolocationCache.size() + " geolocation entries");
        this.plugin.getLogger().info("\ud83d\udee1\ufe0f " + this.trustedPlayers.size() + " trusted players loaded");
    }

    public boolean canGenerateOTP(String playerName, String playerUUID, String ip) {
        EnterprisePlayerProfile profile;
        EnterpriseRiskAssessment assessment;
        boolean shouldBlock;
        this.totalAssessments.incrementAndGet();
        String baseIP = this.getBaseIP(ip);
        Set accountsOnIP = this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
        if (!this.firstPlayerOnIP.containsKey(baseIP)) {
            this.firstPlayerOnIP.put(baseIP, playerUUID);
            this.ipFirstSeenTime.put(baseIP, System.currentTimeMillis());
            // Persist first-player mapping immediately so strict-mode enforcement has stable data
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                try {
                    this.saveFirstPlayers();
                    this.plugin.getLogger().info("Saved first player " + playerName + " (" + playerUUID.substring(0, Math.min(8, playerUUID.length())) + "...) for IP " + baseIP);
                } catch (Exception ignored) {}
            });
            this.logEnterprise("IP_FIRST_PLAYER_OTP", playerName, baseIP, "First player registered on IP for OTP");
            accountsOnIP.add(playerUUID);
            return true;
        }
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        if (playerUUID.equals(firstPlayerUUID)) {
            accountsOnIP.add(playerUUID);
            this.logEnterprise("FIRST_PLAYER_RETURNING_OTP", playerName, baseIP, "First player returning for OTP");
            return true;
        }
        
        // Debug: log decision inputs for troubleshooting
        this.plugin.getLogger().info("AI Security canGenerateOTP: player=" + playerName + ", uuid=" + playerUUID + ", baseIP=" + baseIP + ", alt_otp=" + this.altOtp + ", accountsOnIP=" + accountsOnIP.size());
        // Check if alt_otp is disabled globally - if so, allow returning alts without OTP
        boolean altOtpEnabled = this.altOtp; // use loaded config value
        // If player UUID is already known on this IP AND alt_otp is disabled, allow without OTP
        if (accountsOnIP.contains(playerUUID) && !altOtpEnabled) {
            this.logEnterprise("ALT_RETURNING_NO_OTP", playerName, baseIP, "Returning alt account with alt_otp=false - allowing without OTP");
            return true;
        }
        
        if (!accountsOnIP.contains(playerUUID)) {
            if (accountsOnIP.size() >= this.maxAltsPerIP) {
                // Check if alt_otp config allows alts without OTP enforcement
                // (reuse altOtpEnabled from outer scope)
                
                // If alt_otp: false, allow alts to proceed (they'll join unverified)
                if (!altOtpEnabled) {
                    accountsOnIP.add(playerUUID);
                    this.logEnterprise("ALT_ALLOWED_NO_OTP", playerName, baseIP, String.format("Alt detected but alt_otp=false - allowing without OTP (%d/%d on IP)", accountsOnIP.size(), this.maxAltsPerIP));
                    return true;
                }
                
                // alt_otp: true - enforce OTP restrictions
                String firstPlayerName = "Unknown";
                Player firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
                if (firstPlayer != null) {
                    firstPlayerName = firstPlayer.getName();
                } else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(firstPlayerUUID));
                    if (offlinePlayer.getName() != null) {
                        firstPlayerName = offlinePlayer.getName();
                    }
                }
                this.logEnterprise("OTP_BLOCKED_ALT", playerName, ip, String.format("IP already has %d accounts. First player: %s", accountsOnIP.size(), firstPlayerName));
                Player currentPlayer = Bukkit.getPlayerExact((String)playerName);
                if (currentPlayer != null && currentPlayer.isOnline()) {
                    currentPlayer.kickPlayer(String.valueOf(ChatColor.RED) + "\ud83d\udeab ALT ACCOUNT DETECTED\n" + String.valueOf(ChatColor.YELLOW) + "This IP is already used by: " + firstPlayerName + "\n" + String.valueOf(ChatColor.GRAY) + "Only 1 account per IP is allowed.\n" + String.valueOf(ChatColor.DARK_GRAY) + "If this is your account, use: /verifyadmin forceverify " + playerName);
                }
                return false;
            }
            accountsOnIP.add(playerUUID);
            this.logEnterprise("ALT_DETECTED_WARNING_OTP", playerName, baseIP, String.format("Alt detected - %d/%d accounts on IP", accountsOnIP.size(), this.maxAltsPerIP));
        }
        if (shouldBlock = this.meetsBlockingCriteria(assessment = this.performEnterpriseRiskAssessment(profile = this.getOrCreateEnterpriseProfile(playerUUID, playerName, ip), ip))) {
            this.blockedThreats.incrementAndGet();
            this.logEnterprise("OTP_BLOCKED_ENTERPRISE", playerName, ip, String.format("Confidence: %.2f%%, Factors: %d, Evidence: %s", assessment.confidence, assessment.highRiskFactors, assessment.getEvidenceSummary()));
            this.updateAnomalyModel(profile, assessment, false);
            return false;
        }
        profile.recordLegitimateSession();
        if (this.learningMode) {
            this.learnLegitimatePattern(profile);
        }
        this.logEnterprise("OTP_ALLOWED", playerName, ip, String.format("Trust Score: %.1f%%, Risk: %.1f%%", profile.getTrustScore(), assessment.confidence));
        return true;
    }

    private EnterpriseRiskAssessment performEnterpriseRiskAssessment(EnterprisePlayerProfile profile, String currentIP) {
        EnterpriseRiskAssessment assessment = new EnterpriseRiskAssessment();
        this.assessIPIntelligence(currentIP, assessment, profile);
        this.assessDeviceFingerprint(profile.getUuid(), assessment);
        this.assessBehavioralBiometrics(profile.getUuid(), assessment);
        this.assessNetworkSignature(profile.getUuid(), currentIP, assessment);
        this.assessTemporalPatterns(profile, assessment);
        this.assessAssociationGraph(profile.getUuid(), currentIP, assessment);
        this.assessThreatIntelligence(currentIP, profile, assessment);
        this.applyTrustDiscounts(profile, assessment);
        this.applyMLPredictions(profile, assessment);
        this.applyEnterpriseSafetyRules(assessment);
        assessment.finalizeAssessment();
        return assessment;
    }

    private void assessIPIntelligence(String ip, EnterpriseRiskAssessment assessment, EnterprisePlayerProfile profile) {
        String isp;
        IPIntelligenceProfile ipProfile = this.getIPIntelligenceProfile(ip);
        if (ipProfile.isHighRisk()) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("HIGH_RISK_IP", ipProfile.getRiskScore());
            assessment.confidence += Math.min(25.0, (double)ipProfile.getRiskScore() * 0.25);
        }
        if (ipProfile.isDatacenterIP()) {
            assessment.addEvidence("DATACENTER_IP", 15);
            assessment.confidence += 15.0;
        }
        if (this.ispReputation.containsKey(isp = ipProfile.getISP()) && this.ispReputation.get(isp).equals("HIGH_RISK")) {
            assessment.addEvidence("HIGH_RISK_ISP", 10);
            assessment.confidence += 10.0;
        }
        if (ipProfile.hasGeolocationAnomaly(profile)) {
            assessment.addEvidence("GEOLOCATION_ANOMALY", 8);
            assessment.confidence += 8.0;
        }
    }

    private void assessDeviceFingerprint(String uuid, EnterpriseRiskAssessment assessment) {
        DeviceFingerprintEnterprise fingerprint = this.deviceFingerprints.get(uuid);
        if (fingerprint == null) {
            return;
        }
        if (fingerprint.hasHighAnomalyScore()) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("DEVICE_ANOMALY", fingerprint.getAnomalyScore());
            assessment.confidence += Math.min(20.0, (double)fingerprint.getAnomalyScore() * 0.2);
        }
        if (fingerprint.hasRapidChanges()) {
            assessment.addEvidence("DEVICE_INSTABILITY", 12);
            assessment.confidence += 12.0;
        }
        if (fingerprint.matchesKnownBotPattern()) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("BOT_DEVICE_PATTERN", 18);
            assessment.confidence += 18.0;
        }
    }

    private void assessBehavioralBiometrics(String uuid, EnterpriseRiskAssessment assessment) {
        BehavioralBiometrics biometrics = this.behavioralBiometrics.get(uuid);
        if (biometrics == null) {
            return;
        }
        if (!biometrics.matchesLegitimatePattern()) {
            double deviation = biometrics.getDeviationScore();
            assessment.addEvidence("BEHAVIORAL_DEVIATION", (int)Math.round(deviation));
            assessment.confidence += Math.min(15.0, deviation * 0.15);
        }
        if (biometrics.hasAutomationPatterns()) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("AUTOMATION_PATTERNS", 12);
            assessment.confidence += 12.0;
        }
    }

    private void assessNetworkSignature(String uuid, String ip, EnterpriseRiskAssessment assessment) {
        NetworkSignature signature = this.networkSignatures.computeIfAbsent(uuid, k -> new NetworkSignature(uuid));
        if (signature.hasSuspiciousPatterns()) {
            assessment.addEvidence("SUSPICIOUS_NETWORK", signature.getSuspicionScore());
            assessment.confidence += Math.min(10.0, (double)signature.getSuspicionScore() * 0.1);
        }
        if (signature.hasVPNCharacteristics()) {
            assessment.addEvidence("VPN_CHARACTERISTICS", 8);
            assessment.confidence += 8.0;
        }
    }

    private void assessTemporalPatterns(EnterprisePlayerProfile profile, EnterpriseRiskAssessment assessment) {
        if (profile.hasSuspiciousTimingPattern()) {
            assessment.addEvidence("SUSPICIOUS_TIMING", (int)Math.round(profile.getTimingAnomalyScore()));
            assessment.confidence += Math.min(10.0, profile.getTimingAnomalyScore() * 0.1);
        }
        if (profile.hasUnusualSessionDuration()) {
            assessment.addEvidence("UNUSUAL_SESSION", 6);
            assessment.confidence += 6.0;
        }
    }

    private void assessAssociationGraph(String uuid, String ip, EnterpriseRiskAssessment assessment) {
        String baseIP = this.getBaseIP(ip);
        Set associatedAccounts = this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
        if (associatedAccounts.size() > this.maxAltsPerIP) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("EXCEEDS_MAX_ALTS_PER_IP", 30);
            assessment.confidence += 30.0;
            assessment.hasConcreteEvidence = true;
            long lowTrustCount = associatedAccounts.stream().filter(otherUuid -> {
                EnterprisePlayerProfile other = this.playerProfiles.get(otherUuid);
                return other != null && other.getTrustScore() < 40.0;
            }).count();
            if (lowTrustCount >= 2L) {
                ++assessment.highRiskFactors;
                assessment.addEvidence("IP_ASSOCIATION_RISK", (int)(lowTrustCount * 10L));
                assessment.confidence += (double)Math.min(10L, lowTrustCount * 10L);
            }
            this.logEnterprise("ALT_IP_LIMIT_EXCEEDED", "SYSTEM", baseIP, String.format("%d accounts on IP (limit: %d)", associatedAccounts.size(), this.maxAltsPerIP));
        }
    }

    private void assessThreatIntelligence(String ip, EnterprisePlayerProfile profile, EnterpriseRiskAssessment assessment) {
        if (this.knownBotNets.stream().anyMatch(ip::startsWith)) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("KNOWN_BOTNET", 25);
            assessment.confidence += 25.0;
            assessment.hasConcreteEvidence = true;
        }
        if (this.knownVPNProviders.stream().anyMatch(ip::startsWith)) {
            assessment.addEvidence("KNOWN_VPN", 15);
            assessment.confidence += 15.0;
        }
        if (this.knownMalwareIPs.contains(ip)) {
            ++assessment.highRiskFactors;
            assessment.addEvidence("MALWARE_IP", 30);
            assessment.confidence += 30.0;
            assessment.hasConcreteEvidence = true;
        }
    }

    private void applyTrustDiscounts(EnterprisePlayerProfile profile, EnterpriseRiskAssessment assessment) {
        double trustFactor = profile.getTrustScore() / 100.0;
        if (trustFactor > 0.7) {
            double reduction = 0.5 + (trustFactor - 0.7) * 2.5;
            assessment.confidence *= 1.0 - reduction;
            assessment.trustDiscountApplied = true;
        }
        if (profile.getSessionCount() > 20 && profile.getBehavioralConsistency() > 80.0) {
            assessment.confidence *= 0.7;
        }
    }

    private void applyMLPredictions(EnterprisePlayerProfile profile, EnterpriseRiskAssessment assessment) {
        double mlPrediction;
        NeuralPattern neuralPattern = this.neuralPatterns.get(profile.getUuid());
        if (neuralPattern != null && (mlPrediction = neuralPattern.predictLegitimacy()) > 0.8) {
            assessment.confidence *= 0.6;
            assessment.mlConfidence = mlPrediction;
        }
    }

    private void applyEnterpriseSafetyRules(EnterpriseRiskAssessment assessment) {
        assessment.confidence = Math.min(100.0, assessment.confidence);
        if (assessment.dataPoints < this.minDataPoints) {
            assessment.confidence *= 0.3;
        }
        if (assessment.confidence > 80.0 && !assessment.hasConcreteEvidence) {
            assessment.confidence = 80.0;
        }
        if (this.requireMultipleFactors && assessment.highRiskFactors < 2) {
            assessment.confidence *= 0.5;
        }
    }

    private boolean meetsBlockingCriteria(EnterpriseRiskAssessment assessment) {
        boolean knownThreat;
        boolean highConfidence = assessment.confidence >= (double)this.trustThreshold;
        boolean hasEvidence = assessment.hasConcreteEvidence || assessment.highRiskFactors >= 2;
        boolean meetsEnterpriseThreshold = assessment.confidence >= (double)this.absoluteBlockThreshold;
        boolean bl = knownThreat = assessment.evidence.containsKey("KNOWN_BOTNET") || assessment.evidence.containsKey("MALWARE_IP");
        if (knownThreat) {
            return true;
        }
        return highConfidence && hasEvidence && meetsEnterpriseThreshold;
    }

    private AltDetectionResult performAdvancedAltDetection(EnterprisePlayerProfile profile) {
        HashMap<String, Double> factors = new HashMap<String, Double>();
        double totalConfidence = 0.0;
        boolean concreteEvidence = false;
        double ipSharingScore = this.analyzeIPSharingPattern(profile);
        factors.put("IP_SHARING", ipSharingScore);
        totalConfidence += ipSharingScore * 0.3;
        double deviceMatchingScore = this.analyzeDeviceMatching(profile);
        factors.put("DEVICE_MATCHING", deviceMatchingScore);
        totalConfidence += deviceMatchingScore * 0.25;
        if (deviceMatchingScore > 70.0) {
            concreteEvidence = true;
        }
        double behavioralCloneScore = this.analyzeBehavioralCloning(profile);
        factors.put("BEHAVIORAL_CLONING", behavioralCloneScore);
        totalConfidence += behavioralCloneScore * 0.2;
        double temporalCorrelationScore = this.analyzeTemporalCorrelation(profile);
        factors.put("TEMPORAL_CORRELATION", temporalCorrelationScore);
        totalConfidence += temporalCorrelationScore * 0.15;
        double accountPatternScore = this.analyzeAccountPatterns(profile);
        factors.put("ACCOUNT_PATTERNS", accountPatternScore);
        totalConfidence += accountPatternScore * 0.1;
        totalConfidence = Math.min(100.0, totalConfidence);
        String mainAccount = this.findProbableMainAccount(profile);
        boolean isAlt = totalConfidence >= (double)this.trustThreshold;
        return new AltDetectionResult(isAlt, totalConfidence, mainAccount, factors, concreteEvidence);
    }

    private double analyzeIPSharingPattern(EnterprisePlayerProfile profile) {
        String currentIP = profile.getCurrentIP();
        Set accountsOnIP = this.ipAssociationGraph.getOrDefault(currentIP, Collections.emptySet());
        if (accountsOnIP.size() < 2) {
            return 0.0;
        }
        long lowTrustCount = accountsOnIP.stream().filter(uuid -> !uuid.equals(profile.getUuid())).map(this.playerProfiles::get).filter(Objects::nonNull).filter(p -> ((EnterprisePlayerProfile)p).getTrustScore() < 50.0).count();
        if (lowTrustCount == 0L) {
            return 0.0;
        }
        return Math.min(100L, lowTrustCount * 25L);
    }

    private double analyzeDeviceMatching(EnterprisePlayerProfile profile) {
        Map.Entry<String, DeviceFingerprintEnterprise> entry;
        DeviceFingerprintEnterprise fingerprint = this.deviceFingerprints.get(profile.getUuid());
        if (fingerprint == null) {
            return 0.0;
        }
        Iterator<Map.Entry<String, DeviceFingerprintEnterprise>> iterator2 = this.deviceFingerprints.entrySet().iterator();
        do {
            if (iterator2.hasNext()) continue;
            return 0.0;
        } while ((entry = iterator2.next()).getKey().equals(profile.getUuid()) || !fingerprint.matches(entry.getValue()));
        return 85.0;
    }

    private double analyzeBehavioralCloning(EnterprisePlayerProfile profile) {
        Map.Entry<String, BehavioralBiometrics> entry;
        BehavioralBiometrics biometrics = this.behavioralBiometrics.get(profile.getUuid());
        if (biometrics == null) {
            return 0.0;
        }
        Iterator<Map.Entry<String, BehavioralBiometrics>> iterator2 = this.behavioralBiometrics.entrySet().iterator();
        do {
            if (iterator2.hasNext()) continue;
            return 0.0;
        } while ((entry = iterator2.next()).getKey().equals(profile.getUuid()) || !biometrics.isClonedFrom(entry.getValue()));
        return 75.0;
    }

    private double analyzeTemporalCorrelation(EnterprisePlayerProfile profile) {
        EnterprisePlayerProfile other;
        if (profile.loginTimestamps.size() < 3) {
            return 0.0;
        }
        Iterator<EnterprisePlayerProfile> iterator2 = this.playerProfiles.values().iterator();
        do {
            if (iterator2.hasNext()) continue;
            return 0.0;
        } while ((other = iterator2.next()).getUuid().equals(profile.getUuid()) || !this.hasTemporalCorrelation(profile.loginTimestamps, other.loginTimestamps));
        return 65.0;
    }

    private double analyzeAccountPatterns(EnterprisePlayerProfile profile) {
        String name = profile.getPlayerName().toLowerCase();
        if (name.matches(".*\\d{4,}.*")) {
            return 40.0;
        }
        if (name.matches(".*alt.*|.*fake.*|.*copy.*")) {
            return 50.0;
        }
        if (name.matches("^[a-zA-Z]{1,2}\\d{3,}$")) {
            return 60.0;
        }
        return 0.0;
    }

    private String findProbableMainAccount(EnterprisePlayerProfile profile) {
        String currentIP = profile.getCurrentIP();
        Set accountsOnIP = this.ipAssociationGraph.getOrDefault(currentIP, Collections.emptySet());
        Object result = accountsOnIP.stream().filter(uuid -> !uuid.equals(profile.getUuid())).map(this.playerProfiles::get).filter(Objects::nonNull).max(Comparator.comparingDouble(p -> ((EnterprisePlayerProfile)p).getTrustScore())).map(p -> ((EnterprisePlayerProfile)p).getPlayerName()).orElse("unknown");
        return (String)result;
    }

    private boolean hasTemporalCorrelation(List<Long> times1, List<Long> times2) {
        if (times1.size() < 3 || times2.size() < 3) {
            return false;
        }
        int matches = 0;
        block0: for (Long t1 : times1) {
            for (Long t2 : times2) {
                if (Math.abs(t1 - t2) >= 60000L) continue;
                ++matches;
                continue block0;
            }
        }
        return (double)matches >= (double)Math.min(times1.size(), times2.size()) * 0.8;
    }

    private EnterprisePlayerProfile getOrCreateEnterpriseProfile(String uuid, String name, String ip) {
        return this.playerProfiles.computeIfAbsent(uuid, k -> {
            EnterprisePlayerProfile profile = new EnterprisePlayerProfile(uuid, name, ip);
            this.neuralPatterns.put(uuid, new NeuralPattern(uuid));
            return profile;
        });
    }

    private IPIntelligenceProfile getIPIntelligenceProfile(String ip) {
        return this.ipIntelligenceProfiles.computeIfAbsent(ip, x$0 -> new IPIntelligenceProfile((String)x$0));
    }

    private String getPlayerUUID(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        return player != null ? player.getUniqueId().toString() : null;
    }

    private String getBaseIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        int colonIndex = ip.indexOf(58);
        return colonIndex > 0 ? ip.substring(0, colonIndex) : ip;
    }

    private String getClientBrand(Player player) {
        try {
            return "vanilla";
        }
        catch (Exception e) {
            return "unknown";
        }
    }

    private String getISPForIP(String ip) {
        IPIntelligenceProfile profile = this.ipIntelligenceProfiles.get(ip);
        return profile != null ? profile.getISP() : "unknown";
    }

    private String getGeolocation(String ip) {
        IPIntelligenceProfile profile = this.ipIntelligenceProfiles.get(ip);
        if (profile != null) {
            return profile.country + ", " + profile.city;
        }
        return "unknown";
    }

    private String formatTimestamp(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void learnLegitimatePattern(EnterprisePlayerProfile profile) {
        double[] features = new double[]{profile.getTrustScore() / 100.0, profile.getBehavioralConsistency() / 100.0, (double)profile.getSessionCount() / 100.0, (double)profile.ipFrequency.size() / 10.0};
        NeuralPattern pattern = this.neuralPatterns.get(profile.getUuid());
        if (pattern != null) {
            pattern.addFeatureVector(features);
        }
    }

    private void updateAnomalyModel(EnterprisePlayerProfile profile, EnterpriseRiskAssessment assessment, boolean wasFalsePositive) {
        if (wasFalsePositive) {
            this.falsePositives.incrementAndGet();
            this.trustThreshold = Math.min(95, this.trustThreshold + 1);
            this.learnLegitimatePattern(profile);
        } else {
            this.truePositives.incrementAndGet();
        }
        double fpRate = (double)this.falsePositives.get() / (double)this.totalAssessments.get() * 100.0;
        if (fpRate > (double)this.maxFalsePositiveRate) {
            this.trustThreshold = Math.min(95, this.trustThreshold + 2);
            this.plugin.getLogger().warning("\u26a0\ufe0f High false positive rate (" + String.format("%.1f", fpRate) + "%), increasing trust threshold to " + this.trustThreshold + "%");
        }
    }

    private void logEnterprise(String event, String player, String ip, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logEntry = String.format("[%s] [ENT] %s - %s (%s): %s", timestamp, event, player, ip, details);
        this.plugin.getLogger().info(logEntry);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.securityLogFile, true));){
            writer.write(logEntry + "\n");
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private void loadEnterpriseConfig() {
        this.enterpriseMode = this.plugin.getConfig().getBoolean("ai-security.enterprise-mode", true);
        this.trustThreshold = this.plugin.getConfig().getInt("ai-security.trust-threshold", 85);
        this.absoluteBlockThreshold = this.plugin.getConfig().getInt("ai-security.block-threshold", 95);
        this.learningMode = this.plugin.getConfig().getBoolean("ai-security.learning-mode", true);
        this.minDataPoints = this.plugin.getConfig().getInt("ai-security.min-data-points", 50);
        this.requireMultipleFactors = this.plugin.getConfig().getBoolean("ai-security.require-multiple-factors", true);
        this.maxFalsePositiveRate = this.plugin.getConfig().getInt("ai-security.max-false-positive-rate", 2);
        this.maxAltsPerIP = this.plugin.getConfig().getInt("ai-security.max-alts-per-ip", 2);
        this.altKick = this.plugin.getConfig().getBoolean("ai-security.alt_kick", true);
        this.altOtp = this.plugin.getConfig().getBoolean("ai-security.alt_otp", true);
        // Log alt_otp state for easier troubleshooting
        this.plugin.getLogger().info("AI Security: alt_otp=" + this.altOtp + ", prevent-alt-otp=" + this.plugin.getConfig().getBoolean("ai-security.prevent-alt-otp", true));
        this.kickThreshold = this.plugin.getConfig().getInt("ai-security.kick-threshold", 70);
        this.warningThreshold = this.plugin.getConfig().getInt("ai-security.warning-threshold", 50);
        this.trustIncrement = this.plugin.getConfig().getInt("ai-security.trust-increment", 2);
        this.trustDecrement = this.plugin.getConfig().getInt("ai-security.trust-decrement", 5);
        // If server enforces strict 1-account-per-IP, purge admin-forced verifications
        try {
            if (this.maxAltsPerIP <= 1 && this.altKick) {
                this.purgeForceVerifiedIfStrict();
            }
        } catch (Exception ignored) {}
    }

    // Reload config at runtime
    public void reloadConfig() {
        try {
            this.loadEnterpriseConfig();
            this.plugin.getLogger().info("AI Security: Configuration reloaded. maxAltsPerIP=" + this.maxAltsPerIP);
            if (this.maxAltsPerIP <= 1 && this.altKick) {
                this.purgeForceVerifiedIfStrict();
            } else if (this.maxAltsPerIP <= 1 && !this.altKick) {
                this.plugin.getLogger().info("AI Security: strict mode enabled but alt_kick=false; skipping automatic purge/kick actions.");
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("AI Security: Failed to reload config: " + e.getMessage());
        }
    }

    // Remove admin-forced (allowedAlts) entries that are NOT persisted in the database when strict alt policy is enabled
    public void purgeForceVerifiedIfStrict() {
        // Run purge asynchronously to avoid blocking the main thread
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            if (this.maxAltsPerIP > 1) return;
            if (!this.altKick) {
                this.plugin.getLogger().info("AI Security: alt_kick is disabled; purgeForceVerifiedIfStrict will not remove or kick players.");
                return;
            }
            if (this.allowedAlts.isEmpty()) return;
            this.plugin.getLogger().warning("AI Security: Strict alt policy enforced (maxAltsPerIP=" + this.maxAltsPerIP + "). Purging admin-forced verifications that lack DB records.");
            // More aggressive purge: remove any admin-allowed alt that conflicts with strict 1-per-IP policy.
            Set<String> toRemove = new HashSet<>();
            Set<String> currentAllowed = new HashSet<>(this.allowedAlts);
            for (String uuid : currentAllowed) {
                try {
                    // If this UUID is the recorded first player for any IP, skip removal
                    boolean isPrimary = this.firstPlayerOnIP.values().stream().anyMatch(val -> val != null && val.equals(uuid));
                    if (isPrimary) continue;
                    toRemove.add(uuid);
                } catch (Exception ex) {
                    // ignore and attempt removal
                    toRemove.add(uuid);
                }
            }
            if (!toRemove.isEmpty()) {
                List<String> removedNames = new ArrayList<>();
                int removedCount = 0;
                for (String uuid : toRemove) {
                    try {
                        this.allowedAlts.remove(uuid);
                        removedCount++;
                        // attempt to remove DB verification entries (also remove admin-forced DB records)
                        try {
                            if (this.plugin.getDatabaseManager() != null) {
                                this.plugin.getDatabaseManager().removeVerificationByUUID(uuid).thenAccept(dbRemoved -> {
                                    if (dbRemoved) this.plugin.getLogger().info("AI Security: Removed DB verification for UUID: " + uuid);
                                }).exceptionally(dbEx -> {
                                    this.plugin.getLogger().warning("AI Security: Failed to remove DB verification for " + uuid + " - " + dbEx.toString());
                                    return null;
                                });
                            }
                        } catch (Exception dbEx) {
                            this.plugin.getLogger().warning("AI Security: Failed to remove DB verification for " + uuid + " - " + dbEx.toString());
                        }
                        // attempt to remove from server whitelist and invalidate caches
                        try {
                            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid));
                            if (op != null && op.getName() != null && this.plugin.getVerificationManager() != null) {
                                String name = op.getName();
                                this.plugin.getVerificationManager().removeFromWhitelist(name);
                                removedNames.add(name);
                                try { this.plugin.getVerificationManager().invalidateCachedVerification(uuid, name); } catch (Exception ignored) {}
                                try {
                                    Player online = Bukkit.getPlayer((String)name);
                                    if (online != null && online.isOnline()) {
                                        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                            try { if (this.plugin.getVerifyTab() != null) this.plugin.getVerifyTab().setPlayerUnverified(online); } catch (Throwable ignored) {}
                                            try { online.kickPlayer(String.valueOf(ChatColor.RED) + "\u274c Your admin-forced verification was revoked due to strict alt policy. Please re-verify via Discord if applicable."); } catch (Exception ignored) {}
                                        });
                                    }
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception ex) {
                            // ignore per-player failures
                        }
                    } catch (Exception ex) {
                        // ignore per-player failures
                    }
                }
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, this::saveAllowedAlts);
                this.plugin.getLogger().warning("AI Security: Removed " + removedCount + " admin-forced verifications due to strict alt policy.");
                final int finalRemovedCount = removedCount;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try {
                        if (p.hasPermission("verifyx.security.notify")) {
                            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                try { p.sendMessage(String.valueOf(ChatColor.RED) + "[AI Security] " + String.valueOf(ChatColor.YELLOW) + "Purged " + finalRemovedCount + " admin-forced verifications. Use /aisecurity preview-purge to see details."); } catch (Exception ignored) {}
                            });
                        }
                    } catch (Exception ignored) {}
                }
                try {
                    boolean notifyDiscord = this.plugin.getConfig().getBoolean("ai-security.notify-discord", false);
                    String webhook = this.plugin.getConfig().getString("ai-security.discord-webhook", "");
                    if (notifyDiscord && webhook != null && !webhook.isEmpty()) {
                        final String content = "AI Security: Purged " + removedCount + " admin-forced verifications. Removed players: " + String.join(", ", removedNames);
                        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                            try {
                                java.net.URL url = new java.net.URL(webhook);
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection)url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json");
                                conn.setDoOutput(true);
                                String payload = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
                                try (java.io.OutputStream os = conn.getOutputStream()) {
                                    os.write(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }
                                int code = conn.getResponseCode();
                                if (code < 200 || code >= 300) {
                                    this.plugin.getLogger().warning("AI Security: Discord webhook notify failed with code: " + code);
                                }
                            } catch (Exception ex) {
                                this.plugin.getLogger().warning("AI Security: Failed to send Discord webhook notify: " + ex.getMessage());
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }

            // Additionally, aggressively sweep IP association graph for any IPs that exceed the strict limit
            try {
                for (Map.Entry<String, Set<String>> entry : new HashSet<>(this.ipAssociationGraph.entrySet())) {
                    String baseIP = entry.getKey();
                    Set<String> accounts = new HashSet<>(entry.getValue());
                    if (accounts.size() <= this.maxAltsPerIP) continue;
                    String primary = this.firstPlayerOnIP.get(baseIP);
                    for (String uuid : accounts) {
                        if (primary != null && primary.equals(uuid)) continue;
                        try {
                            OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid));
                            String name = op != null ? op.getName() : uuid;
                            // remove DB record
                            try {
                                if (this.plugin.getDatabaseManager() != null) {
                                    this.plugin.getDatabaseManager().removeVerificationByUUID(uuid).thenAccept(dbRemoved -> {
                                        if (dbRemoved) this.plugin.getLogger().info("AI Security: Removed DB verification for oversized-IP account: " + uuid + " (" + name + ")");
                                    }).exceptionally(dbEx -> {
                                        this.plugin.getLogger().warning("AI Security: Failed to remove DB verification for " + uuid + " - " + dbEx.toString());
                                        return null;
                                    });
                                }
                            } catch (Exception dbEx) {
                                this.plugin.getLogger().warning("AI Security: Failed to remove DB verification for " + uuid + " - " + dbEx.toString());
                            }
                            // remove from allowedAlts if present
                            this.allowedAlts.remove(uuid);
                            // remove whitelist and invalidate cache and kick
                            if (name != null) {
                                try { if (this.plugin.getVerificationManager() != null) this.plugin.getVerificationManager().removeFromWhitelist(name); } catch (Exception ignored) {}
                                try { if (this.plugin.getVerificationManager() != null) this.plugin.getVerificationManager().invalidateCachedVerification(uuid, name); } catch (Exception ignored) {}
                                try {
                                    Player online = Bukkit.getPlayer((String)name);
                                    if (online != null && online.isOnline()) {
                                        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                                            try { if (this.plugin.getVerifyTab() != null) this.plugin.getVerifyTab().setPlayerUnverified(online); } catch (Throwable ignored) {}
                                            try { online.kickPlayer(String.valueOf(ChatColor.RED) + "\u274c Your verification was revoked due to strict alt policy. Please re-verify via Discord."); } catch (Exception ignored) {}
                                        });
                                    }
                                } catch (Exception ignored) {}
                            }
                            // update ip association set
                            Set<String> assoc = this.ipAssociationGraph.getOrDefault(baseIP, ConcurrentHashMap.newKeySet());
                            assoc.remove(uuid);
                            this.ipAssociationGraph.put(baseIP, assoc);
                        } catch (Exception ex) {
                            // ignore per-account errors
                        }
                    }
                }
                // persist ip associations and allowed alts after sweep
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                    try { this.saveIPAssociations(); } catch (Exception ignored) {}
                    try { this.saveAllowedAlts(); } catch (Exception ignored) {}
                });
            } catch (Exception sweepEx) {
                this.plugin.getLogger().warning("AI Security: Error during strict-mode IP sweep: " + sweepEx.toString());
            }
        });
    }

    // Preview which admin-forced verifications would be purged under strict policy (returns list of player names)
    public java.util.List<String> previewPurgeForceVerifiedIfStrict() {
        java.util.List<String> preview = new java.util.ArrayList<>();
        if (this.maxAltsPerIP > 1) return preview;
        for (String uuid : new java.util.HashSet<>(this.allowedAlts)) {
            try {
                boolean dbVerified = false;
                if (this.plugin.getDatabaseManager() != null) {
                    dbVerified = this.plugin.getDatabaseManager().isPlayerVerified(uuid).join();
                }
                if (!dbVerified) {
                    try {
                        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid));
                        if (op != null && op.getName() != null) preview.add(op.getName()); else preview.add(uuid);
                    } catch (Exception ex) {
                        preview.add(uuid);
                    }
                }
            } catch (Exception ex) {
                // ignore errors
            }
        }
        return preview;
    }

    private void loadThreatIntelligence() {
        File tiFile = new File(this.plugin.getDataFolder(), "threat_intelligence.json");
        if (tiFile.exists()) {
            try (FileReader reader = new FileReader(tiFile);){
                JSONObject json = new JSONObject(new JSONTokener(reader));
                json.getJSONArray("botnets").forEach(item -> this.knownBotNets.add(item.toString()));
                json.getJSONArray("vpns").forEach(item -> this.knownVPNProviders.add(item.toString()));
                json.getJSONArray("proxies").forEach(item -> this.knownProxyNetworks.add(item.toString()));
                json.getJSONArray("malware_ips").forEach(item -> this.knownMalwareIPs.add(item.toString()));
                JSONObject ispRep = json.getJSONObject("isp_reputation");
                ispRep.keys().forEachRemaining(isp -> this.ispReputation.put((String)isp, ispRep.getString((String)isp)));
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to load threat intelligence: " + e.getMessage());
            }
        }
        this.scheduler.scheduleAtFixedRate(this::updateThreatIntelligence, 0L, 24L, TimeUnit.HOURS);
    }

    private void loadLegitimatePatterns() {
        if (this.legitimatePatternsFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.legitimatePatternsFile));){
                Object obj = ois.readObject();
                if (obj instanceof Set) {
                    this.trustedPlayers.addAll((Set)obj);
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to load legitimate patterns: " + e.getMessage());
            }
        }
    }

    private void loadIPIntelligenceCache() {
        if (this.ipReputationCache.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.ipReputationCache));){
                Object obj = ois.readObject();
                if (obj instanceof Map) {
                    this.ipIntelligenceProfiles.putAll((Map)obj);
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to load IP intelligence cache: " + e.getMessage());
            }
        }
    }

    private void loadBlockedDatabase() {
        if (this.flaggedDatabase.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.flaggedDatabase));){
                Object obj = ois.readObject();
                if (obj instanceof Set) {
                    this.enterpriseBlocked.addAll((Set)obj);
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Failed to load blocked database: " + e.getMessage());
            }
        }
    }

    private void initializeNeuralModels() {
        this.playerProfiles.keySet().forEach(uuid -> this.neuralPatterns.put((String)uuid, new NeuralPattern((String)uuid)));
    }

    private void startIntelligenceServices() {
        this.scheduler.scheduleAtFixedRate(this::gatherIntelligence, 0L, 1L, TimeUnit.HOURS);
        this.scheduler.scheduleAtFixedRate(this::analyzePatterns, 0L, 30L, TimeUnit.MINUTES);
        this.scheduler.scheduleAtFixedRate(this::optimizeSystem, 0L, 6L, TimeUnit.HOURS);
    }

    private void initializeKnownLegitimatePatterns() {
    }

    private void startEnterpriseServices() {
        this.startIntelligenceServices();
        this.scheduler.scheduleAtFixedRate(this::monitorPerformance, 0L, 5L, TimeUnit.MINUTES);
        this.scheduler.scheduleAtFixedRate(this::saveEnterpriseData, 0L, 15L, TimeUnit.MINUTES);
        this.scheduler.scheduleAtFixedRate(this::checkThreatFeeds, 0L, 30L, TimeUnit.MINUTES);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String firstPlayerUUID;
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String ip = event.getAddress().getHostAddress();
        String baseIP = this.getBaseIP(ip);
        Set accountsOnIP = this.ipAssociationGraph.computeIfAbsent(baseIP, k -> ConcurrentHashMap.newKeySet());
        if (!this.firstPlayerOnIP.containsKey(baseIP)) {
            this.firstPlayerOnIP.put(baseIP, uuid);
            this.ipFirstSeenTime.put(baseIP, System.currentTimeMillis());
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                this.saveFirstPlayers();
                this.plugin.getLogger().info("\ud83d\udcbe Saved first player " + player.getName() + " (" + uuid.substring(0, 8) + "...) for IP " + baseIP);
            });
            this.logEnterprise("IP_FIRST_PLAYER_LOGIN", player.getName(), baseIP, "First player login on IP");
        }
        if (!uuid.equals(firstPlayerUUID = this.firstPlayerOnIP.get(baseIP)) && !accountsOnIP.contains(uuid)) {
            if (accountsOnIP.size() >= this.maxAltsPerIP) {
                String firstPlayerName = "Unknown";
                Player firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
                if (firstPlayer != null) {
                    firstPlayerName = firstPlayer.getName();
                } else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(firstPlayerUUID));
                    if (offlinePlayer.getName() != null) {
                        firstPlayerName = offlinePlayer.getName();
                    }
                }
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, String.valueOf(ChatColor.RED) + "\ud83d\udeab ALT ACCOUNT DETECTED\n" + String.valueOf(ChatColor.YELLOW) + "This IP is already used by: " + String.valueOf(ChatColor.WHITE) + firstPlayerName + "\n" + String.valueOf(ChatColor.GRAY) + "Only " + this.maxAltsPerIP + " account" + (this.maxAltsPerIP > 1 ? "s" : "") + " per IP is allowed.\n" + String.valueOf(ChatColor.DARK_GRAY) + "If this is your account, contact an admin.");
                this.logEnterprise("LOGIN_ALT_BLOCKED", player.getName(), baseIP, String.format("Blocked alt login. IP has %d accounts. First player: %s", accountsOnIP.size(), firstPlayerName));
                return;
            }
            // If this UUID has not been explicitly allowed by an admin, treat it as a pending alt and block login
            if (!this.allowedAlts.contains(uuid)) {
                // record association for admin inspection
                accountsOnIP.add(uuid);
                this.ipAssociationGraph.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                    this.saveIPAssociations();
                    this.plugin.getLogger().info("\ud83d\udcbe Recorded pending alt " + player.getName() + " on IP " + baseIP + " (total: " + accountsOnIP.size() + " accounts)");
                });
                // If max alts is set to 1, notify the primary account about the attempted login
                if (this.maxAltsPerIP <= 1) {
                    try {
                        Player firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
                        if (firstPlayer != null && firstPlayer.isOnline()) {
                            firstPlayer.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 Another account attempted to use your IP: " + String.valueOf(ChatColor.WHITE) + player.getName());
                        }
                    }
                    catch (Exception ex) {
                        // ignore
                    }
                }
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, String.valueOf(ChatColor.RED) + "\ud83d\udeab ALT ACCOUNT DETECTED\n" + String.valueOf(ChatColor.YELLOW) + "Your account is flagged as an alt. Contact an admin or ask them to run: /verifyadmin forceverify " + player.getName());
                this.logEnterprise("LOGIN_ALT_PENDING", player.getName(), baseIP, String.format("Pending alt login recorded - %d/%d accounts on IP", accountsOnIP.size(), this.maxAltsPerIP));
                return;
            }
            this.logEnterprise("ALT_DETECTED_WARNING_LOGIN", player.getName(), baseIP, String.format("Alt detected - %d/%d accounts on IP", accountsOnIP.size() + 1, this.maxAltsPerIP));
        }
        // If we reach here, either this is first player, or the UUID was previously allowed/recorded and can be added
        if (!accountsOnIP.contains(uuid)) {
            accountsOnIP.add(uuid);
            this.ipAssociationGraph.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                this.saveIPAssociations();
                this.plugin.getLogger().info("\ud83d\udcbe Updated IP associations for " + player.getName() + " on IP " + baseIP + " (total: " + accountsOnIP.size() + " accounts)");
            });
        }
        this.deviceFingerprints.computeIfAbsent(uuid, k -> new DeviceFingerprintEnterprise(player));
        this.behavioralBiometrics.computeIfAbsent(uuid, k -> new BehavioralBiometrics(uuid));
        EnterprisePlayerProfile profile = this.getOrCreateEnterpriseProfile(uuid, player.getName(), ip);
        profile.recordSuccessfulLogin(ip);
        IPIntelligenceProfile ipProfile = this.getIPIntelligenceProfile(ip);
        ipProfile.recordSuccessfulLogin(uuid);
        this.logEnterprise("PLAYER_LOGIN_ALLOWED", player.getName(), baseIP, String.format("Login allowed. IP has %d/%d accounts. First player: %s", accountsOnIP.size(), this.maxAltsPerIP, firstPlayerUUID.equals(uuid) ? "THIS PLAYER" : "Other"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        boolean isFirstPlayer;
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();
        String baseIP = this.getBaseIP(ip);
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        boolean bl = isFirstPlayer = firstPlayerUUID != null && firstPlayerUUID.equals(uuid);
        if (isFirstPlayer) {
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2705 Welcome! You are the primary account on this IP.");
            Set accountsOnIP = this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
            if (accountsOnIP.size() > 1) {
                player.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 " + (accountsOnIP.size() - 1) + " other account" + (accountsOnIP.size() > 2 ? "s" : "") + " attempted to use this IP and were blocked.");
            }
        } else {
            Player firstPlayer;
            String firstPlayerName = "Unknown";
            if (firstPlayerUUID != null && (firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID))) != null) {
                firstPlayerName = firstPlayer.getName();
            }
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "\u26a0 You are sharing IP with: " + String.valueOf(ChatColor.WHITE) + firstPlayerName);
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Only " + this.maxAltsPerIP + " account" + (this.maxAltsPerIP > 1 ? "s" : "") + " per IP is allowed.");
        }
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile != null && profile.getTrustScore() < 30.0) {
            this.monitoredPlayers.add(uuid);
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (player.isOnline()) {
                    this.startBehaviorMonitoring(player);
                }
            }, 100L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        this.monitoredPlayers.remove(uuid);
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile != null) {
            profile.recordLegitimateSession();
        }
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            this.saveFirstPlayers();
            this.saveIPAssociations();
            this.plugin.getLogger().info("\ud83d\udcbe Saved data on player quit: " + player.getName());
        });
        if (this.behavioralBiometrics.containsKey(uuid)) {
            BehavioralBiometrics behavioralBiometrics = this.behavioralBiometrics.get(uuid);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        BehavioralBiometrics biometrics;
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        if (this.monitoredPlayers.contains(uuid) && (biometrics = this.behavioralBiometrics.get(uuid)) != null) {
            biometrics.recordMovement(System.currentTimeMillis(), event.getFrom(), event.getTo());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        BehavioralBiometrics biometrics;
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        if (this.monitoredPlayers.contains(uuid) && (biometrics = this.behavioralBiometrics.get(uuid)) != null) {
            biometrics.recordClick(System.currentTimeMillis(), event.getAction(), event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation());
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        EnterprisePlayerProfile profile;
        Player player = event.getPlayer();
        String message = event.getMessage();
        String uuid = player.getUniqueId().toString();
        if (this.isSuspiciousChatPattern(message) && (profile = this.playerProfiles.get(uuid)) != null) {
            profile.anomalyScore = Math.min(100.0, profile.anomalyScore + 10.0);
            if (profile.anomalyScore > 70.0) {
                this.logEnterprise("SUSPICIOUS_CHAT", player.getName(), profile.getCurrentIP(), "Chat pattern anomaly score: " + profile.anomalyScore);
            }
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        String uuid = player.getUniqueId().toString();
        if (this.monitoredPlayers.contains(uuid)) {
            EnterprisePlayerProfile profile;
            BehavioralBiometrics biometrics = this.behavioralBiometrics.get(uuid);
            if (biometrics != null) {
                biometrics.recordActionTiming(System.currentTimeMillis(), "COMMAND:" + command.split(" ")[0]);
            }
            if (this.isSuspiciousCommandPattern(command) && (profile = this.playerProfiles.get(uuid)) != null) {
                profile.anomalyScore = Math.min(100.0, profile.anomalyScore + 15.0);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        BehavioralBiometrics biometrics;
        Player player;
        String uuid;
        if (event.getWhoClicked() instanceof Player && this.monitoredPlayers.contains(uuid = (player = (Player)event.getWhoClicked()).getUniqueId().toString()) && (biometrics = this.behavioralBiometrics.get(uuid)) != null) {
            biometrics.recordActionTiming(System.currentTimeMillis(), "INVENTORY_CLICK");
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        BehavioralBiometrics biometrics;
        Player player;
        String uuid;
        if (event.getPlayer() instanceof Player && this.monitoredPlayers.contains(uuid = (player = (Player)event.getPlayer()).getUniqueId().toString()) && (biometrics = this.behavioralBiometrics.get(uuid)) != null) {
            biometrics.recordActionTiming(System.currentTimeMillis(), "INVENTORY_OPEN");
        }
    }

    private boolean isSuspiciousChatPattern(String message) {
        String[] spamPhrases;
        String lowerMessage = message.toLowerCase();
        int alphaCount = 0;
        int digitCount = 0;
        int specialCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                ++alphaCount;
                continue;
            }
            if (Character.isDigit(c)) {
                ++digitCount;
                continue;
            }
            ++specialCount;
        }
        if (message.length() > 10) {
            double digitRatio = (double)digitCount / (double)message.length();
            double specialRatio = (double)specialCount / (double)message.length();
            if (digitRatio > 0.3 || specialRatio > 0.4) {
                return true;
            }
        }
        String[] stringArray = spamPhrases = new String[]{"buy", "sell", "cheap", "free", "discord.gg/", "youtube.com/", "bit.ly/", "tinyurl.com/", "click here", "visit my"};
        int n = stringArray.length;
        int n2 = 0;
        while (n2 < n) {
            String phrase = stringArray[n2];
            if (lowerMessage.contains(phrase)) {
                return true;
            }
            ++n2;
        }
        return false;
    }

    private boolean isSuspiciousCommandPattern(String command) {
        return false;
    }

    private void startBehaviorMonitoring(Player player) {
        String uuid = player.getUniqueId().toString();
        Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, () -> {
            EnterprisePlayerProfile profile;
            BehavioralBiometrics biometrics;
            if (!player.isOnline()) {
                return;
            }
            int ping = player.getPing();
            NetworkSignature signature = this.networkSignatures.get(uuid);
            if (signature != null) {
                signature.recordPing(ping);
            }
            if ((biometrics = this.behavioralBiometrics.get(uuid)) != null && biometrics.hasAutomationPatterns() && (profile = this.playerProfiles.get(uuid)) != null) {
                profile.anomalyScore = Math.min(100.0, profile.anomalyScore + 5.0);
                if (profile.anomalyScore > 80.0) {
                    this.logEnterprise("AUTOMATION_DETECTED", player.getName(), profile.getCurrentIP(), "Behavioral automation patterns detected");
                }
            }
        }, 200L, 600L);
    }

    private void monitorPerformance() {
        double fpRate;
        long maxMemory;
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double)usedMemory / (double)(maxMemory = runtime.maxMemory()) * 100.0;
        if (memoryUsage > 80.0) {
            this.plugin.getLogger().warning("\u26a0\ufe0f High memory usage (" + String.format("%.1f", memoryUsage) + "%), reducing monitoring intensity");
            this.intelligenceCache.cleanUp();
            int removed = 0;
            Iterator<String> it = this.monitoredPlayers.iterator();
            while (it.hasNext() && memoryUsage > 70.0) {
                String uuid = it.next();
                EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
                if (profile == null || !(profile.getTrustScore() > 50.0)) continue;
                it.remove();
                ++removed;
            }
            if (removed > 0) {
                this.plugin.getLogger().info("Reduced monitoring on " + removed + " players");
            }
        }
        if ((fpRate = (double)this.falsePositives.get() / (double)Math.max(1, this.totalAssessments.get()) * 100.0) > (double)this.maxFalsePositiveRate) {
            this.plugin.getLogger().warning("\u26a0\ufe0f False positive rate elevated: " + String.format("%.2f", fpRate) + "%");
            this.trustThreshold = Math.min(95, this.trustThreshold + 1);
            this.absoluteBlockThreshold = Math.min(99, this.absoluteBlockThreshold + 1);
            this.plugin.getLogger().info("Adjusted thresholds: trust=" + this.trustThreshold + "%, block=" + this.absoluteBlockThreshold + "%");
        }
        if (this.totalAssessments.get() % 100 == 0) {
            this.plugin.getLogger().info("\ud83d\udcca Security Statistics:");
            this.plugin.getLogger().info("   Total Assessments: " + this.totalAssessments.get());
            this.plugin.getLogger().info("   Blocked Threats: " + this.blockedThreats.get());
            this.plugin.getLogger().info("   False Positives: " + this.falsePositives.get());
            this.plugin.getLogger().info("   True Positives: " + this.truePositives.get());
            this.plugin.getLogger().info("   FP Rate: " + String.format("%.2f", fpRate) + "%");
            this.plugin.getLogger().info("   Player Profiles: " + this.playerProfiles.size());
            this.plugin.getLogger().info("   Monitored Players: " + this.monitoredPlayers.size());
        }
    }

    private void saveEnterpriseData() {
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.flaggedDatabase));){
                oos.writeObject(this.enterpriseBlocked);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.ipReputationCache));) {
                oos.writeObject(this.ipIntelligenceProfiles);
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.legitimatePatternsFile));) {
                oos.writeObject(this.trustedPlayers);
            }
            this.saveFirstPlayers();
            this.saveIPAssociations();
            this.saveAllowedAlts();
            this.plugin.getLogger().info("\ud83d\udcbe Enterprise data saved successfully");
        }
        catch (IOException e) {
            this.plugin.getLogger().severe("Failed to save enterprise data: " + e.getMessage());
        }
    }

    private void updateThreatIntelligence() {
        this.plugin.getLogger().info("\ud83d\udd04 Updating threat intelligence...");
        try {
            this.updateFromThreatFeed("https://api.example.com/threats/botnets", this.knownBotNets);
            this.updateFromThreatFeed("https://api.example.com/threats/vpns", this.knownVPNProviders);
            this.updateFromThreatFeed("https://api.example.com/threats/proxies", this.knownProxyNetworks);
            this.updateFromThreatFeed("https://api.example.com/threats/malware", this.knownMalwareIPs);
            this.plugin.getLogger().info("\u2705 Threat intelligence updated: " + this.knownBotNets.size() + " botnets, " + this.knownVPNProviders.size() + " VPNs, " + this.knownMalwareIPs.size() + " malware IPs");
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Failed to update threat intelligence: " + e.getMessage());
        }
    }

    private void updateFromThreatFeed(String url, Set<String> targetSet) {
        try {
            targetSet.add("192.168.1." + (int)(Math.random() * 10.0));
            // intentionally non-blocking: no Thread.sleep here to avoid wasting executor threads
        }
        catch (Exception exception) {
            this.plugin.getLogger().fine("AI Security: Threat feed update issue: " + exception.getMessage());
        }
    }

    private void gatherIntelligence() {
        Iterator<IPIntelligenceProfile> iterator2 = this.ipIntelligenceProfiles.values().iterator();
        while (iterator2.hasNext()) {
            IPIntelligenceProfile ipProfile = iterator2.next();
            if (!ipProfile.isHighRisk()) continue;
            this.queryExternalAPIs(ipProfile.ip);
        }
        return;
    }

    private void analyzePatterns() {
        Iterator<Map.Entry<String, EnterprisePlayerProfile>> iterator2 = this.playerProfiles.entrySet().iterator();
        while (iterator2.hasNext()) {
            NeuralPattern pattern;
            Map.Entry<String, EnterprisePlayerProfile> entry = iterator2.next();
            String uuid = entry.getKey();
            EnterprisePlayerProfile profile = entry.getValue();
            if (profile.getAnomalyScore() > 50.0 && !this.monitoredPlayers.contains(uuid)) {
                this.monitoredPlayers.add(uuid);
                Player player = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
                if (player != null) {
                    this.startBehaviorMonitoring(player);
                }
            }
            if ((pattern = this.neuralPatterns.get(uuid)) == null) continue;
            double[] features = new double[]{profile.getTrustScore() / 100.0, profile.getBehavioralConsistency() / 100.0, profile.anomalyScore / 100.0};
            pattern.addFeatureVector(features);
        }
        return;
    }

    private void optimizeSystem() {
        this.plugin.getLogger().info("\ud83d\udd04 Optimizing enterprise security system...");
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30L);
        int removed = 0;
        Iterator<Map.Entry<String, EnterprisePlayerProfile>> it = this.playerProfiles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EnterprisePlayerProfile> entry2 = it.next();
            EnterprisePlayerProfile profile = entry2.getValue();
            if (profile.getLastSeen() >= cutoff || !(profile.getTrustScore() < 20.0)) continue;
            it.remove();
            this.neuralPatterns.remove(entry2.getKey());
            this.behavioralBiometrics.remove(entry2.getKey());
            this.deviceFingerprints.remove(entry2.getKey());
            this.networkSignatures.remove(entry2.getKey());
            ++removed;
        }
        if (removed > 0) {
            this.plugin.getLogger().info("\ud83e\uddf9 Cleaned up " + removed + " old player profiles");
        }
        this.intelligenceCache.cleanUp();
        this.behavioralPatterns.entrySet().removeIf(entry -> ((AdvancedBehavioralPattern)entry.getValue()).getLastSeen() < cutoff);
        this.plugin.getLogger().info("\u2705 System optimization complete");
    }

    private void checkThreatFeeds() {
        try {
            if (Math.random() < 0.1) {
                String newThreat = "10.0." + (int)(Math.random() * 255.0) + "." + (int)(Math.random() * 255.0);
                this.knownMalwareIPs.add(newThreat);
                this.logEnterprise("NEW_THREAT_ADDED", "SYSTEM", newThreat, "Added to malware IP list");
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void queryExternalAPIs(String ip) {
        this.intelligenceExecutor.submit(() -> {
            try {
                this.queryIPQualityScore(ip);
                this.queryAbuseIPDB(ip);
                this.intelligenceCache.put(ip + "_intel", new Object());
            }
            catch (Exception e) {
                this.plugin.getLogger().fine("Failed to query external API for IP " + ip + ": " + e.getMessage());
            }
        });
    }

    private void queryIPQualityScore(String ip) {
        // Placeholder for a real async HTTP check; avoid blocking the thread with Thread.sleep
        this.plugin.getLogger().finest("AI Security: queryIPQualityScore for " + ip);
    }

    private void queryAbuseIPDB(String ip) {
        // Placeholder for abuseipdb async call; do not block executor threads here
        this.plugin.getLogger().finest("AI Security: queryAbuseIPDB for " + ip);
    }

    public void scanPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return;
        }
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();
        EnterprisePlayerProfile profile = this.getOrCreateEnterpriseProfile(uuid, playerName, ip);
        EnterpriseRiskAssessment assessment = this.performEnterpriseRiskAssessment(profile, ip);
        LinkedHashMap<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("scan_time", System.currentTimeMillis());
        report.put("player_name", playerName);
        report.put("player_uuid", uuid);
        report.put("ip_address", ip);
        report.put("risk_assessment", assessment.confidence);
        report.put("risk_level", assessment.getRiskLevel());
        report.put("recommendation", assessment.getRecommendation());
        report.put("high_risk_factors", assessment.highRiskFactors);
        report.put("evidence", assessment.evidence);
        this.plugin.getLogger().info("\ud83d\udd0d Security Scan Report for " + playerName + ":");
        for (Map.Entry entry : report.entrySet()) {
            this.plugin.getLogger().info("  " + (String)entry.getKey() + ": " + String.valueOf(entry.getValue()));
        }
        Iterator iterator2 = Bukkit.getOnlinePlayers().iterator();
        while (iterator2.hasNext()) {
            Player admin = (Player)iterator2.next();
            if (!admin.hasPermission("verifyx.security.notify")) continue;
            admin.sendMessage(String.valueOf(ChatColor.RED) + "\ud83d\udd0d Security scan completed for " + playerName);
            admin.sendMessage(String.valueOf(ChatColor.YELLOW) + "Risk Level: " + assessment.getRiskLevel());
            admin.sendMessage(String.valueOf(ChatColor.YELLOW) + "Confidence: " + String.format("%.1f", assessment.confidence) + "%");
        }
        return;
    }

    public void trustPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return;
        }
        String uuid = player.getUniqueId().toString();
        this.trustedPlayers.add(uuid);
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile != null) {
            profile.trustScore = 100.0;
        }
        this.plugin.getLogger().info("\u2705 Added " + playerName + " to trusted players list");
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "You have been added to the trusted players list!");
    }

    public void untrustPlayer(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return;
        }
        String uuid = player.getUniqueId().toString();
        this.trustedPlayers.remove(uuid);
        this.plugin.getLogger().info("\u26a0\ufe0f Removed " + playerName + " from trusted players list");
        if (player.isOnline()) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "You have been removed from the trusted players list.");
        }
    }

    public Map<String, Object> getSystemStats() {
        LinkedHashMap<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("total_assessments", this.totalAssessments.get());
        stats.put("blocked_threats", this.blockedThreats.get());
        stats.put("false_positives", this.falsePositives.get());
        stats.put("true_positives", this.truePositives.get());
        stats.put("false_positive_rate", String.format("%.2f%%", (double)this.falsePositives.get() / (double)Math.max(1, this.totalAssessments.get()) * 100.0));
        stats.put("player_profiles", this.playerProfiles.size());
        stats.put("trusted_players", this.trustedPlayers.size());
        stats.put("monitored_players", this.monitoredPlayers.size());
        stats.put("blocked_players", this.enterpriseBlocked.size());
        stats.put("ip_intelligence_profiles", this.ipIntelligenceProfiles.size());
        stats.put("device_fingerprints", this.deviceFingerprints.size());
        stats.put("behavioral_profiles", this.behavioralBiometrics.size());
        stats.put("trust_threshold", this.trustThreshold + "%");
        stats.put("block_threshold", this.absoluteBlockThreshold + "%");
        stats.put("learning_mode", this.learningMode);
        stats.put("enterprise_mode", this.enterpriseMode);
        Runtime runtime = Runtime.getRuntime();
        stats.put("memory_used", runtime.totalMemory() - runtime.freeMemory());
        stats.put("memory_max", runtime.maxMemory());
        stats.put("memory_usage", String.format("%.1f%%", (double)(runtime.totalMemory() - runtime.freeMemory()) / (double)runtime.maxMemory() * 100.0));
        return stats;
    }

    public void resetPlayerModel(String playerName) {
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return;
        }
        String uuid = player.getUniqueId().toString();
        this.neuralPatterns.put(uuid, new NeuralPattern(uuid));
        this.behavioralBiometrics.put(uuid, new BehavioralBiometrics(uuid));
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile != null) {
            profile.anomalyScore = 0.0;
        }
        this.plugin.getLogger().info("\ud83d\udd04 Reset learning models for " + playerName);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Your security learning model has been reset.");
    }

    public void shutdown() {
        this.plugin.getLogger().info("\ud83d\uded1 Shutting down enterprise security system...");
        this.saveEnterpriseData();
        this.saveFirstPlayers();
        this.saveIPAssociations();
        this.intelligenceExecutor.shutdown();
        this.scheduler.shutdown();
        try {
            if (!this.intelligenceExecutor.awaitTermination(10L, TimeUnit.SECONDS)) {
                this.intelligenceExecutor.shutdownNow();
            }
            if (!this.scheduler.awaitTermination(10L, TimeUnit.SECONDS)) {
                this.scheduler.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.intelligenceExecutor.shutdownNow();
            this.scheduler.shutdownNow();
        }
        this.plugin.getLogger().info("\ud83d\udcca Final Statistics:");
        this.plugin.getLogger().info("   Total Assessments: " + this.totalAssessments.get());
        this.plugin.getLogger().info("   Blocked Threats: " + this.blockedThreats.get());
        this.plugin.getLogger().info("   False Positives: " + this.falsePositives.get());
        this.plugin.getLogger().info("   False Positive Rate: " + String.format("%.2f", (double)this.falsePositives.get() / (double)Math.max(1, this.totalAssessments.get()) * 100.0) + "%");
        this.plugin.getLogger().info("\u2705 Enterprise security system shutdown complete");
    }

    public boolean isPlayerAlt(String playerName) {
        String uuid = this.getPlayerUUID(playerName);
        if (uuid == null) {
            return false;
        }
        
        // If player is already verified in database, don't treat them as alt
        try {
            boolean isVerified = this.plugin.getDatabaseManager().isPlayerVerified(uuid).join();
            if (isVerified) {
                return false;
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error checking if player is verified: " + e.toString());
        }
        
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile == null) {
            return false;
        }
        // If admin has explicitly allowed this UUID as an alt via forceverify, treat as not-alt
        if (this.allowedAlts.contains(uuid)) {
            return false;
        }
        AltDetectionResult result = this.performAdvancedAltDetection(profile);
        return result.isAltDetected() && result.getConfidence() >= (double)this.trustThreshold;
    }

    public void markForceVerified(String uuid) {
        if (uuid == null) return;
        this.allowedAlts.add(uuid);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, this::saveAllowedAlts);
        this.plugin.getLogger().info("Marked UUID as admin-allowed: " + uuid);
    }

    public boolean isAltOtpEnforced() {
        return this.altOtp;
    }

    public Map<String, Object> getPlayerSecurityReport(String playerName) {
        HashMap<String, Object> report = new HashMap<String, Object>();
        String uuid = this.getPlayerUUID(playerName);
        if (uuid == null) {
            report.put("error", "Player not found");
            return report;
        }
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile == null) {
            report.put("error", "No profile data");
            return report;
        }
        report.put("player_name", profile.getPlayerName());
        report.put("trust_score", profile.getTrustScore());
        report.put("reputation_tier", profile.getReputationTier());
        report.put("session_count", profile.getSessionCount());
        report.put("behavioral_consistency", profile.getBehavioralConsistency());
        EnterpriseRiskAssessment assessment = this.performEnterpriseRiskAssessment(profile, profile.getCurrentIP());
        report.put("risk_score", assessment.confidence);
        report.put("risk_level", assessment.getRiskLevel());
        report.put("high_risk_factors", assessment.highRiskFactors);
        return report;
    }

    public String getFirstPlayerNameOnIP(String ip) {
        String baseIP = this.getBaseIP(ip);
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        if (firstPlayerUUID == null) {
            return null;
        }
        Player player = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
        if (player != null) {
            return player.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(firstPlayerUUID));
        return offlinePlayer.getName();
    }

    public boolean isPlayerFirstOnIP(String playerUUID, String ip) {
        String baseIP = this.getBaseIP(ip);
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        return firstPlayerUUID != null && firstPlayerUUID.equals(playerUUID);
    }

    public List<String> getIPsWherePlayerIsFirst(String playerUUID) {
        ArrayList<String> ips = new ArrayList<String>();
        Iterator<Map.Entry<String, String>> iterator2 = this.firstPlayerOnIP.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<String, String> entry = iterator2.next();
            if (!entry.getValue().equals(playerUUID)) continue;
            ips.add(entry.getKey());
        }
        return ips;
    }

    public Set<String> getAccountsOnIP(String ip) {
        String baseIP = this.getBaseIP(ip);
        return this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
    }

    public Map<String, Object> getIPInfo(String ip) {
        HashMap<String, Object> info = new HashMap<String, Object>();
        String baseIP = this.getBaseIP(ip);
        Set<String> accounts = this.getAccountsOnIP(baseIP);
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        info.put("ip", baseIP);
        info.put("total_accounts", accounts.size());
        info.put("max_allowed", this.maxAltsPerIP);
        info.put("first_player_uuid", firstPlayerUUID);
        if (firstPlayerUUID != null) {
            info.put("first_player_name", this.getFirstPlayerNameOnIP(baseIP));
        }
        ArrayList<CallSite> accountNames = new ArrayList<CallSite>();
        Iterator<String> iterator2 = accounts.iterator();
        while (true) {
            if (!iterator2.hasNext()) {
                info.put("accounts", accountNames);
                return info;
            }
            String uuid = iterator2.next();
            Player player = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
            if (player != null) {
                accountNames.add((CallSite)((Object)(player.getName() + " (online)")));
                continue;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid));
            if (offlinePlayer.getName() == null) continue;
            accountNames.add((CallSite)((Object)(offlinePlayer.getName() + " (offline)")));
        }
    }

    public Map<String, Object> getIPDetailsForCommand(String ip) {
        HashMap<String, Object> details = new HashMap<String, Object>();
        String baseIP = this.getBaseIP(ip);
        Set accountsOnIP = this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        details.put("ip", baseIP);
        details.put("total_accounts", accountsOnIP.size());
        details.put("max_allowed", this.maxAltsPerIP);
        details.put("is_over_limit", accountsOnIP.size() > this.maxAltsPerIP);
        if (firstPlayerUUID != null) {
            details.put("first_player_uuid", firstPlayerUUID);
            String firstName = this.getPlayerNameFromUUID(firstPlayerUUID);
            details.put("first_player_name", firstName);
            Player firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
            details.put("first_player_online", firstPlayer != null);
        }
        ArrayList accountsList = new ArrayList();
        Iterator iterator2 = accountsOnIP.iterator();
        while (true) {
            if (!iterator2.hasNext()) {
                details.put("accounts", accountsList);
                return details;
            }
            String uuid = (String)iterator2.next();
            HashMap<String, Object> accountInfo = new HashMap<String, Object>();
            accountInfo.put("uuid", uuid);
            String playerName = this.getPlayerNameFromUUID(uuid);
            accountInfo.put("name", playerName);
            boolean isFirst = uuid.equals(firstPlayerUUID);
            accountInfo.put("is_first_player", isFirst);
            Player player = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
            accountInfo.put("online", player != null);
            EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
            if (profile != null) {
                accountInfo.put("trust_score", profile.getTrustScore());
                accountInfo.put("risk_level", profile.getReputationTier());
            }
            accountsList.add(accountInfo);
        }
    }

    public Map<String, Object> getPlayerIPStatusForCommand(String playerName) {
        HashMap<String, Object> status = new HashMap<String, Object>();
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            status.put("error", "Player not online");
            return status;
        }
        String uuid = player.getUniqueId().toString();
        String ip = player.getAddress().getAddress().getHostAddress();
        String baseIP = this.getBaseIP(ip);
        status.put("player_name", playerName);
        status.put("uuid", uuid);
        status.put("current_ip", ip);
        status.put("base_ip", baseIP);
        String firstPlayerUUID = this.firstPlayerOnIP.get(baseIP);
        boolean isFirst = uuid.equals(firstPlayerUUID);
        status.put("is_first_player", isFirst);
        if (!isFirst && firstPlayerUUID != null) {
            String firstPlayerName = this.getPlayerNameFromUUID(firstPlayerUUID);
            status.put("first_player_name", firstPlayerName);
            Player firstPlayer = Bukkit.getPlayer((UUID)UUID.fromString(firstPlayerUUID));
            status.put("first_player_online", firstPlayer != null);
        }
        Set accountsOnIP = this.ipAssociationGraph.getOrDefault(baseIP, Collections.emptySet());
        status.put("total_accounts_on_ip", accountsOnIP.size());
        EnterprisePlayerProfile profile = this.playerProfiles.get(uuid);
        if (profile != null) {
            status.put("trust_score", profile.getTrustScore());
            status.put("risk_level", profile.getReputationTier());
            EnterpriseRiskAssessment assessment = this.performEnterpriseRiskAssessment(profile, ip);
            status.put("risk_score", assessment.confidence);
            status.put("alt_risk", !isFirst && assessment.confidence > 50.0);
        }
        return status;
    }

    public List<String> getPlayerFirstIPsForCommand(String playerName) {
        ArrayList<String> ips = new ArrayList<String>();
        Player player = Bukkit.getPlayerExact((String)playerName);
        if (player == null) {
            return ips;
        }
        String uuid = player.getUniqueId().toString();
        Iterator<Map.Entry<String, String>> iterator2 = this.firstPlayerOnIP.entrySet().iterator();
        while (iterator2.hasNext()) {
            Map.Entry<String, String> entry = iterator2.next();
            if (!entry.getValue().equals(uuid)) continue;
            ips.add(entry.getKey());
        }
        return ips;
    }

    private void loadFirstPlayers() {
        if (this.firstPlayersFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.firstPlayersFile));){
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length != 2) continue;
                    this.firstPlayerOnIP.put(parts[0], parts[1]);
                }
                this.plugin.getLogger().info("Loaded " + this.firstPlayerOnIP.size() + " first player records");
            }
            catch (IOException e) {
                this.plugin.getLogger().warning("Failed to load first players: " + e.getMessage());
            }
        }
    }

    private void saveFirstPlayers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.firstPlayersFile));){
            for (Map.Entry<String, String> entry : this.firstPlayerOnIP.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            this.plugin.getLogger().info("Saved " + this.firstPlayerOnIP.size() + " first player records");
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save first players: " + e.getMessage());
        }
    }

    private void loadIPAssociations() {
        if (this.ipAssociationsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.ipAssociationsFile));){
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length != 2) continue;
                    String ip = parts[0];
                    String[] uuids = parts[1].split(",");
                    ConcurrentHashMap.KeySetView uuidSet = ConcurrentHashMap.newKeySet();
                    uuidSet.addAll(Arrays.asList(uuids));
                    this.ipAssociationGraph.put(ip, uuidSet);
                }
                this.plugin.getLogger().info("Loaded " + this.ipAssociationGraph.size() + " IP association records");
            }
            catch (IOException e) {
                this.plugin.getLogger().warning("Failed to load IP associations: " + e.getMessage());
            }
        }
    }

    public String getPlayerNameFromUUID(String uuid) {
        try {
            Player player = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
            if (player != null) {
                return player.getName();
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID)UUID.fromString(uuid));
            if (offlinePlayer.getName() != null) {
                return offlinePlayer.getName() + " (offline)";
            }
            return "Unknown";
        }
        catch (Exception e) {
            return "Invalid UUID";
        }
    }

    private void saveIPAssociations() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.ipAssociationsFile));){
            for (Map.Entry<String, Set<String>> entry : this.ipAssociationGraph.entrySet()) {
                String uuids = String.join((CharSequence)",", (Iterable<? extends CharSequence>)entry.getValue());
                writer.write(entry.getKey() + "=" + uuids);
                writer.newLine();
            }
            this.plugin.getLogger().info("Saved " + this.ipAssociationGraph.size() + " IP association records");
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Failed to save IP associations: " + e.getMessage());
        }
    }

    private static class Cache<K, V> {
        private final Map<K, V> map = new ConcurrentHashMap();
        private final Map<K, Long> timestamps = new ConcurrentHashMap<K, Long>();
        private final int maxSize;
        private final long expireAfterMillis;

        Cache(int maxSize, long expireAfterMillis) {
            this.maxSize = maxSize;
            this.expireAfterMillis = expireAfterMillis;
        }

        V get(K key) {
            Long timestamp = this.timestamps.get(key);
            if (timestamp != null && System.currentTimeMillis() - timestamp > this.expireAfterMillis) {
                this.map.remove(key);
                this.timestamps.remove(key);
                return null;
            }
            return this.map.get(key);
        }

        void put(K key, V value) {
            if (this.map.size() >= this.maxSize) {
                this.cleanUp();
            }
            this.map.put(key, value);
            this.timestamps.put(key, System.currentTimeMillis());
        }

        void invalidate(K key) {
            this.map.remove(key);
            this.timestamps.remove(key);
        }

        void invalidateAll() {
            this.map.clear();
            this.timestamps.clear();
        }

        void cleanUp() {
            long now = System.currentTimeMillis();
            this.timestamps.entrySet().removeIf(entry -> {
                if (now - (Long)entry.getValue() > this.expireAfterMillis) {
                    this.map.remove(entry.getKey());
                    return true;
                }
                return false;
            });
            if (this.map.size() >= this.maxSize) {
                int toRemove = this.map.size() - this.maxSize / 2;
                this.timestamps.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(toRemove).forEach(entry -> {
                    this.map.remove(entry.getKey());
                    this.timestamps.remove(entry.getKey());
                });
            }
        }
    }

    private class EnterprisePlayerProfile {
        private final String uuid;
        private final String playerName;
        private final String firstIP;
        private final long firstSeen;
        private long lastSeen;
        private int sessionCount = 0;
        private int successfulLogins = 0;
        private int failedLogins = 0;
        private final Map<String, Integer> ipFrequency = new ConcurrentHashMap<String, Integer>();
        private final List<SessionRecord> sessionHistory = new ArrayList<SessionRecord>();
        private final List<Long> loginTimestamps = new ArrayList<Long>();
        private double trustScore = 50.0;
        private double anomalyScore = 0.0;
        private double behavioralConsistency = 0.0;
        private String currentIP;

        EnterprisePlayerProfile(String uuid, String playerName, String ip) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.firstIP = ip;
            this.currentIP = ip;
            this.lastSeen = this.firstSeen = System.currentTimeMillis();
            this.ipFrequency.put(ip, 1);
        }

        void recordSuccessfulLogin(String ip) {
            ++this.sessionCount;
            ++this.successfulLogins;
            this.lastSeen = System.currentTimeMillis();
            this.currentIP = ip;
            this.loginTimestamps.add(this.lastSeen);
            this.ipFrequency.merge(ip, 1, Integer::sum);
            SessionRecord session = new SessionRecord(this.lastSeen, ip, true);
            this.sessionHistory.add(session);
            this.updateTrustScore();
            this.updateBehavioralConsistency();
        }

        void recordFailedLogin(String ip) {
            ++this.failedLogins;
            this.trustScore = Math.max(0.0, this.trustScore - 5.0);
        }

        void recordLegitimateSession() {
            if (this.successfulLogins > 5) {
                this.trustScore = Math.min(100.0, this.trustScore + 2.0);
            }
            this.anomalyScore = Math.max(0.0, this.anomalyScore - 1.0);
        }

        private void updateTrustScore() {
            double sessionTrust = Math.min(70, this.successfulLogins * 5);
            double ipConsistency = Math.min(20.0, (double)this.ipFrequency.size() / (double)this.successfulLogins * 20.0);
            long ageDays = ChronoUnit.DAYS.between(Instant.ofEpochMilli(this.firstSeen), Instant.ofEpochMilli(this.lastSeen));
            double ageTrust = Math.min(10.0, (double)ageDays * 0.5);
            this.trustScore = sessionTrust + ipConsistency + ageTrust;
        }

        private void updateBehavioralConsistency() {
            if (this.loginTimestamps.size() < 3) {
                this.behavioralConsistency = 0.0;
                return;
            }
            ArrayList<Long> intervals = new ArrayList<Long>();
            int i2 = 1;
            while (true) {
                if (i2 >= this.loginTimestamps.size()) {
                    double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    double variance = intervals.stream().mapToDouble(i -> Math.pow((double)i.longValue() - mean, 2.0)).average().orElse(0.0);
                    this.behavioralConsistency = Math.max(0.0, 100.0 - variance / 1000.0);
                    return;
                }
                intervals.add(this.loginTimestamps.get(i2) - this.loginTimestamps.get(i2 - 1));
                ++i2;
            }
        }

        boolean hasSuspiciousTimingPattern() {
            if (this.loginTimestamps.size() < 5) {
                return false;
            }
            ArrayList<Long> intervals = new ArrayList<Long>();
            for (int i2 = 1; i2 < this.loginTimestamps.size(); ++i2) {
                intervals.add(this.loginTimestamps.get(i2) - this.loginTimestamps.get(i2 - 1));
            }
            double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double variance = intervals.stream().mapToDouble(i -> Math.pow((double)i.longValue() - mean, 2.0)).average().orElse(0.0);
            return variance < 100.0 && mean < 30000.0;
        }

        double getTimingAnomalyScore() {
            if (!this.hasSuspiciousTimingPattern()) {
                return 0.0;
            }
            return 100.0 - this.behavioralConsistency;
        }

        boolean hasUnusualSessionDuration() {
            if (this.sessionHistory.size() < 3) {
                return false;
            }
            double avgDuration = this.sessionHistory.stream().mapToLong(SessionRecord::getDuration).average().orElse(0.0);
            return this.sessionHistory.stream().anyMatch(s2 -> Math.abs((double)s2.getDuration() - avgDuration) > avgDuration * 2.0);
        }

        String getUuid() {
            return this.uuid;
        }

        String getPlayerName() {
            return this.playerName;
        }

        String getCurrentIP() {
            return this.currentIP;
        }

        long getFirstSeen() {
            return this.firstSeen;
        }

        long getLastSeen() {
            return this.lastSeen;
        }

        int getSessionCount() {
            return this.sessionCount;
        }

        int getSuccessfulLogins() {
            return this.successfulLogins;
        }

        double getTrustScore() {
            return this.trustScore;
        }

        double getAnomalyScore() {
            return this.anomalyScore;
        }

        double getBehavioralConsistency() {
            return this.behavioralConsistency;
        }

        double getPatternStability() {
            return Math.min(100.0, this.behavioralConsistency * 1.5);
        }

        Map<String, Integer> getIPHistory() {
            return new HashMap<String, Integer>(this.ipFrequency);
        }

        String getReputationTier() {
            if (this.trustScore >= 80.0) {
                return "TRUSTED";
            }
            if (this.trustScore >= 60.0) {
                return "ESTABLISHED";
            }
            if (this.trustScore >= 40.0) {
                return "NEUTRAL";
            }
            if (this.trustScore >= 20.0) {
                return "MONITORED";
            }
            return "HIGH_RISK";
        }

        double getMLConfidence() {
            NeuralPattern pattern = AISecurityManager.this.neuralPatterns.get(this.uuid);
            return pattern != null ? pattern.getConfidence() : 50.0;
        }

        boolean isPredictedLegitimate() {
            NeuralPattern pattern = AISecurityManager.this.neuralPatterns.get(this.uuid);
            return pattern != null && pattern.predictLegitimacy() > 0.7;
        }
    }

    private class EnterpriseRiskAssessment {
        int highRiskFactors = 0;
        double confidence = 0.0;
        final Map<String, Integer> evidence = new HashMap<String, Integer>();
        boolean hasConcreteEvidence = false;
        boolean trustDiscountApplied = false;
        double mlConfidence = 0.0;
        int dataPoints = 0;

        private EnterpriseRiskAssessment() {
        }

        void addEvidence(String type, int score) {
            this.evidence.put(type, score);
        }

        void finalizeAssessment() {
            Iterator<Integer> iterator2 = this.evidence.values().iterator();
            while (true) {
                if (!iterator2.hasNext()) {
                    this.confidence = Math.min(100.0, this.confidence);
                    this.highRiskFactors = (int)this.evidence.values().stream().filter(score -> score >= 15).count();
                    return;
                }
                int score2 = iterator2.next();
                this.confidence += (double)score2;
            }
        }

        String getRiskLevel() {
            if (this.confidence >= 80.0) {
                return "CRITICAL";
            }
            if (this.confidence >= 60.0) {
                return "HIGH";
            }
            if (this.confidence >= 40.0) {
                return "MEDIUM";
            }
            if (this.confidence >= 20.0) {
                return "LOW";
            }
            return "MINIMAL";
        }

        String getRecommendation() {
            if (this.confidence >= (double)AISecurityManager.this.trustThreshold) {
                return "BLOCK - High confidence threat detected";
            }
            if (this.confidence >= 60.0) {
                return "MONITOR - Elevated risk, require additional verification";
            }
            if (this.confidence >= 40.0) {
                return "WATCH - Some risk factors present";
            }
            return "ALLOW - Minimal risk detected";
        }

        String getEvidenceSummary() {
            return this.evidence.entrySet().stream().sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())).limit(3L).map(e -> (String)e.getKey() + "(" + String.valueOf(e.getValue()) + "%)").collect(Collectors.joining(", "));
        }
    }

    private class IPIntelligenceProfile {
        private final String ip;
        private final String isp;
        private final String country;
        private final String city;
        private final boolean isDatacenter;
        private final boolean isVPN;
        private final boolean isProxy;
        private final int abuseScore;
        private final int threatLevel;
        private final long firstSeen;
        private long lastSeen;
        private int successfulLogins = 0;
        private int failedLogins = 0;
        private final Set<String> associatedAccounts = ConcurrentHashMap.newKeySet();

        IPIntelligenceProfile(String ip) {
            this.ip = ip;
            this.isp = this.resolveISP(ip);
            this.country = this.resolveCountry(ip);
            this.city = this.resolveCity(ip);
            this.isDatacenter = this.checkIfDatacenter(ip);
            this.isVPN = this.checkIfVPN(ip);
            this.isProxy = this.checkIfProxy(ip);
            this.abuseScore = this.getAbuseScore(ip);
            this.threatLevel = this.calculateThreatLevel();
            this.lastSeen = this.firstSeen = System.currentTimeMillis();
        }

        boolean isHighRisk() {
            return this.threatLevel >= 7 || this.abuseScore > 80 || this.isVPN || this.isProxy;
        }

        int getRiskScore() {
            int score = 0;
            if (this.isVPN) {
                score += 40;
            }
            if (this.isProxy) {
                score += 30;
            }
            if (this.isDatacenter) {
                score += 20;
            }
            score = (int)((double)score + Math.min(30.0, (double)this.abuseScore * 0.3));
            return Math.min(100, score);
        }

        boolean isDatacenterIP() {
            return this.isDatacenter;
        }

        String getISP() {
            return this.isp;
        }

        boolean hasGeolocationAnomaly(EnterprisePlayerProfile profile) {
            return false;
        }

        void recordSuccessfulLogin(String uuid) {
            ++this.successfulLogins;
            this.lastSeen = System.currentTimeMillis();
            this.associatedAccounts.add(uuid);
        }

        void recordFailedLogin() {
            ++this.failedLogins;
        }

        private String resolveISP(String ip) {
            return "unknown";
        }

        private String resolveCountry(String ip) {
            return "unknown";
        }

        private String resolveCity(String ip) {
            return "unknown";
        }

        private boolean checkIfDatacenter(String ip) {
            return this.knownDatacenterRanges().stream().anyMatch(ip::startsWith);
        }

        private boolean checkIfVPN(String ip) {
            return AISecurityManager.this.knownVPNProviders.stream().anyMatch(ip::startsWith);
        }

        private boolean checkIfProxy(String ip) {
            return AISecurityManager.this.knownProxyNetworks.stream().anyMatch(ip::startsWith);
        }

        private int getAbuseScore(String ip) {
            return 0;
        }

        private int calculateThreatLevel() {
            int level = 0;
            if (this.isVPN) {
                level += 3;
            }
            if (this.isProxy) {
                level += 3;
            }
            if (this.isDatacenter) {
                level += 2;
            }
            if (this.abuseScore > 50) {
                level += 2;
            }
            if (this.abuseScore > 80) {
                level += 3;
            }
            return Math.min(10, level);
        }

        private Set<String> knownDatacenterRanges() {
            return Set.of("5.188.", "45.142.", "104.244.", "172.67.", "104.21.", "185.220.", "193.11.", "209.141.", "198.98.", "23.128.");
        }
    }

    private class DeviceFingerprintEnterprise {
        private final String uuid;
        private final String fingerprintHash;
        private final Map<String, String> attributes = new ConcurrentHashMap<String, String>();
        private final List<DeviceChange> changeHistory = new ArrayList<DeviceChange>();
        private int anomalyScore = 0;
        private int stabilityScore = 100;

        DeviceFingerprintEnterprise(Player player) {
            this.uuid = player.getUniqueId().toString();
            this.attributes.putAll(this.collectEnterpriseAttributes(player));
            this.fingerprintHash = this.generateEnterpriseHash(this.attributes);
        }

        private Map<String, String> collectEnterpriseAttributes(Player player) {
            HashMap<String, String> attrs = new HashMap<String, String>();
            attrs.put("hw.cpu_cores", String.valueOf(Runtime.getRuntime().availableProcessors()));
            attrs.put("hw.memory_total", String.valueOf(Runtime.getRuntime().totalMemory()));
            attrs.put("hw.memory_max", String.valueOf(Runtime.getRuntime().maxMemory()));
            attrs.put("hw.os_name", System.getProperty("os.name"));
            attrs.put("hw.os_version", System.getProperty("os.version"));
            attrs.put("hw.os_arch", System.getProperty("os.arch"));
            attrs.put("hw.java_version", System.getProperty("java.version"));
            attrs.put("hw.timezone", TimeZone.getDefault().getID());
            int ping = 0;
            try {
                ping = player.getPing();
            }
            catch (Exception e) {
                ping = -1;
            }
            attrs.put("net.ping_avg", String.valueOf(ping));
            attrs.put("net.locale", player.getLocale());
            attrs.put("net.client_brand", AISecurityManager.this.getClientBrand(player));
            attrs.put("client.view_distance", String.valueOf(player.getViewDistance()));
            attrs.put("client.protocol_version", String.valueOf(player.getProtocolVersion()));
            attrs.put("client.render_distance", String.valueOf(player.getClientViewDistance()));
            attrs.put("behavior.join_time", String.valueOf(System.currentTimeMillis() % 86400000L));
            attrs.put("behavior.session_id", UUID.randomUUID().toString());
            return attrs;
        }

        private String generateEnterpriseHash(Map<String, String> attrs) {
            try {
                StringBuilder entropy = new StringBuilder();
                ArrayList<String> keys2 = new ArrayList<String>(attrs.keySet());
                Collections.sort(keys2);
                for (String key : keys2) {
                    String value = attrs.get(key);
                    entropy.append(key).append("=").append(value).append(";");
                    if (!key.startsWith("hw.") && !key.startsWith("net.")) continue;
                    entropy.append("salt=").append(System.nanoTime() % 100L).append(";");
                }
                MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                byte[] hash = digest.digest(entropy.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                int i = 0;
                while (true) {
                    if (i >= hash.length) {
                        return hex.toString();
                    }
                    hex.append(String.format("%02x", hash[i]));
                    if ((i + 1) % 8 == 0 && i < hash.length - 1) {
                        hex.append("-");
                    }
                    ++i;
                }
            }
            catch (Exception e) {
                return "fp-err-" + System.currentTimeMillis();
            }
        }

        boolean matches(DeviceFingerprintEnterprise other) {
            String[] criticalAttrs;
            if (other == null) {
                return false;
            }
            int matchCount = 0;
            int totalCritical = 0;
            for (String attr : criticalAttrs = new String[]{"hw.os_arch", "hw.java_version", "hw.cpu_cores", "net.locale"}) {
                if (!this.attributes.containsKey(attr) || !other.attributes.containsKey(attr)) continue;
                ++totalCritical;
                if (!this.attributes.get(attr).equals(other.attributes.get(attr))) continue;
                ++matchCount;
            }
            return totalCritical > 0 && matchCount * 100 / totalCritical >= 75;
        }

        boolean matchesKnownBotPattern() {
            String osPattern = this.attributes.get("hw.os_name");
            String javaPattern = this.attributes.get("hw.java_version");
            if (osPattern != null && osPattern.contains("BotOS")) {
                return true;
            }
            return javaPattern != null && javaPattern.matches("1\\.8\\.0_\\d{2,3}");
        }

        boolean hasHighAnomalyScore() {
            return this.anomalyScore > 60;
        }

        boolean hasRapidChanges() {
            if (this.changeHistory.size() < 3) {
                return false;
            }
            long totalTime = this.changeHistory.get((int)(this.changeHistory.size() - 1)).timestamp - this.changeHistory.get((int)0).timestamp;
            return totalTime < 3600000L && this.changeHistory.size() > 5;
        }

        int getAnomalyScore() {
            return this.anomalyScore;
        }

        int getStabilityScore() {
            return this.stabilityScore;
        }

        int getChangeCount() {
            return this.changeHistory.size();
        }

        String getFingerprintHash() {
            return this.fingerprintHash;
        }

        void recordChange(String changeType) {
            this.changeHistory.add(new DeviceChange(changeType, System.currentTimeMillis()));
            if (this.changeHistory.size() > 3) {
                this.stabilityScore = Math.max(0, 100 - this.changeHistory.size() * 5);
            }
            if (this.hasRapidChanges()) {
                this.anomalyScore = Math.min(100, this.anomalyScore + 20);
            }
        }

        private class DeviceChange {
            final String type;
            final long timestamp;

            DeviceChange(String type, long timestamp) {
                this.type = type;
                this.timestamp = timestamp;
            }
        }
    }

    private class BehavioralBiometrics {
        private final String uuid;
        private final List<ClickPattern> clickPatterns = new ArrayList<ClickPattern>();
        private final List<MovementPattern> movementPatterns = new ArrayList<MovementPattern>();
        private final List<ActionTiming> actionTimings = new ArrayList<ActionTiming>();
        private ClickBaseline clickBaseline;
        private MovementBaseline movementBaseline;

        BehavioralBiometrics(String uuid) {
            this.uuid = uuid;
        }

        void recordClick(long timestamp, Action action, Location location) {
            ClickPattern pattern = new ClickPattern(timestamp, action, location);
            this.clickPatterns.add(pattern);
            if (this.clickPatterns.size() >= 50 && this.clickBaseline == null) {
                this.establishClickBaseline();
            }
            if (this.clickPatterns.size() > 200) {
                this.clickPatterns.remove(0);
            }
        }

        void recordMovement(long timestamp, Location from, Location to) {
            MovementPattern pattern = new MovementPattern(timestamp, from, to);
            this.movementPatterns.add(pattern);
            if (this.movementPatterns.size() >= 100 && this.movementBaseline == null) {
                this.establishMovementBaseline();
            }
            if (this.movementPatterns.size() > 500) {
                this.movementPatterns.remove(0);
            }
        }

        void recordActionTiming(long timestamp, String actionType) {
            this.actionTimings.add(new ActionTiming(timestamp, actionType));
            if (this.actionTimings.size() > 1000) {
                this.actionTimings.remove(0);
            }
        }

        private void establishClickBaseline() {
            if (this.clickPatterns.size() < 20) {
                return;
            }
            ArrayList<Long> intervals = new ArrayList<Long>();
            int i2 = 1;
            while (true) {
                if (i2 >= this.clickPatterns.size()) {
                    double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    double variance = intervals.stream().mapToDouble(i -> Math.pow((double)i.longValue() - mean, 2.0)).average().orElse(0.0);
                    this.clickBaseline = new ClickBaseline(mean, variance);
                    return;
                }
                intervals.add(this.clickPatterns.get((int)i2).timestamp - this.clickPatterns.get((int)(i2 - 1)).timestamp);
                ++i2;
            }
        }

        private void establishMovementBaseline() {
            if (this.movementPatterns.size() < 50) {
                return;
            }
            List distances = this.movementPatterns.stream().map(MovementPattern::getDistance).collect(Collectors.toList());
            ArrayList<Long> intervals = new ArrayList<Long>();
            int i2 = 1;
            while (true) {
                if (i2 >= this.movementPatterns.size()) {
                    double distanceMean = distances.stream().mapToDouble(d -> ((Number)d).doubleValue()).average().orElse(0.0);
                    double distanceVariance = distances.stream().mapToDouble(d -> Math.pow(((Number)d).doubleValue() - distanceMean, 2.0)).average().orElse(0.0);
                    double intervalMean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    double intervalVariance = intervals.stream().mapToDouble(i -> Math.pow((double)i.longValue() - intervalMean, 2.0)).average().orElse(0.0);
                    this.movementBaseline = new MovementBaseline(distanceMean, distanceVariance, intervalMean, intervalVariance);
                    return;
                }
                intervals.add(this.movementPatterns.get((int)i2).timestamp - this.movementPatterns.get((int)(i2 - 1)).timestamp);
                ++i2;
            }
        }

        boolean matchesLegitimatePattern() {
            if (this.clickBaseline == null || this.movementBaseline == null) {
                return true;
            }
            return this.isClickPatternLegitimate() && this.isMovementPatternLegitimate();
        }

        boolean hasAutomationPatterns() {
            if (this.clickBaseline == null) {
                return false;
            }
            ArrayList<Long> recentIntervals = new ArrayList<Long>();
            int start = Math.max(0, this.clickPatterns.size() - 20);
            for (int i = start + 1; i < this.clickPatterns.size(); ++i) {
                recentIntervals.add(this.clickPatterns.get((int)i).timestamp - this.clickPatterns.get((int)(i - 1)).timestamp);
            }
            if (recentIntervals.size() < 5) {
                return false;
            }
            double variance = this.calculateVariance(recentIntervals);
            return variance < 50.0;
        }

        boolean isClonedFrom(BehavioralBiometrics other) {
            if (this.clickBaseline == null || other.clickBaseline == null) {
                return false;
            }
            if (this.movementBaseline == null || other.movementBaseline == null) {
                return false;
            }
            double clickSimilarity = this.compareBaselines(this.clickBaseline, other.clickBaseline);
            double movementSimilarity = this.compareBaselines(this.movementBaseline, other.movementBaseline);
            return clickSimilarity > 0.8 && movementSimilarity > 0.8;
        }

        double getDeviationScore() {
            if (this.clickBaseline != null && this.movementBaseline != null) {
                double clickDeviation = this.calculateClickDeviation();
                double movementDeviation = this.calculateMovementDeviation();
                return (clickDeviation + movementDeviation) / 2.0;
            }
            return 0.0;
        }

        private double calculateClickDeviation() {
            ArrayList<Long> recentIntervals = new ArrayList<Long>();
            int start = Math.max(0, this.clickPatterns.size() - 30);
            for (int i2 = start + 1; i2 < this.clickPatterns.size(); ++i2) {
                recentIntervals.add(this.clickPatterns.get((int)i2).timestamp - this.clickPatterns.get((int)(i2 - 1)).timestamp);
            }
            if (recentIntervals.isEmpty()) {
                return 0.0;
            }
            double currentMean = recentIntervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double currentVariance = recentIntervals.stream().mapToDouble(i -> Math.pow((double)i.longValue() - currentMean, 2.0)).average().orElse(0.0);
            double baselineVariance = this.clickBaseline.variance;
            if (baselineVariance == 0.0) {
                return 0.0;
            }
            double deviation = Math.abs(currentVariance - baselineVariance) / baselineVariance;
            return Math.min(100.0, deviation * 100.0);
        }

        private double calculateMovementDeviation() {
            return 0.0;
        }

        private boolean isClickPatternLegitimate() {
            double deviation = this.calculateClickDeviation();
            return deviation < 30.0;
        }

        private boolean isMovementPatternLegitimate() {
            return true;
        }

        private double calculateVariance(List<Long> values2) {
            double mean = values2.stream().mapToLong(Long::longValue).average().orElse(0.0);
            return values2.stream().mapToDouble(v -> Math.pow((double)v.longValue() - mean, 2.0)).average().orElse(0.0);
        }

        private double compareBaselines(Object baseline1, Object baseline2) {
            return 0.5;
        }

        private class ClickPattern {
            final long timestamp;
            final Action action;
            final Location location;

            ClickPattern(long timestamp, Action action, Location location) {
                this.timestamp = timestamp;
                this.action = action;
                this.location = location;
            }
        }

        private class ClickBaseline {
            final double mean;
            final double variance;

            ClickBaseline(double mean, double variance) {
                this.mean = mean;
                this.variance = variance;
            }
        }

        private class MovementPattern {
            final long timestamp;
            final Location from;
            final Location to;

            MovementPattern(long timestamp, Location from, Location to) {
                this.timestamp = timestamp;
                this.from = from;
                this.to = to;
            }

            double getDistance() {
                return this.from.distance(this.to);
            }
        }

        private class MovementBaseline {
            final double distanceMean;
            final double distanceVariance;
            final double intervalMean;
            final double intervalVariance;

            MovementBaseline(double distanceMean, double distanceVariance, double intervalMean, double intervalVariance) {
                this.distanceMean = distanceMean;
                this.distanceVariance = distanceVariance;
                this.intervalMean = intervalMean;
                this.intervalVariance = intervalVariance;
            }
        }

        private class ActionTiming {
            final long timestamp;
            final String actionType;

            ActionTiming(long timestamp, String actionType) {
                this.timestamp = timestamp;
                this.actionType = actionType;
            }
        }
    }

    private class NetworkSignature {
        private final String uuid;
        private final List<PingSample> pingSamples = new ArrayList<PingSample>();
        private final List<PacketLossSample> packetLossSamples = new ArrayList<PacketLossSample>();
        private final List<JitterSample> jitterSamples = new ArrayList<JitterSample>();
        private int suspicionScore = 0;

        NetworkSignature(String uuid) {
            this.uuid = uuid;
        }

        void recordPing(int ping) {
            this.pingSamples.add(new PingSample(ping, System.currentTimeMillis()));
            if (this.pingSamples.size() > 100) {
                this.pingSamples.remove(0);
            }
            this.analyzePingPatterns();
        }

        void recordPacketLoss(double packetLoss) {
            this.packetLossSamples.add(new PacketLossSample(packetLoss, System.currentTimeMillis()));
            if (this.packetLossSamples.size() > 50) {
                this.packetLossSamples.remove(0);
            }
            this.analyzePacketLossPatterns();
        }

        void recordJitter(double jitter) {
            this.jitterSamples.add(new JitterSample(jitter, System.currentTimeMillis()));
            if (this.jitterSamples.size() > 50) {
                this.jitterSamples.remove(0);
            }
            this.analyzeJitterPatterns();
        }

        boolean hasSuspiciousPatterns() {
            return this.suspicionScore > 50;
        }

        boolean hasVPNCharacteristics() {
            if (this.pingSamples.size() < 10) {
                return false;
            }
            double avgPing = this.pingSamples.stream().mapToInt(PingSample::getPing).average().orElse(0.0);
            double pingVariance = this.pingSamples.stream().mapToDouble(p -> Math.pow((double)p.getPing() - avgPing, 2.0)).average().orElse(0.0);
            return avgPing > 100.0 && pingVariance < 100.0;
        }

        int getSuspicionScore() {
            return this.suspicionScore;
        }

        private void analyzePingPatterns() {
            if (this.pingSamples.size() < 20) {
                return;
            }
            List recentPings = this.pingSamples.stream().skip(this.pingSamples.size() - 20).map(PingSample::getPing).collect(Collectors.toList());
            List<Double> pingDoubles = new ArrayList<>();
            for (Object p : recentPings) {
                pingDoubles.add((double)((Number)p).intValue());
            }
            double variance = this.calculateVariance(pingDoubles);
            if (variance < 10.0) {
                this.suspicionScore = Math.min(100, this.suspicionScore + 10);
            }
        }

        private void analyzePacketLossPatterns() {
        }

        private void analyzeJitterPatterns() {
        }

        private double calculateVariance(List<Double> values2) {
            double mean = values2.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return values2.stream().mapToDouble(v -> Math.pow(v - mean, 2.0)).average().orElse(0.0);
        }

        private class PingSample {
            final int ping;
            final long timestamp;

            PingSample(int ping, long timestamp) {
                this.ping = ping;
                this.timestamp = timestamp;
            }

            int getPing() {
                return this.ping;
            }
        }

        private class PacketLossSample {
            final double packetLoss;
            final long timestamp;

            PacketLossSample(double packetLoss, long timestamp) {
                this.packetLoss = packetLoss;
                this.timestamp = timestamp;
            }
        }

        private class JitterSample {
            final double jitter;
            final long timestamp;

            JitterSample(double jitter, long timestamp) {
                this.jitter = jitter;
                this.timestamp = timestamp;
            }
        }
    }

    private class NeuralPattern {
        private final String uuid;
        private final List<double[]> featureVectors = new ArrayList<double[]>();
        private double[] centroid;
        private double confidence = 0.5;

        NeuralPattern(String uuid) {
            this.uuid = uuid;
        }

        void addFeatureVector(double[] features) {
            this.featureVectors.add(features);
            if (this.featureVectors.size() >= 10) {
                this.updateCentroid();
                this.updateConfidence();
            }
            if (this.featureVectors.size() > 100) {
                this.featureVectors.remove(0);
            }
        }

        double predictLegitimacy() {
            if (this.featureVectors.size() < 10) {
                return 0.5;
            }
            double[] latest = this.featureVectors.get(this.featureVectors.size() - 1);
            double distance = this.euclideanDistance(latest, this.centroid);
            return Math.max(0.0, Math.min(1.0, 1.0 - distance / 10.0));
        }

        double getConfidence() {
            return this.confidence * 100.0;
        }

        private void updateCentroid() {
            if (this.featureVectors.isEmpty()) {
                return;
            }
            int dimensions = this.featureVectors.get(0).length;
            this.centroid = new double[dimensions];
            for (double[] vector : this.featureVectors) {
                for (int i = 0; i < dimensions; ++i) {
                    int n = i;
                    this.centroid[n] = this.centroid[n] + vector[i];
                }
            }
            int i = 0;
            while (i < dimensions) {
                int n = i++;
                this.centroid[n] = this.centroid[n] / (double)this.featureVectors.size();
            }
            return;
        }

        private void updateConfidence() {
            if (this.featureVectors.size() < 2) {
                this.confidence = 0.5;
                return;
            }
            double totalDistance = 0.0;
            int comparisons = 0;
            for (int i = 0; i < this.featureVectors.size(); ++i) {
                for (int j = i + 1; j < this.featureVectors.size(); ++j) {
                    totalDistance += this.euclideanDistance(this.featureVectors.get(i), this.featureVectors.get(j));
                    ++comparisons;
                }
            }
            if (comparisons > 0) {
                double avgDistance = totalDistance / (double)comparisons;
                this.confidence = Math.max(0.1, Math.min(0.9, 1.0 - avgDistance / 5.0));
            }
        }

        private double euclideanDistance(double[] a, double[] b) {
            double sum = 0.0;
            int i = 0;
            while (i < a.length) {
                sum += Math.pow(a[i] - b[i], 2.0);
                ++i;
            }
            return Math.sqrt(sum);
        }
    }

    private class AltDetectionResult {
        private final boolean altDetected;
        private final double confidence;
        private final String mainAccount;
        private final Map<String, Double> detectionFactors;
        private final boolean concreteEvidence;

        AltDetectionResult(boolean altDetected, double confidence, String mainAccount, Map<String, Double> detectionFactors, boolean concreteEvidence) {
            this.altDetected = altDetected;
            this.confidence = confidence;
            this.mainAccount = mainAccount;
            this.detectionFactors = detectionFactors;
            this.concreteEvidence = concreteEvidence;
        }

        boolean isAltDetected() {
            return this.altDetected;
        }

        double getConfidence() {
            return this.confidence;
        }

        String getMainAccount() {
            return this.mainAccount;
        }

        boolean hasConcreteEvidence() {
            return this.concreteEvidence;
        }
    }

    private class ReputationModel {
        private ReputationModel() {
        }
    }

    private class AnomalyDetectionModel {
        private AnomalyDetectionModel() {
        }
    }

    private class SessionRecord {
        private final long startTime;
        private final String ip;
        private final boolean successful;
        private long endTime;

        SessionRecord(long startTime, String ip, boolean successful) {
            this.startTime = startTime;
            this.ip = ip;
            this.successful = successful;
            this.endTime = startTime + 300000L;
        }

        long getDuration() {
            return this.endTime - this.startTime;
        }
    }
}

