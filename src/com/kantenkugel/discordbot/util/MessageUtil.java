package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class MessageUtil {
    public static boolean isGlobalAdmin(User user) {
        return user.getId().equals("122758889815932930") || user.getId().equals("107562988810027008");
    }

    public static void reply(MessageReceivedEvent event, String txt) {
        if(event.isPrivate()) {
            event.getPrivateChannel().sendMessage(txt);
        } else {
            event.getTextChannel().sendMessage(new MessageBuilder().appendString(event.getAuthor().getUsername()).appendString(": ").appendString(txt).build());
        }
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg, int limit) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+", limit);
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+");
    }

}
