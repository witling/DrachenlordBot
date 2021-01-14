package de.eliaspr;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.Compression;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public class Drache extends ListenerAdapter {

    private static ArrayList<String> messages = new ArrayList<>();
    private static ArrayList<File> photos = new ArrayList<>();
    private static ArrayList<File> photosNSFW = new ArrayList<>();
    private static Random random = new Random();

    public static void startDracheBot() throws LoginException, InterruptedException, IOException {
        messages.addAll(Files.readAllLines(new File("assets/drache/questions.txt").toPath()));
        messages.addAll(Files.readAllLines(new File("assets/drache/quotes.txt").toPath()));
        System.out.println("Loaded " + messages.size() + " quoutes/questions");

        photos.addAll(Arrays.asList(Objects.requireNonNull(new File("assets/drache/gifs/").listFiles())));
        photos.addAll(Arrays.asList(Objects.requireNonNull(new File("assets/drache/pics/").listFiles())));
        System.out.println("Found " + photos.size() + " gifs/pictures");

        photosNSFW.addAll(Arrays.asList(Objects.requireNonNull(new File("assets/drache/nsfw/").listFiles())));
        System.out.println("Found " + photosNSFW.size() + " nsfw gifs/pictures");

        String apiKey = new BufferedReader(new FileReader(new File("apikey.txt"))).readLine();
        JDABuilder builder = JDABuilder.createDefault(apiKey);
        builder.setActivity(Activity.playing("sich am Speer"));
        builder.setCompression(Compression.NONE);

        builder.addEventListeners(new Drache());
        JDA jda = builder.build();
        jda.awaitReady();
    }

    private void log(MessageReceivedEvent event, String info) {
        System.out.println("[" + event.getAuthor() + "/" + event.getChannel().getName() + "]: " + info);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        try {
            if (DiscordBots.checkChannel(event.getChannel()) && !event.getAuthor().isBot()) {
                String msg = event.getMessage().getContentRaw().toLowerCase().trim();
                if (msg.contains("etzala") || msg.contains("meddl")) {
                    String answer = messages.get(random.nextInt(messages.size()));
                    log(event, "Sending \"" + answer + "\"");
                    event.getChannel().sendMessage(answer).queue();
                }
                if (msg.contains("foddo")) {
                    File picture = photos.get(random.nextInt(photos.size()));
                    log(event, "Sending \"" + picture.getPath() + "\"");
                    event.getChannel().sendFile(picture).queue();
                }
                if (msg.contains("schanze")) {
                    log(event, "Sending Schanze in Google Maps");
                    event.getChannel().sendMessage("https://goo.gl/maps/gTke3Bdej1QCe72r9").queue();
                }
                if (msg.contains("naggig") && ((TextChannel) event.getChannel()).isNSFW()) {
                    File picture = photosNSFW.get(random.nextInt(photosNSFW.size()));
                    log(event, "Sending \"" + picture.getPath() + "\"");
                    event.getChannel().sendFile(picture).queue();
                }
//                if (msg.contains("rainer") || msg.contains("winkler")) {
//                    new Thread(() -> {
//                        for (int i = 0; i < 3; i++) {
//                            String url = getWikiLink();
//                            if (url != null) {
//                                event.getChannel().sendMessage(url).queue();
//                                break;
//                            }
//                        }
//                    }).start();
//                }
                if(msg.contains("exmatrikulation")) {
                    event.getChannel().sendMessage("https://www.mosbach.dhbw.de/service-einrichtungen/pruefungsamt/exmatrikulation/").queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getWikiLink() {
        try {
            URL url = new URL("https://altschauerberg.com/index.php/Spezial:Zuf%C3%A4llige_Seite");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setInstanceFollowRedirects(false);
            return conn.getHeaderField("Location");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
