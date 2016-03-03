package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.Main;
import com.kantenkugel.discordbot.commands.CommandRegistry;
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
            urlConnection.setRequestProperty("user-agent", "KanzeBot DiscordBot (https://github.com/Kantenkugel/KanzeBot, " + Main.VERSION + ')');
            urlConnection.setRequestProperty("authorization", Main.api.getAuthToken());
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
        long diff = System.currentTimeMillis()- CommandRegistry.START_TIME;
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

    public static void shutdown() {
        System.exit(UpdateWatcher.NORMAL_EXIT_CODE);
    }

    public static void restart() {
        System.exit(UpdateWatcher.RESTART_CODE);
    }

    public static void update() {
        System.exit(UpdateWatcher.UPDATE_EXIT_CODE);
    }
}
