package com.psbelov.java.tg.bot;

import com.psbelov.java.tg.bot.Utils.TgMsgUtil;
import com.psbelov.java.tg.bot.Utils.Utils;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.psbelov.java.tg.bot.Utils.FileUtils.WORKING_DIR;
import static com.psbelov.java.tg.bot.Utils.Utils.LOGDIR;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "StatementWithEmptyBody"})
class CommandsHelper extends BaseEventsHelper {
    private static final String TAG = "MessageHelper";

    private CommandsHelper() {
    }

    static String getCommandsData(CommandMessageReceivedEvent event) {
        // command params
        String[] args = event.getArgs();
        argsString = event.getArgsString();
        String command = event.getCommand();

        Utils.println(TAG, "handleCommand: args = " + Arrays.toString(args) + ", argsString = " + argsString + ", command = " + command);

        return command;
    }

    static void handleMeCommand(CommandMessageReceivedEvent event) {
        event.getMessage().getBotInstance().deleteMessage(event.getMessage());
        SendableTextMessage sendableMessage = SendableTextMessage.builder()
                .message(senderUserName.replaceFirst("@", "") + " *" + argsString + "*")
                .parseMode(ParseMode.MARKDOWN)
                .build();
        event.getMessage().getBotInstance().sendMessage(event.getChat(), sendableMessage);
    }

    static void handleTopCommand(CommandMessageReceivedEvent event) {
        TgMsgUtil.sendToChat(event, CommandsHelper.getTop3ForTodayText(chatName));
    }

    static void handleStatsCommand(CommandMessageReceivedEvent event) {
        if (argsString.length() == 0) {
            TgMsgUtil.sendToChat(event, CommandsHelper.getStats(chatName, 3));
        } else {
            TgMsgUtil.sendToChat(event, CommandsHelper.getStats(chatName, argsString.trim()));
        }
    }

