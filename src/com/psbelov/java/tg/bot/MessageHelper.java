package com.psbelov.java.tg.bot;

import com.psbelov.java.tg.bot.Utils.StringUtils;
import com.psbelov.java.tg.bot.Utils.TgMsgUtil;
import com.psbelov.java.tg.bot.Utils.Utils;
import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.chat.message.content.Content;
import pro.zackpollard.telegrambot.api.chat.message.content.type.PhotoSize;
import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.chat.message.PhotoMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored", "StatementWithEmptyBody"})
class MessageHelper extends BaseEventsHelper {
    private static final String TAG = "MessageHelper";

    private static final String FPS_SUBDIR = "fps";
    static final String BOT_PREFIX = "бот, ";
    private static long fasID = -1;
    private static long muteID = -1;
    private static String userToFas = null;
    private static String userToMute = null;

    static final String CMD_AGR = "агрись на ";
    static final String CMD_CANCEL = "хватит";
    static final String CMD_MUTE = "выключи ";
    static final String CMD_RATES = "курс";
    static final String CMD_TRANSLATE = "переведи";

    //TODO: add others
    static final String HNTR = "хнтр";

    static final String BOT = "@pbelov_bot";

    private MessageHelper() {
    }

    static void handleArgMessage(TextMessageReceivedEvent event) {
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

    static void handleMuteMessage(TextMessageReceivedEvent event) {
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

    static void handleCancelMessage(TextMessageReceivedEvent event) {
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
