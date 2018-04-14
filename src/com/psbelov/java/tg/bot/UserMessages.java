package com.psbelov.java.tg.bot;

import pro.zackpollard.telegrambot.api.user.User;

public class UserMessages {
    public User user;
    public String userName;
    public int messagesCount;

    public UserMessages(String key, int value) {
        userName = key;
        messagesCount = value;
    }

    @Override
    public String toString() {
        return userName + (": ") + messagesCount;
    }
}
