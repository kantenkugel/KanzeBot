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
