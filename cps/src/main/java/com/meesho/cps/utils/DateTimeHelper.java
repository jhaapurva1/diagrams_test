package com.meesho.cps.utils;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * @author shubham.aggarwal
 * 29/11/21
 */
public class DateTimeHelper {

    public static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String MONGO_DATE_FORMAT = "yyyy-MM-dd";

}
