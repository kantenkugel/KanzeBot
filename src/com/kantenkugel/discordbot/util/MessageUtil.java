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
        reply(event, txt, true);
    }

    public static void reply(MessageReceivedEvent event, String txt, boolean addName) {
        if(event.isPrivate()) {
            event.getPrivateChannel().sendMessage(txt);
        } else {
            MessageBuilder mb = new MessageBuilder();
            if(addName) {
                mb.appendString(event.getAuthor().getUsername()).appendString(": ");
            }
            event.getTextChannel().sendMessage(mb.appendString(txt).build());
        }
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg, int limit) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+", limit);
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+");
    }

}
