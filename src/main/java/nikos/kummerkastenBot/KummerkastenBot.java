package nikos.kummerkastenBot;


import nikos.kummerkastenBot.util.AnonUser;
import nikos.kummerkastenBot.util.Authorization;
import nikos.kummerkastenBot.util.Util;

import org.json.JSONObject;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.Color;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KummerkastenBot {
    private final static Path CONFIG_PATH = Paths.get("config/config.json");

    private static IDiscordClient client;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private String prefix;

    private long channelID;
    private IChannel channel;

    private long modRoleID;
    private IRole modRole;

    private long ownerID;

    private long period;
    private String informationMessage;

    private HashMap<String, AnonUser> anonUsers;
    private JSONObject jsonBlacklist;


    private KummerkastenBot() {
        this.anonUsers = new HashMap<>();

        // Read config
        final String configFileContent = Util.readFile(CONFIG_PATH);
        final JSONObject jsonConfig = new JSONObject(configFileContent);

        final String token = jsonConfig.getString("token");
        client = Authorization.createClient(token, true);

        try {
            EventDispatcher dispatcher = client.getDispatcher();
            dispatcher.registerListener(this);
        }
        catch (NullPointerException e) {
            System.err.println("[ERR] Could not get EventDispatcher: " + '\n' + e.getMessage());
            e.printStackTrace();
        }

        this.prefix = jsonConfig.getString("prefix");

        this.channelID = jsonConfig.getLong("channel");
        this.modRoleID = jsonConfig.getLong("modRole");
        this.ownerID = jsonConfig.getLong("owner");

        this.period = jsonConfig.getLong("resetPeriod");
        this.informationMessage = jsonConfig.getString("message");

        // Wait until next midnight to start cycle
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime nextMidnight = LocalDateTime.of(LocalDate.now().plusDays(1L), LocalTime.of(0, 0));
        final long timeUntilMidnight = now.until(nextMidnight, ChronoUnit.SECONDS);
        scheduler.schedule(this::startCycle, timeUntilMidnight, TimeUnit.SECONDS);
    }

    @EventSubscriber
    public void onStartup(ReadyEvent event) {
        this.channel = client.getChannelByID(this.channelID);
        this.modRole = channel.getGuild().getRoleByID(this.modRoleID);

        client.changePresence(StatusType.ONLINE, ActivityType.PLAYING, this.prefix + "kummerkasten");
        System.out.println("[INFO] Bot ready! Prefix: " + this.prefix);
    }

    private void startCycle() {
        scheduler.scheduleAtFixedRate(this::resetIDs, 0, this.period, TimeUnit.HOURS);
    }

    @EventSubscriber
    public void onMessageRecieved(MessageReceivedEvent event) {
        final IMessage message = event.getMessage();
        final String messageContent = message.getContent();

        if (messageContent.equalsIgnoreCase(this.prefix + "kummerkasten")) {
            this.command_Kummerkasten(message);
        }
        else if (messageContent.matches(this.prefix + "mute \\d+") ||
                messageContent.matches(this.prefix + "blacklist \\d+")) {
            this.command_Blacklist(message);
        }
        else if (messageContent.matches(this.prefix + "whitelist \\d+")) {
            this.command_Whitelist(message);
        }
        else if (messageContent.equalsIgnoreCase(this.prefix + "resetIDs")) {
            this.command_ResetIDs(message);
        }
        else if (message.getChannel().isPrivate()) {
            this.relayMessage(message);
        }
    }

    private void command_ResetIDs(final IMessage message) {
        if (message.getAuthor().getStringID().equals("165857945471418368")) {
            this.resetIDs();
        }
    }

    private void command_Blacklist(final IMessage message) {
        if (!message.getAuthor().getRolesForGuild(message.getGuild()).contains(this.modRole)) {
            return;
        }

        final String anonID = Util.getContext(message.getContent());

        boolean found = false;
        for (String discordID : this.anonUsers.keySet()) {
            final AnonUser anonUser = this.anonUsers.get(discordID);
            if (anonUser.getID().equals(anonID)) {
                anonUser.blacklist();
                found = true;
            }
        }

        if (found) {
            Util.sendMessage(message.getChannel(), ":white_check_mark: **Nutzer #" + anonID + "** geblacklisted!");
        }
        else {
            Util.sendMessage(message.getChannel(), ":x: Fehler! Nutzer mit der ID `" + anonID + "` nicht gefunden");
        }
    }

    private void command_Whitelist(final IMessage message) {
        if (!message.getAuthor().getRolesForGuild(message.getGuild()).contains(this.modRole)) {
            return;
        }

        final String anonID = Util.getContext(message.getContent());

        boolean found = false;
        for (String discordID : this.anonUsers.keySet()) {
            final AnonUser anonUser = this.anonUsers.get(discordID);
            if (anonUser.getID().equals(anonID)) {
                anonUser.whitelist();
                found = true;
            }
        }

        if (found) {
            Util.sendMessage(message.getChannel(), ":white_check_mark: **Nutzer #" + anonID + "** gewhitelisted!");
        }
        else {
            Util.sendMessage(message.getChannel(), ":x: Fehler! Nutzer mit der ID `" + anonID + "` nicht gefunden");
        }
    }

    private void command_Kummerkasten(final IMessage message) {
        final String kummerkastenExplanation = "Das Prinzip des Kummerkastens ist ganz simpel: " +
                "Du willst etwas loswerden, ohne dass alle wissen wer du bist? **Schreibe einfach eine PM an diesen Bot** " +
                "und er gibt es im Kanal #kummerkasten wieder. Du erhälst eine anonyme Identität, damit man weiß, " +
                "welche Nachrichten zusammengehören, **diese Identität wird allerdings täglich zurückgesetzt**.";

        Util.sendPM(message.getAuthor(), kummerkastenExplanation);

        if (!message.getChannel().isPrivate()) {
            Util.sendMessage(message.getChannel(), ":mailbox_with_mail:");
        }
    }

    private void relayMessage(final IMessage message) {
        final IUser discordUser = message.getAuthor();
        final String discordID = discordUser.getStringID();

        if (!anonUsers.containsKey(discordID)) {
            this.makeAnonUser(discordUser);
        }

        final AnonUser anonUser = anonUsers.get(discordID);

        if (!anonUser.hasApproved()) {
            Util.sendPM(discordUser, this.informationMessage);
            anonUser.setDidApprove();
            return;
        }

        if (anonUser.isBlacklisted()) {
            Util.sendPM(message.getAuthor(), "Du bist geblacklisted! Wenn du glaubst dass das ein Fehler ist, " +
                    "melde dich bei einem Moderator!");
            return;
        }

        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.withColor(anonUser.getColor());
        embedBuilder.withAuthorName("Nutzer #" + anonUser.getID());
        embedBuilder.withDesc(message.getContent());

        for (IMessage.Attachment attachment : message.getAttachments()) {
            embedBuilder.withImage(attachment.getUrl());
        }

        Util.sendEmbed(channel, embedBuilder.build());
    }

    private void makeAnonUser(final IUser discordUser) {
        final String anonID = "" + (int)(Math.random()*99999);

        final int red = 100 + (int)(Math.random()*155);
        final int green = 100 + (int)(Math.random()*155);
        final int blue = 100 + (int)(Math.random()*155);

        final Color anonColor = new Color(red, green, blue);

        final AnonUser anonUser = new AnonUser(anonID, anonColor);
        anonUsers.put(discordUser.getStringID(), anonUser);
    }

    private void resetIDs() {
        this.anonUsers = new HashMap<>();
        Util.sendMessage(this.channel, "Identitäten wurden zurückgesetzt!");
        System.out.println("[INFO] Identitäten wurden zurückgesetzt!");
    }

    public static void main(String[] args) {
        new KummerkastenBot();
    }
}
