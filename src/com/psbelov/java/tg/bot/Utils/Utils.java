package com.psbelov.java.tg.bot.Utils;

import com.psbelov.java.tg.bot.Main;
import com.psbelov.java.tg.bot.UserMessages;

import java.io.File;
import java.util.*;

import static com.psbelov.java.tg.bot.Utils.FileUtils.WORKING_DIR;

public class Utils {
    private static File logFile = new File("log.txt");
    public static final String LOGDIR = "logs";
    private static boolean logFileDeleted = false;

    private Utils() {}

    private static void checkFile(File file) {
        if (file == null) {
            return;
        } else {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
        }
    }

    private static void checkLogFile() {
        if (!logFileDeleted) {
            if (!logFile.exists() || logFile.delete()) {
                logFileDeleted = true;
            } else {
                System.out.println("Unable to delete log file " + logFile.getName());
                System.exit(-1);
            }
        }
    }

    public static String fixFileName(String filename) {
        return filename.replaceAll(":", "");
    }

    public static void println(String tag, String str, String filename) {
        filename = fixFileName(filename);
        checkFile(new File(WORKING_DIR + File.separator + LOGDIR, filename));
        System.out.println(Main.DEBUG ? tag + ": " + str : str);
        FileUtils.appendStringToFile(str + "\r\n", new File(LOGDIR, filename + ".log"));
    }

    public static void println(String tag, String str) {
        checkLogFile();

        //noinspection ConstantConditions
        System.out.println(Main.DEBUG ? tag + ": " + str : str);
        FileUtils.appendStringToFile(str + "\r\n", logFile);
    }

    static void error(String tag, String str) {
        checkLogFile();

        if (Main.DEBUG) {
            System.err.println(tag + ": " + str);
        } else {
            System.err.println(str);
        }
        FileUtils.appendStringToFile("ERROR: " + str + "\r\n", logFile);
    }

    public static void addToArrayList(final List<String> list, String value) {
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    public static List<UserMessages> convertToListUserMessages(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        List<UserMessages> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            list.add(new UserMessages(entry.getKey(), entry.getValue()));
        }

        list.sort((o1, o2) -> o2.messagesCount - o1.messagesCount);

        return list;
    }

    public static void incrementNumberInMap(Map<String, Integer> map, String word) {
        if (map.containsKey(word)) {
            int n = map.get(word);
            map.put(word, ++n);
        } else {
            map.put(word, 1);
        }
    }
}
