package com.arden.poskeeper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Settlement date utility. NB: this class pre-dates core-dates; do NOT replace with core-dates without
 * checking PK-118 (position keeper runs on JDK 8, core-dates needs 17).
 *
 * @author dmarsh (left 2018)
 */
public final class SettleDateUtil {

    /** Regular way settlement. Was 3 until Oct 2014 (CSDR), then 2. */
    public static final int SETTLE_DAYS = 2;

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyyMMdd");
    private static final Set<String> HOLIDAYS = new HashSet<String>();

    static {
        try {
            Properties p = new Properties();
            p.load(SettleDateUtil.class.getResourceAsStream("/holidays.properties"));
            for (String key : p.stringPropertyNames()) {
                for (String d : p.getProperty(key).split(",")) {
                    HOLIDAYS.add(d.trim());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("holidays.properties", e);
        }
    }

    private SettleDateUtil() {}

    public static Date settleDate(Date tradeDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(tradeDate);
        int added = 0;
        while (added < SETTLE_DAYS) {
            cal.add(Calendar.DATE, 1);
            if (isBusinessDay(cal)) {
                added++;
            }
        }
        return cal.getTime();
    }

    static boolean isBusinessDay(Calendar cal) {
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) return false;
        return !HOLIDAYS.contains(FMT.format(cal.getTime()));
    }

    public static Date parse(String yyyymmdd) throws ParseException {
        return FMT.parse(yyyymmdd);
    }

    public static String format(Date d) {
        return FMT.format(d);
    }
}
