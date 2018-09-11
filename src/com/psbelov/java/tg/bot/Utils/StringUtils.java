package com.psbelov.java.tg.bot.Utils;


import com.psbelov.java.tg.bot.Main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private StringUtils() {}

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date now = new Date();

        return sdfDate.format(now);
    }

    static void handleException(String TAG, Exception e) {
        if (Main.DEBUG) {
            e.printStackTrace();
        } else {
            Utils.error(TAG, "Error: " + e.getMessage());
        }

        FileUtils.writeStringToFile(convertExceptionToString(e), new File("stacktrace.log"));
    }

    private static String convertExceptionToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e).append(" at:\n");
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            sb.append(stackTraceElement).append("\n");
        }

        return sb.toString();
    }

    public static String getNextParamValue(String[] args, int index) {
        StringBuilder value = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                value.append(args[i]).append(" ");
            } else {
                break;
            }
        }

        return value.toString().trim();
    }

    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    public static List<String> extractUrls(String text) {
        text = text.replaceAll("https://", "http://");

        List<String> containedUrls = new ArrayList<>();
        String urlRegex = "((https?|ftp):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }

        return containedUrls;
    }
}
