package com.kantenkugel.discordbot.listener;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.*;

public class MessageEvent {
    private final Message msg;
    private final int responseNumber;

    public MessageEvent(Message msg, int responseNumber) {
        this.msg = msg;
        this.responseNumber = responseNumber;
    }

    public boolean isEdit() {
        return msg.isEdited();
    }

    public boolean isPrivate() {
        return msg.isPrivate();
    }

    public User getAuthor() {
        return msg.getAuthor();
    }

    public Message getMessage() {
        return msg;
    }

    public MessageChannel getChannel() {
        return isPrivate() ? msg.getJDA().getPrivateChannelById(msg.getChannelId()) : msg.getJDA().getTextChannelById(msg.getChannelId());
    }

    public TextChannel getTextChannel() {
        return isPrivate() ? null : (TextChannel) getChannel();
    }

    public Guild getGuild() {
        return isPrivate() ? null : getTextChannel().getGuild();
    }

    public PrivateChannel getPrivateChannel() {
        return isPrivate() ? (PrivateChannel) getChannel() : null;
    }

    public String getContent() {
        return msg.getContent();
    }

    public JDA getJDA() {
        return msg.getJDA();
    }

    public int getResponseNumber() {
        return responseNumber;
    }
}
