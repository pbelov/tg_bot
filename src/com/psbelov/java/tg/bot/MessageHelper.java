package com.psbelov.java.tg.bot;

import org.json.JSONArray;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.Content;
import pro.zackpollard.telegrambot.api.chat.message.content.type.PhotoSize;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.ParseMode;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
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

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
class MessageHelper {
    private static final String TAG = "MessageHelper";
    private static final boolean DEBUG = false;

    private static final String FPS_SUBDIR = "fps";
    static final String BOT_PREFIX = "бот, ";
    private static final String GLOBAL = "global";

    private static long fasID = -1;
    private static long muteID = -1;
    private static String userToFas = null;
    private static String userToMute = null;

    static final String CMD_AGR = "агрись на ";
    static final String CMD_CANCEL = "хватит";
    static final String CMD_MUTE = "выключи ";
    static final String CMD_FIND_TEXT = "найди ";
    static final String CMD_RATES = "курс";
    static final String CMD_TRANSLATE = "переведи";

    //TODO: add others
    static final String HNTR = "хнтр";

    private static File catsFile = MessageHelper.getGoogleQuery("cats");

    // if true - it will work, otherwise - not. obviously.
    private static Map<String, Boolean> botStatus = new HashMap<String, Boolean>() {{
        put(GLOBAL, false);
    }};

    private static final String ME = "@pbelov";
    static final String BOT = "@pbelov_bot";
    private static final String RU_TAG = "ru";


    private static Map<String, Map<String, Integer>> messagesMap = new HashMap<>();

    private static String chatName = null;
    private static String messageText = null;
    private static String senderUserName;
    private static Message message;
    private static User sender;

    private static String argsString;
    private static int hour, day;

    private MessageHelper() {
    }

