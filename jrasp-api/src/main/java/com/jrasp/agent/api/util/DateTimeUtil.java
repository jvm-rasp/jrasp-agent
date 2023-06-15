package com.jrasp.agent.api.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String timestamp2DateTime(long timestamp) {
        Instant instant;
        if (String.valueOf(timestamp).length() == 13) {
            instant = Instant.ofEpochMilli(timestamp);
        } else {
            instant = Instant.ofEpochSecond(timestamp);
        }
        return formatter.format(LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(8)));
    }
}
