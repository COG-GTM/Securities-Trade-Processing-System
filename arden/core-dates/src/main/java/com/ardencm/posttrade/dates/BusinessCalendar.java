package com.ardencm.posttrade.dates;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/** Holiday calendar for a market (ISO 10383 MIC) or payment system (e.g. TARGET2). */
public final class BusinessCalendar {

    private static final Map<String, BusinessCalendar> CACHE = new HashMap<>();

    private final String code;
    private final Set<LocalDate> holidays;

    private BusinessCalendar(String code, Set<LocalDate> holidays) {
        this.code = code;
        this.holidays = Collections.unmodifiableSet(holidays);
    }

    public static synchronized BusinessCalendar of(String code) {
        return CACHE.computeIfAbsent(code, BusinessCalendar::load);
    }

    @SuppressWarnings("unchecked")
    private static BusinessCalendar load(String code) {
        try (InputStream in = BusinessCalendar.class.getResourceAsStream("/holidays.yaml")) {
            if (in == null) {
                throw new IllegalStateException("holidays.yaml missing from classpath");
            }
            Map<String, Object> root = new Yaml().load(in);
            Map<String, List<String>> calendars = (Map<String, List<String>>) root.get("calendars");
            List<String> dates = calendars.get(code);
            if (dates == null) {
                throw new IllegalArgumentException("Unknown calendar: " + code);
            }
            Set<LocalDate> parsed = new HashSet<>();
            for (String d : dates) {
                parsed.add(LocalDate.parse(d));
            }
            return new BusinessCalendar(code, parsed);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read holidays.yaml", e);
        }
    }

    public String code() {
        return code;
    }

    public boolean isBusinessDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

    public LocalDate addBusinessDays(LocalDate from, int days) {
        LocalDate d = from;
        int remaining = Math.abs(days);
        int step = days >= 0 ? 1 : -1;
        while (remaining > 0) {
            d = d.plusDays(step);
            if (isBusinessDay(d)) {
                remaining--;
            }
        }
        return d;
    }

    public LocalDate nextBusinessDay(LocalDate from) {
        return addBusinessDays(from, 1);
    }
}
