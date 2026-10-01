package com.ardencm.custody.gateway;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Custodian-side view of tag 64. Three of our four custodians reject anything that is not YYYYMMDD. */
public final class FixSettlDateParser {
    private static final DateTimeFormatter LOCAL_MKT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FixSettlDateParser() {}

    public static LocalDate parse(String tag64) {
        if (tag64 == null || tag64.length() != 8) {
            throw new IllegalArgumentException("tag 64 must be LocalMktDate (YYYYMMDD): " + tag64);
        }
        return LocalDate.parse(tag64, LOCAL_MKT_DATE);
    }
}
