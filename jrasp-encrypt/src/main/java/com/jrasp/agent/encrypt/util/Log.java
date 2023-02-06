package com.jrasp.agent.encrypt.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    public static void debug(Object msg) {
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String log = datetimeFormat.format(new Date()) + " [DEBUG] " + msg;
        System.out.println(log);
    }
}
