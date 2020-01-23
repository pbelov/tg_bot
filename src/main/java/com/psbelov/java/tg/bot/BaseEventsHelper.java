package com.psbelov.java.tg.bot;

import com.psbelov.java.tg.bot.Utils.StringUtils;
import com.psbelov.java.tg.bot.Utils.TgMsgUtil;
import com.psbelov.java.tg.bot.Utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.send.InputFile;
import pro.zackpollard.telegrambot.api.chat.message.send.SendablePhotoMessage;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("ResultOfMethodCallIgnored")
class BaseEventsHelper {
    private static final String TAG = "BaseEventsHelper";
    static String chatName = null;
    static String messageText = null;
    static String senderUserName;
    static Message message;
    static User sender;

    static String argsString;

    static int hour, day;

    static final String CMD_FIND_TEXT = "найди ";
//    private static File catsFile = getSearchQuery("cats");

    static final String GLOBAL = "global";

    static final String ME = "@pbelov";
    static final String RU_TAG = "ru";

    static Map<String, Map<String, Integer>> messagesMap = new HashMap<>();

    // if true - it will work, otherwise - not. obviously.
    static Map<String, Boolean> botStatus = new HashMap<String, Boolean>() {{
        put(GLOBAL, false);
    }};

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

    static boolean checkBotStatus() {
        // if debug mode, react only on owner
        if (!botStatus.get(GLOBAL) || (Main.DEBUG && !senderUserName.equals(ME))) {
            return false;
        } else {
            Utils.println(TAG, "debug mode = " + Main.DEBUG + ", bot status = " + botStatus.get(GLOBAL));
            return true;
        }
    }

    // tries to find an image for the query
    static void find(TextMessageReceivedEvent event) {
//        TgMsgUtil.replyInChat(event, "отказать");
        messageText = messageText.replace(CMD_FIND_TEXT, "");

        File file = getSearchQuery(argsString);
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

//    static void handleCatsMessage(TextMessageReceivedEvent event) {
//        Utils.println(TAG, "cats activated!");
//        TgMsgUtil.replyInChat(event, "котики всё");
//        if (catsFile != null) {
//            InputFile inputFile = new InputFile(catsFile);
//            SendablePhotoMessage sendablePhotoMessage = SendablePhotoMessage.builder()
//                    .photo(inputFile)
//                    .build();
//            event.getChat().sendMessage(sendablePhotoMessage);
//            catsFile.delete();
//            catsFile = getSearchQuery("cats");
//        } else {
//            TgMsgUtil.replyInChat(event, "у меня котопередоз, подождите");
//        }
//    }

    private static File getSearchQuery(String query) {
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
}
