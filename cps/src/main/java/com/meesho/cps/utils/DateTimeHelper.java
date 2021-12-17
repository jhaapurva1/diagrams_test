package com.meesho.cps.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * @author shubham.aggarwal
 * 29/11/21
 */
public class DateTimeHelper {

    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final SimpleDateFormat hbaseDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static String getDateInYYYYMMDDFormat(String date) throws Exception {
        return hbaseDateFormat.format(hbaseDateFormat.parse(date));
    }

}
