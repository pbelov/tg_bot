package com.psbelov.java.tg.bot;

import com.psbelov.java.tg.bot.Utils.TgMsgUtil;
import com.psbelov.java.tg.bot.Utils.Utils;
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
        BaseEventsHelper.getBaseData(event, "command");
        handleCommand(event);
    }

    @Override
    public void onTextMessageReceived(TextMessageReceivedEvent event) {
        BaseEventsHelper.getBaseData(event, "text message");
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
        final String command = CommandsHelper.getCommandsData(event);

        final String STATS = "stats";
        final String COMMAND_DAY = "day";
        final String COMMAND_ME = "me";
        final String COMMAND_FIND = "find";
        final String COMMAND_RATE = "rate";

        if (command.equals("time")) {
            CommandsHelper.handleTimeCommand(event);
        } else if (command.equals("date")) {
            CommandsHelper.handleDateCommand(event);
        } else if (command.startsWith(COMMAND_ME)) {
            CommandsHelper.handleMeCommand(event);
        } else if (command.equals("ping")) {
            TgMsgUtil.replyInChat(event, "pong!");
        } else if (command.equals("top")) {
            CommandsHelper.handleTopCommand(event);
        } else if (command.equals(COMMAND_DAY)) {
            CommandsHelper.showDay(event);
        } else if (command.equals(STATS)) {
            CommandsHelper.handleStatsCommand(event);
        } else if (command.equals(COMMAND_FIND)) {
            CommandsHelper.find(event);
        } else if (command.equals(COMMAND_RATE)) {
            CommandsHelper.getRates(event);
        }
    }

    private void handleMessage(TextMessageReceivedEvent event) {
        String messageText = MessageHelper.getMessagesData();

        if (messageText.startsWith(BOT_PREFIX)) {
            MessageHelper.handleBotCommands(event);
        }

        if (!BaseEventsHelper.checkBotStatus()) {
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
                MessageHelper.handleArgMessage(event);
            } else if (messageText.startsWith(MessageHelper.CMD_MUTE)) {
                MessageHelper.handleMuteMessage(event);
            } else if (messageText.startsWith(MessageHelper.CMD_CANCEL)) {
                MessageHelper.handleCancelMessage(event);
            } else if (messageText.equals("котиков!")) {
                BaseEventsHelper.handleCatsMessage(event);
            } else if (messageText.startsWith(MessageHelper.CMD_FIND_TEXT)) {
                CommandsHelper.find(event);
            } else if (messageText.equals(MessageHelper.CMD_RATES)) {
                CommandsHelper.getRates(event);
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
