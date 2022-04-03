package de.eliaspr.drache;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.TimeZone;

public class DiscordBots {

    public static boolean checkChannel(MessageChannel channel) {
        return channel.getType() == ChannelType.TEXT;
    }

    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        System.out.println("Setting time zone");
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

        System.out.println("Starting Drache bot");
        Drache.startDracheBot();
    }

}