    static void getBaseData(TextMessageReceivedEvent event, String type) {
        chatName = event.getChat().getName();
        message = event.getMessage();
        sender = message.getSender();
        senderUserName = sender.getUsername();
        String personName = sender.getFullName() + " (" + senderUserName + ")";
        messageText = event.getContent().getContent().trim();

        Calendar calendar = Calendar.getInstance();
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        day = calendar.get(Calendar.DAY_OF_WEEK);


        Utils.println(TAG, "[" + type + "] " + chatName + ", " + StringUtils.getCurrentTimeStamp() + "; " + personName + ": " + messageText, chatName);
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

    @SuppressWarnings("ForLoopReplaceableByForEach")
    static void checkForBayan(TextMessageReceivedEvent event) {
        final List<String> urls = StringUtils.extractUrls(messageText);

        HashMap<String, ArrayList<String>> urlsStored = loadUrls();
        if (urlsStored == null) {
            urlsStored = new HashMap<>();
        }

        final int urlsSize = urls.size();

        for (int i = 0; i < urlsSize; i++) {
            final String url = urls.get(i);
            ArrayList<String> urlsInChat = urlsStored.get(chatName);
            if (urlsInChat == null) {
                urlsInChat = new ArrayList<>();
            }

            if (urlsInChat.contains(url)) {
                TgMsgUtil.replyInChat(event, "баян!");
            } else {
                urlsInChat.add(url);
                urlsStored.put(chatName, urlsInChat);
            }
        }

        if (urlsSize > 0) {
            saveSerializable(urlsStored, "urls");
        }
    }

    private static void saveSerializable(Serializable prevDayMap, String name) {
        File file = new File(name);
        if (file.exists()) {
            file.delete();
        }

        ObjectOutputStream oos = null;
        FileOutputStream fout;
        try {
            fout = new FileOutputStream(name, true);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(prevDayMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static HashMap<String, Integer> loadDayMap(String name) {
        HashMap<String, Integer> readCase = null;

        if (new File(name).exists()) {
            ObjectInputStream objectinputstream = null;
            try {
                FileInputStream streamIn = new FileInputStream(name);
                objectinputstream = new ObjectInputStream(streamIn);
                readCase = (HashMap<String, Integer>) objectinputstream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (objectinputstream != null) {
                    try {
                        objectinputstream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        return readCase;
    }

    private static HashMap<String, ArrayList<String>> loadUrls() {
        final String urlsFilename = "urls";
        HashMap<String, ArrayList<String>> readCase = null;

        File file = new File(urlsFilename);
        if (file.exists()) {
            ObjectInputStream objectinputstream = null;
            try {
                FileInputStream streamIn = new FileInputStream(urlsFilename);
                objectinputstream = new ObjectInputStream(streamIn);
                readCase = (HashMap<String, ArrayList<String>>) objectinputstream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (objectinputstream != null) {
                    try {
                        objectinputstream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        return readCase;
    }

    static void translateMessage(TextMessageReceivedEvent event) {
        Message repliedToMessage = event.getMessage().getRepliedTo();
        Content content = repliedToMessage.getContent();

        Utils.println(TAG, "content = " + content);
    }

    static void analyzeImage(PhotoMessageReceivedEvent event) throws IOException {
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
        if (fpssFiles != null) {
            for (File file : fpssFiles) {
                byte[] fppsss = getFP(file, false);
                fpss.add(fppsss);
            }
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

    private static byte[] getFP(File file, boolean foFP) throws IOException {
        byte[] newData;
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

    private static void saveFingerprint(File file, String chatDir) {
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


    static int getPartOfDayInt(String text) {
        switch (text) {
            case "утро?":
                return 0;
            case "день?":
                return 1;
            case "вечер?":
                return 2;
            case "ночь?":
                return 3;
            default:
                return -1;
        }
    }

    static int getPartOfDay() {
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

    // tries to find an image for the query
    static void find(TextMessageReceivedEvent event) {
//        TgMsgUtil.replyInChat(event, "отказать");
        messageText = messageText.replace(CMD_FIND_TEXT, "");

        File file = getGoogleQuery(argsString);
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

    static void showDay(TextMessageReceivedEvent event) {
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

    private static File getGoogleQuery(String query) {
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
            org.apache.commons.io.FileUtils.copyURLToFile(new URL("http://www.calend.ru/img/export/informer_names.png?" + format.format(new Date())), file);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getTop3ForTodayText(String chatName) {
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

    static void incrementMessagesNumberFor() {
        String userName = senderUserName.replaceAll("@", "");

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

    static void handleMorning(TextMessageReceivedEvent event, Random random) {
        HashMap<String, Integer> newDayMap = MessageHelper.loadDayMap("newDayMap");
        if (newDayMap == null) {
            newDayMap = new HashMap<>();
        }

        newDayMap.put(chatName, day);
        MessageHelper.saveSerializable(newDayMap, "newDayMap");

        HashMap<String, Integer> prevDayMap = MessageHelper.loadDayMap("prevDayMap");
        if (prevDayMap == null) {
            prevDayMap = new HashMap<>();
        }

        if (!newDayMap.get(chatName).equals(prevDayMap.get(chatName))) {
            if (hour > 6 && hour < 12) {
                TgMsgUtil.replyInChat(event, MessageHelper.getMorningText(random.nextInt(mornings.length)));
            }
            SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
            TgMsgUtil.sendToChat(event, format.format(new Date()));
            prevDayMap.put(chatName, day);
            MessageHelper.saveSerializable(prevDayMap, "prevDayMap");
//            showDay(event);
        }
    }

    private static final String[] mornings = {"утро", "утрецо", "утрота", "морнинг"};

    private static String getMorningText(int i) {
        return mornings[i];
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

    static void handleTopCommand(CommandMessageReceivedEvent event) {
        TgMsgUtil.sendToChat(event, MessageHelper.getTop3ForTodayText(chatName));
    }

    static void handleStatsCommand(CommandMessageReceivedEvent event) {
        if (argsString.length() == 0) {
            TgMsgUtil.sendToChat(event, MessageHelper.getStats(chatName, 3));
        } else {
            TgMsgUtil.sendToChat(event, MessageHelper.getStats(chatName, argsString.trim()));
        }
    }

    //TODO: update
    static String getMessagesData() {
        return messageText;
    }

    static void handleBotCommands(TextMessageReceivedEvent event) {
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
    }

    static void handleTimeCommand(CommandMessageReceivedEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag(RU_TAG));
        TgMsgUtil.replyInChat(event, format.format(new Date()));
    }

    static void handleDateCommand(CommandMessageReceivedEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("d MMMM, EEEE", Locale.forLanguageTag(RU_TAG));
        TgMsgUtil.replyInChat(event, format.format(new Date()));
    }

    static boolean checkBotStatus() {

        // if debug mode, react only on owner
        if (!botStatus.get(GLOBAL) || (DEBUG && !senderUserName.equals(ME))) {
            return false;
        } else {
            Utils.println(TAG, "debug mode = " + DEBUG + ", bot status = " + botStatus.get(GLOBAL));
            return true;
        }

    }

    static void handleArgCommand(TextMessageReceivedEvent event) {
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
    }

    static void handleMuteCommand(TextMessageReceivedEvent event) {
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
    }

    static void handleCancelCommand(TextMessageReceivedEvent event) {
        if (senderUserName.equals(ME)) {
            userToFas = null;
            fasID = -1;
            userToMute = null;
            muteID = -1;
            System.out.println("ладно...");
            TgMsgUtil.replyInChat(event, "ладно...");
        } else {
            TgMsgUtil.replyInChat(event, "не ты мой хозяин!");
        }
    }

    static void handleCatsCommand(TextMessageReceivedEvent event) {
        Utils.println(TAG, "cats activated!");
        TgMsgUtil.replyInChat(event, "котики всё");
        if (catsFile != null) {
            InputFile inputFile = new InputFile(catsFile);
            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder()
                    .photo(inputFile)
                    .build();
            event.getChat().sendMessage(sendablePhotoMessage);
            catsFile.delete();
            catsFile = MessageHelper.getGoogleQuery("cats");
        } else {
            TgMsgUtil.replyInChat(event, "у меня котопередоз, подождите");
        }
    }

    static void handleHntrInvocation(TextMessageReceivedEvent event) {
        TgMsgUtil.sendToChat(event, ME + ", тебя призывает в чат " + senderUserName);
    }

    static void handleFasAndMuted(TextMessageReceivedEvent event, Random random) {
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
                // TODO: TBD
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
}
