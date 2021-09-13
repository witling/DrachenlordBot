package de.eliaspr.drache;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
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

    private static final Scheduler scheduler = new Scheduler();

    public static Scheduler getScheduler() {
        return scheduler;
    }

    private static final Random random = new Random();
    public static final String[] panikEmotes = {"pepeMinigun", "pepeShotgun", "pepeSteckdose", "pepeHands", "pepeGalgen", "panik", "noose"};
    public static final String[] happyEmotes = {"pepega", "yes", "pogChamp", "pog", "uzbl"};
    public static final String[] alcoholEmotes = {"vodka", "jaegermeister", "bier", "asbach"};
    private static final HashMap<Long, Countdown> activeCountdowns = new HashMap<>();
    private static boolean isPauseActive = false;

    private static ArrayList<String> messages = new ArrayList<>();
    private static ArrayList<String> trumpTweets = new ArrayList<>();
    private static ArrayList<File> photos = new ArrayList<>();
    private static ArrayList<File> photosNSFW = new ArrayList<>();

    public static void startDracheBot() throws LoginException, InterruptedException, IOException {
        messages.addAll(Files.readAllLines(new File("assets/drache/questions.txt").toPath()));
        messages.addAll(Files.readAllLines(new File("assets/drache/quotes.txt").toPath()));
        System.out.println("Loaded " + messages.size() + " quoutes/questions");

        trumpTweets.addAll(Files.readAllLines(new File("assets/drache/trump.txt").toPath()));
        System.out.println("Loaded " + trumpTweets.size() + " trump tweets");

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
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
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
                boolean isNerdServer = event.getGuild().getIdLong() == 657602012179070988L;
                if(isNerdServer && msg.contains("\u1794") || msg.contains("\u1796") || msg.contains("\uD83C\uDDFA\uD83C\uDDF8")) {
                    String answer = trumpTweets.get(random.nextInt(trumpTweets.size()));
                    log(event, "Sending trump quote");
                    event.getChannel().sendMessage("**Trump:** " + answer).queue(message -> message.suppressEmbeds(true).queue());
                    return;
                }
                if ((etzala = msg.contains("etzala")) || msg.contains("meddl")) {
                    if (etzala && isNerdServer) {
                        boolean normalEtzala = false;
                        if (msg.contains("wi") && msg.contains("lang") && msg.contains("noch")) {
                            sendRemainingTime(event);
                        } else if (msg.contains("wi") && msg.contains("schaf") && msg.contains("klausur")) {
                            PinToPDF.createPDF(event);
                        } else if (msg.contains("mach") && msg.contains("countdown")) {
                            startCountdown(event);
                        } else if (msg.contains("ein")) {
                            if (msg.contains("freiwillig")) {
                                Role nerdRole = event.getGuild().getRoleById(657894994186731520L);
                                Guild guild = event.getJDA().getGuildById(657602012179070988L);
                                guild.findMembers(member -> member.getRoles().contains(nerdRole)).onSuccess(list -> {
                                    Member member = list.get(random.nextInt(list.size()));
                                    event.getChannel().sendMessage("Der Lord der Drachen hat " + member.getAsMention() + " auserwählt").queue();
                                });
                            } else if (msg.contains("zitat")) {
                                sendRandomQuote(event);
                            }
                        } else if (msg.contains("pause")) {
                            createPauseReminder(event, msg, msg.contains("mittag"));
                        } else if (msg.contains("hilfe") || msg.contains("hälp") || msg.contains("help")) {
                            StringBuilder sb = new StringBuilder("**Drache-Bot**").append('\n');
                            sb.append('\n').append("*Folgende Befehle fangen immer mit `etzala` an:*").append('\n').append('\n');
                            sb.append(" - wi[e] lang[e] noch").append('\n');
                            sb.append(" - mach countdown").append('\n');
                            sb.append(" - ein[e] freiwillig[e[r]]").append('\n');
                            sb.append(" - ein zitat").append('\n');
                            sb.append(" - pause <zeit in minuten>").append('\n');
                            sb.append(" - pause bis <hh>:<mm>").append('\n');
                            sb.append(" - hilfe / help / hälp").append('\n');
                            sb.append('\n').append("*Alle weiteren Befehle*").append('\n').append('\n');
                            sb.append(" - meddl / etzala").append('\n');
                            sb.append(" - foddo").append('\n');
                            sb.append(" - schanze").append('\n');
                            sb.append(" - exmatrikulation");
                            event.getChannel().sendMessage(sb).queue();
                        } else {
                            normalEtzala = true;
                        }
                        if (!normalEtzala)
                            return;
                    }
                    String answer = messages.get(random.nextInt(messages.size()));
                    log(event, "Sending \"" + answer + "\"");
                    event.getChannel().sendMessage(answer).queue();
                } else if (msg.contains("foddo")) {
                    File picture = photos.get(random.nextInt(photos.size()));
                    log(event, "Sending \"" + picture.getPath() + "\"");
                    event.getChannel().sendFile(picture).queue();
                } else if (msg.contains("schanze")) {
                    log(event, "Sending Schanze in Google Maps");
                    event.getChannel().sendMessage("https://goo.gl/maps/gTke3Bdej1QCe72r9").queue();
                } else if (msg.contains("naggig") && ((TextChannel) event.getChannel()).isNSFW()) {
                    File picture = photosNSFW.get(random.nextInt(photosNSFW.size()));
                    log(event, "Sending \"" + picture.getPath() + "\"");
                    event.getChannel().sendFile(picture).queue();
                } else if (isNerdServer && msg.contains("was") && msg.contains("verpasst")) {
                    event.getChannel().sendMessage("Du hast nix verpasst " + getServerEmoteAsMention(event.getGuild(), "dhbw_logo")).queue();
                } else if (isNerdServer && msg.contains("exmatrikulation")) {
                    event.getChannel().sendMessage("https://www.mosbach.dhbw.de/service-einrichtungen/pruefungsamt/exmatrikulation/").queue();
                } else if (isNerdServer && (msg.contains("bachelor") || msg.contains("abgabe") || msg.contains("abgeben") || msg.contains("arbeit"))) {
                    Calendar c = Calendar.getInstance();
                    c.set(2022, Calendar.SEPTEMBER, 9, 12, 0, 0);
                    long timeUntilMS = c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                    long timeUntilSec = timeUntilMS / 1000;
                    long days = timeUntilSec / 86400;
                    timeUntilSec %= 86400;
                    long hours = timeUntilSec / 3600;
                    timeUntilSec %= 3600;
                    long minutes = timeUntilSec / 60;
                    long seconds = timeUntilSec % 60;
                    String responseMessage = String.format("Noch %d Tage und %02d:%02d:%02d bis zur Abgabe der Bachelorarbeit %s %s",
                            days, hours, minutes, seconds,
                            getServerEmoteAsMention(event.getGuild(), "dhbw_logo"),
                            randomEmote(event.getGuild(), panikEmotes));
                    event.getChannel().sendMessage(responseMessage).queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPauseReminder(MessageReceivedEvent event, String msg, boolean isLunchBreak) {
        if (isPauseActive)
            return;
        Calendar c = Calendar.getInstance();

        int pauseTime, endMinute, endHour;
        if(msg.contains("bis")) {
            int i = msg.indexOf("bis");
            msg = msg.substring(i + 3).trim();
            if(msg.isEmpty())
                return;
            msg = msg.replace(" ", "").replace("\t", "").replace("\n", "");
            String[] spl = msg.split(":");
            if(spl.length < 2)
                return;
            try {
                endHour = Integer.parseInt(spl[0]);
                endMinute = Integer.parseInt(spl[1]);
            } catch (NumberFormatException e) {
                return;
            }
            endHour = endHour % 24;
            endMinute = endMinute % 60;
            int nowHour = c.get(Calendar.HOUR_OF_DAY);
            int nowMinute = c.get(Calendar.MINUTE);
            if(endHour < nowHour || (endHour == nowHour && endMinute < nowMinute))
                return;

            int pauseEndTimestamp = endHour * 60 + endMinute;
            int nowTimestamp = nowHour * 60 + nowMinute;
            pauseTime = pauseEndTimestamp - nowTimestamp;
        } else {
            pauseTime = -1;
            for (String w : msg.split(" ")) {
                int i;
                try {
                    i = Integer.parseInt(w);
                    if (i > 0 && i < 120)
                        pauseTime = i;
                    break;
                } catch (Exception ignored) {
                }
            }
            if (pauseTime <= 0)
                return;
            endMinute = c.get(Calendar.MINUTE) + pauseTime;
            endHour = c.get(Calendar.HOUR_OF_DAY);
        }
        while (endMinute >= 60) {
            endMinute -= 60;
            endHour++;
        }

        isPauseActive = true;
        event.getChannel().sendMessage(String.format("Pause bis %02d:%02d :beer:", endHour, endMinute)).queue();

        long reminderDelay = ((pauseTime * 60L) - c.get(Calendar.SECOND));

        getScheduler().newTask(() -> {
            isPauseActive = false;
            Role nerdRole = event.getGuild().getRoleById(657894994186731520L);
            event.getChannel().sendMessage(nerdRole.getAsMention() + " etzala geht's weiter " + randomEmote(event.getGuild(), panikEmotes)).queue();
            return true;
        }).setName("Pause-Reminder-" + pauseTime).setStartTime(System.currentTimeMillis() + reminderDelay * 1000L).start();

        if(isLunchBreak && reminderDelay > 10L * 60L) {
            getScheduler().newTask(() -> {
                isPauseActive = false;
                event.getChannel().sendMessage(event.getGuild().getRoleById(657894994186731520L).getAsMention() +
                        " in 10 Minuten gehts weiter " + randomEmote(event.getGuild(), panikEmotes)).queue();
                return true;
            }).setName("Lunch-Reminder-" + pauseTime).setStartTime(System.currentTimeMillis() + (reminderDelay - 10L * 60L) * 1000L).start();
        }
    }

    private void sendRandomQuote(MessageReceivedEvent event) {
        MessageChannel channel = event.getGuild().getTextChannelById(715529189486231654L);
        if (channel != null) {
            MessageHistory history = channel.getHistoryFromBeginning(100).complete();
            List<Message> msgList = history.getRetrievedHistory();
            if (!msgList.isEmpty()) {
                Message quote;
                int attempts = 0;
                do {
                    quote = msgList.get(random.nextInt(msgList.size()));
                } while (quote.getContentRaw().isEmpty() && (++attempts) < 100);
                event.getChannel().sendMessage(quote).queue();
            }
        }
    }

    private void startCountdown(MessageReceivedEvent event) {
        final Lecture currentLecture = Lecture.getCurrentLecture(event.getGuild());
        if (currentLecture != null) {
            TextChannel channel = event.getTextChannel();
            long channelId = channel.getIdLong();
            boolean countdownExists;
            synchronized (activeCountdowns) {
                countdownExists = activeCountdowns.containsKey(channelId);
            }
            if (!countdownExists) {
                Countdown countdown = new Countdown(currentLecture, channel);
                countdown.setOnCountdownEnd(() -> {
                    synchronized (activeCountdowns) {
                        activeCountdowns.remove(channelId);
                    }
                });
                synchronized (activeCountdowns) {
                    activeCountdowns.put(channelId, countdown);
                }
                countdown.createThread().start();
            }
        } else {
            event.getChannel().sendMessage("Gerade läuft keine Vorlesung").queue();
        }
    }

    private void sendRemainingTime(MessageReceivedEvent event) {
        Calendar c = Calendar.getInstance();
        int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        Lecture lecture = Lecture.getCurrentLecture(event.getGuild());
        if (lecture != null) {
            int remainingSecnds = (lecture.endTime - dayAgeMinutes) * 60 - c.get(Calendar.SECOND);
            log(event, "Sending remaining time of " + lecture.name);
            int remainingMinutes = remainingSecnds / 60;
            remainingSecnds %= 60;
            int rem = remainingMinutes;
            int remainingHours = remainingMinutes / 60;
            remainingMinutes %= 60;
            event.getChannel().sendMessage(
                    String.format("%s dauert noch %02d:%02d:%02d %s",
                            lecture.name, remainingHours, remainingMinutes, remainingSecnds,
                            rem > 30 ? randomEmote(event.getGuild(), panikEmotes) : rem < 5 ? randomEmote(event.getGuild(), happyEmotes) : ""
                    )).queue();
        } else {
            event.getChannel().sendMessage("Gerade läuft keine Vorlesung").queue();
        }
    }

    public static String randomEmote(Guild guild, String[] emotes) {
        String emoteName = emotes[random.nextInt(emotes.length)];
        return getServerEmoteAsMention(guild, emoteName);
    }

    public static String getServerEmoteAsMention(Guild guild, String emoteName) {
        Emote emote = getServerEmote(guild, emoteName);
        if (emote == null)
            return "";
        return emote.getAsMention();
    }

    public static Emote getServerEmote(Guild guild, String emoteName) {
        List<Emote> result = guild.getEmotesByName(emoteName, true);
        if (result.isEmpty())
            return null;
        return result.get(0);
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

//                else if (msg.contains("rainer") || msg.contains("winkler")) {
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

}
