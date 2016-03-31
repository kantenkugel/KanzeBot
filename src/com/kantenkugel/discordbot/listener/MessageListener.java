package com.kantenkugel.discordbot.listener;

import com.kantenkugel.discordbot.DbEngine;
import com.kantenkugel.discordbot.commands.CommandRegistry;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.message.MessageDeleteEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.MessageUpdateEvent;
import net.dv8tion.jda.hooks.EventListener;

public class MessageListener implements EventListener {
    @Override
    public void onEvent(Event event) {
        if(event.getClass() == MessageReceivedEvent.class) {
            MessageEvent e = new MessageEvent(((MessageReceivedEvent) event).getMessage(), event.getResponseNumber());
            DbEngine.handleMessage(e);
            CommandRegistry.handle(e);
        } else if(event.getClass() == MessageUpdateEvent.class) {
            MessageEvent e = new MessageEvent(((MessageUpdateEvent) event).getMessage(), event.getResponseNumber());
            DbEngine.handleMessage(e);
            CommandRegistry.handle(e);
        } else if(event.getClass() == MessageDeleteEvent.class) {
            MessageDeleteEvent e = (MessageDeleteEvent) event;
            DbEngine.deleteMessage(e.getMessageId());
        }
    }
}
