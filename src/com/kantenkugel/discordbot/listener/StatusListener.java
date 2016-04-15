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
import com.kantenkugel.discordbot.commands.CommandRegistry;
import com.kantenkugel.discordbot.config.BotConfig;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.hooks.EventListener;

public class StatusListener implements EventListener {
    @Override
    public void onEvent(Event event) {
        if(event.getClass() == ReadyEvent.class) {
            onReady((ReadyEvent) event);
        } else if(event.getClass() == ReconnectedEvent.class) {
            onReconnect((ReconnectedEvent) event);
        } else if(event.getClass() == GuildJoinEvent.class) {
            onGuildJoin((GuildJoinEvent) event);
        } else if(event.getClass() == GuildLeaveEvent.class) {
            onGuildLeave((GuildLeaveEvent) event);
        } else if(event.getClass() == UserNameUpdateEvent.class) {
            DbEngine.updateUser(((UserNameUpdateEvent) event).getUser());
        }
    }

    public void onReady(ReadyEvent event) {
        initVars(event.getJDA());
    }

    public void onReconnect(ReconnectedEvent event) {
        initVars(event.getJDA());
    }

    private void initVars(JDA jda) {
        jda.getAccountManager().setGame("JDA");
        CommandRegistry.serverConfigs.clear();
        for(Guild guild : jda.getGuilds()) {
            CommandRegistry.serverConfigs.put(guild.getId(), new ServerConfig(jda, guild));
        }
        Statics.botOwner = jda.getUserById(BotConfig.get("ownerId"));
        updateCarbon();
    }

    public void onGuildJoin(GuildJoinEvent event) {
        Statics.LOG.info("Joined Guild " + event.getGuild().getName());
        CommandRegistry.serverConfigs.put(event.getGuild().getId(), new ServerConfig(event.getJDA(), event.getGuild()));
        updateCarbon();
    }

    public void onGuildLeave(GuildLeaveEvent event) {
        Statics.LOG.info("Left Guild " + event.getGuild().getName());
        CommandRegistry.serverConfigs.remove(event.getGuild().getId());
        updateCarbon();
    }

    private static void updateCarbon() {
        String carbonKey = BotConfig.get("carbonKey");
        if(carbonKey == null || carbonKey.trim().isEmpty()) {
            return;
        }
        try {
            Unirest.post("https://www.carbonitex.net/discord/data/botdata.php").field("key", carbonKey).field("servercount", Statics.jdaInstance.getGuilds().size()).asString();
        } catch(UnirestException e) {
            e.printStackTrace();
        }
    }
}
