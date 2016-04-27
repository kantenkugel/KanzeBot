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
import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.config.BotConfig;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.events.channel.text.TextChannelUpdateNameEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.guild.GuildUpdateEvent;
import net.dv8tion.jda.events.message.MessageDeleteEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.events.message.MessageUpdateEvent;
import net.dv8tion.jda.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.hooks.EventListener;

public class DbListener implements EventListener {

    @Override
    public void onEvent(Event event) {
        if(event.getClass() == ReadyEvent.class) {                          //Init
            DbEngine.handleGuilds(event.getJDA().getGuilds());
        } else if(event.getClass() == ReconnectedEvent.class) {             //Reinit
            DbEngine.handleGuilds(event.getJDA().getGuilds());
        } else if(event.getClass() == UserNameUpdateEvent.class) {          //username
            DbEngine.updateUser(((UserNameUpdateEvent) event).getUser());
        } else if(event.getClass() == GuildJoinEvent.class) {               //Guild Join
            DbEngine.updateGuild(((GuildJoinEvent) event).getGuild());
        } else if(event.getClass() == GuildLeaveEvent.class) {              //Guild Leave
            DbEngine.deleteGuild(((GuildLeaveEvent) event).getGuild());
        } else if(event.getClass() == GuildUpdateEvent.class) {             //Guild Update
            DbEngine.updateGuild(((GuildUpdateEvent) event).getGuild());
        } else if(event.getClass() == TextChannelCreateEvent.class) {       //TextChannel Create
            DbEngine.updateChannel(((TextChannelCreateEvent) event).getChannel());
        } else if(event.getClass() == TextChannelDeleteEvent.class) {       //TextChannel Delete
            DbEngine.deleteChannel(((TextChannelDeleteEvent) event).getChannel());
        } else if(event.getClass() == TextChannelUpdateNameEvent.class) {   //TextChannel Name-update
            DbEngine.updateChannel(((TextChannelUpdateNameEvent) event).getChannel());
        } else if(event.getClass() == MessageReceivedEvent.class) {         //Message Received
            MessageEvent e = new MessageEvent(((MessageReceivedEvent) event).getMessage(), event.getResponseNumber());

            if(!e.isPrivate() &&  e.getContent().startsWith("-kbhistory")
                    && (MessageUtil.isGlobalAdmin(e.getAuthor()) || e.getGuild().getOwner() == e.getAuthor())) {
                MessageUtil.reply(e, new MessageBuilder().appendString("History-link: ")
                        .appendString(BotConfig.get("historyBase"))
                        .appendString(Long.toString(
                                DbEngine.createHistory(
                                        e.getMessage().getMentionedUsers().size() == 0 ? e.getAuthor() : e.getMessage().getMentionedUsers().get(0)
                                        , e.getTextChannel()))).build());
            } else if(MessageUtil.isGlobalAdmin(e.getAuthor()) && e.getContent().equals("-kbshutdown")) {
                MiscUtil.shutdown(Statics.NORMAL_EXIT_CODE);
            }

            DbEngine.handleMessage(e);
        } else if(event.getClass() == MessageUpdateEvent.class) {           //Message Updated
            MessageEvent e = new MessageEvent(((MessageUpdateEvent) event).getMessage(), event.getResponseNumber());
            DbEngine.handleMessage(e);
        } else if(event.getClass() == MessageDeleteEvent.class) {           //Message Deleted
            MessageDeleteEvent e = (MessageDeleteEvent) event;
            DbEngine.deleteMessage(e.getMessageId());
        }
    }
}
