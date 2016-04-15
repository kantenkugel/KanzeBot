/*
 * Copyright 2016 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
