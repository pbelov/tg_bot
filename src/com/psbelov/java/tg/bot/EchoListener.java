package com.psbelov.java.tg.bot;

import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.psbelov.java.tg.bot.FileUtils.WORKING_DIR;
import static com.psbelov.java.tg.bot.Utils.LOGDIR;

public class EchoListener implements Listener {
    private final boolean DEBUG = !true;
    private final String TAG = "EchoListener";

    private int lastDay = -1;

    private long fasID = -1;
    private long muteID = -1;
    private String userToFas = null;
    private String userToMute = null;
    private final String BOT_PREFIX = "бот, ";
    private final String GLOBAL = "global";

    private final String ME = "@pbelov";
    private final String BOT = "@pbelov_bot";
    private final String RU_TAG = "ru";

    private final String[] mornings = {"утро", "утрецо", "утрота", "морнинг"};

    private Map<String, Boolean> botStatus = new HashMap<String, Boolean>() {{
        put("global", false);
    }};

    private Map<String, Map<String, Integer>> messagesMap = new HashMap<>();

    private Map<String, Integer> newDayMap = new HashMap<>();
    private Map<String, Integer> prevDayMap = new HashMap<>();
    private File catsFile = getGoogleQuery("cats");

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {
        handleMessage(event);
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        handleMessage(event);
    }

    private void handleMessage(TextMessageReceivedEvent event) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String messageText = event.getContent().getContent().trim();
        String chatName = event.getChat().getName();
        Message message = event.getMessage();
        User sender = message.getSender();
        String senderUserName = sender.getUsername();
        String personName = sender.getFullName() + " (" + senderUserName + ")";
        messageText = messageText.replace(BOT, "").trim().toLowerCase();

        if (!messageText.startsWith("/")) {
            incrementMessagesNumberFor(day, chatName, personName.replaceAll("@", ""));
        }

        newDayMap.put(chatName, day);

        Utils.println(TAG, chatName + ", " + StringUtils.getCurrentTimeStamp() + "; " + personName + ": " + messageText, chatName);

        Random random = new Random(System.currentTimeMillis());

        final String fasString = "агрись на ";
        final String antifasString = "хватит";
        final String muteString = "выключи ";
        final String BOT_PREFIX = "бот, ";
        final String GTFO_TEXT = "gtfo!";
        final String FIND_TEXT = "найди ";
        final String STATS = "stats ";
        final String COMMAND_DAY = "day";
        final String COMMAND_ME = "me";

        if (messageText.equals(BOT_PREFIX + "включись")) {
            if (senderUserName.equals(ME)) {
                botStatus.put(GLOBAL, true);
            } else {
                TgMsgUtil.sendToChat(event, GTFO_TEXT);
            }
        } else if (messageText.equals(BOT_PREFIX + "выключись")) {
            if (senderUserName.equals(ME)) {
                botStatus.put(GLOBAL, false);
            } else {
                TgMsgUtil.sendToChat(event, GTFO_TEXT);
            }
        }

