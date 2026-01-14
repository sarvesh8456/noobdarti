/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package io.verifyx;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.verifyx.DiscordBot;
import io.verifyx.Main;
import io.verifyx.models.ConfigData;
import io.verifyx.models.OTPData;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DatabaseManager {
    private final Main plugin;
    private final ConfigData config;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String supabaseUrl;
    private final String supabaseServiceKey;
    private boolean connected = false;
    private int connectionAttempts = 0;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;

    public DatabaseManager(Main plugin, ConfigData config) {
        this.plugin = plugin;
        this.config = config;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
        this.gson = new Gson();
        this.supabaseUrl = config.getSupabaseUrl().replaceAll("/$", "");
        this.supabaseServiceKey = config.getSupabaseServiceKey();
        if (this.supabaseServiceKey != null && !this.supabaseServiceKey.isEmpty()) {
            plugin.getLogger().info("\u2705 DatabaseManager: Service key loaded (starts with): " + this.supabaseServiceKey.substring(0, Math.min(10, this.supabaseServiceKey.length())) + "...");
        } else {
            plugin.getLogger().severe("\u274c DatabaseManager: Service key NOT loaded or empty!");
            plugin.getLogger().severe("\u274c Please check your config.yml: supabase.service-role-key");
        }
        this.initializeWithRetry();
    }

    private void initializeWithRetry() {
        if (!this.supabaseUrl.isEmpty() && !this.supabaseServiceKey.isEmpty()) {
            this.plugin.getLogger().info("Connecting to Supabase with SERVICE KEY...");
            this.plugin.getLogger().info("URL: " + this.supabaseUrl);
            this.plugin.getLogger().info("Service key starts with: " + this.supabaseServiceKey.substring(0, Math.min(10, this.supabaseServiceKey.length())) + "...");
            this.testConnectionWithRetry(3);
            return;
        }
        this.plugin.getLogger().severe("\u274c Supabase URL or Service key not set in config!");
    }

    private void testConnectionWithRetry(int maxAttempts) {
        if (this.connectionAttempts >= maxAttempts) {
            this.plugin.getLogger().severe("\u274c Max connection attempts reached (" + maxAttempts + ")");
            return;
        }
        ++this.connectionAttempts;
        this.plugin.getLogger().info("Connection attempt " + this.connectionAttempts + "/" + maxAttempts);
        this.testConnection().thenAccept(success -> {
            if (success.booleanValue()) {
                this.connected = true;
                this.plugin.getLogger().info("\u2705 Connected to Supabase API!");
            } else {
                this.plugin.getLogger().warning("\u274c Connection attempt " + this.connectionAttempts + " failed");
                if (this.connectionAttempts < maxAttempts) {
                    this.plugin.getLogger().info("Retrying in 3 seconds...");
                    this.plugin.getServer().getScheduler().runTaskLaterAsynchronously((Plugin)this.plugin, () -> this.testConnectionWithRetry(maxAttempts), 60L);
                } else {
                    this.plugin.getLogger().severe("\u274c Failed to connect to Supabase after " + maxAttempts + " attempts!");
                }
            }
        });
    }

    public CompletableFuture<Boolean> forceVerifyPlayer(String playerName) {
        return this.forceVerifyPlayer(playerName, null);
    }

    public CompletableFuture<Boolean> forceVerifyPlayer(String playerName, String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String checkSql = "SELECT COUNT(*) FROM verifications WHERE player_name = ?";
                Integer count = this.querySingle(checkSql, Integer.class, playerName);
                if (count != null && count > 0) {
                    this.plugin.getLogger().info("Player " + playerName + " is already verified");
                    return true;
                }
                String playerUUID = this.fetchUUIDFromName(playerName);
                if (playerUUID == null) {
                    playerUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
                }
                JsonObject json = new JsonObject();
                json.addProperty("player_name", playerName);
                json.addProperty("player_uuid", playerUUID);
                json.addProperty("discord_id", "ADMIN_FORCE");
                json.addProperty("discord_name", "Admin Force Verify");
                json.addProperty("verified_at", Instant.now().toString());
                json.addProperty("last_seen", Instant.now().toString());
                if (ipAddress != null && !ipAddress.isEmpty()) {
                    // store last known IP only; trusted_ip feature removed in favor of loginsecurity
                    json.addProperty("last_known_ip", ipAddress);
                }
                String endpoint = this.supabaseUrl + "/rest/v1/verifications";
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "resolution=merge-duplicates,return=minimal").POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                this.plugin.getLogger().info("Force verify response: " + response.statusCode() + " - " + response.body());
                return response.statusCode() == 201 || response.statusCode() == 200 || response.statusCode() == 204;
            }
            catch (Exception e) {
                this.plugin.getLogger().severe("Error in forceVerifyPlayer: " + e.toString());
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> checkPlayerExists(String playerName) {
        try {
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_name=eq." + URLEncoder.encode(playerName, "UTF-8") + "&select=player_name";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
            return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                this.plugin.getLogger().info("Check player exists response: " + response.statusCode() + " - " + (String)response.body());
                if (response.statusCode() != 200) {
                    return false;
                }
                JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
                return results.size() > 0;
            });
        }
        catch (Exception e) {
            this.plugin.getLogger().severe("Error checking player: " + e.toString());
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> removeVerificationByUUID(String playerUUID) {
        try {
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_uuid=eq." + URLEncoder.encode(playerUUID, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                this.plugin.getLogger().info("Remove verification by UUID response: " + response.statusCode() + " for UUID: " + playerUUID);
                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    this.plugin.getLogger().warning("Delete failed: " + (String)response.body());
                    return false;
                }
                return true;
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("Error removing verification by UUID: " + e.toString());
                return false;
            });
        }
        catch (Exception e2) {
            this.plugin.getLogger().severe("Error encoding URL: " + e2.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> removeVerificationByPlayerName(String playerName) {
        try {
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_name=eq." + URLEncoder.encode(playerName, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                this.plugin.getLogger().info("Remove verification by name response: " + response.statusCode() + " for player: " + playerName);
                if (response.statusCode() != 200 && response.statusCode() != 204) {
                    this.plugin.getLogger().warning("Delete failed: " + (String)response.body());
                    return false;
                }
                return true;
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("Error removing verification by name: " + e.toString());
                return false;
            });
        }
        catch (Exception e2) {
            this.plugin.getLogger().severe("Error encoding URL: " + e2.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<String> findDiscordIdByPlayerName(String playerName) {
        try {
            String encodedName = URLEncoder.encode(playerName, "UTF-8");
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_name=eq." + encodedName + "&select=discord_id";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                JsonObject result;
                JsonArray results;
                this.plugin.getLogger().info("Find Discord ID response: " + response.statusCode() + " for player: " + playerName);
                if (response.statusCode() == 200 && (results = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0 && (result = results.get(0).getAsJsonObject()).has("discord_id") && !result.get("discord_id").isJsonNull()) {
                    String discordId = result.get("discord_id").getAsString();
                    this.plugin.getLogger().info("Found Discord ID: " + discordId + " for player: " + playerName);
                    return discordId;
                }
                this.plugin.getLogger().warning("No Discord ID found for player: " + playerName);
                return null;
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("Error finding Discord ID: " + e.toString());
                return null;
            });
        }
        catch (Exception e2) {
            this.plugin.getLogger().severe("Error encoding URL in findDiscordIdByPlayerName: " + e2.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<Boolean> testDeletePermission() {
        try {
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_uuid=eq.test-test-test-test-test";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                this.plugin.getLogger().info("DELETE permission test response: " + response.statusCode() + " - " + (String)response.body());
                return response.statusCode() == 200 || response.statusCode() == 204 || response.statusCode() == 404;
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("DELETE permission test error: " + e.toString());
                return false;
            });
        }
        catch (Exception e2) {
            this.plugin.getLogger().severe("Error testing DELETE permission: " + e2.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    public Integer querySingle(String sql, Class<Integer> type, String playerName) {
        try {
            JsonArray results;
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_name=eq." + playerName + "&select=count";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && (results = this.gson.fromJson(response.body(), JsonArray.class)).size() > 0) {
                return results.get(0).getAsJsonObject().get("count").getAsInt();
            }
            return 0;
        }
        catch (Exception e) {
            this.plugin.getLogger().severe("Error in querySingle: " + e.toString());
            return null;
        }
    }

    public String fetchUUIDFromName(String playerName) {
        try {
            JsonObject json;
            Player player = Bukkit.getPlayerExact((String)playerName);
            if (player != null) {
                return player.getUniqueId().toString();
            }
            String mojangUrl = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(mojangUrl)).timeout(Duration.ofSeconds(5L)).GET().build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && (json = this.gson.fromJson(response.body(), JsonObject.class)).has("id")) {
                String uuidWithoutDashes = json.get("id").getAsString();
                return uuidWithoutDashes.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            }
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error fetching UUID for " + playerName + ": " + e.toString());
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
        }
    }

    public int executeUpdate(String sql, String playerName, String playerUUID) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("player_name", playerName);
            json.addProperty("player_uuid", playerUUID);
            json.addProperty("discord_id", "ADMIN_FORCE");
            json.addProperty("discord_name", "Admin Force Verify");
            json.addProperty("verified_at", Instant.now().toString());
            json.addProperty("last_seen", Instant.now().toString());
            String endpoint = this.supabaseUrl + "/rest/v1/verifications";
            String jsonBody = this.gson.toJson(json);
            this.plugin.getLogger().info("[ForceVerify] Sending to Supabase: " + jsonBody);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "resolution=merge-duplicates,return=minimal").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            this.plugin.getLogger().info("[ForceVerify] Response status: " + response.statusCode() + ", body: " + response.body());
            if (response.statusCode() != 201 && response.statusCode() != 200 && response.statusCode() != 204) {
                this.plugin.getLogger().warning("Execute update failed with status: " + response.statusCode() + " | Response: " + response.body());
                return 0;
            }
            return 1;
        }
        catch (Exception e) {
            this.plugin.getLogger().severe("Error in executeUpdate: " + e.toString());
            e.printStackTrace();
            return 0;
        }
    }

    private CompletableFuture<Boolean> testConnection() {
        String endpoint = this.supabaseUrl + "/rest/v1/";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).timeout(Duration.ofSeconds(5L)).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Connection test response code: " + response.statusCode());
            return response.statusCode() == 200;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Supabase connection test failed: " + e.toString());
            return false;
        });
    }

    public boolean otpExists(String otp) {
        return this.checkOTP(otp).join();
    }

    public CompletableFuture<Boolean> checkOTP(String otp) {
        String endpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp + "&or=(is_used.eq.false,is_used.is.null)&expires_at=gt.now()";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return false;
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            return results.size() > 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error checking OTP: " + e.toString());
            return false;
        });
    }

    public void saveOTPAsync(OTPData otpData) {
        this.saveOTP(otpData.getOtp(), otpData.getPlayerName(), otpData.getPlayerUUID(), otpData.getIpAddress());
    }

    public CompletableFuture<Boolean> saveOTP(String otp, String playerName, String playerUUID, String ipAddress) {
        JsonObject json = new JsonObject();
        json.addProperty("otp_code", otp);
        json.addProperty("player_name", playerName);
        json.addProperty("player_uuid", playerUUID);
        json.addProperty("ip_address", ipAddress);
        Instant expiresAt = Instant.now().plusSeconds((long)this.config.getOtpExpiryMinutes() * 60L);
        json.addProperty("expires_at", expiresAt.toString());
        String endpoint = this.supabaseUrl + "/rest/v1/otps";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "return=minimal").POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Save OTP response: " + response.statusCode() + " - " + response.body());
            if (response.statusCode() == 400 && response.body() != null && response.body().contains("player_uid")) {
                this.plugin.getLogger().warning("Detected PostgREST schema mismatch: 'player_uid' not found. Ensure your DB schema uses 'player_uuid' and that PostgREST schema cache has been refreshed or the service restarted.");
            }
            return response.statusCode() == 201 || response.statusCode() == 200;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error saving OTP: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<Boolean> isDiscordVerified(String discordId) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?discord_id=eq." + discordId;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return false;
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            return results.size() > 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error checking Discord verification: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<Boolean> isPlayerVerified(String playerUUID) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_uuid=eq." + playerUUID;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return false;
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            return results.size() > 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error checking verification: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<Boolean> isIPFromVerifiedAccount(String ipAddress) {
        try {
            if (ipAddress == null || ipAddress.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            String endpoint = this.supabaseUrl + "/rest/v1/otps?ip_address=eq." + URLEncoder.encode(ipAddress, "UTF-8") + "&select=player_uuid&limit=50";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
            return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                if (response.statusCode() != 200) {
                    return false;
                }
                JsonArray results = this.gson.fromJson(response.body(), JsonArray.class);
                if (results == null || results.size() == 0) {
                    return false;
                }
                for (int i = 0; i < results.size(); i++) {
                    try {
                        JsonObject rec = results.get(i).getAsJsonObject();
                        if (!rec.has("player_uuid") || rec.get("player_uuid").isJsonNull()) continue;
                        String uuid = rec.get("player_uuid").getAsString();
                        if (uuid == null || uuid.isEmpty()) continue;
                        Boolean verified = this.isPlayerVerified(uuid).join();
                        if (verified != null && verified.booleanValue()) {
                            this.plugin.getLogger().info("\u2705 IP " + ipAddress + " associated with verified account");
                            return true;
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
                return false;
            }).exceptionally(e -> {
                this.plugin.getLogger().warning("Error checking IP verification: " + e.toString());
                return false;
            });
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Error preparing IP request: " + e.toString());
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<Boolean> isDiscordAlreadyLinked(String discordId, String currentPlayerUUID) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?discord_id=eq." + discordId + "&player_uuid=neq." + currentPlayerUUID;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return false;
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            return results.size() > 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error checking Discord link: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<Integer> countVerifiedAccountsWithDiscordOnIP(String discordId, String ipAddress) {
        // Count how many verified accounts have this Discord ID and are on the same IP
        // This requires checking the verification_ips table or a similar mechanism
        // For now, we'll return a count based on the verifications table
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?discord_id=eq." + discordId + "&select=count()&head";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return 0;
            }
            try {
                String contentRange = response.headers().firstValue("content-range").orElse("0-0/0");
                String[] parts = contentRange.split("/");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1]);
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("Error parsing count response: " + e.toString());
            }
            return 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error counting Discord accounts: " + e.toString());
            return 0;
        });
    }

    // Overload: save verification and optionally persist a trusted IP for the verified account
    public CompletableFuture<Boolean> saveVerification(String playerName, String minecraftUUID, String discordId, String discordName) {
        return this.saveVerification(playerName, minecraftUUID, discordId, discordName, null);
    }

    public CompletableFuture<Boolean> saveVerification(String playerName, String minecraftUUID, String discordId, String discordName, String ipAddress) {
        JsonObject json = new JsonObject();
        json.addProperty("player_name", playerName);
        json.addProperty("player_uuid", minecraftUUID);
        json.addProperty("discord_id", discordId);
        json.addProperty("discord_name", discordName);
        json.addProperty("verified_at", Instant.now().toString());
        json.addProperty("last_seen", Instant.now().toString());
        if (ipAddress != null && !ipAddress.isEmpty()) {
            // trusted_ip removed; keep last_known_ip for informational purposes
            json.addProperty("last_known_ip", ipAddress);
        }
        String endpoint = this.supabaseUrl + "/rest/v1/verifications";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "resolution=merge-duplicates,return=minimal").POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Save verification response: " + response.statusCode());
            return response.statusCode() == 201 || response.statusCode() == 200 || response.statusCode() == 204;
        })).exceptionally(e -> {
            this.plugin.getLogger().severe("\u274c Error saving verification: " + e.toString());
            return false;
        });
    }

    public void saveVerificationAsync(String playerName, String playerUUID, String discordId, String discordName, String ipAddress) {
        this.saveVerification(playerName, playerUUID, discordId, discordName, ipAddress);
    }

    public CompletableFuture<VerificationResult> verifyOTP(String otp, String discordId, String discordName) {
        this.plugin.getLogger().info("\ud83d\udd10 Starting OTP verification for OTP: " + otp + ", Discord: " + discordName);
        String checkEndpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp + "&select=player_name,player_uuid,is_used,expires_at";
        return ((CompletableFuture)this.httpClient.sendAsync(HttpRequest.newBuilder().uri(URI.create(checkEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build(), HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            this.plugin.getLogger().info("OTP check response: " + response.statusCode() + " - " + (String)response.body());
            if (response.statusCode() != 200) {
                this.plugin.getLogger().warning("\u274c OTP check failed with status: " + response.statusCode());
                return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Database error checking OTP.", null, null));
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            if (results.size() <= 0) {
                this.plugin.getLogger().warning("\u274c OTP not found: " + otp);
                return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Invalid or expired OTP.", null, null));
            }
            JsonObject otpRecord = results.get(0).getAsJsonObject();
            if (otpRecord.has("is_used") && otpRecord.get("is_used").getAsBoolean()) {
                this.plugin.getLogger().warning("\u274c OTP already used: " + otp);
                return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c OTP already used.", null, null));
            }
            if (otpRecord.has("expires_at")) {
                String expiresAtStr = otpRecord.get("expires_at").getAsString();
                try {
                    Instant expiresAt = Instant.parse(expiresAtStr);
                    if (expiresAt.isBefore(Instant.now())) {
                        this.plugin.getLogger().warning("\u274c OTP expired: " + otp);
                        return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c OTP expired.", null, null));
                    }
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Error parsing expires_at: " + e.toString());
                }
            }
            String playerName = otpRecord.get("player_name").getAsString();
            String playerUUID = otpRecord.get("player_uuid").getAsString();
            this.plugin.getLogger().info("\u2705 Found valid OTP for player: " + playerName + " (" + playerUUID + ")");
            return this.isDiscordAlreadyLinked(discordId, playerUUID).thenCompose(isLinked -> {
                if (isLinked.booleanValue()) {
                    this.plugin.getLogger().warning("\u274c Discord already linked to another account: " + discordId);
                    return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c This Discord account is already linked to another Minecraft account.", null, null));
                }
                return this.markOTPUsed(otp, discordId, discordName).thenCompose(success -> {
                    if (success.booleanValue()) {
                        this.plugin.getLogger().info("\u2705 OTP marked as used: " + otp);
                        return this.saveVerification(playerName, playerUUID, discordId, discordName).thenApply(verificationSuccess -> {
                            if (verificationSuccess.booleanValue()) {
                                this.plugin.getLogger().info("\u2705 Verification saved for " + playerName);
                                return new VerificationResult(true, "\u2705 Verification successful!", playerName, playerUUID);
                            }
                            return new VerificationResult(false, "\u274c Failed to save verification.", null, null);
                        });
                    }
                    this.plugin.getLogger().warning("\u274c Failed to mark OTP as used: " + otp);
                    return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Failed to mark OTP as used.", null, null));
                });
            });
        })).exceptionally(e -> {
            this.plugin.getLogger().severe("\u274c Supabase API error: " + e.toString());
            return new VerificationResult(false, "\u274c Database connection error: " + e.toString(), null, null);
        });
    }

    public CompletableFuture<VerificationResult> checkOTPWithoutCommit(String otp) {
        this.plugin.getLogger().info("\ud83d\udd0d Checking OTP without committing: " + otp);
        String checkEndpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp + "&select=player_name,player_uuid,is_used,expires_at";
        return ((CompletableFuture)this.httpClient.sendAsync(HttpRequest.newBuilder().uri(URI.create(checkEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build(), HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) {
                return new VerificationResult(false, "\u274c Database error.", null, null);
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            if (results.size() <= 0) {
                return new VerificationResult(false, "\u274c Invalid OTP.", null, null);
            }
            JsonObject otpRecord = results.get(0).getAsJsonObject();
            if (otpRecord.has("is_used") && otpRecord.get("is_used").getAsBoolean()) {
                return new VerificationResult(false, "\u274c OTP already used.", null, null);
            }
            if (otpRecord.has("expires_at")) {
                String expiresAtStr = otpRecord.get("expires_at").getAsString();
                try {
                    Instant expiresAt = Instant.parse(expiresAtStr);
                    if (expiresAt.isBefore(Instant.now())) {
                        return new VerificationResult(false, "\u274c OTP expired.", null, null);
                    }
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Error parsing expires_at: " + e.toString());
                }
            }
            String playerName = otpRecord.get("player_name").getAsString();
            String playerUUID = otpRecord.get("player_uuid").getAsString();
            this.plugin.getLogger().info("\u2705 OTP is valid for player: " + playerName);
            return new VerificationResult(true, "\u2705 OTP is valid.", playerName, playerUUID);
        })).exceptionally(e -> new VerificationResult(false, "\u274c Database connection error.", null, null));
    }

    public CompletableFuture<VerificationResult> finalizeVerification(String otp, String discordId, String discordName) {
        this.plugin.getLogger().info("\ud83d\udd10 Step 2: Finalizing verification for OTP: " + otp + ", Discord: " + discordName);
        String checkEndpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp + "&select=player_name,player_uuid,is_used,expires_at,ip_address";
        return ((CompletableFuture)this.httpClient.sendAsync(HttpRequest.newBuilder().uri(URI.create(checkEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build(), HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() != 200) {
                this.plugin.getLogger().warning("\u274c OTP check failed with status: " + response.statusCode());
                return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Database error checking OTP.", null, null));
            }
            JsonArray results = this.gson.fromJson((String)response.body(), JsonArray.class);
            if (results.size() > 0) {
                JsonObject otpRecord = results.get(0).getAsJsonObject();
                String playerName = otpRecord.get("player_name").getAsString();
                String playerUUID = otpRecord.get("player_uuid").getAsString();
                String ipAddress = otpRecord.has("ip_address") && !otpRecord.get("ip_address").isJsonNull() ? otpRecord.get("ip_address").getAsString() : null;
                return this.isDiscordAlreadyLinked(discordId, playerUUID).thenCompose(isLinked -> {
                    if (isLinked.booleanValue()) {
                        this.plugin.getLogger().warning("\u274c Discord already linked to another account: " + discordId);
                        return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c This Discord account is already linked to another Minecraft account.", null, null));
                    }
                    return this.markOTPUsed(otp, discordId, discordName).thenCompose(success -> {
                        if (success.booleanValue()) {
                            this.plugin.getLogger().info("\u2705 OTP marked as used: " + otp);
                            return this.saveVerification(playerName, playerUUID, discordId, discordName, ipAddress).thenApply(verificationSuccess -> {
                                if (verificationSuccess.booleanValue()) {
                                    this.plugin.getLogger().info("\u2705 Verification finalized for " + playerName);
                                    return new VerificationResult(true, "\u2705 Verification successful!", playerName, playerUUID);
                                }
                                return new VerificationResult(false, "\u274c Failed to save verification.", null, null);
                            });
                        }
                        this.plugin.getLogger().warning("\u274c Failed to mark OTP as used: " + otp);
                        return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Failed to mark OTP as used.", null, null));
                    });
                });
            }
            this.plugin.getLogger().warning("\u274c OTP not found during finalization: " + otp);
            return CompletableFuture.completedFuture(new VerificationResult(false, "\u274c Invalid or expired OTP.", null, null));
        })).exceptionally(e -> {
            this.plugin.getLogger().severe("\u274c Error during verification finalization: " + e.toString());
            return new VerificationResult(false, "\u274c Database connection error: " + e.toString(), null, null);
        });
    }

    private CompletableFuture<Boolean> markOTPUsed(String otp, String discordId, String discordName) {
        JsonObject json = new JsonObject();
        json.addProperty("is_used", true);
        json.addProperty("used_at", Instant.now().toString());
        json.addProperty("discord_id", discordId);
        json.addProperty("discord_name", discordName);
        String endpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "return=minimal").method("PATCH", HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Mark OTP used response: " + response.statusCode());
            return response.statusCode() == 204 || response.statusCode() == 200;
        })).exceptionally(e -> {
            this.plugin.getLogger().severe("\u274c Error marking OTP as used: " + e.toString());
            return false;
        });
    }



    public CompletableFuture<JsonArray> getAllVerifications() {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?select=*&order=verified_at.desc";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                return this.gson.fromJson((String)response.body(), JsonArray.class);
            }
            return new JsonArray();
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting all verifications: " + e.toString());
            return new JsonArray();
        });
    }

    // trusted-IP support removed; return empty set when queried to preserve compatibility
    public CompletableFuture<java.util.Set<String>> getTrustedIPs(String playerUUID) {
        return CompletableFuture.completedFuture(java.util.Collections.emptySet());
    }

    // Add a trusted IP for a verification, enforce a configurable max (evict oldest if needed)


    // Public helper to touch last_seen when player joins (keeps status accurate for /status)
    public CompletableFuture<Boolean> updateLastSeen(String playerUUID) {
        try {
            if (playerUUID == null || playerUUID.isEmpty()) return CompletableFuture.completedFuture(false);
            JsonObject json = new JsonObject();
            json.addProperty("last_seen", Instant.now().toString());
            String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_uuid=eq." + URLEncoder.encode(playerUUID, "UTF-8");
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "return=minimal").method("PATCH", HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                this.plugin.getLogger().fine("Update last_seen response: " + response.statusCode() + " for UUID: " + playerUUID);
                return response.statusCode() == 200 || response.statusCode() == 204;
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("Error updating last_seen: " + e.toString());
                return false;
            });
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error preparing last_seen update: " + e.toString());
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<JsonArray> getAllOTPs() {
        String endpoint = this.supabaseUrl + "/rest/v1/otps?select=*&order=created_at.desc";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                return this.gson.fromJson((String)response.body(), JsonArray.class);
            }
            return new JsonArray();
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting all OTPs: " + e.toString());
            return new JsonArray();
        });
    }

    // Admin/debug: fetch recent OTP rows (admin only). Includes raw otp_code — callers MUST mask it before displaying publicly.
    public CompletableFuture<JsonArray> getRecentOtps(int limit) {
        try {
            if (limit <= 0) limit = 10;
            if (limit > 200) limit = 200;
            String endpoint = this.supabaseUrl + "/rest/v1/otps?select=otp_code,player_name,player_uuid,is_used,expires_at,created_at,ip_address&order=created_at.desc&limit=" + limit;
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
            return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                if (response.statusCode() == 200) {
                    return this.gson.fromJson((String)response.body(), JsonArray.class);
                }
                this.plugin.getLogger().warning("Failed to fetch recent OTPs: HTTP " + response.statusCode() + " - " + response.body());
                return new JsonArray();
            })).exceptionally(e -> {
                this.plugin.getLogger().warning("Error fetching recent OTPs: " + e.toString());
                return new JsonArray();
            });
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error preparing recent OTP request: " + e.toString());
            return CompletableFuture.completedFuture(new JsonArray());
        }
    }

    public CompletableFuture<DiscordBot.PlayerInfo> getPlayerInfoFromOTP(String otp) {
        String endpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp + "&select=player_name,player_uuid";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonArray results;
            if (response.statusCode() == 200 && (results = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0) {
                UUID playerUUID;
                JsonObject firstResult = results.get(0).getAsJsonObject();
                String playerName = firstResult.get("player_name").getAsString();
                String playerUUIDStr = firstResult.get("player_uuid").getAsString();
                try {
                    playerUUID = UUID.fromString(playerUUIDStr);
                }
                catch (IllegalArgumentException e) {
                    this.plugin.getLogger().warning("Invalid UUID format: " + playerUUIDStr);
                    return null;
                }
                return new DiscordBot.PlayerInfo(playerName, playerUUID.toString(), "additionalParameter");
            }
            return null;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting player info from OTP: " + e.toString());
            return null;
        });
    }

    public CompletableFuture<DiscordBot.VerificationDetails> getVerificationDetails(String discordId) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?discord_id=eq." + discordId + "&select=player_name,player_uuid,verified_at,last_seen";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonArray results;
            if (response.statusCode() == 200 && (results = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0) {
                JsonObject result = results.get(0).getAsJsonObject();
                DiscordBot.VerificationDetails details = new DiscordBot.VerificationDetails();
                details.playerName = result.get("player_name").getAsString();
                if (result.has("player_uuid") && !result.get("player_uuid").isJsonNull()) {
                    details.playerUUID = result.get("player_uuid").getAsString();
                }
                details.verifiedDate = result.get("verified_at").getAsString();
                if (result.has("last_seen") && !result.get("last_seen").isJsonNull()) {
                    String lastSeenStr = result.get("last_seen").getAsString();
                    try {
                        Instant lastSeen = Instant.parse(lastSeenStr);
                        details.isOnline = lastSeen.isAfter(Instant.now().minusSeconds(300L));
                        details.daysSinceLastSeen = (int)Duration.between(lastSeen, Instant.now()).toDays();
                    }
                    catch (Exception e) {
                        details.isOnline = false;
                        details.daysSinceLastSeen = 999;
                    }
                } else {
                    details.isOnline = false;
                    details.daysSinceLastSeen = 999;
                }
                return details;
            }
            return null;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting verification details: " + e.toString());
            return null;
        });
    }

    public CompletableFuture<Boolean> removeVerification(String discordId) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?discord_id=eq." + discordId;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Remove verification response: " + response.statusCode());
            return response.statusCode() == 204 || response.statusCode() == 200;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error removing verification: " + e.toString());
            return false;
        });
    }

    public void updateOnlinePlayers() {
        if (!this.connected) {
            return;
        }
        Iterator iterator2 = Bukkit.getOnlinePlayers().iterator();
        while (iterator2.hasNext()) {
            Player player = (Player)iterator2.next();
            String playerUUID = player.getUniqueId().toString();
            this.isPlayerVerified(playerUUID).thenAccept(verified -> {
                if (verified.booleanValue()) {
                    this.updateLastSeen(playerUUID);
                }
            });
        }
        return;
    }

    // consolidated: use public CompletableFuture<Boolean> updateLastSeen(String) instead


    public CompletableFuture<Boolean> checkRateLimit(String identifier) {
        return CompletableFuture.completedFuture(false);
    }

    public void recordAttempt(String identifier, boolean success) {
    }

    public void clearRateLimit(String identifier) {
    }

    private void forceVerify(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            try {
                Player player = Bukkit.getPlayer((String)playerName);
                String playerUUID = player != null ? player.getUniqueId().toString() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes()).toString();
                boolean alreadyVerified = false;
                if (this.plugin.getDatabaseManager() != null) {
                    alreadyVerified = this.plugin.getDatabaseManager().isPlayerVerified(playerUUID).join();
                }
                if (alreadyVerified) {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.YELLOW) + playerName + " is already verified!"));
                    return;
                }
                boolean dbSuccess = false;
                if (this.plugin.getDatabaseManager() != null) {
                    dbSuccess = this.plugin.getDatabaseManager().forceVerifyPlayer(playerName).join();
                }
                boolean whitelistSuccess = false;
                if (this.plugin.getVerificationManager() != null) {
                    this.plugin.getVerificationManager().addToWhitelist(playerName, playerUUID);
                    whitelistSuccess = true;
                }
                boolean finalDbSuccess = dbSuccess;
                boolean finalWhitelistSuccess = whitelistSuccess;
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                    sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Force Verify Results ===");
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Player: " + String.valueOf(ChatColor.YELLOW) + playerName);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "UUID: " + String.valueOf(ChatColor.AQUA) + playerUUID);
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Database: " + (finalDbSuccess ? String.valueOf(ChatColor.GREEN) + "SUCCESS" : String.valueOf(ChatColor.RED) + "FAILED"));
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Whitelist: " + (finalWhitelistSuccess ? String.valueOf(ChatColor.GREEN) + "SUCCESS" : String.valueOf(ChatColor.RED) + "FAILED"));
                    if (!finalDbSuccess) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "\u26a0 Could not save to database!");
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Manual SQL:");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "INSERT INTO verifications (player_name, player_uuid, discord_id, discord_name)");
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + "VALUES ('" + playerName + "', '" + playerUUID + "', 'ADMIN_FORCE', 'Admin');");
                    }
                });
            }
            catch (Exception e) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(String.valueOf(ChatColor.RED) + "Error force verifying: " + e.toString()));
            }
        });
    }

    public CompletableFuture<Integer> cleanupExpiredData() {
        return this.cleanupExpiredOTPs();
    }

    public CompletableFuture<Integer> cleanupExpiredOTPs() {
        // First request the count of expired OTPs, then delete them and return the deleted count.
        String countEndpoint = this.supabaseUrl + "/rest/v1/otps?expires_at=lt.now()&select=count";
        HttpRequest countRequest = HttpRequest.newBuilder().uri(URI.create(countEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(countRequest, HttpResponse.BodyHandlers.ofString()).thenCompose(countResp -> {
            try {
                if (countResp.statusCode() != 200) {
                    this.plugin.getLogger().warning("Failed to query expired OTP count: HTTP " + countResp.statusCode());
                    return CompletableFuture.completedFuture(0);
                }
                com.google.gson.JsonArray arr = this.gson.fromJson((String)countResp.body(), com.google.gson.JsonArray.class);
                int count = 0;
                if (arr != null && arr.size() > 0) {
                    try {
                        count = arr.get(0).getAsJsonObject().get("count").getAsInt();
                    } catch (Exception ex) {
                        count = 0;
                    }
                }
                String deleteEndpoint = this.supabaseUrl + "/rest/v1/otps?expires_at=lt.now()";
                HttpRequest deleteRequest = HttpRequest.newBuilder().uri(URI.create(deleteEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
                int finalCount = count;
                return this.httpClient.sendAsync(deleteRequest, HttpResponse.BodyHandlers.ofString()).thenApply(deleteResp -> {
                    if (deleteResp.statusCode() != 204 && deleteResp.statusCode() != 200) {
                        this.plugin.getLogger().warning("Failed to delete expired OTPs: HTTP " + deleteResp.statusCode());
                        return 0;
                    }
                    this.plugin.getLogger().info("Cleaned up expired OTPs: " + finalCount);
                    return finalCount;
                });
            } catch (Exception e) {
                this.plugin.getLogger().warning("Error during OTP cleanup flow: " + e.toString());
                return CompletableFuture.completedFuture(0);
            }
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error cleaning up OTPs: " + e.toString());
            return 0;
        });
    }

    public CompletableFuture<Integer> purgeAllOTPs() {
        // Delete ALL non-expired OTPs (regardless of expiry status)
        String countEndpoint = this.supabaseUrl + "/rest/v1/otps?is_used=eq.false&expires_at=gt.now()&select=count";
        HttpRequest countRequest = HttpRequest.newBuilder().uri(URI.create(countEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(countRequest, HttpResponse.BodyHandlers.ofString()).thenCompose(countResp -> {
            try {
                if (countResp.statusCode() != 200) {
                    this.plugin.getLogger().warning("Failed to query active OTP count: HTTP " + countResp.statusCode());
                    return CompletableFuture.completedFuture(0);
                }
                com.google.gson.JsonArray arr = this.gson.fromJson((String)countResp.body(), com.google.gson.JsonArray.class);
                int count = 0;
                if (arr != null && arr.size() > 0) {
                    try {
                        count = arr.get(0).getAsJsonObject().get("count").getAsInt();
                    } catch (Exception ex) {
                        count = 0;
                    }
                }
                String deleteEndpoint = this.supabaseUrl + "/rest/v1/otps?is_used=eq.false&expires_at=gt.now()";
                HttpRequest deleteRequest = HttpRequest.newBuilder().uri(URI.create(deleteEndpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).DELETE().build();
                int finalCount = count;
                return this.httpClient.sendAsync(deleteRequest, HttpResponse.BodyHandlers.ofString()).thenApply(deleteResp -> {
                    if (deleteResp.statusCode() != 204 && deleteResp.statusCode() != 200) {
                        this.plugin.getLogger().warning("Failed to delete all OTPs: HTTP " + deleteResp.statusCode());
                        return 0;
                    }
                    this.plugin.getLogger().info("Purged ALL active OTPs: " + finalCount);
                    return finalCount;
                });
            } catch (Exception e) {
                this.plugin.getLogger().warning("Error during OTP purge flow: " + e.toString());
                return CompletableFuture.completedFuture(0);
            }
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error purging all OTPs: " + e.toString());
            return 0;
        });
    }

    public CompletableFuture<Integer> getVerifiedCount() {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?select=count";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonArray array;
            if (response.statusCode() == 200 && (array = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0) {
                return array.get(0).getAsJsonObject().get("count").getAsInt();
            }
            return 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting verified count: " + e.toString());
            return 0;
        });
    }

    public CompletableFuture<Integer> getPendingOTPCount() {
        String endpoint = this.supabaseUrl + "/rest/v1/otps?is_used=eq.false&expires_at=gt.now()&select=count";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonArray array;
            if (response.statusCode() == 200 && (array = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0) {
                return array.get(0).getAsJsonObject().get("count").getAsInt();
            }
            return 0;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting pending OTP count: " + e.toString());
            return 0;
        });
    }

    public OTPData getOTP(String otp) {
        return new OTPData(otp, "unknown", "unknown", "unknown", Instant.now().plusSeconds((long)this.config.getOtpExpiryMinutes() * 60L));
    }

    public void markOTPAsUsed(String otp, String discordId) {
        this.markOTPUsed(otp, discordId).thenAccept(success -> {
            if (success.booleanValue()) {
                this.plugin.getLogger().info("\u2705 OTP marked as used: " + otp);
            } else {
                this.plugin.getLogger().warning("\u274c Failed to mark OTP as used: " + otp);
            }
        });
    }

    public CompletableFuture<Boolean> markOTPUsed(String otp, String discordId) {
        JsonObject json = new JsonObject();
        json.addProperty("is_used", true);
        json.addProperty("discord_id", discordId);
        json.addProperty("used_at", Instant.now().toString());
        String endpoint = this.supabaseUrl + "/rest/v1/otps?otp_code=eq." + otp;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "return=minimal").method("PATCH", HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            this.plugin.getLogger().info("Mark OTP used response: " + response.statusCode());
            return response.statusCode() == 204 || response.statusCode() == 200;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error marking OTP used: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<Boolean> validateOTP(String otp, String discordUserId) {
        return this.checkOTP(otp).thenCompose(otpExists -> {
            if (!otpExists.booleanValue()) {
                return CompletableFuture.completedFuture(false);
            }
            return this.markOTPUsed(otp, discordUserId).thenApply(success -> {
                if (success.booleanValue()) {
                    this.plugin.getLogger().info("\u2705 OTP " + otp + " validated for Discord user: " + discordUserId);
                }
                return success;
            });
        });
    }

    public CompletableFuture<Boolean> storeOTP(String otp, String discordUserId, String minecraftUUID) {
        JsonObject json = new JsonObject();
        json.addProperty("otp_code", otp);
        json.addProperty("player_uuid", minecraftUUID);
        json.addProperty("discord_id", discordUserId);
        Instant expiresAt = Instant.now().plusSeconds((long)this.config.getOtpExpiryMinutes() * 60L);
        json.addProperty("expires_at", expiresAt.toString());
        String endpoint = this.supabaseUrl + "/rest/v1/otps";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).header("Content-Type", "application/json").header("Prefer", "return=minimal").POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(json))).build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> response.statusCode() == 201 || response.statusCode() == 200)).exceptionally(e -> {
            this.plugin.getLogger().warning("Error storing OTP: " + e.toString());
            return false;
        });
    }

    public CompletableFuture<JsonObject> getVerification(String playerUUID) {
        String endpoint = this.supabaseUrl + "/rest/v1/verifications?player_uuid=eq." + playerUUID;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint)).header("apikey", this.supabaseServiceKey).header("Authorization", "Bearer " + this.supabaseServiceKey).GET().build();
        return ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            JsonArray results;
            if (response.statusCode() == 200 && (results = this.gson.fromJson((String)response.body(), JsonArray.class)).size() > 0) {
                return results.get(0).getAsJsonObject();
            }
            return null;
        })).exceptionally(e -> {
            this.plugin.getLogger().warning("Error getting verification: " + e.toString());
            return null;
        });
    }

    public void close() {
        this.connected = false;
    }

    public void reloadConfig() {
        try {
            this.plugin.reloadConfig();
            this.plugin.getLogger().info("DatabaseManager: reloadConfig invoked");
            // re-run a quick connection check asynchronously
            this.testConnectionWithRetry(1);
        } catch (Exception e) {
            this.plugin.getLogger().warning("DatabaseManager reload failed: " + e.toString());
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public boolean testConnectionSync() {
        return this.testConnection().join();
    }

    public static class VerificationResult {
        private final boolean success;
        private final String message;
        private final String playerName;
        private final String playerUUID;

        public VerificationResult(boolean success, String message, String playerName, String playerUUID) {
            this.success = success;
            this.message = message;
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }

        public boolean isSuccess() {
            return this.success;
        }

        public String getMessage() {
            return this.message;
        }

        public String getPlayerName() {
            return this.playerName;
        }

        public String getPlayerUUID() {
            return this.playerUUID;
        }
    }

    public CompletableFuture<Boolean> storeIPForPlayer(String playerUUID, String playerName, String ipAddress) {
        try {
            if (ipAddress == null || ipAddress.isEmpty() || playerUUID == null || playerUUID.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }
            // Insert into otps table with the player's IP for alt account verification
            // Using current timestamp for expires_at (set far in future)
            String expiresAt = java.time.Instant.now().plusSeconds(365 * 24 * 60 * 60L).toString();
            String body = "{\"player_uuid\":\"" + playerUUID + "\",\"player_name\":\"" + playerName + "\",\"ip_address\":\"" + ipAddress + "\",\"otp_code\":\"ADMIN_IP_STORE\",\"expires_at\":\"" + expiresAt + "\",\"is_used\":true}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.supabaseUrl + "/rest/v1/otps"))
                    .header("apikey", this.supabaseServiceKey)
                    .header("Authorization", "Bearer " + this.supabaseServiceKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            
            return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
                if (response.statusCode() != 201 && response.statusCode() != 200) {
                    this.plugin.getLogger().warning("Failed to store IP for alt verification: " + response.statusCode());
                    return false;
                }
                this.plugin.getLogger().info("\u2705 Stored IP " + ipAddress + " for player " + playerName + " (alt account verification)");
                return true;
            }).exceptionally(e -> {
                this.plugin.getLogger().warning("Error storing IP for alt verification: " + e.toString());
                return false;
            });
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error preparing IP store request: " + e.toString());
            return CompletableFuture.completedFuture(false);
        }
    }
}

