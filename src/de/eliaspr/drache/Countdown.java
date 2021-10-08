package de.eliaspr.drache;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Calendar;

public class Countdown implements Runnable {

    private final RaplaParser.CalendarEntry lecture;
    private final TextChannel channel;
    private boolean shouldStop = false;
    private Runnable onCountdownEnd = () -> {
    };
    private long nextUpdateTime = 0;
    private int lastFinalCountdownBroadcast = -1;

    public Countdown(RaplaParser.CalendarEntry lecture, TextChannel channel) {
        this.lecture = lecture;
        this.channel = channel;
    }

    public synchronized void setOnCountdownEnd(Runnable onCountdownEnd) {
        this.onCountdownEnd = onCountdownEnd;
    }

    public synchronized void stop() {
        shouldStop = true;
    }

    public Thread createThread() {
        Thread th = new Thread(this);
        th.setDaemon(true);
        return th;
    }

    @Override
    public void run() {
        String oldTopic = channel.getTopic();
        channel.sendMessage("Countdown aktiv :thumbsup:").queue();

        Drache.getScheduler().newTask(() -> {
            Calendar c = Calendar.getInstance();
            int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            int remainingSeconds = (lecture.endTimeMinutes() - dayAgeMinutes) * 60 - c.get(Calendar.SECOND);
            if (remainingSeconds <= 0) {
                channel.getManager().setTopic(oldTopic).queue();
                channel.sendMessage(lecture.name + " ist zu Ende! :beer:").queue(message -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    long messageID = message.getIdLong();
                    for(String emoteName : Drache.alcoholEmotes) {
                        Emote emote = Drache.getServerEmote(channel.getGuild(), emoteName);
                        if(emote != null)
                            channel.addReactionById(messageID, emote).queue();
                    }
                });

                return true;
            }

            if (remainingSeconds > 5 * 60) {
                int remainingMinutes = remainingSeconds / 60;
                long now = System.currentTimeMillis();
                if (now > nextUpdateTime) {
                    int remainingHours = remainingMinutes / 60;
                    remainingMinutes %= 60;
                    String topic = String.format("%s ist in %02d:%02d zu Ende", lecture.name, remainingHours, remainingMinutes);
                    System.out.println(topic);
                    channel.getManager().setTopic(topic).queue();
                    nextUpdateTime = now + 5 * 60 * 1000;
                }
            } else if (remainingSeconds <= 10) {
                if (lastFinalCountdownBroadcast != remainingSeconds) {
                    lastFinalCountdownBroadcast = remainingSeconds;
                    channel.sendMessage(String.valueOf(remainingSeconds)).queue();
                }
            }

            synchronized (this) {
                if (shouldStop)
                    return true;
            }

            return false;
        }).setName("Countdown_" + lecture.name.replace(' ', '-')).after(() -> {
            channel.getManager().setTopic(oldTopic).queue();
            synchronized (this) {
                onCountdownEnd.run();
            }
        }).setRepeatTime(1).start();
    }
}