        if (!DEBUG && botStatus.get(GLOBAL)) {
            if (messageText.startsWith("/")) {
                messageText = messageText.substring(1);
                Utils.println(TAG, "command [" + messageText + "]");
                if (!newDayMap.get(chatName).equals(prevDayMap.get(chatName)) && !messageText.equals(COMMAND_DAY)) {
                    if (hour > 6 && hour < 12) {
                        TgMsgUtil.replyInChat(event, getMorningText(random.nextInt(mornings.length), hour));
                    }
                    SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
                    TgMsgUtil.sendToChat(event, format.format(new Date()));
                    prevDayMap.put(chatName, day);
//            showDay(event);
                }

                if (messageText.equals("time")) {
                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag(RU_TAG));
                    TgMsgUtil.replyInChat(event, format.format(new Date()));
                } else if (messageText.equals("date")) {
                    SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
                    TgMsgUtil.replyInChat(event, format.format(new Date()));
                } else if (messageText.startsWith(COMMAND_ME)) {
                    event.getMessage().getBotInstance().deleteMessage(event.getMessage());
                    SendableTextMessage sendableMessage = SendableTextMessage.builder()
                            .message(senderUserName.replaceFirst("@", "") + "*" + messageText.replaceFirst(COMMAND_ME, "") + "*")
                            .parseMode(ParseMode.MARKDOWN)
                            .build();
                    event.getMessage().getBotInstance().sendMessage(event.getChat(), sendableMessage);
                } else if (messageText.startsWith("ping")) {
                    TgMsgUtil.replyInChat(event, "pong!");
                } else if (messageText.startsWith("top")) {
                    TgMsgUtil.sendToChat(event, getTop3ForTodayText(chatName));
                } else if (messageText.equals(COMMAND_DAY)) {
                    showDay(event);
                } else if (messageText.equals(STATS.trim())) {
                    TgMsgUtil.sendToChat(event, getStats(chatName, 3));
                } else if (messageText.startsWith(STATS)) {
                    String wordString = messageText.replaceFirst(STATS, "");
                    TgMsgUtil.sendToChat(event, getStats(chatName, wordString.trim()));
                }
            } else if (messageText.startsWith(BOT_PREFIX)) {
                messageText = messageText.replace(BOT_PREFIX, "");
                Utils.println(TAG, "request [" + messageText + "]");
                if (messageText.startsWith(fasString)) {
                    if (senderUserName.equals(ME)) {
                        userToFas = messageText.replaceFirst(fasString, "");
                        System.out.println("принято!");
                        TgMsgUtil.replyInChat(event, "принято!");
                        if (userToMute.equals(userToFas)) {
                            userToMute = null;
                            muteID = -1;
                        }
                    } else {
                        TgMsgUtil.replyInChat(event, "не ты мой хозяин!");
                    }
                } else if (messageText.startsWith(antifasString) && senderUserName.equals(ME)) {
                    userToFas = null;
                    fasID = -1;
                    userToMute = null;
                    muteID = -1;
                    System.out.println("ладно...");
                    TgMsgUtil.replyInChat(event, "ладно...");
                } else if (messageText.equals("котиков!")) {
                    Utils.println(TAG, "cats activated!");
                    if (catsFile != null) {
                        InputFile inputFile = new InputFile(catsFile);
                        SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder()
                                .photo(inputFile)
                                .build();
                        event.getChat().sendMessage(sendablePhotoMessage);
                        catsFile.delete();
                        catsFile = getGoogleQuery("cats");
                    } else {
                        TgMsgUtil.replyInChat(event, "у меня котопередоз, подождите");
                    }
                } else if (messageText.startsWith(FIND_TEXT)) {
                    find(messageText.replace(FIND_TEXT, ""), event);
                } else if (messageText.equals("иди нахуй")) {
                    TgMsgUtil.replyInChat(event, "fuck you!");
                }
            } else {
                if (messageText.equals("42")) {
                    TgMsgUtil.replyInChat(event, "don't panic!");
                } else if (messageText.startsWith("ты ") && messageText.split(" ", -1).length == 2) {
                    TgMsgUtil.replyInChat(event, "нет ты!");
                } else if (messageText.startsWith("иди нахуй") && messageText.split(" ", -1).length == 2) {
                    TgMsgUtil.replyInChat(event, "сам иди!");
                } else if (messageText.startsWith(muteString)) {
                    if (senderUserName.equals(ME)) {
                        TgMsgUtil.replyInChat(event, "принято!");
                        userToMute = messageText.replaceFirst(muteString, "");
                        if (userToMute.equals(userToFas)) {
                            userToFas = null;
                            fasID = -1;
                        }
                    } else {
                        TgMsgUtil.replyInChat(event, "не ты мой хозяин!");
                    }
                } else if (messageText.equals("что было?") || messageText.equals("что тут было?") || messageText.equals("что тут у вас?") || messageText.equals("что тут?")) {
                    int i = random.nextInt(3);
                    if (i == 0) {
                        TgMsgUtil.replyInChat(event, "ничего интересного");
                    } else if (i == 1) {
                        TgMsgUtil.replyInChat(event, "хрень какая-то");
                    } else if (i == 2) {
                        TgMsgUtil.replyInChat(event, "фигня");
                    }
                }
            }
            if (getPartOfDay() == getPartOfDayInt(messageText)) {
                TgMsgUtil.replyInChat(event, "д.");
            } else if (getPartOfDayInt(messageText) != -1) {
                TgMsgUtil.replyInChat(event, "нт.");
            }

            if (userToFas != null && senderUserName.contains(userToFas)) {
                fasID = event.getMessage().getSender().getId();
            }

            if (userToMute != null && senderUserName.contains(userToMute)) {
                muteID = event.getMessage().getSender().getId();
            }

            if (sender.getId() == fasID) {
                String text = null;
                if (hour >= 8 && hour <= 17) {
                    text = "работать иди";
                } else if (hour >= 18 || hour <= 3) {
                    text = "спать иди";
                } else {
                    // TBD
                }

                if (text != null && random.nextInt(42) == 0) {
                    text += ", пожалуйста";
                }
                TgMsgUtil.replyInChat(event, text);
            }

