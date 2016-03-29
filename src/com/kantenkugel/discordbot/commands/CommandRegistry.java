package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.commands.sections.CommandSection;
import com.kantenkugel.discordbot.config.BlackList;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.util.ClassEnumerator;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.utils.SimpleLog;

import java.util.HashMap;
import java.util.Map;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandRegistry {
    private static final Map<String, Command> commands = new HashMap<>();
    public static final Map<String, ServerConfig> serverConfigs = new HashMap<>();

    private static final SimpleLog pmLog = SimpleLog.getLog("PM");
    private static final SimpleLog mentionLog = SimpleLog.getLog("Mention");
    private static final SimpleLog commandLog = SimpleLog.getLog("Command");

    private static long msgCount = 0;
    private static int cmdCount = 0;

    public static void loadCommands(JDA api) {
        commands.clear();
        ClassEnumerator.getClassesForPackage(CommandSection.class.getPackage()).stream()
                .filter(aClass -> CommandSection.class.isAssignableFrom(aClass) && !aClass.equals(CommandSection.class))
                .forEach(aClass -> {
            @SuppressWarnings("unchecked")
            Class<? extends CommandSection> sectionClass = (Class<? extends CommandSection>) aClass;
            try {
                CommandSection section = sectionClass.newInstance();
                section.register(commands, api);
            } catch(InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public static void handle(MessageEvent event) {
        msgCount++;

        //load correct config-file
        ServerConfig cfg;
        if(!event.isPrivate()) {
            cfg = serverConfigs.get(event.getGuild().getId());
            if(event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()) || event.getMessage().getMentionedUsers().contains(Statics.botOwner)) {
                mentionLog.info(String.format("[%s][%s] %s:%s", event.getGuild().getName(), event.getTextChannel().getName(),
                        event.getAuthor().getUsername(), event.getContent()));
            }
        } else {
            cfg = ServerConfig.PMConfig.getInstance(event.getJDA());
            if(event.getAuthor() != event.getJDA().getSelfInfo())
                pmLog.info(event.getAuthor().getUsername() + ": " + event.getContent());
        }


        //let modules handle the message and break if requested
        if(cfg.getModules().values().stream().map(m -> m.handle(event, cfg)).anyMatch(b -> b)) {
            return;
        }

        //stop if message is not created or user is blacklisted
        if(event.isEdit() || BlackList.contains(event.getAuthor())) {
            return;
        }

        //handle hard-coded commands
        if(event.getMessage().getContent().equals("-kbreset") && cfg.isOwner(event.getAuthor())) {
            cfg.setPrefix(ServerConfig.DEFAULT_PREFIX);
            reply(event, cfg, "Prefix was reset to default (" + ServerConfig.DEFAULT_PREFIX + ")");
            return;
        }
        if(event.getMessage().getContent().equals("-kbprefix")) {
            reply(event, cfg, "Current command-prefix is: `" + cfg.getPrefix() + '`');
            return;
        }
        if(event.isPrivate() && event.getContent().equalsIgnoreCase("help")) {
            commands.get("help").accept(event, cfg);
            return;
        }

        //Handle registered commands
        if(event.getContent().startsWith(cfg.getPrefix())) {
            String[] args = MessageUtil.getArgs(event, cfg);
            if(commands.containsKey(args[0])) {
                if(commands.get(args[0]).isAvailable(event, cfg)) {
                    cmdCount++;
                    commandLog.info(String.format("[%s][%s] %s: %s", event.isPrivate() ? "PM" : event.getGuild().getName(),
                            event.isPrivate() ? event.getAuthor().getUsername() : event.getTextChannel().getName(),
                            event.getAuthor().getUsername(), event.getMessage().getContent().substring(cfg.getPrefix().length())));
                    commands.get(args[0]).accept(event, cfg);
                }

            } else if(cfg.getCommands().containsKey(args[0])) {
                if(cfg.getCommands().get(args[0]).isAvailable(event, cfg)) {
                    cmdCount++;
                    commandLog.info(String.format("[%s][%s] %s: %s", event.isPrivate() ? "PM" : event.getGuild().getName(),
                            event.isPrivate() ? event.getAuthor().getUsername() : event.getTextChannel().getName(),
                            event.getAuthor().getUsername(), event.getMessage().getContent().substring(cfg.getPrefix().length())));
                    cfg.getCommands().get(args[0]).accept(event, cfg);
                }
            } else if(!event.isPrivate()) {
                if(cfg.isRestrictTexts() && !cfg.isMod(event.getAuthor())) {
                    //texts only available to mods
                    return;
                }
                Map<String, String> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(args[0])) {
                    reply(event, cfg, textCommands.get(args[0]));
                }
            }
        }
    }

    public static long getMessageCount() {
        return msgCount;
    }

    public static int getCommandCount() {
        return cmdCount;
    }
}
