package com.psbelov.java.tg.bot;

import pro.zackpollard.telegrambot.api.chat.message.send.SendableTextMessage;
import pro.zackpollard.telegrambot.api.event.chat.message.TextMessageReceivedEvent;

/**
 * Created by pbelov on 10/10/2016.
 */
public class TgMsgUtil {
    public static void replyInChat(TextMessageReceivedEvent event, String message) {
        SendableTextMessage sendableMessage = SendableTextMessage.builder()
                .message(message)
                .replyTo(event.getMessage())
                .build();
        event.getChat().sendMessage(sendableMessage);
    }

    public static void sendToChat(TextMessageReceivedEvent event, String message) {
        SendableTextMessage sendableMessage = SendableTextMessage.builder()
                .message(message)
                .build();
        event.getChat().sendMessage(sendableMessage);
    }
}
