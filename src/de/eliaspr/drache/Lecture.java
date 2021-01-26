package de.eliaspr.drache;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Calendar;

public class Lecture {

    public final int startTime, endTime;
    public final String name;

    public Lecture(int startTime, int endTime, String name) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.name = name;
    }

    public static Lecture getCurrentLecture(Guild guild) {
        Calendar c = Calendar.getInstance();
        int dayAgeMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        for (Lecture lecture : getAllToday(guild)) {
            if (lecture.startTime < dayAgeMinutes && lecture.endTime > dayAgeMinutes) {
                return lecture;
            }
        }
        return null;
    }

    public static ArrayList<Lecture> getAllToday(Guild guild) {
        Calendar c = Calendar.getInstance();
        ArrayList<Lecture> list = new ArrayList<>();
        for (TextChannel channel : guild.getTextChannelsByName("ankündigungen", true)) {
            for (Message message : channel.getHistoryAround(channel.getLatestMessageId(), 100).complete().getRetrievedHistory()) {
                String textMsg = message.getContentRaw();
                if (textMsg.contains("Stundenplan für morgen")) {
                    String date = textMsg.substring(0, 8);
                    if (date.equals(String.format("%02d.%02d.%02d", c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR) % 100))) {
                        String[] msgLines = textMsg.split("\n");
                        for (String line : msgLines) {
                            line = line.trim();
                            if (line.startsWith("- ")) {
                                line = line.substring(2);
                                int uhrInx;
                                String time = line.substring(0, uhrInx = line.indexOf(" Uhr"));
                                String lectureName = line.substring(line.indexOf(':', uhrInx) + 2);
                                String[] timeStamps = time.split("-");
                                list.add(new Lecture(parseTimeStamp(timeStamps[0]), parseTimeStamp(timeStamps[1]), lectureName));
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    private static int parseTimeStamp(String timeStamp) {
        String[] spl = timeStamp.split(":");
        int hours = Integer.parseInt(spl[0]);
        int minutes = Integer.parseInt(spl[1]);
        return 60 * hours + minutes;
    }

}
