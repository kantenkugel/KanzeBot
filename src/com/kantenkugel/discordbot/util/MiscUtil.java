package com.kantenkugel.discordbot.util;

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

    public static OffsetDateTime getDateTimeFromStamp(String timestamp) {
        return OffsetDateTime.parse(timestamp);
    }
}
