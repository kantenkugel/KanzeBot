package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class MessageUtil {
    public static boolean isGlobalAdmin(User user) {
        return user.getId().equals("122758889815932930") || user.getId().equals("107562988810027008") || user.getId().equals("107490111414882304");
    }

    public static boolean reply(MessageReceivedEvent event, ServerConfig config, String txt) {
        return reply(event, config, txt, true);
    }

    public static boolean reply(MessageReceivedEvent event, ServerConfig config, String txt, boolean addName) {
        return reply(event, txt, addName, !config.isAllowEveryone());
    }

    public static boolean reply(MessageReceivedEvent event, String txt, boolean addName, boolean doStrip) {
        MessageBuilder mb = new MessageBuilder();
        if(!event.isPrivate() && addName) {
            mb.appendString(event.getAuthor().getUsername()).appendString(": ");
        }
        mb.appendString(doStrip ? strip(txt) : txt);
        return reply(event, mb.build());
    }

    public static boolean reply(MessageReceivedEvent event, Message message) {
        if(!event.isPrivate() && !event.getTextChannel().checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE)) {
            return false;
        }
        event.getChannel().sendMessage(message);
        return true;
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg, int limit) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+", limit);
    }

    public static String[] getArgs(MessageReceivedEvent event, ServerConfig cfg) {
        return event.getMessage().getContent().substring(cfg.getPrefix().length()).split("\\s+");
    }

    public static String strip(String in) {
        return in.replace("@everyone", "@\u180Eeveryone");
    }

}
