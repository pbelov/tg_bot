package com.psbelov.java.tg.bot;

import org.json.JSONArray;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.type.PhotoSize;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.PhotoMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.psbelov.java.tg.bot.FileUtils.WORKING_DIR;
import static com.psbelov.java.tg.bot.Utils.LOGDIR;

@SuppressWarnings("FieldCanBeLocal")
public class EchoListener implements Listener {
    private final boolean DEBUG = false;
    private final String TAG = "EchoListener";

    private long fasID = -1;
    private long muteID = -1;
    private String userToFas = null;
    private String userToMute = null;
    private final String GLOBAL = "global";

    private static final String FPS_SUBDIR = "fps";

    private final String ME = "@pbelov";
    private final String BOT = "@pbelov_bot";
    private final String RU_TAG = "ru";

    private final String[] mornings = {"утро", "утрецо", "утрота", "морнинг"};

    // if true - it will work, otherwise - not. obviously.
    private Map<String, Boolean> botStatus = new HashMap<String, Boolean>() {{
        put("global", false);
    }};

    private Map<String, Map<String, Integer>> messagesMap = new HashMap<>();

    private Map<String, Integer> newDayMap = new HashMap<>();
    private Map<String, Integer> prevDayMap = new HashMap<>();
    private File catsFile = getGoogleQuery("cats");

    private String chatName = null;
    private String personName = null;
    private String messageText = null;
    private String senderUserName;
    private Message message;
    private User sender;

