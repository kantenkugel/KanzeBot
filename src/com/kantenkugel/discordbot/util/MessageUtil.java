package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class MessageUtil {
    private static final Set<String> globalAdmins = new HashSet<>();
    static {
        globalAdmins.add("122758889815932930");
        globalAdmins.add("107562988810027008");
        globalAdmins.add("107490111414882304");
    }

    public static boolean isGlobalAdmin(User user) {
        return globalAdmins.contains(user.getId());
    }

    public static Set<String> getGlobalAdmins() {
        return Collections.unmodifiableSet(globalAdmins);
    }

    public static boolean reply(MessageEvent event, ServerConfig config, String txt) {
        return reply(event, config, txt, true);
    }

    public static boolean reply(MessageEvent event, ServerConfig config, String txt, boolean addName) {
        MessageBuilder mb = new MessageBuilder();
        if(!event.isPrivate() && addName) {
            mb.appendString(event.getAuthor().getUsername()).appendString(": ");
        }
        mb.appendString(config.isAllowEveryone() ? txt : strip(txt));
        return reply(event, mb.build());
    }


    public static boolean reply(MessageEvent event, Message message) {
        if(!event.isPrivate() && !event.getTextChannel().checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE)) {
            return false;
        }
        event.getChannel().sendMessageAsync(message, null);
        return true;
    }

    public static boolean replySync(MessageEvent event, ServerConfig cfg, String text) {
        return replySync(event, cfg, text, true);
    }

    public static boolean replySync(MessageEvent event, ServerConfig cfg, String text, boolean addName) {
        if(!event.isPrivate() && !event.getTextChannel().checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE)) {
            return false;
        }
        MessageBuilder mb = new MessageBuilder();
        if(!event.isPrivate() && addName) {
            mb.appendString(event.getAuthor().getUsername()).appendString(": ");
        }
        mb.appendString(cfg.isAllowEveryone() ? text : strip(text));
        event.getChannel().sendMessage(mb.build());
        return true;
    }

    public static String[] getArgs(MessageEvent event, ServerConfig cfg, int limit) {
        return event.getContent().substring(cfg.getPrefix().length()).split("\\s+", limit);
    }

    public static String[] getArgs(MessageEvent event, ServerConfig cfg) {
        return event.getContent().substring(cfg.getPrefix().length()).split("\\s+");
    }

    public static String strip(String in) {
        return in.replace("@everyone", "@\u180Eeveryone");
    }

}
