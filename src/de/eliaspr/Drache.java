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
import java.nio.file.Files;
import java.util.*;

public class Drache extends ListenerAdapter {

    private static final ArrayList<String> activeCountdowns = new ArrayList<>();

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
                boolean etzala;
                if ((etzala = msg.contains("etzala")) || msg.contains("meddl")) {
                    if (etzala/* && event.getGuild().getIdLong() == 657602012179070988L*/) {
                        if (msg.contains("wi") && msg.contains("lang") && msg.contains("noch")) {
                            Calendar c = Calendar.getInstance();
                            int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
                            Lecture lecture = Lecture.getCurrentLecture(event.getGuild());
                            if (lecture != null) {
                                int remainingSecnds = (lecture.endTime - dayAgeMinutes) * 60 - c.get(Calendar.SECOND);
                                log(event, "Sending remaining time of " + lecture.name);
                                int remainingMinutes = remainingSecnds / 60;
                                remainingSecnds %= 60;
                                boolean minigun = remainingMinutes > 30;
                                int remainingHours = remainingMinutes / 60;
                                remainingMinutes %= 60;
                                event.getChannel().sendMessage(
                                        String.format("%s dauert noch %02d:%02d:%02d %s",
                                                lecture.name, remainingHours, remainingMinutes, remainingSecnds,
                                                minigun ? ":pepeMinigun:" : ""
                                        )).queue();
                            } else {
                                event.getChannel().sendMessage("Gerade läuft keine Vorlesung").queue();
                            }
                            return;
                        } else if (msg.contains("mach") && msg.contains("countdown")) {
                            final Lecture currentLecture = Lecture.getCurrentLecture(event.getGuild());
                            if (currentLecture != null) {
                                boolean alreadyStarted;
                                synchronized (activeCountdowns) {
                                    alreadyStarted = activeCountdowns.contains(currentLecture.name);
                                }
                                if (!alreadyStarted) {
                                    Thread th = new Thread(() -> {
                                        synchronized (activeCountdowns) {
                                            activeCountdowns.add(currentLecture.name);
                                        }
                                        event.getChannel().sendMessage("Countdown aktiv ab 15 Minuten :thumbsup:").queue();
                                        int lastDisplayedRemaining = -1;
                                        while (true) {
                                            Calendar c = Calendar.getInstance();
                                            int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
                                            int remainingMinutes = currentLecture.endTime - dayAgeMinutes;
                                            if (remainingMinutes != lastDisplayedRemaining) {
                                                lastDisplayedRemaining = remainingMinutes;
                                                if(remainingMinutes == 0) {
                                                    event.getChannel().sendMessage(
                                                            currentLecture.name + " ist vorbei! :beer: "
                                                    ).queue();
                                                    break;
                                                } else if (remainingMinutes < 15) {
                                                    event.getChannel().sendMessage(
                                                            currentLecture.name + " ist in " + remainingMinutes + " " +
                                                                    (remainingMinutes == 1 ? "Minute" : "Minuten") + " zu Ende!"
                                                    ).queue();
                                                }
                                            }
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                break;
                                            }
                                        }
                                        synchronized (activeCountdowns) {
                                            activeCountdowns.remove(currentLecture.name);
                                        }
                                    });
                                    th.setDaemon(true);
                                    th.start();
                                }
                            } else {
                                event.getChannel().sendMessage("Gerade läuft keine Vorlesung").queue();
                            }
                            return;
                        } else if (msg.contains("ein") && msg.contains("freiwillig")) {
                            Role nerdRole = event.getGuild().getRoleById(657894994186731520L);
                            List<Member> available = event.getGuild().getMembersWithRoles(nerdRole);
                            Random random = new Random();
                            Member member = available.get(random.nextInt(available.size()));
                            event.getChannel().sendMessage("Der Lord der Drachen hat " + member.getAsMention() + " auserwählt").queue();
                            return;
                        }
                    }
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
                if (msg.contains("exmatrikulation")) {
                    event.getChannel().sendMessage("https://www.mosbach.dhbw.de/service-einrichtungen/pruefungsamt/exmatrikulation/").queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int parseTimeStamp(String timeStamp) {
        String[] spl = timeStamp.split(":");
        int hours = Integer.parseInt(spl[0]);
        int minutes = Integer.parseInt(spl[1]);
        return 60 * hours + minutes;
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
