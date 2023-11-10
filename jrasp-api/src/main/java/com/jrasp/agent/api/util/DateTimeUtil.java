package com.jrasp.agent.api.util;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtil {

    public static String timestamp2DateTime(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        Date date;
        if (String.valueOf(timestamp).length() == 13) {
            date = new Date(timestamp);
        } else {
            date = new Date(timestamp * 1000);
        }
        return formatter.format(date);
    }
}
