package de.eliaspr.drache;

import de.eliaspr.json.JSONObject;
import de.eliaspr.json.JSONParser;
import de.eliaspr.json.JSONValue;
import de.eliaspr.json.JSONValueType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RaplaParser {

    public static final String CALENDAR_URL = "https://api.stuv.app/rapla/lectures/MGH-TINF19";

    public static CalendarEntry getNextEvent() {
        List<CalendarEntry> list = parseRaplaCalendar(true);
        return list.isEmpty() ? null : list.get(0);
    }

    public static CalendarEntry getCurrentEvent() {
        List<CalendarEntry> list = parseRaplaCalendar(false);
        Date now = Calendar.getInstance().getTime();
        for (CalendarEntry ce : list) {
            if (ce.startDate.before(now) && ce.endDate.after(now))
                return ce;
        }
        return null;
    }

    public static List<CalendarEntry> getEntriesForTomorrow() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        return getEntriesForDay(calendar);
    }

    public static List<CalendarEntry> getEntriesForDate(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, 12, 0, 0);
        return getEntriesForDay(calendar);
    }

    private static List<CalendarEntry> getEntriesForDay(Calendar day) {
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        Date timeRangeStart = day.getTime();
        day.add(Calendar.HOUR, 24);
        Date timeRangeEnd = day.getTime();

        List<CalendarEntry> list = parseRaplaCalendar(true);
        return list.stream().filter(entry -> entry.startDate.after(timeRangeStart) && entry.startDate.before(timeRangeEnd)).collect(Collectors.toList());
    }

    public static List<CalendarEntry> parseRaplaCalendar(boolean onlyFutureEvents) {
        List<CalendarEntry> entries = new ArrayList<>();

        try {
            URL url = new URL(CALENDAR_URL + (onlyFutureEvents ? "?archived=false" : "?archived=true"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            if (status == 200) {
                JSONParser parser = new JSONParser();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                JSONValue json = parser.parseJSON(content.toString(), true);
                DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                json.getArray().forEach().action(entry -> {
                    JSONObject obj = entry.getObject();
                    try {
                        entries.add(new CalendarEntry(
                                df1.parse(obj.getString("startTime")),
                                df1.parse(obj.getString("endTime")),
                                obj.getString("name"),
                                new String[]{obj.getString("lecturer")},
                                obj.hasValue("rooms", JSONValueType.ARRAY) ? obj.getArray("rooms").stream().map(JSONValue::toString).toArray(String[]::new) : new String[]{}
                        ));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }).run();
            } else {
                throw new Exception("HTTP status " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to fetch/parse calender from StuV");
        }

        return entries;
    }


    public static class CalendarEntry {

        public final Date startDate, endDate;
        public final String name;
        public final String[] locations, lecturers;

        public CalendarEntry(Date startDate, Date endDate, String name, String[] lecturers, String[] locations) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.name = name;
            this.lecturers = lecturers != null && lecturers.length > 0 ? lecturers : null;
            this.locations = locations != null && locations.length > 0 ? locations : null;
        }

        public static final DateFormat dateFormatFull = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        public static final DateFormat dateFormatDayOnly = new SimpleDateFormat("dd.MM.yyyy");
        public static final DateFormat dateFormatTimeOnly = new SimpleDateFormat("HH:mm");

        public String startDay() {
            return dateFormatDayOnly.format(startDate);
        }

        public String endDay() {
            return dateFormatDayOnly.format(endDate);
        }

        public String startTime() {
            return dateFormatTimeOnly.format(startDate);
        }

        public String endTime() {
            return dateFormatTimeOnly.format(endDate);
        }

        public int startTimeMinutes() {
            return startDate.getHours() * 60 + startDate.getMinutes();
        }

        public int endTimeMinutes() {
            return endDate.getHours() * 60 + endDate.getMinutes();
        }

        @Override
        public String toString() {
            return "CalendarEntry{" + "startDate=" + dateFormatFull.format(startDate) +
                    ", endDate=" + dateFormatFull.format(startDate) +
                    ", name='" + name + '\'' +
                    ", lecturers='" + Arrays.toString(lecturers) + '\'' +
                    ", location='" + Arrays.toString(locations) + '\'' +
                    '}';
        }

    }

}