    private void getBaseData(TextMessageReceivedEvent event) {
        chatName = event.getChat().getName();
        message = event.getMessage();
        sender = message.getSender();
        senderUserName = sender.getUsername();
        personName = sender.getFullName() + " (" + senderUserName + ")";
        messageText = event.getContent().getContent().trim();
    }

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        getBaseData(event);
        Utils.println(TAG, chatName + ", " + StringUtils.getCurrentTimeStamp() + "; " + personName + ": " + messageText, chatName);
        handleCommand(event);
//        handleMessage(event);
    }

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {
        getBaseData(event);
        Utils.println(TAG, chatName + ", " + StringUtils.getCurrentTimeStamp() + "; " + personName + ": " + messageText, chatName);

        handleMessage(event);
    }

    @Override
    public void onPhotoMessageReceived(PhotoMessageReceivedEvent event) {
        try {
            analyzeImage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(CommandMessageReceivedEvent event) {
        String[] args = event.getArgs();
        String argsString = event.getArgsString();
        String command = event.getCommand();
        Utils.println(TAG, "handleCommand: args = " + Arrays.toString(args) + ", argsString = " + argsString + ", command = " + command);

        final String STATS = "stats";
        final String COMMAND_DAY = "day";
        final String COMMAND_ME = "me";
        final String COMMAND_FIND = "find";
        final String COMMAND_RATE = "rate";

        if (command.equals("time")) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag(RU_TAG));
            TgMsgUtil.replyInChat(event, format.format(new Date()));
        } else if (command.equals("date")) {
            SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
            TgMsgUtil.replyInChat(event, format.format(new Date()));
        } else if (command.startsWith(COMMAND_ME)) {
            event.getMessage().getBotInstance().deleteMessage(event.getMessage());
            SendableTextMessage sendableMessage = SendableTextMessage.builder()
                    .message(senderUserName.replaceFirst("@", "") + " *" + argsString + "*")
                    .parseMode(ParseMode.MARKDOWN)
                    .build();
            event.getMessage().getBotInstance().sendMessage(event.getChat(), sendableMessage);
        } else if (command.equals("ping")) {
            TgMsgUtil.replyInChat(event, "pong!");
        } else if (command.equals("top")) {
            TgMsgUtil.sendToChat(event, getTop3ForTodayText(chatName));
        } else if (command.equals(COMMAND_DAY)) {
            showDay(event);
        } else if (command.equals(STATS)) {
            if (argsString.length() == 0) {
                TgMsgUtil.sendToChat(event, getStats(chatName, 3));
            } else {
                TgMsgUtil.sendToChat(event, getStats(chatName, argsString.trim()));
            }
        } else if (command.equals(COMMAND_FIND)) {
            find(argsString, event);
        } else if (command.equals(COMMAND_RATE)) {
            if (argsString.trim().length() > 0) {
                getRates(event, argsString);
            } else {
                getRates(event);
            }
        }
    }

    private void handleMessage(TextMessageReceivedEvent event) {
        final String BOT_PREFIX = "бот, ";
        final String GTFO_TEXT = "gtfo!";

        if (messageText.equals(BOT_PREFIX + "вкл")) {
            if (senderUserName.equals(ME)) {
                botStatus.put(GLOBAL, true);
            } else {
                TgMsgUtil.sendToChat(event, GTFO_TEXT);
            }
        } else if (messageText.equals(BOT_PREFIX + "выкл")) {
            if (senderUserName.equals(ME)) {
                botStatus.put(GLOBAL, false);
            } else {
                TgMsgUtil.sendToChat(event, GTFO_TEXT);
            }
        }


        // if debug mode, react only on owner
        if (!botStatus.get(GLOBAL) || (DEBUG && !senderUserName.equals(ME))) {
            return;
        } else {
            Utils.println(TAG, "debug mode = " + DEBUG + ", bot status = " + botStatus.get(GLOBAL));
        }

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        incrementMessagesNumberFor(day, chatName, personName.replaceAll("@", ""));
        newDayMap.put(chatName, day);

        messageText = messageText.replace(BOT, "").trim().toLowerCase();

        final String CMD_AGR = "агрись на ";
        final String CMD_CANCEL = "хватит";
        final String CMD_MUTE = "выключи ";
        final String CMD_FIND_TEXT = "найди ";
        final String CMD_RATES = "курс";
        final String HNTR = "хнтр";

        final Random random = new Random(System.currentTimeMillis());

        if (!newDayMap.get(chatName).equals(prevDayMap.get(chatName))) {
            if (hour > 6 && hour < 12) {
                TgMsgUtil.replyInChat(event, getMorningText(random.nextInt(mornings.length), hour));
            }
            SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
            TgMsgUtil.sendToChat(event, format.format(new Date()));
            prevDayMap.put(chatName, day);
//            showDay(event);
        }

        if (messageText.startsWith(BOT_PREFIX)) {
            messageText = messageText.replace(BOT_PREFIX, "");
            Utils.println(TAG, "request [" + messageText + "]");
            if (messageText.startsWith(CMD_AGR)) {
                if (senderUserName.equals(ME)) {
                    userToFas = messageText.replaceFirst(CMD_AGR, "");
                    System.out.println("принято!");
                    TgMsgUtil.replyInChat(event, "принято!");
                    if (userToMute.equals(userToFas)) {
                        userToMute = null;
                        muteID = -1;
                    }
                } else {
                    TgMsgUtil.replyInChat(event, "не ты мой хозяин!");
                }
            } else if (messageText.startsWith(CMD_MUTE)) {
                if (senderUserName.equals(ME)) {
                    TgMsgUtil.replyInChat(event, "принято!");
                    userToMute = messageText.replaceFirst(CMD_MUTE, "");
                    if (userToMute.equals(userToFas)) {
                        userToFas = null;
                        fasID = -1;
                    }
                } else {
                    TgMsgUtil.replyInChat(event, "не ты мой хозяин!");
                }
            } else if (messageText.startsWith(CMD_CANCEL) && senderUserName.equals(ME)) {
                userToFas = null;
                fasID = -1;
                userToMute = null;
                muteID = -1;
                System.out.println("ладно...");
                TgMsgUtil.replyInChat(event, "ладно...");
            } else if (messageText.equals("котиков!")) {
                Utils.println(TAG, "cats activated!");
                TgMsgUtil.replyInChat(event, "котики всё");
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
            } else if (messageText.startsWith(CMD_FIND_TEXT)) {
                find(messageText.replace(CMD_FIND_TEXT, ""), event);
            } else if (messageText.equals(CMD_RATES)) {
                getRates(event);
            } else if (messageText.equals("иди нахуй")) {
                TgMsgUtil.replyInChat(event, "fuck you");
            } else if (messageText.equals("fuck you")) {
                TgMsgUtil.replyInChat(event, "иди нахуй");
            }
        } else {
            Message repliedTo = event.getMessage().getRepliedTo();
            if (repliedTo != null) {
                User repliedSender = repliedTo.getSender();
                if (repliedSender.getUsername().equals(BOT)) {
                    if (messageText.equals("а ты?")) {
                        TgMsgUtil.replyInChat(event, "а я — бот");
                    } else if (messageText.equals("ты кто?")) {
                        TgMsgUtil.replyInChat(event, "я — бот");
                    }
                }
            } else if (messageText.equals("42")) {
                TgMsgUtil.replyInChat(event, "don't panic!");
            } else if (messageText.startsWith("ты ") && messageText.split(" ", -1).length == 2) {
                TgMsgUtil.replyInChat(event, "нет ты!");
            } else if (messageText.startsWith("иди нахуй") && messageText.split(" ", -1).length == 2) {
                TgMsgUtil.replyInChat(event, "сам иди!");
            } else if (messageText.equals("что было?") || messageText.equals("что тут было?") || messageText.equals("что тут у вас?") || messageText.equals("что тут?")) {
                int i = random.nextInt(3);
                if (i == 0) {
                    TgMsgUtil.replyInChat(event, "ничего интересного");
                } else if (i == 1) {
                    TgMsgUtil.replyInChat(event, "хрень какая-то");
                } else if (i == 2) {
                    TgMsgUtil.replyInChat(event, "фигня");
                }
            } else if (messageText.startsWith(HNTR)) {
                TgMsgUtil.sendToChat(event, ME + ", тебя призывает в чат " + senderUserName);
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

        // remove all messages from muted user
        if (sender.getId() == muteID) {
            message.getBotInstance().deleteMessage(message);
        }
    }

    private void analyzeImage(PhotoMessageReceivedEvent event) throws IOException {
        final String tempImgFilename = "tempimg";
        PhotoSize[] photoSize = event.getContent().getContent();
        File infile = new File(tempImgFilename);
        photoSize[0].downloadFile(event.getMessage().getBotInstance(), infile);
        byte[] currentFps = getFP(infile, true);

        boolean bayanFound = false;
        File fpsDir = new File(FPS_SUBDIR + File.separator + event.getChat().getId());
        if (!fpsDir.exists()) {
            fpsDir.mkdirs();
        }

        File fpssFiles[] = fpsDir.listFiles();
        List<byte[]> fpss = new ArrayList<>();
        for (File file : fpssFiles) {
            byte[] fppsss = getFP(file, false);
            fpss.add(fppsss);
        }

        for (byte[] bytes : fpss) {
            if (Arrays.equals(bytes, currentFps)) {
                bayanFound = true;
                SendableTextMessage sendableMessage = SendableTextMessage.builder()
                        .message("баян!")
                        .replyTo(event.getMessage())
                        .build();
                event.getChat().sendMessage(sendableMessage);
                break;
            }
        }

        if (!bayanFound) {
            saveFingerprint(infile, event.getChat().getId());
        }
    }

    private byte[] getFP(File file, boolean foFP) throws IOException {
        byte[] newData = null;
        Path pathIn = Paths.get(file.getAbsolutePath());
        byte[] data = Files.readAllBytes(pathIn);
        int mult = foFP ? 42 : 1;
        int fpSize = data.length / mult;
        newData = new byte[fpSize];
        for (int i = 0; i < fpSize; i++) {
            newData[i] = data[i * mult];
        }
        return newData;
    }

    private void saveFingerprint(File file, String chatDir) {
        File dir = new File(FPS_SUBDIR);
        if (dir.isDirectory() && !dir.exists()) {
            dir.mkdir();
        }
        Path pathOut = Paths.get(FPS_SUBDIR + File.separator + chatDir + File.separator + System.currentTimeMillis() + ".dat");
        try {
            Files.write(pathOut, getFP(file, true), StandardOpenOption.CREATE_NEW);
        } catch (IOException e1) {
            e1.printStackTrace();
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

    private String getValue(JSONObject valutes, String code) {
        JSONObject usdJson = valutes.getJSONObject(code);
        double value = usdJson.getDouble("Value");
        double prevValue = usdJson.getDouble("Previous");

        return value + " (" + (Math.round((value - prevValue) * 1000f) / 1000f) + ")";
    }

    private void getRates(TextMessageReceivedEvent event) {
        try {
            JSONObject jsonObject = getJSON("https://www.cbr-xml-daily.ru/daily_json.js");
            JSONObject valutes = jsonObject.getJSONObject("Valute");
            String usdValue = getValue(valutes, "USD");
            String eurValue = getValue(valutes, "EUR");
            String msg = "USD: " + usdValue + "\r\n" + "EUR: " + eurValue;
            TgMsgUtil.replyInChat(event, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getRates(TextMessageReceivedEvent event, String code) {
        try {
            JSONObject jsonObject = getJSON("https://www.cbr-xml-daily.ru/daily_json.js");
            JSONObject valutes = jsonObject.getJSONObject("Valute");
            String usdValue = getValue(valutes, code);
            String msg = code + ": " + usdValue;
            TgMsgUtil.replyInChat(event, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // tries to find an image for the query
    private void find(String query, TextMessageReceivedEvent event) {
//        TgMsgUtil.replyInChat(event, "отказать");

        File file = getGoogleQuery(query);
        if (file != null) {
            InputFile inputFile = new InputFile(file);
            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder()
                    .replyTo(event.getMessage())
                    .photo(inputFile)
                    .build();
            event.getChat().sendMessage(sendablePhotoMessage);
            file.delete();
        } else {
            TgMsgUtil.replyInChat(event, "что-то пошло не так");
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

        final String q = "https://contextualwebsearch.com/api/Search/ImageSearchAPI?q=" + query + "&count=100";
        try {
            JSONObject jsonObject = getJSON(q);
            JSONArray array = jsonObject.getJSONArray("value");
            if (array.length() == 0) {
                return null;
            }
            int randomIndex = random.nextInt(array.length());
            String imageUrl = array.getJSONObject(randomIndex).getString("url");
//            imageUrl = "https:" + imageUrl;
            BufferedImage image = ImageIO.read(new URL(imageUrl));

            File outputfile = new File("image.jpg");
            ImageIO.write(image, "jpg", outputfile);
            return outputfile;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONObject getJSON(String query) throws IOException {
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
