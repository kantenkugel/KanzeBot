package com.kantenkugel.discordbot.listener;

import com.kantenkugel.discordbot.commands.CommandRegistry;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.MessageUpdateEvent;
import net.dv8tion.jda.hooks.EventListener;

public class MessageListener implements EventListener {
    @Override
    public void onEvent(Event event) {
        if(event.getClass() == MessageReceivedEvent.class) {
            CommandRegistry.handle(new MessageEvent(((MessageReceivedEvent) event).getMessage(), event.getResponseNumber()));
        } else if(event.getClass() == MessageUpdateEvent.class) {
            CommandRegistry.handle(new MessageEvent(((MessageUpdateEvent) event).getMessage(), event.getResponseNumber()));
        }
    }
}
