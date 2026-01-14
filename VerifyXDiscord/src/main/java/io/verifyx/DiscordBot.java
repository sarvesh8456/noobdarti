/*
 * Decompiled with CFR.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package io.verifyx;

import io.verifyx.Main;
import java.awt.Color;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordBot
extends ListenerAdapter {
    private final Main plugin;
    private JDA jda;
    private final ConcurrentHashMap<String, Integer> failedAttempts = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Long> timeoutEnds = new ConcurrentHashMap();
    private String verificationMessageId = null;
    private int otpLength = 6;
    private int otpExpiryMinutes = 10;
    private int maxOtpAttempts = 3;
    private String otpCharset = "0123456789";

    public DiscordBot(Main plugin) {
        this.plugin = plugin;
        if (plugin.getConfigData() != null) {
            this.otpLength = plugin.getConfigData().getOtpLength();
            this.otpExpiryMinutes = plugin.getConfigData().getOtpExpiryMinutes();
            this.maxOtpAttempts = plugin.getConfigData().getMaxOtpAttempts();
            this.otpCharset = plugin.getConfigData().getOtpCharset();
        }
        plugin.getLogger().info("\ud83d\udd10 Discord Bot OTP Config:");
        plugin.getLogger().info("  Length: " + this.otpLength);
        plugin.getLogger().info("  Expiry: " + this.otpExpiryMinutes + " minutes");
        plugin.getLogger().info("  Max Attempts: " + this.maxOtpAttempts);
        plugin.getLogger().info("  Charset: " + this.otpCharset);
    }

    public void start() throws LoginException, InterruptedException {
        String token = this.plugin.getConfigData().getDiscordBotToken();
        if (token != null && !token.isEmpty() && !token.equals("YOUR_BOT_TOKEN")) {
            this.jda = JDABuilder.createLight(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS).addEventListeners(this).build();
            this.jda.awaitReady();
            this.registerCommands();
            this.setupVerificationChannel();
            this.plugin.getLogger().info("\u2705 Discord bot started successfully!");
            return;
        }
        this.plugin.getLogger().severe("\u274c Discord bot token not set in config!");
    }

    private void registerCommands() {
        String guildId = this.plugin.getConfigData().getDiscordGuildId();
        if (guildId == null || guildId.isEmpty()) {
            this.plugin.getLogger().warning("\u26a0\ufe0f Discord guild ID not configured!");
            return;
        }
        Guild guild = this.jda.getGuildById(guildId);
        if (guild != null) {
            guild.updateCommands().addCommands(Commands.slash("verify", "Verify your Minecraft account with OTP").addOptions(new OptionData(OptionType.STRING, "otp", "The " + this.otpLength + "-digit OTP from Minecraft kick screen", true)), Commands.slash("unverify", "Unlink your Minecraft account"), Commands.slash("status", "Check your verification status"), Commands.slash("setupverify", "Setup verification channel (Admin only)"), Commands.slash("resendverify", "Resend verification embed (Admin only)")).queue(success -> this.plugin.getLogger().info("\u2705 Slash commands registered!"), error -> this.plugin.getLogger().severe("\u274c Failed to register slash commands: " + error.toString()));
        }
    }

    private void setupVerificationChannel() {
        String channelId = this.plugin.getConfigData().getDiscordVerifyChannelId();
        if (channelId == null || channelId.isEmpty()) {
            this.plugin.getLogger().warning("\u26a0\ufe0f No verification channel configured!");
            return;
        }
        TextChannel channel = this.jda.getTextChannelById(channelId);
        if (channel != null) {
            this.sendVerificationEmbed(channel);
        } else {
            this.plugin.getLogger().warning("\u26a0\ufe0f Verification channel not found with ID: " + channelId);
        }
    }

    private void sendVerificationEmbed(TextChannel channel) {
        channel.getHistory().retrievePast(20).queue(messages -> {
            Iterator iterator2 = messages.iterator();
            while (true) {
                if (!iterator2.hasNext()) {
                    EmbedBuilder embed = this.buildVerificationEmbed();
                    Button startButton = Button.primary("verify_start", "Start Verification").withEmoji(Emoji.fromUnicode("\ud83d\udd10"));
                    Button helpButton = Button.secondary("verify_help", "Help").withEmoji(Emoji.fromUnicode("\u2753"));
                    Button statusButton = Button.secondary("verify_status", "Check Status").withEmoji(Emoji.fromUnicode("\ud83d\udcca"));
                    ((MessageCreateAction)channel.sendMessageEmbeds(embed.build(), new MessageEmbed[0]).setComponents(ActionRow.of(startButton, helpButton, statusButton))).queue(message -> {
                        this.verificationMessageId = message.getId();
                        this.plugin.getLogger().info("\u2705 Verification embed sent to channel: " + channel.getName());
                    });
                    return;
                }
                Message message2 = (Message)iterator2.next();
                if (!message2.getAuthor().equals(this.jda.getSelfUser()) || message2.getEmbeds().size() <= 0) continue;
                if (message2.getEmbeds().get(0).getTitle() != null && message2.getEmbeds().get(0).getTitle().contains("Verification")) {
                    this.verificationMessageId = message2.getId();
                    return;
                }
                message2.delete().queue();
            }
        });
    }

    @Override
    public void onReady(ReadyEvent event) {
        this.plugin.getLogger().info("\u2705 Discord bot is ready! Logged in as: " + event.getJDA().getSelfUser().getName());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();
        this.plugin.getLogger().info("\ud83d\udcdd Discord command received: " + command + " from " + event.getUser().getName());
        switch (command) {
            case "verify": {
                this.handleVerifyCommand(event);
                break;
            }
            case "unverify": {
                this.handleUnverifyCommand(event);
                break;
            }
            case "status": {
                this.handleStatusCommand(event);
                break;
            }
            case "setupverify": {
                this.handleSetupVerifyCommand(event);
                break;
            }
            case "resendverify": {
                this.handleResendVerifyCommand(event);
                break;
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        this.plugin.getLogger().info("\ud83d\udd18 Button clicked: " + buttonId + " by " + event.getUser().getName());


        // Security escalation buttons (allow/deny) use prefixes: sec_allow:<uuid>:<b64ip> and sec_deny:<uuid>:<b64ip>
        try {
            if (buttonId.startsWith("sec_allow:")) {
                // request password modal
                String[] parts = buttonId.split(":", 3);
                if (parts.length >= 3) {
                    String uuid = parts[1];
                    String b64ip = parts[2];
                    String ip = new String(java.util.Base64.getUrlDecoder().decode(b64ip), java.nio.charset.StandardCharsets.UTF_8);
                    TextInput passInput = TextInput.create("sec_pass", "Confirm your Minecraft account password", TextInputStyle.SHORT).setPlaceholder("Enter your account password").setMinLength(6).setMaxLength(128).setRequired(true).build();
                    Modal modal = Modal.create("sec_pass_modal:" + uuid + ":" + b64ip, "Confirm Login Attempt").addComponents(ActionRow.of(passInput)).build();
                    event.replyModal(modal).queue();
                    return;
                }
            } else if (buttonId.startsWith("sec_deny:")) {
                String[] parts = buttonId.split(":", 3);
                if (parts.length >= 3) {
                    String uuid = parts[1];
                    String b64ip = parts[2];
                    String ip = new String(java.util.Base64.getUrlDecoder().decode(b64ip), java.nio.charset.StandardCharsets.UTF_8);
                    // Deny: block IP for this account
                    if (this.plugin.getLoginSecurityManager() != null) {
                        this.plugin.getLoginSecurityManager().denyIpForAccount(uuid, ip);
                    }
                    event.deferReply(true).queue(hook -> hook.editOriginal("\u274c Access denied. This IP has been blocked for the account.").queue());
                    return;
                }
            }
        } catch (Exception ignored) {}

        switch (buttonId) {
            case "verify_start": {
                this.handleStartVerification(event);
                break;
            }
            case "verify_help": 
            case "verify_status": {
                event.deferReply(true).queue();
                if (buttonId.equals("verify_help")) {
                    this.sendHelpEmbed(event);
                    break;
                }
                this.checkUserStatus(event);
                break;
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("verify_otp_modal")) {
            String otp = event.getValue("otp").getAsString().trim();
            String discordUserId = event.getUser().getId();
            String discordName = event.getUser().getName();
            event.deferReply(true).queue(hook -> {
                this.plugin.getLogger().info("\ud83d\udcde Discord user " + discordName + " submitted OTP via modal: " + otp);
                ((CompletableFuture)this.checkIfVerified(discordUserId).thenAccept(isVerified -> {
                    if (isVerified.booleanValue()) {
                        hook.editOriginal("\u2705 You are already verified! Use `/unverify` to unlink your account first.").queue();
                        return;
                    }
                    if (this.isInTimeout(discordUserId)) {
                        long remaining = this.timeoutEnds.get(discordUserId) - System.currentTimeMillis();
                        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
                        long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60L;
                        hook.editOriginal("\u23f0 You're in timeout! Please wait " + minutes + "m " + seconds + "s before trying again.").queue();
                        return;
                    }
                    if (otp.length() == this.otpLength && otp.matches("^[" + this.otpCharset + "]{" + this.otpLength + "}$")) {
                        this.verifyOTPFromDatabase(otp, discordUserId, discordName, (InteractionHook)hook);
                        return;
                    }
                    hook.editOriginal("\u274c Invalid OTP format. Must be " + this.otpLength + " digits from: " + this.otpCharset).queue();
                })).exceptionally(error -> {
                    hook.editOriginal("\u274c Error checking verification: " + error.toString()).queue();
                    return null;
                });
            });
            return;
        }
        // Security password modal for escalation confirmation: sec_pass_modal:<uuid>:<b64ip>
        if (event.getModalId().startsWith("sec_pass_modal:")) {
            String id = event.getModalId().substring("sec_pass_modal:".length());
            String[] parts = id.split(":", 2);
            if (parts.length >= 2) {
                String uuid = parts[0];
                String b64ip = parts[1];
                String ip = new String(java.util.Base64.getUrlDecoder().decode(b64ip), java.nio.charset.StandardCharsets.UTF_8);
                String pass = event.getValue("sec_pass").getAsString();
                String discordUserId = event.getUser().getId();
                event.deferReply(true).queue(hook -> {
                    this.plugin.getLogger().info("\ud83d\udd0d Security modal submitted by " + event.getUser().getName() + " for uuid " + uuid + " ip " + ip);
                    boolean ok = false;
                    try {
                        if (this.plugin.getLoginSecurityManager() != null) {
                            ok = this.plugin.getLoginSecurityManager().verifyPasswordForUuid(uuid, pass);
                        }
                    } catch (Exception ex) {
                        ok = false;
                    }
                    if (ok) {
                        // Allow IP for account
                        if (this.plugin.getLoginSecurityManager() != null) {
                            this.plugin.getLoginSecurityManager().allowIpForAccount(uuid, ip);
                        }
                        hook.editOriginal("\u2705 Access allowed. The IP has been added and cooldowns cleared.").queue();
                    } else {
                        hook.editOriginal("\u274c Password does not match. Access not granted.").queue();
                    }
                });
            }
            return;
        }
    }

    private void handleStartVerification(ButtonInteractionEvent event) {
        TextInput otpInput = TextInput.create("otp", "OTP Code", TextInputStyle.SHORT).setPlaceholder("Enter " + this.otpLength + "-digit code from Minecraft").setMinLength(this.otpLength).setMaxLength(this.otpLength).setRequired(true).build();
        Modal modal = Modal.create("verify_otp_modal", "\ud83d\udd10 Enter Your OTP").addComponents(ActionRow.of(otpInput)).build();
        event.replyModal(modal).queue();
    }

    private void handleVerifyCommand(SlashCommandInteractionEvent event) {
        String discordUserId = event.getUser().getId();
        String discordName = event.getUser().getName();
        OptionMapping otpOption = event.getOption("otp");
        if (otpOption == null) {
            event.reply("\u274c Please provide the OTP from the Minecraft kick screen.\nExample: `/verify 830829`").setEphemeral(true).queue();
            return;
        }
        String otp = otpOption.getAsString().trim();
        if (this.isInTimeout(discordUserId)) {
            long remaining = this.timeoutEnds.get(discordUserId) - System.currentTimeMillis();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60L;
            event.reply("\u23f0 You're in timeout! Please wait " + minutes + "m " + seconds + "s before trying again.").setEphemeral(true).queue();
            return;
        }
        if (otp.length() == this.otpLength && otp.matches("^[" + this.otpCharset + "]{" + this.otpLength + "}$")) {
            this.plugin.getLogger().info("\ud83d\udcde Discord user " + discordName + " (" + discordUserId + ") attempting OTP: " + otp);
            event.deferReply(true).queue(hook -> ((CompletableFuture)this.checkIfVerified(discordUserId).thenAccept(isVerified -> {
                if (isVerified.booleanValue()) {
                    hook.editOriginal("\u2705 You are already verified! Use `/unverify` to unlink your account first.").queue();
                    return;
                }
                this.verifyOTPFromDatabase(otp, discordUserId, discordName, (InteractionHook)hook);
            })).exceptionally(error -> {
                hook.editOriginal("\u274c Error checking verification: " + error.toString()).queue();
                return null;
            }));
            return;
        }
        event.reply("\u274c Invalid OTP format. Must be " + this.otpLength + " digits from: " + this.otpCharset).setEphemeral(true).queue();
    }

    private CompletableFuture<Boolean> checkIfVerified(String discordUserId) {
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        if (this.plugin.getDatabaseManager() != null) {
            ((CompletableFuture)this.plugin.getDatabaseManager().isDiscordVerified(discordUserId).thenAccept(future::complete)).exceptionally(error -> {
                this.plugin.getLogger().warning("\u26a0\ufe0f Error checking verification: " + error.toString());
                future.complete(false);
                return null;
            });
        } else {
            future.complete(false);
        }
        return future;
    }

    private void verifyOTPFromDatabase(String otp, String discordUserId, String discordName, InteractionHook hook) {
        this.plugin.getLogger().info("\ud83d\udd0d Checking OTP " + otp + " in database for Discord user: " + discordUserId);
        if (this.plugin.getDatabaseManager() == null) {
            hook.editOriginal("\u274c Verification system not ready. Please try again later.").queue();
            return;
        }
        ((CompletableFuture)this.plugin.getDatabaseManager().verifyOTP(otp, discordUserId, discordName).thenAccept(result -> {
            EmbedBuilder embed = new EmbedBuilder();
            if (result.isSuccess()) {
                this.plugin.getLogger().info("\u2705 Verification successful for: " + result.getPlayerName());
                this.resetFailedAttempts(discordUserId);
                this.addToWhitelist(result.getPlayerName(), result.getPlayerUUID());
                this.giveVerifiedRole(discordUserId, discordName);
                this.updateDiscordNickname(discordUserId, result.getPlayerName());
                // Link Discord to loginsecurity account (if present)
                try { if (this.plugin.getLoginSecurityManager() != null && result.getPlayerUUID() != null) this.plugin.getLoginSecurityManager().linkDiscordAccount(result.getPlayerUUID(), discordUserId); } catch (Exception ignored) {}
                embed.setTitle("\u2705 **Verification Complete!**");
                embed.setDescription("Your Discord account has been successfully linked!");
                embed.setColor(Color.GREEN);
                embed.addField("\ud83c\udfae Minecraft Account", "`" + result.getPlayerName() + "`", true);
                embed.addField("\ud83d\udcac Discord Account", discordName, true);
                embed.addField("\ud83d\udcca Status", "You can now rejoin the Minecraft server!", false);
                embed.setFooter("Verification successful | Use /status to check", this.jda.getSelfUser().getAvatarUrl());
                this.notifyMinecraftPlayer(result.getPlayerName());
                // If this verification belongs to a Bedrock join, cache the XUID mapping so future joins can auto-login
                try {
                    String playerUUID = result.getPlayerUUID();
                    if (playerUUID != null && this.plugin.getVerificationManager() != null) {
                        java.util.List<String> xuids = this.plugin.getVerificationManager().getBedrockXuidsForJavaUuid(playerUUID);
                        for (String xuid : xuids) {
                            try { this.plugin.getVerificationManager().cacheBedrockXuid(xuid, playerUUID); } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                this.plugin.getLogger().warning("\u274c Verification failed: " + result.getMessage());
                this.recordFailedAttempt(discordUserId);
                embed.setTitle("\u274c Verification Failed");
                embed.setDescription(result.getMessage());
                embed.setColor(Color.RED);
                if (result.getMessage().contains("already used")) {
                    embed.addField("\u26a0\ufe0f Status", "This OTP has already been used", false);
                    embed.addField("\ud83d\udd04 Next Steps", "Get a new OTP by rejoining the server", false);
                } else if (result.getMessage().contains("expired")) {
                    embed.addField("\u26a0\ufe0f Status", "This OTP has expired", false);
                    embed.addField("\ud83d\udd04 Next Steps", "Get a new OTP by rejoining the server", false);
                } else if (result.getMessage().contains("already linked")) {
                    embed.addField("\u26a0\ufe0f Status", "This Discord is already linked to another Minecraft account", false);
                    embed.addField("\ud83d\udd04 Next Steps", "Use `/unverify` first or contact staff", false);
                } else {
                    embed.addField("\u26a0\ufe0f Status", "Invalid OTP or system error", false);
                    embed.addField("\ud83d\udd04 Next Steps", "Try again with the correct OTP", false);
                }
                embed.setFooter("Verification failed | Try again or contact staff", this.jda.getSelfUser().getAvatarUrl());
            }
            hook.editOriginalEmbeds(embed.build()).queue();
        })).exceptionally(error -> {
            this.plugin.getLogger().severe("\u274c Error during verification: " + error.toString());
            EmbedBuilder errorEmbed = new EmbedBuilder().setTitle("\u26a0\ufe0f Internal Error").setDescription("An error occurred during verification. Please try again.").setColor(Color.ORANGE).addField("Error Details", (String)(error.toString().length() > 500 ? error.toString().substring(0, 500) + "..." : error.toString()), false).setFooter("System error | Please try again later", this.jda.getSelfUser().getAvatarUrl());
            hook.editOriginalEmbeds(errorEmbed.build()).queue();
            return null;
        });
    }

    private void sendHelpEmbed(ButtonInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("\u2753 Verification Help");
        embed.setDescription("Need help with verification? Here's what to do:");
        embed.setColor(new Color(0, 153, 255));
        embed.addField("\ud83d\udccb **Step-by-Step Guide**", "```\n1. Join the Minecraft server\n2. Copy your " + this.otpLength + "-digit OTP code\n3. Click 'Start Verification' button\n4. Enter your OTP code\n5. Get instant verification results!\n```", false);
        embed.addField("\u26a0\ufe0f **Common Issues**", "\u2022 **OTP not working?** Make sure you're entering the correct code\n\u2022 **OTP expired?** Codes expire in " + this.otpExpiryMinutes + " minutes - rejoin for a new one\n\u2022 **Already verified?** Use `/status` to check, `/unverify` to unlink\n\u2022 **Max attempts?** Wait 2 minutes after " + this.maxOtpAttempts + " failed attempts", false);
        embed.addField("\ud83c\udd98 **Need More Help?**", "Contact server staff or use `/verify <otp>` directly", false);
        embed.setFooter("Verification Help | Always double-check your OTP", this.jda.getSelfUser().getAvatarUrl());
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private void checkUserStatus(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        this.checkIfVerified(userId).thenAccept(isVerified -> {
            if (!isVerified.booleanValue()) {
                EmbedBuilder embed = new EmbedBuilder().setTitle("\u274c Not Verified").setDescription("You are not verified yet!").setColor(Color.RED).addField("\ud83d\udccb **How to Verify**", "```\n1. Join the Minecraft server\n2. Get your " + this.otpLength + "-digit OTP code\n3. Click 'Start Verification' button\n4. Enter your OTP code\n```", false).setFooter("Not verified | Click 'Start Verification' to begin", this.jda.getSelfUser().getAvatarUrl());
                event.getHook().editOriginalEmbeds(embed.build()).queue();
                return;
            }
            if (this.plugin.getDatabaseManager() != null) {
                this.plugin.getDatabaseManager().getVerificationDetails(userId).thenAccept(details -> {
                    if (details != null) {
                        String statusEmoji = details.isOnline ? "\ud83d\udfe2" : "\ud83d\udd34";
                        String statusText = details.isOnline ? "Online Now" : "Last seen: " + details.daysSinceLastSeen + " days ago";
                        EmbedBuilder embed = new EmbedBuilder().setTitle("\u2705 Verification Status").setDescription("Your account is verified!").setColor(Color.GREEN).addField("\ud83c\udfae **Minecraft Account**", "`" + details.playerName + "`", true).addField(statusEmoji + " **Status**", statusText, true).addField("\ud83d\udcc5 **Verified On**", details.verifiedDate, false).addField("\u2699\ufe0f **Actions**", "\u2022 Use `/unverify` to unlink your account\n\u2022 Contact staff for help", false).setFooter("Verification status | All good to play!", this.jda.getSelfUser().getAvatarUrl());
                        event.getHook().editOriginalEmbeds(embed.build()).queue();
                    }
                });
            }
        });
    }

    private void handleSetupVerifyCommand(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("\u274c This command requires administrator permissions.").setEphemeral(true).queue();
            return;
        }
        this.setupVerificationChannel();
        event.reply("\u2705 Verification channel has been set up!").setEphemeral(true).queue();
    }

    private void handleResendVerifyCommand(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("\u274c This command requires administrator permissions.").setEphemeral(true).queue();
            return;
        }
        String channelId = this.plugin.getConfigData().getDiscordVerifyChannelId();
        if (channelId == null || channelId.isEmpty()) {
            event.reply("\u274c No verification channel configured in config.").setEphemeral(true).queue();
            return;
        }
        TextChannel channel = this.jda.getTextChannelById(channelId);
        if (channel != null) {
            this.sendVerificationEmbed(channel);
            event.reply("\u2705 Verification embed has been resent!").setEphemeral(true).queue();
        } else {
            event.reply("\u274c Verification channel not found.").setEphemeral(true).queue();
        }
    }

    private void addToWhitelist(String playerName, String playerUUID) {
        if (this.plugin.getVerificationManager() != null) {
            this.plugin.getVerificationManager().addToWhitelist(playerName, playerUUID);
            this.plugin.getLogger().info("\u2705 Added " + playerName + " to whitelist");
        } else {
            this.plugin.getLogger().warning("\u274c VerificationManager not available for whitelisting");
        }
    }

    private void giveVerifiedRole(String userId, String discordName) {
        String roleId = this.plugin.getConfigData().getDiscordVerifiedRoleId();
        if (roleId == null || roleId.isEmpty()) {
            this.plugin.getLogger().warning("\u26a0\ufe0f No verified role ID configured");
            return;
        }
        Guild guild = this.jda.getGuildById(this.plugin.getConfigData().getDiscordGuildId());
        if (guild != null) {
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                guild.addRoleToMember(User.fromId(userId), role).queue(success -> this.plugin.getLogger().info("\u2705 Gave verified role to " + discordName), error -> this.plugin.getLogger().warning("\u274c Failed to give role: " + error.toString()));
            } else {
                this.plugin.getLogger().warning("\u274c Role not found with ID: " + roleId);
            }
        }
    }

    private void updateDiscordNickname(String userId, String playerName) {
        Guild guild = this.jda.getGuildById(this.plugin.getConfigData().getDiscordGuildId());
        if (guild == null) {
            return;
        }
        guild.retrieveMemberById(userId).queue(member -> {
            if (!guild.getSelfMember().canInteract((Member)member)) {
                this.plugin.getLogger().warning("Cannot modify nickname for " + userId + " (insufficient permissions)");
                return;
            }
            String finalNickname = playerName.length() > 32 ? playerName.substring(0, 32) : playerName;
            member.modifyNickname(finalNickname).queue(success -> this.plugin.getLogger().info("\u2705 Updated nickname for " + userId + " to " + finalNickname), error -> this.plugin.getLogger().warning("\u274c Failed to update nickname: " + error.toString()));
        }, error -> this.plugin.getLogger().warning("Could not retrieve member for nickname update: " + error.toString()));
    }

    private void notifyMinecraftPlayer(String playerName) {
        Player player = Bukkit.getPlayer((String)playerName);
        if (player == null) {
            // Try normalized variant for Bedrock names that may have a leading dot
            String norm = playerName != null ? playerName.replaceAll("^[\\.\\s]+", "") : null;
            if (norm != null && !norm.equals(playerName)) {
                player = Bukkit.getPlayer((String)norm);
            }
        }
        if (player != null) {
            player.sendMessage("\u00a7a\u2705 Discord verification complete! You can now access the main server.");
            player.sendMessage("\u00a7aYour Discord account has been linked successfully.");
            try {
                // If login security is enabled, ensure the player is prompted to register/login
                if (this.plugin.getConfig().getBoolean("loginsecurity.enabled", false) && this.plugin.getLoginSecurityManager() != null) {
                    Player p = player;
                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                        try {
                            this.plugin.getLoginSecurityManager().checkOnJoin(p);
                            if (!this.plugin.getLoginSecurityManager().isRegistered(p.getUniqueId().toString())) {
                                p.sendMessage("\u00a7eNote: this server uses password login security. Use: \u00a7a/register <password> <password>\u00a7e to register your account.");
                            }
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    // Send a security alert DM to the linked Discord account with Allow/Deny actions
    public void sendSecurityAlert(String discordUserId, String accountUuid, String playerName, String ip) {
        try {
            if (discordUserId == null) return;
            String b64ip = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(ip.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            net.dv8tion.jda.api.entities.User user = this.jda.getUserById(discordUserId);
            if (user == null) {
                this.jda.retrieveUserById(discordUserId).queue(u -> sendSecurityAlertToUser(u, accountUuid, playerName, ip, b64ip), error -> this.plugin.getLogger().warning("Could not retrieve user for security alert: " + error.toString()));
            } else {
                sendSecurityAlertToUser(user, accountUuid, playerName, ip, b64ip);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error sending security alert: " + e.toString());
        }
    }

    private void sendSecurityAlertToUser(net.dv8tion.jda.api.entities.User user, String accountUuid, String playerName, String ip, String b64ip) {
        try {
            java.awt.Color clr = new java.awt.Color(255, 165, 0);
            EmbedBuilder embed = new EmbedBuilder().setTitle("\u26a0\ufe0f Security Alert: Login Attempt").setDescription("We detected multiple failed login attempts on your Minecraft account").setColor(clr);
            embed.addField("Minecraft Account", playerName, true);
            embed.addField("IP Address", ip != null ? ip : "unknown", true);
            embed.addField("Action", "Use the buttons below to allow or deny this IP.", false);
            Button allow = Button.success("sec_allow:" + accountUuid + ":" + b64ip, "Allow (confirm with password)");
            Button deny = Button.danger("sec_deny:" + accountUuid + ":" + b64ip, "Deny (block IP)");
            user.openPrivateChannel().queue(ch -> {
                ((MessageCreateAction)ch.sendMessageEmbeds(embed.build(), new MessageEmbed[0]).setComponents(ActionRow.of(allow, deny))).queue(msg -> this.plugin.getLogger().info("Sent security alert DM to " + user.getName()), error -> this.plugin.getLogger().warning("Failed to send security DM: " + error.toString()));
            }, error -> this.plugin.getLogger().warning("Failed to open private channel for security alert: " + error.toString()));
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error building security alert: " + e.toString());
        }
    }

    private void handleUnverifyCommand(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        event.deferReply(true).queue(hook -> this.checkIfVerified(userId).thenAccept(isVerified -> {
            if (!isVerified.booleanValue()) {
                hook.editOriginal("\u274c You are not verified yet!").queue();
                return;
            }
            if (this.plugin.getDatabaseManager() != null) {
                this.plugin.getDatabaseManager().getVerificationDetails(userId).thenAccept(details -> {
                    if (details != null) {
                        String playerName = details.playerName;
                        this.plugin.getDatabaseManager().removeVerification(userId).thenAccept(success -> {
                            if (success.booleanValue()) {
                                // pass along UUID if available so we can fully clear IP tracker and caches
                                this.plugin.getVerificationManager().removeFromWhitelist(playerName, details.playerUUID);
                                this.removeVerifiedRole(userId);
                                this.resetNickname(userId);
                                hook.editOriginal("\u2705 Your account has been unlinked from Minecraft.").queue();
                                this.plugin.getLogger().info("\u2705 " + event.getUser().getName() + " unverified from " + playerName);
                            } else {
                                hook.editOriginal("\u274c Failed to unlink your account. Please contact admin.").queue();
                            }
                        });
                    } else {
                        hook.editOriginal("\u274c Could not find your verification details.").queue();
                    }
                });
            } else {
                hook.editOriginal("\u274c Database not available. Please contact admin.").queue();
            }
        }));
    }

    private void removeFromWhitelist(String playerName) {
        if (this.plugin.getVerificationManager() != null) {
            this.plugin.getVerificationManager().removeFromWhitelist(playerName);
            this.plugin.getLogger().info("\u2705 Removed " + playerName + " from whitelist");
        } else {
            this.plugin.getLogger().warning("\u274c VerificationManager not available for whitelist removal");
        }
    }

    private void removeVerifiedRole(String userId) {
        Role role;
        String roleId = this.plugin.getConfigData().getDiscordVerifiedRoleId();
        if (roleId == null || roleId.isEmpty()) {
            return;
        }
        Guild guild = this.jda.getGuildById(this.plugin.getConfigData().getDiscordGuildId());
        if (guild != null && (role = guild.getRoleById(roleId)) != null) {
            guild.removeRoleFromMember(User.fromId(userId), role).queue(success -> this.plugin.getLogger().info("\u2705 Removed verified role from " + userId), error -> this.plugin.getLogger().warning("\u274c Failed to remove role: " + error.toString()));
        }
    }

    private void resetNickname(String userId) {
        Guild guild = this.jda.getGuildById(this.plugin.getConfigData().getDiscordGuildId());
        if (guild != null) {
            guild.retrieveMemberById(userId).queue(member -> {
                if (member.getNickname() != null) {
                    member.modifyNickname(null).queue(success -> this.plugin.getLogger().info("\u2705 Reset nickname for " + userId), error -> this.plugin.getLogger().warning("\u274c Failed to reset nickname: " + error.toString()));
                }
            });
        }
    }

    private void handleStatusCommand(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        event.deferReply(true).queue(hook -> this.checkIfVerified(userId).thenAccept(isVerified -> {
            if (!isVerified.booleanValue()) {
                hook.editOriginal("\u274c You are not verified!\n\nTo verify:\n1. Join the Minecraft server\n2. Get your OTP from the kick screen\n3. Type: `/verify <your-otp>`").queue();
                return;
            }
            if (this.plugin.getDatabaseManager() != null) {
                this.plugin.getDatabaseManager().getVerificationDetails(userId).thenAccept(details -> {
                    if (details != null) {
                        String lastSeen = details.isOnline ? "\ud83d\udfe2 Online now" : "Last seen: " + details.daysSinceLastSeen + " days ago";
                        hook.editOriginal("\u2705 **You are verified!**\n\n**Minecraft Account:** `" + details.playerName + "`\n**Status:** " + lastSeen + "\n**Verified:** " + details.verifiedDate).queue();
                    } else {
                        hook.editOriginal("\u2705 You are verified, but details are unavailable.").queue();
                    }
                });
            } else {
                hook.editOriginal("\u2705 You are verified!").queue();
            }
        }));
    }

    private boolean isInTimeout(String userId) {
        if (!this.timeoutEnds.containsKey(userId)) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime < this.timeoutEnds.get(userId)) {
            return true;
        }
        this.timeoutEnds.remove(userId);
        this.failedAttempts.remove(userId);
        return false;
    }

    private void recordFailedAttempt(String userId) {
        int attempts = this.failedAttempts.getOrDefault(userId, 0) + 1;
        this.failedAttempts.put(userId, attempts);
        this.plugin.getLogger().warning("Failed OTP attempt #" + attempts + " from user ID: " + userId);
        if (attempts >= this.maxOtpAttempts) {
            long timeoutDuration = 120000L;
            this.timeoutEnds.put(userId, System.currentTimeMillis() + timeoutDuration);
            this.failedAttempts.remove(userId);
            this.plugin.getLogger().warning("User " + userId + " timed out for 2 minutes due to " + this.maxOtpAttempts + " failed attempts");
        }
    }

    private void resetFailedAttempts(String userId) {
        this.failedAttempts.remove(userId);
        this.timeoutEnds.remove(userId);
    }

    public boolean isConnected() {
        return this.jda != null && this.jda.getStatus() == JDA.Status.CONNECTED;
    }

    public boolean isReady() {
        return isConnected();
    }



    public void shutdown() {
        if (this.jda != null) {
            this.jda.shutdown();
        }
    }

    public void reloadConfig() {
        if (this.plugin.getConfigData() != null) {
            this.otpLength = this.plugin.getConfigData().getOtpLength();
            this.otpExpiryMinutes = this.plugin.getConfigData().getOtpExpiryMinutes();
            this.maxOtpAttempts = this.plugin.getConfigData().getMaxOtpAttempts();
            this.otpCharset = this.plugin.getConfigData().getOtpCharset();
            this.plugin.getLogger().info("\ud83d\udd10 Discord Bot OTP Config reloaded:");
            this.plugin.getLogger().info("  Length: " + this.otpLength);
            this.plugin.getLogger().info("  Expiry: " + this.otpExpiryMinutes + " minutes");
            this.plugin.getLogger().info("  Max Attempts: " + this.maxOtpAttempts);
            this.plugin.getLogger().info("  Charset: " + this.otpCharset);
        }
        try {
            if (this.jda != null && this.jda.getStatus() == JDA.Status.CONNECTED) {
                try {
                    this.registerCommands();
                } catch (Exception ignored) {}
                String channelId = this.plugin.getConfigData().getDiscordVerifyChannelId();
                if (channelId != null && !channelId.isEmpty()) {
                    TextChannel channel = this.jda.getTextChannelById(channelId);
                    if (channel != null) {
                        if (this.verificationMessageId != null) {
                            channel.retrieveMessageById(this.verificationMessageId).queue(message -> {
                                try {
                                    message.editMessageEmbeds(this.buildVerificationEmbed().build()).queue();
                                } catch (Exception e) {
                                    this.plugin.getLogger().warning("Failed to edit existing verification embed: " + e.toString());
                                }
                            }, err -> this.sendVerificationEmbed(channel));
                        } else {
                            this.sendVerificationEmbed(channel);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            this.plugin.getLogger().warning("Error reloading DiscordBot config: " + t.toString());
        }
    }

    private EmbedBuilder buildVerificationEmbed() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("\ud83d\udd10 Server Verification");
        embed.setDescription("Click the button below to start the verification process in a clean ticket-style interface.");
        embed.setColor(new Color(0, 153, 255));
        embed.addField("\ud83d\udccb **How to Verify**", "```\n1. Join the Minecraft server\n2. Get your " + this.otpLength + "-digit OTP code\n3. Click 'Start Verification' below\n4. Enter your OTP code\n5. Get instant results!\n```", false);
        embed.addField("\u26a0\ufe0f **Important Notes**", "\u2022 OTP codes expire in " + this.otpExpiryMinutes + " minutes\n\u2022 Max " + this.maxOtpAttempts + " attempts per user\n\u2022 One Discord account per Minecraft account\n\u2022 Need help? Contact staff", false);
        embed.setFooter("Verification System | Use /status to check your verification", this.jda.getSelfUser().getAvatarUrl());
        return embed;
    }

    public JDA getJda() {
        return this.jda;
    }

    public static class VerificationDetails {
        public String playerName;
        public String playerUUID;
        public String discordId;
        public String otp;
        public long timestamp;
        public String verifiedDate;
        public boolean isOnline;
        public int daysSinceLastSeen;

        public VerificationDetails(String playerName, String playerUUID, String discordId, String otp, long timestamp) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.discordId = discordId;
            this.otp = otp;
            this.timestamp = timestamp;
        }

        public VerificationDetails() {
        }
    }

    public static class PlayerInfo {
        public String playerName;
        public String playerUUID;
        public String discordId;

        public PlayerInfo(String playerName, String playerUUID, String discordId) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            this.discordId = discordId;
        }
    }
}

