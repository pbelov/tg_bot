package com.psbelov.java.tg.bot.Utils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Paths;

public class FileUtils {
    public static final File WORKING_DIR = new File(Paths.get("").toAbsolutePath().toString());
    private static final String TAG = "FileUtils";
    private static final Charset[] charsets = {Charset.forName("UTF-8"), Charset.forName("ISO-8859-1")};

    private FileUtils() {}

    static void appendStringToFile(String string, File outputFile) {
        writeStringToFile(string, outputFile, true);
    }

    static void writeStringToFile(String string, File outputFile) {
        writeStringToFile(string, outputFile, false);
    }

    private static void writeStringToFile(String string, File outputFile, boolean append) {
        BufferedWriter out = null;
        Exception exception = null;

        //try to write with each supported charset until we find one that works
        for (Charset charset : charsets) {
            try {
                CharsetEncoder encoder = charset.newEncoder();
                encoder.onMalformedInput(CodingErrorAction.REPORT);
                encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, append), encoder));
                out.append(string);
                return;
            } catch (Exception e) {
                exception = e;
            } finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (!append) {
                        System.out.println("File " + outputFile.getName() + " has written");
                    }
                } catch (IOException e) {
                    StringUtils.handleException(TAG, e);
                }
            }
        }

        //if no charsets worked, print exception and give up
        Utils.println(TAG, "PRINTING FAILED! " + exception);
    }
}
