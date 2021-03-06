package nikos.kummerkastenBot.util;

import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;
import sx.blah.discord.util.RequestBuffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Util {

    /*
    RequestBuffer.request(() -> {
            // Stuff
        });
     */

    public static synchronized IMessage sendMessage(final IChannel channel, final String message) {
        try {
            return channel.sendMessage(message);
        } catch (RateLimitException e) {
            System.err.println("[ERR] Ratelimited!");
        } catch (MissingPermissionsException e) {
            System.err.println("[ERR] Missing Permissions");
        } catch (DiscordException e) {
            System.err.println("[ERR] " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized IMessage sendEmbed(final IChannel channel, final EmbedObject embedObject) {
        try {
            RequestBuffer.request(() -> {
                return channel.sendMessage(embedObject);
            });

        } catch (RateLimitException e) {
            System.err.println("[ERR] Ratelimited!");
        } catch (MissingPermissionsException e) {
            System.err.println("[ERR] Missing Permissions");
        } catch (DiscordException e) {
            System.err.println("[ERR] " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized IMessage sendPM(final IUser user, final String message) {
        try {
            final IPrivateChannel channel = user.getOrCreatePMChannel();
            return channel.sendMessage(message);
        } catch (RateLimitException e) {
            System.err.println("[ERR] Ratelimited!");
        } catch (MissingPermissionsException e) {
            System.err.println("[ERR] Missing Permissions");
        } catch (DiscordException e) {
            System.err.println("[ERR] " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static String getContext(final String message) {
        if (message.contains(" ")) {
            return message.substring(message.indexOf(' ') + 1);
        }
        else {
            return "";
        }
    }

    public static boolean hasRoleByID(final IUser user, final IGuild guild, final String roleID) {
        final List<IRole> roles = user.getRolesForGuild(guild);

        for (IRole role : roles) {
            if (roleID.equals(role.getStringID())) {
                return true;
            }
        }
        return false;
    }

    public static String readFile(Path path) {
        try {
            return new String(Files.readAllBytes(path));
        }
        catch (IOException e){
            System.err.println("[ERR]  " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static void writeToFile(Path file, String text) {
        try {
            Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            System.err.println("[ERR] " + e.getMessage());
            e.printStackTrace();
        }

    }

}