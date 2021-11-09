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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Drache extends ListenerAdapter {

    private static final Scheduler scheduler = new Scheduler();

    public static Scheduler getScheduler() {
        return scheduler;
    }

    private static final Random random = new Random();

    public static Random getRandom() {
        return random;
    }

    public static final HashMap<String, String> personEmotes = new HashMap<>();
    public static final String[] panikEmotes = {"pepeMinigun", "pepeShotgun", "pepeSteckdose", "pepeHands", "pepeGalgen", "panik", "noose"};
    public static final String[] happyEmotes = {"pepega", "yes", "pogChamp", "pog", "uzbl"};
    public static final String[] alcoholEmotes = {"vodka", "jaegermeister", "bier"};
    private static final HashMap<Long, Countdown> activeCountdowns = new HashMap<>();
    private static boolean isPauseActive = false;
    private static long bachelorCooldown = 0;

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

        personEmotes.put("müller", "carstenPilot");
        personEmotes.put("zang", "christina");
        personEmotes.put("becker", "marvin");
        personEmotes.put("kim", "kim");
        personEmotes.put("steur", "niko");
        personEmotes.put("graupner", "georg");
        personEmotes.put("höfling", "juergen");

        String apiKey = new BufferedReader(new FileReader("apikey.txt")).readLine();
        JDABuilder builder = JDABuilder.createDefault(apiKey);
        builder.setActivity(Activity.playing("sich am Speer"));
        builder.setCompression(Compression.NONE);

        Drache drache = new Drache();
        builder.addEventListeners(drache);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        JDA jda = builder.build();
        jda.awaitReady();

        drache.startTimetableSender(jda);
    }

    private void startTimetableSender(JDA jda) {
        Timer timer = new Timer("DailyTimetable");
        long now = Calendar.getInstance().getTimeInMillis();
        Calendar firstStart = Calendar.getInstance();
        firstStart.set(Calendar.HOUR_OF_DAY, 16);
        firstStart.set(Calendar.MINUTE, 0);
        firstStart.set(Calendar.SECOND, 0);
        long delay = firstStart.getTimeInMillis() - now;
        if (delay < 0L)
            delay += 1000L * 60L * 60L * 24L;
        System.out.printf("Starting timetable sender in %,d ms\n", delay);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Performing daily timetable sending");
                showCalendar(jda.getTextChannelById(706825779664912385L), jda.getGuildById(657602012179070988L), true, null);
            }
        }, delay, 1000L * 60L * 60L * 24L);
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
                if (isNerdServer && msg.contains("\u1794") || msg.contains("\u1796") || msg.contains("\uD83C\uDDFA\uD83C\uDDF8")) {
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
                        } else if (msg.contains("wi") && msg.contains("viel") && (msg.contains("termin") || msg.contains("vorlesung")) && (msg.contains("präsenz") || msg.contains("praesenz") || (msg.contains("vor") && msg.contains("ort")))) {
                            showNumberOfRemainingDates(event.getChannel(), event.getGuild());
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
                        } else if ((msg.contains("nächst") || msg.contains("nachst") || msg.contains("naechst")) && (msg.contains("vorlesung") || msg.contains("klausur") || msg.contains("veranstaltung"))) {
                            RaplaParser.CalendarEntry nextEvent = RaplaParser.getNextEvent();
                            if (nextEvent == null) {
                                event.getChannel().sendMessage("Es stehen keine Vorlesungen an " + getServerEmoteAsMention(event.getGuild(), "croco")).queue();
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Nächste Vorlesung ").append(getServerEmoteAsMention(event.getGuild(), "dhbw_logo")).append("\n");
                                sb.append("**").append(nextEvent.startDay()).append(" | ").append(nextEvent.startTime()).append(" - ").append(nextEvent.endTime()).append("**\n");
                                sb.append("**").append(nextEvent.name).append("**\n");
                                // if (nextEvent.type != null) sb.append("*").append(nextEvent.type).append("*\n");

                                boolean showSeparator = nextEvent.lecturers != null && nextEvent.locations != null;
                                if (nextEvent.lecturers != null)
                                    formatEventLecturers(sb, event.getGuild(), nextEvent.lecturers);
                                if (showSeparator) sb.append(" | ");
                                if (nextEvent.locations != null) sb.append(String.join(", ", nextEvent.locations));

                                event.getChannel().sendMessage(sb.toString()).queue();
                            }
                        } else if (msg.contains("stundenplan") || msg.contains("vorlesung")) {
                            if (msg.contains("morgen")) {
                                showCalendar(event.getChannel(), event.getGuild(), false, null);
                            } else {
                                for (String e : msg.split(" ")) {
                                    if (!e.contains("."))
                                        continue;
                                    int n = 0;
                                    for (char c : e.toCharArray())
                                        if (c == '.')
                                            n++;
                                    if (n >= 1) {
                                        showCalendar(event.getChannel(), event.getGuild(), false, e);
                                        break;
                                    }
                                }
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
                    boolean checkCooldown = msg.contains("abgabe") || msg.contains("abgeben") || msg.contains("arbeit");
                    if (checkCooldown) {
                        long now = System.currentTimeMillis();
                        if (now < bachelorCooldown)
                            return;
                        bachelorCooldown = now + 45 * 60 * 1000;
                    }

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
                } else if (msg.contains("audi") || msg.contains("skrrr")) {
                    event.getChannel().sendMessage("https://www.youtube.com/watch?v=E0SN614t2WI").queue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNumberOfRemainingDates(MessageChannel channel, Guild guild) {
        List<RaplaParser.CalendarEntry> entries = RaplaParser.parseRaplaCalendar(true);
        entries.sort(Comparator.comparing(entry -> entry.startDate));
        int n = 0;
        int lastDay = -1;
        DateFormat df = new SimpleDateFormat("dd");
        for (RaplaParser.CalendarEntry e : entries) {
            if (e.locations != null) {
                if (Arrays.stream(e.locations).noneMatch(l -> l.contains("Lehrsaal")))
                    continue;
                int day = Integer.parseInt(df.format(e.startDate));
                if (day == lastDay)
                    continue;
                lastDay = day;
                System.out.println(e);
                n++;
            }
        }
        if (n == 0)
            channel.sendMessage("Es sind aktuell keine Präsenz-Vorlesungen/Klausuren geplant " + randomEmote(guild, happyEmotes)).queue();
        else
            channel.sendMessage("Es sind noch " + n + " Präsenz-Vorlesungen/Klausuren " + randomEmote(guild, panikEmotes)).queue();
    }

    private void showCalendar(MessageChannel channel, Guild guild, boolean skipIfEmpty, String selectedDate) {
        int day = 0, month = 0, year = 0;
        List<RaplaParser.CalendarEntry> entries;
        if (selectedDate == null)
            entries = RaplaParser.getEntriesForTomorrow();
        else {
            try {
                String[] spl = selectedDate.split("\\.");
                day = Integer.parseInt(spl[0]);
                month = Integer.parseInt(spl[1]) - 1;
                year = spl.length >= 3 ? Integer.parseInt(spl[2]) : Calendar.getInstance().get(Calendar.YEAR);
                if (day < 0 || day > 31 || month < 0 || month >= 12)
                    return;
                if (year < 100)
                    year += 2000;
                entries = RaplaParser.getEntriesForDate(year, month, day);
            } catch (NumberFormatException e) {
                return;
            }
        }

        if (entries.isEmpty() && skipIfEmpty)
            return;
        StringBuilder sb = new StringBuilder();

        if (selectedDate == null) {
            Calendar today = Calendar.getInstance();
            today.add(Calendar.HOUR, 24);
            sb.append("**Stundenplan für morgen, ").append(RaplaParser.CalendarEntry.dateFormatDayOnly.format(today.getTime())).append(" :clipboard:**\n\n");
        } else {
            sb.append("**Stundenplan für ").append(String.format("%02d.%02d.%04d", day, month + 1, year)).append(" :clipboard:**\n\n");
        }

        if (entries.isEmpty()) {
            if (selectedDate == null)
                sb.append("*Morgen stehen keine Termine an*");
            else
                sb.append("*Am ").append(String.format("%02d.%02d.%04d", day, month + 1, year)).append(" stehen keine Termine an*");
        } else {
            for (RaplaParser.CalendarEntry event : entries) {
                sb.append(event.startTime()).append(" - ").append(event.endTime());
                sb.append(" | ").append(event.name);
                if (event.locations != null || event.lecturers != null) {
                    sb.append("\n");
                    boolean both = event.locations != null && event.lecturers != null;
                    if (event.lecturers != null) formatEventLecturers(sb, guild, event.lecturers);
                    if (both) sb.append(" | ");
                    if (event.locations != null) sb.append(String.join(", ", event.locations));
                }
                sb.append("\n\n");
            }
        }

        channel.sendMessage(sb.toString()).queue();
    }

    private void formatEventLecturers(StringBuilder sb, Guild guild, String[] lecturers) {
        for (int i = 0; i < lecturers.length; i++) {
            String emote = personEmotes.get(lecturers[i].toLowerCase().trim());
            if (emote == null)
                sb.append(lecturers[i]).append(" ").append(getServerEmoteAsMention(guild, "jensFaehler"));
            else
                sb.append(getServerEmoteAsMention(guild, emote));

            if (i < lecturers.length - 1)
                sb.append(", ");
        }
    }

    private void createPauseReminder(MessageReceivedEvent event, String msg, boolean isLunchBreak) {
        if (isPauseActive)
            return;
        Calendar c = Calendar.getInstance();

        int pauseTime, endMinute, endHour;
        if (msg.contains("bis")) {
            int i = msg.indexOf("bis");
            msg = msg.substring(i + 3).trim();
            if (msg.isEmpty())
                return;
            msg = msg.replace(" ", "").replace("\t", "").replace("\n", "");
            String[] spl = msg.split(":");
            if (spl.length < 2)
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
            if (endHour < nowHour || (endHour == nowHour && endMinute < nowMinute))
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

        if (isLunchBreak && reminderDelay > 10L * 60L) {
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
        RaplaParser.CalendarEntry currentLecture = RaplaParser.getCurrentEvent();
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
        RaplaParser.CalendarEntry lecture = RaplaParser.getCurrentEvent();
        if (lecture != null) {
            int remainingSecnds = (lecture.endTimeMinutes() - dayAgeMinutes) * 60 - c.get(Calendar.SECOND);
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
