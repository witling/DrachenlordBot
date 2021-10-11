package de.eliaspr.drache;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RaplaParser {

    public static final String CALENDAR_URL = "https://rapla.dhbw.de/rapla/calendar?key=25q8zGuMAw3elezlMsiegXs3Z-sCY45qHbigy7wiQ2e27FEEw1gUZrt95IawaK3jxZy_Y5bukYcuFWfh6SXaWY7MSSM5PUNOz287D3zip86_F6eY1VUpkgPRQ8l5aezCD3g6LoEwPOfJ2YoaMHf7UxtQsvlQz6A4gnJeFNckmgjSRYFGmbc_wwnrPF3FRyYS&salt=-2070726140&day=6&month=10&year=2021&goto=Datum+anzeigen&pages=12";

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
        Date now = Calendar.getInstance().getTime();
        System.out.println("Parsing calendar");
        try {
            Document doc = Jsoup.connect(CALENDAR_URL).get();
            Elements tooltips = doc.select(".tooltip");
            for (Element tooltip : tooltips) {
                Date startDate = null, endDate = null;
                String type = null, name = null, examType = null;

                for (Element ch : tooltip.children()) {
                    if (ch.tagName().equalsIgnoreCase("div") &&
                            !ch.hasAttr("style")) {
                        String dateInfo = ch.text();
                        String[] el = dateInfo.split(" ");
                        String[] dayMonthYear = el[1].split("\\.");
                        String[] times = el[2].split("-");
                        startDate = convertToDate(dayMonthYear, times[0]);
                        endDate = convertToDate(dayMonthYear, times[1]);
                    } else if (ch.tagName().equalsIgnoreCase("strong")) {
                        type = ch.text();
                    } else if (ch.tagName().equalsIgnoreCase("table") && ch.hasClass("infotable")) {
                        for (Element tr : ch.getElementsByTag("tr")) {
                            String label = tr.select(".label").get(0).ownText().trim();
                            String value = tr.select(".value").get(0).ownText().trim();
                            if (label.equals("Veranstaltungsname:") || label.equals("Name:")) {
                                name = value;
                            } else if (label.equals("Prüfungsart:") && value.length() > 0) {
                                examType = value;
                            }
                        }
                    }
                }

                if (name != null && examType != null)
                    name = examType + " " + name;

                Element weekBlock = tooltip.parent();
                if (weekBlock == null || (weekBlock = weekBlock.parent()) == null)
                    continue;

                String[] lecturers = weekBlock.select(".person").stream().map(Element::ownText).toArray(String[]::new);
                String[] locations = weekBlock.select(".resource").stream().map(Element::ownText).filter(str -> !str.contains("MGH-TINF")).toArray(String[]::new);

                if (startDate != null && endDate != null && name != null) {
                    if (onlyFutureEvents && startDate.before(now))
                        continue;
                    entries.add(new CalendarEntry(startDate, endDate, type, name, lecturers, locations));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to parse Rapla calendar");
        }
        return entries;
    }

    private static Date convertToDate(String[] dayMonthYear, String time) {
        String[] timeEl = time.split(":");
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        int year = Integer.parseInt(dayMonthYear[2]);
        if (year < 100)
            year += 2000;
        //noinspection MagicConstant
        c.set(year,
                Integer.parseInt(dayMonthYear[1]) - 1,
                Integer.parseInt(dayMonthYear[0]),
                Integer.parseInt(timeEl[0]),
                Integer.parseInt(timeEl[1]),
                0);
        return c.getTime();
    }

    public static class CalendarEntry {

        public final Date startDate, endDate;
        public final String type, name;
        public final String[] locations, lecturers;

        public CalendarEntry(Date startDate, Date endDate, String type, String name, String[] lecturers, String[] locations) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.type = type;
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
                    ", type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", lecturers='" + Arrays.toString(lecturers) + '\'' +
                    ", location='" + Arrays.toString(locations) + '\'' +
                    '}';
        }

    }

}
