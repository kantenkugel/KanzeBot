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

package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.DbEngine;
import com.kantenkugel.discordbot.Statics;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.impl.JDAImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.OffsetDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michael Ritter on 07.12.2015.
 */
public class MiscUtil {
    private static final Pattern timeRegex = Pattern.compile("^(?:(?<hours>\\d*)h)?(?:(?<mins>\\d*)m)?(?:(?<secs>\\d*)s)?$");

    public static OffsetDateTime getOffsettedTime(String text) {
        Matcher matcher = timeRegex.matcher(text);
        if(matcher.matches()) {
            OffsetDateTime dateTime = OffsetDateTime.now();
            if(matcher.group("hours") != null) {
                dateTime = dateTime.minusHours(Long.parseLong(matcher.group("hours")));
            }
            if(matcher.group("mins") != null) {
                dateTime = dateTime.minusMinutes(Long.parseLong(matcher.group("mins")));
            }
            if(matcher.group("secs") != null) {
                dateTime = dateTime.minusSeconds(Long.parseLong(matcher.group("secs")));
            }
            return dateTime;
        }
        return null;
    }

    public static InputStream getDataStream(String url) {
        try {
            URL u = new URL(url);
            URLConnection urlConnection = u.openConnection();
            urlConnection.setRequestProperty("user-agent", "KanzeBot DiscordBot (https://github.com/Kantenkugel/KanzeBot, " + Statics.VERSION + ')');
            urlConnection.setRequestProperty("authorization", Statics.jdaInstance.getAuthToken());
            return urlConnection.getInputStream();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OffsetDateTime getDateTimeFromStamp(String timestamp) {
        return OffsetDateTime.parse(timestamp);
    }

    public static String getUptime() {
        long diff = System.currentTimeMillis()- Statics.START_TIME;
        diff = diff/1000; //to s
        long days = diff/86400;
        long hrs = (diff%86400)/3600;
        long mins = (diff%3600)/60;
        long secs = diff%60;
        return String.format("%dd %dh %dm %ds", days, hrs, mins, secs);
    }

    public static void await(JDA api, Runnable runnable) {
        Thread thread = new Thread(() -> {
            JDAImpl jda = (JDAImpl) api;
            while(jda.getClient().isConnected()) {
                try {
                    Thread.sleep(100);
                } catch(InterruptedException ignored) {
                }
            }
            runnable.run();
        });
        thread.setDaemon(false);
        thread.start();
    }

    public static void shutdown(int code) {
        await(Statics.jdaInstance, () -> {
            DbEngine.close();
            System.exit(code);
        });
        Statics.jdaInstance.shutdown();
    }
}
