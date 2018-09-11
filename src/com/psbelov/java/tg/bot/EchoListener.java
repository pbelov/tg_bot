package com.psbelov.java.tg.bot;

import pro.zackpollard.telegrambot.api.chat.message.Message;
import pro.zackpollard.telegrambot.api.event.Listener;
import pro.zackpollard.telegrambot.api.event.chat.message.CommandMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.PhotoMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;
import pro.zackpollard.telegrambot.api.user.User;

import java.io.IOException;
import java.util.Random;

import static com.psbelov.java.tg.bot.MessageHelper.BOT;
import static com.psbelov.java.tg.bot.MessageHelper.BOT_PREFIX;

@SuppressWarnings({"FieldCanBeLocal", "unchecked", "ResultOfMethodCallIgnored"})
public class EchoListener implements Listener {
    private final String TAG = "EchoListener";

    @Override
    public void onCommandMessageReceived(CommandMessageReceivedEvent event) {
        MessageHelper.getBaseData(event, "command");
        handleCommand(event);
    }

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {
        MessageHelper.getBaseData(event, "text message");
        handleMessage(event);
    }

    @Override
    public void onPhotoMessageReceived(PhotoMessageReceivedEvent event) {
        try {
            MessageHelper.analyzeImage(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(CommandMessageReceivedEvent event) {
        final String command = MessageHelper.getCommandsData(event);

        final String STATS = "stats";
        final String COMMAND_DAY = "day";
        final String COMMAND_ME = "me";
        final String COMMAND_FIND = "find";
        final String COMMAND_RATE = "rate";

        if (command.equals("time")) {
            MessageHelper.handleTimeCommand(event);
        } else if (command.equals("date")) {
            MessageHelper.handleDateCommand(event);
        } else if (command.startsWith(COMMAND_ME)) {
            MessageHelper.handleMeCommand(event);
        } else if (command.equals("ping")) {
            TgMsgUtil.replyInChat(event, "pong!");
        } else if (command.equals("top")) {
            MessageHelper.handleTopCommand(event);
        } else if (command.equals(COMMAND_DAY)) {
            MessageHelper.showDay(event);
        } else if (command.equals(STATS)) {
            MessageHelper.handleStatsCommand(event);
        } else if (command.equals(COMMAND_FIND)) {
            MessageHelper.find(event);
        } else if (command.equals(COMMAND_RATE)) {
            MessageHelper.getRates(event);
        }
    }

    private void handleMessage(TextMessageReceivedEvent event) {
        String messageText = MessageHelper.getMessagesData();

        if (messageText.startsWith(BOT_PREFIX)) {
            MessageHelper.handleBotCommands(event);
        }

        if (!MessageHelper.checkBotStatus()) {
            return;
        }

        MessageHelper.checkForBayan(event);
        MessageHelper.incrementMessagesNumberFor();

        final Random random = new Random(System.currentTimeMillis());
        MessageHelper.handleMorning(event, random);

        messageText = messageText.replace(BOT, "").trim().toLowerCase();
        if (messageText.startsWith(BOT_PREFIX)) {
            messageText = messageText.replace(BOT_PREFIX, "");
            Utils.println(TAG, "request [" + messageText + "]");
            if (messageText.startsWith(MessageHelper.CMD_AGR)) {
                MessageHelper.handleArgCommand(event);
            } else if (messageText.startsWith(MessageHelper.CMD_MUTE)) {
                MessageHelper.handleMuteCommand(event);
            } else if (messageText.startsWith(MessageHelper.CMD_CANCEL)) {
                MessageHelper.handleCancelCommand(event);
            } else if (messageText.equals("котиков!")) {
                MessageHelper.handleCatsCommand(event);
            } else if (messageText.startsWith(MessageHelper.CMD_FIND_TEXT)) {
                MessageHelper.find(event);
            } else if (messageText.equals(MessageHelper.CMD_RATES)) {
                MessageHelper.getRates(event);
            } else if (messageText.equals(MessageHelper.CMD_TRANSLATE)) {
                MessageHelper.translateMessage(event);
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
            } else if (messageText.startsWith(MessageHelper.HNTR)) {
                MessageHelper.handleHntrInvocation(event);
            }
        }

        if (MessageHelper.getPartOfDay() == MessageHelper.getPartOfDayInt(messageText)) {
            TgMsgUtil.replyInChat(event, "д.");
        } else if (MessageHelper.getPartOfDayInt(messageText) != -1) {
            TgMsgUtil.replyInChat(event, "нт.");
        }

        MessageHelper.handleFasAndMuted(event, random);
    }
}
