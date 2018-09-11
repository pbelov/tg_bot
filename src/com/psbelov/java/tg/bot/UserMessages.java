package com.psbelov.java.tg.bot;

public class UserMessages {
    String userName;
    int messagesCount;

    UserMessages(String key, int value) {
        userName = key;
        messagesCount = value;
    }

    @Override
    public String toString() {
        return userName + (": ") + messagesCount;
    }
}