    static void handleTimeCommand(CommandMessageReceivedEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag(RU_TAG));
        TgMsgUtil.replyInChat(event, format.format(new Date()));
    }

    static void handleDateCommand(CommandMessageReceivedEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
        TgMsgUtil.replyInChat(event, format.format(new Date()));
    }

    private static String getValue(JSONObject valutes, String code) {
        JSONObject usdJson = valutes.getJSONObject(code);
        double value = usdJson.getDouble("Value");
        double prevValue = usdJson.getDouble("Previous");

        return value + " (" + (Math.round((value - prevValue) * 1000f) / 1000f) + ")";
    }

    private static String getRates() {
        try {
            JSONObject jsonObject = getJSON("https://www.cbr-xml-daily.ru/daily_json.js");
            JSONObject valutes = jsonObject.getJSONObject("Valute");
            String usdValue = getValue(valutes, "USD");
            String eurValue = getValue(valutes, "EUR");
            return "USD: " + usdValue + "\r\n" + "EUR: " + eurValue;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static void getRates(TextMessageReceivedEvent event) {
        //TODO: update for several codes?
        final String code = argsString;

        try {
            String msg = code + ": ";
            if (code.length() == 3) { // going to Russian Central Bank
                JSONObject jsonObject = getJSON("https://www.cbr-xml-daily.ru/daily_json.js");
                JSONObject valutes = jsonObject.getJSONObject("Valute");
                String usdValue = getValue(valutes, code);
                msg = code + ": " + usdValue;
            } else if (code.length() == 4) { // getting values of
                // TODO: add support for current day via function=TIME_SERIES_INTRADAY  https://www.alphavantage.co/documentation/#intraday
                String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + code + "&apikey=" + Tokens.ALPHAVANTAGE_KEY;
                JSONObject jsonObject = getJSON(url);
                String lastKnownDay = jsonObject.getJSONObject("Meta Data").getString("3. Last Refreshed");
                lastKnownDay = lastKnownDay.substring(0, lastKnownDay.indexOf(" ")).trim();
                double closeValue = jsonObject.getJSONObject("Time Series (Daily)").getJSONObject(lastKnownDay).getDouble("4. close");
                msg += "$" + closeValue + " (for " + lastKnownDay + ")";
            } else if (code.equalsIgnoreCase("pbelov")) {
                msg = "42";
            } else if (code.length() == 0) {
                msg = getRates();
            } else {
                msg = "I don't know how to provide rates for " + code;
            }

            if (msg != null) {
                TgMsgUtil.replyInChat(event, msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void showDay(TextMessageReceivedEvent event) {
        File file = getHolidays();
        if (file != null && file.exists()) {
            InputFile inputFile = new InputFile(file);
            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder().photo(inputFile).build();
            event.getChat().sendMessage(sendablePhotoMessage);
            file.delete();
        } else {
            TgMsgUtil.replyInChat(event, "что-то пошло не так");
        }
    }

    private static JSONObject getJSON(String query) throws IOException {
        URL url = new URL(query);
        URLConnection connection = url.openConnection();

        String line;
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        return new JSONObject(builder.toString());
    }
//    static String getYandexSearch(final String query) {
//        final String q = "https://yandex.ru/search/xml?user=pbelov42&key=" + Tokens.YANDEX_KEY + "&query=" + query;
//
//        return null;
//    }

    private static File getHolidays() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.forLanguageTag(RU_TAG));
        try {
            File file = new File("holydays.png");
            URL url = new URL("https://www.calend.ru/img/export/informer_names.png?" + format.format(new Date()));
            org.apache.commons.io.FileUtils.copyURLToFile(url, file);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getTop3ForTodayText(String chatName) {
        Map<String, Integer> userMessagesInChat = messagesMap.get(chatName);
        List<UserMessages> userMessagesList = Utils.convertToListUserMessages(userMessagesInChat);

        StringBuilder sb = new StringBuilder();
        int topCount = Math.min(userMessagesList.size(), 3);
        if (topCount > 0) {
            sb.append("Топ 3 за сегодня:");
            for (int i = 0; i < topCount; i++) {
                UserMessages userMessages = userMessagesList.get(i);
                sb.append("\r\n").append(userMessages);
            }
        } else {
            sb.append("Нет статы");
        }

        return sb.toString();
    }

    private static String getStats(String chatName, int count) {
        Map<String, Integer> wordsRepeat = repeatedWords(chatName);
        StringBuilder sb = new StringBuilder();

        List<UserMessages> topWords = Utils.convertToListUserMessages(wordsRepeat);
        count = count == 0 ? Math.min(1, topWords.size()) : Math.min(count, 10);
        sb.append("Топ ").append(count).append(" слов:\r\n");
        for (int i = 0; i < count; i++) {
            UserMessages userMessages = topWords.get(i);
            Utils.println(TAG, "userMessages = " + userMessages.toString());
            sb.append(i + 1).append(") ").append(userMessages.userName).append(": ").append(userMessages.messagesCount).append("\r\n");
        }

        return sb.toString();
    }

    private static String getStats(String chatName, String word) {
        Map<String, Integer> wordsRepeat = repeatedWords(chatName);

        return "\"" + word + "\": " + wordsRepeat.get(word.toLowerCase()) + "";
    }

    private static Map<String, Integer> repeatedWords(String chatName) {
        File file = new File(WORKING_DIR + File.separator + LOGDIR, Utils.fixFileName(chatName) + ".log");
        List<String> lines = null;
        try {
            lines = org.apache.commons.io.FileUtils.readLines(file, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Integer> wordsRepeat = new HashMap<>();

        if (lines != null) {
            List<String> wordsList = new ArrayList<>();
            for (String line : lines) {
                if (line.contains("): ")) {
                    line = line.substring(line.indexOf("): ") + 2);
                } else {
                    //TODO: ???
                }

                String wordsInLine[] = line.trim().toLowerCase().split(" ", -1);
                if (wordsInLine.length > 0 && wordsInLine[0].startsWith("/")) {
                    continue;
                }

                for (String word : wordsInLine) {
                    word = word
//                        .replaceAll(",", "")
//                        .replaceAll("\\.", "")
//                        .replaceAll("\\?", "")
//                        .replaceAll("!", "")
//                        .replaceAll("\"", "")
//                        .replaceAll("\\(", "")
//                        .replaceAll("\\)", "")
                            .replaceAll("[^\\p{L}\\p{Nd}]+", "");
                    if (word.length() > 2) {
                        wordsList.add(word);
                    }
                }
            }

            for (String word : wordsList) {
                Utils.incrementNumberInMap(wordsRepeat, word.toLowerCase());
            }

            lines.clear();
        }

        return wordsRepeat;
    }
}