            if (sender.getId() == muteID) {
                message.getBotInstance().deleteMessage(message);
            }
        } else {
            Utils.println(TAG, "debug mode = " + DEBUG + ", bot status = " + botStatus.get(GLOBAL));
        }
    }

    private int getPartOfDayInt(String text) {
        if (text.equals("утро?")) {
            return 0;
        } else if (text.equals("день?")) {
            return 1;
        } else if (text.equals("вечер?")) {
            return 2;
        } else if (text.equals("ночь?")) {
            return 3;
        } else {
            return -1;
        }
    }

    private int getPartOfDay() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            return 0;
        } else if (hour >= 12 && hour < 18) {
            return 1;
        } else if (hour >= 18 && hour <= 23) {
            return 2;
        } else {
            return 3;
        }
    }

    private void find(String messageText, TextMessageReceivedEvent event) {
        File file = getGoogleQuery(messageText);
        if (file != null) {
            InputFile inputFile = new InputFile(file);
            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder()
                    .replyTo(event.getMessage())
                    .photo(inputFile)
                    .build();
            event.getChat().sendMessage(sendablePhotoMessage);
            file.delete();
        } else {
            TgMsgUtil.replyInChat(event, "у меня передоз, подождите");
        }
    }

    private void showDay(TextMessageReceivedEvent event) {
        File file = getHolidays();
        if (file != null) {
            InputFile inputFile = new InputFile(file);
            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder().photo(inputFile).build();
            event.getChat().sendMessage(sendablePhotoMessage);
            file.delete();
        } else {
            TgMsgUtil.replyInChat(event, "что-то пошло не так");
        }
    }

    private File getGoogleQuery(String query) {
        Random random = new Random(System.currentTimeMillis());
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        query = query.replaceAll(" ", "%20");

        final String q = "https://api.qwant.com/api/search/images?count=1&offset=" + random.nextInt(200) + "&q=" + query;
        try {
            URL url = new URL(q);
            URLConnection connection = url.openConnection();

            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            JSONObject json = new JSONObject(builder.toString());
            String imageUrl = json.getJSONObject("data").getJSONObject("result").getJSONArray("items").getJSONObject(0).getString("media_fullsize");
            imageUrl = "https:" + imageUrl;

            BufferedImage image = ImageIO.read(new URL(imageUrl));


            File outputfile = new File("image.jpg");
            ImageIO.write(image, "jpg", outputfile);
            return outputfile;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getYandexSearch(final String query) {
        final String q = "https://yandex.ru/search/xml?user=pbelov42&key=" + Tokens.YANDEX_KEY + "&query=" + query;

        String result = null;

        return result;
    }

    private File getHolidays() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.forLanguageTag(RU_TAG));
        try {
            File file = new File("holydays.png");
            org.apache.commons.io.FileUtils.copyURLToFile(new URL("http://www.calend.ru/img/export/informer_names.png?" + format.format(new Date())), file);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private String getTop3ForTodayText(String chatName) {
        Map<String, Integer> userMessagesInChat = messagesMap.get(chatName);
        List<UserMessages> userMessagesList = Utils.convertToListUserMessages(userMessagesInChat);

        StringBuilder sb = new StringBuilder("Топ 3 за сегодня:");

        int topCount = Math.min(userMessagesList.size(), 3);
        for (int i = 0; i < topCount; i++) {
            UserMessages userMessages = userMessagesList.get(i);
            sb.append("\r\n").append(userMessages);
        }

        return sb.toString();
    }

    private void incrementMessagesNumberFor(int day, String chatName, String userName) {
        if (messagesMap.containsKey(chatName)) {
            Map<String, Integer> userMessages = messagesMap.get(chatName);
            if (userMessages.containsKey(userName)) {
                int number = userMessages.get(userName);
                userMessages.put(userName, ++number);
            } else {
                userMessages.put(userName, 1);
            }
            messagesMap.put(chatName, userMessages);
        } else {
            Map<String, Integer> map = new HashMap<>();
            map.put(userName, 1);
            messagesMap.put(chatName, map);
        }
    }

    private String getMorningText(int i, int hour) {
        return mornings[i];
    }

    private String getStats(String chatName, int count) {
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

    private String getStats(String chatName, String word) {
        Map<String, Integer> wordsRepeat = repeatedWords(chatName);

        return "\"" + word + "\": " + wordsRepeat.get(word.toLowerCase()) + "";
    }


    private Map<String, Integer> repeatedWords(String chatName) {
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

                }

                String wordsInLine[] = line.trim().toLowerCase().split(" ", -1);
                for (String word : wordsInLine) {
                    if (word.startsWith("/")) {
                        continue;
                    }
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
