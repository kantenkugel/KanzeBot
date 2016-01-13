/**
 * Copyright 2015-2016 Austin Keener & Michael Ritter
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kantenkugel.discordbot;

import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

public class TestMain extends ListenerAdapter {
    public static void main(String[] args) {
        String[] splits = "this is only a test, bla".split("\\s+");
        System.out.println(StringUtils.join(splits, '-', 2, splits.length));
//        try {
//            new JDABuilder("EMAIL", "PASSWORD").addListener(new TestMain()).build();
//        } catch(LoginException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("EVENT GOTTEN");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.isPrivate()) {
            System.out.println("[PM][" + event.getAuthor().getUsername() + "]: " + event.getMessage().getContent());
        } else {
            System.out.printf("[%s][%s][%s]: %s\n", event.getGuild().getName(),
                    event.getTextChannel().getName(), event.getAuthor().getUsername(), event.getMessage().getContent());
        }
    }
}
