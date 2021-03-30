package com.colosas.skywars.objects;

public class ParsedMessage {

    private final String subChannel;
    private final String[] messages;

    public ParsedMessage(String subChannel, String ...messages) {
        this.subChannel = subChannel;
        this.messages = messages;
    }

    public String[] getMessages() {
        return messages;
    }

    public String getSubChannel() {
        return subChannel;
    }
}
