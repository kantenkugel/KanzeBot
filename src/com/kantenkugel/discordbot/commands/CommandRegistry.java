package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.events.InviteReceivedEvent;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.InviteUtil;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandRegistry extends ListenerAdapter {
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<String, ServerConfig> serverConfigs = new HashMap<>();

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");

    public static void init() {
        loadCommands();
    }

    public static void setJDA(JDA jda) {
        engine.put("api", jda);
    }

    private static void loadCommands() {
        commands.clear();
        EveCommands.init(commands);
        commands.put("config", new CommandWrapper((m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg);
            if(args.length == 1) {
                MessageUtil.reply(m, "Available subcommands: prefix, restrictTexts, leave, admins, mods\nTo get more details, run " + cfg.getPrefix() + args[0] + " SUBCOMMAND");
            } else if(args.length > 1) {
                switch(args[1].toLowerCase()) {
                    case "prefix":
                        if(args.length == 2) {
                            MessageUtil.reply(m, "This command modifies the prefix used to call commands of this bot." +
                                    "\nCurrent Prefix: " + cfg.getPrefix() +
                                    "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " PREFIX");
                        } else {
                            cfg.setPrefix(args[2]);
                            MessageUtil.reply(m, "Prefix changed to " + args[2]);
                        }
                        break;
                    case "restricttexts":
                        if(args.length == 2) {
                            MessageUtil.reply(m, "This command changes the behavior of text-commands." +
                                    "\nIf restrictTexts is set to true, only mods can call the text-commands" +
                                    "\nIf set to false, everyone can (default)" +
                                    "\nrestrictTexts is currently set to: " + cfg.isRestrictTexts() +
                                    "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false");
                        } else {
                            cfg.setRestrictTexts(Boolean.parseBoolean(args[2]));
                            MessageUtil.reply(m, "restrictTexts changed to " + cfg.isRestrictTexts());
                        }
                        break;
                    case "leave":
                        if(args.length == 2) {
                            MessageUtil.reply(m, "This will make the bot leave this server!" +
                                    "\nTo leave, call " + cfg.getPrefix() + args[0] + " " + args[1] + " YES");
                        } else if(args[2].equals("YES")) {
                            m.getGuild().leave();
                        }
                        break;
                    case "admins":
                        if(args.length < 4) {
                            MessageUtil.reply(m, "This will add/remove Users and/or Roles to the admin-set" +
                                    "\nAdmins have access to everything mods can, + access to the clear command (may change)" +
                                    "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                    "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        } else {
                            switch(args[2].toLowerCase()) {
                                case "adduser":
                                    m.getMessage().getMentionedUsers().forEach(cfg::addAdmin);
                                    MessageUtil.reply(m, "User(s) added as admin(s)");
                                    break;
                                case "removeuser":
                                    m.getMessage().getMentionedUsers().forEach(cfg::removeAdmin);
                                    MessageUtil.reply(m, "User(s) removed from admin(s)");
                                    break;
                                case "addrole":
                                    Optional<Role> any = m.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(args[3])).findAny();
                                    if(any.isPresent()) {
                                        cfg.addAdminRole(any.get());
                                        MessageUtil.reply(m, "Role " + any.get().getName() + " added as admin role");
                                    } else {
                                        MessageUtil.reply(m, "No role matching given name found");
                                    }
                                    break;
                                case "removerole":
                                    Optional<Role> anyremove = m.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(args[3])).findAny();
                                    if(anyremove.isPresent()) {
                                        cfg.removeAdminRole(anyremove.get());
                                        MessageUtil.reply(m, "Role " + anyremove.get().getName() + " removed from admin roles");
                                    } else {
                                        MessageUtil.reply(m, "No role matching given name found");
                                    }
                                    break;
                                default:
                                    MessageUtil.reply(m, "Invalid syntax");
                            }
                        }
                        break;
                    case "mods":
                        if(args.length < 4) {
                            MessageUtil.reply(m, "This will add/remove Users and/or Roles to the mods-set" +
                                    "\nMods have access to adding, removing and editing text-commands, and also calling them, when they were locked via the restrictTexts config" +
                                    "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                    "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        } else {
                            switch(args[2].toLowerCase()) {
                                case "adduser":
                                    m.getMessage().getMentionedUsers().forEach(cfg::addMod);
                                    MessageUtil.reply(m, "User(s) added as mod(s)");
                                    break;
                                case "removeuser":
                                    m.getMessage().getMentionedUsers().forEach(cfg::removeMod);
                                    MessageUtil.reply(m, "User(s) removed from mod(s)");
                                    break;
                                case "addrole":
                                    Optional<Role> any = m.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(args[3])).findAny();
                                    if(any.isPresent()) {
                                        cfg.addModRole(any.get());
                                        MessageUtil.reply(m, "Role " + any.get().getName() + " added as mod role");
                                    } else {
                                        MessageUtil.reply(m, "No role matching given name found");
                                    }
                                    break;
                                case "removerole":
                                    Optional<Role> anyremove = m.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(args[3])).findAny();
                                    if(anyremove.isPresent()) {
                                        cfg.removeModRole(anyremove.get());
                                        MessageUtil.reply(m, "Role " + anyremove.get().getName() + " removed from mod roles");
                                    } else {
                                        MessageUtil.reply(m, "No role matching given name found");
                                    }
                                    break;
                                default:
                                    MessageUtil.reply(m, "Invalid syntax");
                            }
                        }
                        break;
                    default:
                        MessageUtil.reply(m, "Invalid syntax");
                }
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.OWNER));
        commands.put("addcom", new CommandWrapper((m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 3) {
                if(commands.containsKey(args[1].toLowerCase())) {
                    MessageUtil.reply(m, "Command " + args[1] + " is reserved");
                } else {
                    String channelId = m.getTextChannel().getId();
                    Map<String, Map<String, String>> textCommands = cfg.getTextCommands();
                    if(!textCommands.containsKey(channelId)) {
                        textCommands.put(channelId, new HashMap<>());
                    }
                    Map<String, String> cmds = textCommands.get(channelId);
                    if(cmds.containsKey(args[1].toLowerCase())) {
                        MessageUtil.reply(m, "Command " + args[1] + " is already defined, edit it with !editcom");
                    } else {
                        cmds.put(args[1].toLowerCase(), args[2]);
                        cfg.save();
                        MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was created!");
                    }
                }
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("editcom", new CommandWrapper((m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 3) {
                String channelId = m.getTextChannel().getId();
                Map<String, Map<String, String>> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(channelId)) {
                    if(textCommands.get(channelId).containsKey(args[1].toLowerCase())) {
                        textCommands.get(channelId).put(args[1].toLowerCase(), args[2]);
                        cfg.save();
                        MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was created!");
                        return;
                    }
                }
                MessageUtil.reply(m, "Command " + args[1] + " is not defined");
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("delcom", new CommandWrapper((m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 2);
            if(args.length == 2) {
                String channelId = m.getTextChannel().getId();
                Map<String, Map<String, String>> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(channelId)) {
                    if(textCommands.get(channelId).containsKey(args[1].toLowerCase())) {
                        textCommands.get(channelId).remove(args[1].toLowerCase());
                        cfg.save();
                        MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was removed!");
                        return;
                    }
                }
                MessageUtil.reply(m, "Command " + args[1] + " is not defined");
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("help", new CommandWrapper((m, cfg) -> {
            Optional<String> reduce = commands.keySet().stream().filter(key -> commands.get(key).isAvailable(m, cfg)).sorted().map(orig -> cfg.getPrefix() + orig).reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                MessageUtil.reply(m, "Available Commands: " + reduce.get());
            } else {
                MessageUtil.reply(m, "No Commands available to you at this time in this chat");
            }
        }));
        commands.put("texts", new CommandWrapper((m, cfg) -> {
            if(cfg.getTextCommands().containsKey(m.getTextChannel().getId())) {
                Optional<String> reduce = cfg.getTextCommands().get(m.getTextChannel().getId()).keySet().stream()
                        .map(key -> cfg.getPrefix() + key).sorted().reduce((s1, s2) -> s1 + ", " + s2);
                if(reduce.isPresent()) {
                    MessageUtil.reply(m, "Defined Text-Commands: " + reduce.get());
                    return;
                }
            }
            MessageUtil.reply(m, "No Text-Commands defined for this channel");
        }).acceptCustom((m, cfg) -> !m.isPrivate() && (!cfg.isRestrictTexts() || cfg.isMod(m.getAuthor()))));
        commands.put("clear", new CommandWrapper((m, cfg) -> {
            if(!m.getTextChannel().checkPermission(m.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE)) {
                MessageUtil.reply(m, "I do not have permissions!");
                return;
            }
            String[] args = MessageUtil.getArgs(m, cfg);
            if(args.length == 1) {
                MessageHistory history = new MessageHistory(m.getJDA(), m.getTextChannel());
                List<Message> messages = history.retrieve();
                while(messages != null) {
                    messages.forEach(Message::deleteMessage);
                    messages = history.retrieve();
                }
            } else if(args.length == 2) {
                OffsetDateTime clearTo = MiscUtil.getOffsettedTime(args[1]);
                if(clearTo != null) {
                    MessageHistory history = new MessageHistory(m.getJDA(), m.getTextChannel());
                    List<Message> messages = history.retrieve();
                    while(messages != null) {
                        for(Message message : messages) {
                            if(message.getTime().isAfter(clearTo)) {
                                message.deleteMessage();
                            } else {
                                return;
                            }
                        }
                        messages = history.retrieve();
                    }
                }
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("eval", new CommandWrapper((m, cfg) -> {
            Message msg;
            engine.put("event", m);
            try {
                Object out = engine.eval("(function(){"
                        + m.getMessage().getContent().substring(cfg.getPrefix().length() + 5)
                        + "})();");
                msg = new MessageBuilder().appendString(out == null ? "Done!" : out.toString(), MessageBuilder.Formatting.BLOCK).build();
            } catch(Exception e) {
                msg = new MessageBuilder().appendString(e.getMessage(), MessageBuilder.Formatting.BLOCK).build();
            }
            if(m.isPrivate()) {
                m.getPrivateChannel().sendMessage(msg);
            } else {
                m.getTextChannel().sendMessage(msg);
            }
        }).acceptCustom((e, cfg) -> MessageUtil.isGlobalAdmin(e.getAuthor())));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        ServerConfig cfg;
        if(!event.isPrivate()) {
            cfg = serverConfigs.get(event.getTextChannel().getGuild().getId());
        } else {
            cfg = new ServerConfig.PMConfig();
        }

        if(event.getMessage().getContent().startsWith(cfg.getPrefix())) {
            String[] args = MessageUtil.getArgs(event, cfg);
            if(commands.containsKey(args[0])) {
                if(commands.get(args[0]).isAvailable(event, cfg)) {
                    commands.get(args[0]).accept(event, cfg);
                }

            } else if(!event.isPrivate()) {
                if(cfg.isRestrictTexts() && !cfg.isMod(event.getAuthor())) {
                    //texts only available to mods
                    return;
                }
                String channelId = event.getTextChannel().getId();
                Map<String, Map<String, String>> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(channelId)) {
                    Map<String, String> groupcmds = textCommands.get(channelId);
                    if(groupcmds.containsKey(args[0])) {
                        MessageUtil.reply(event, groupcmds.get(args[0]));
                    }
                }
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getAccountManager().setGame("JDA").update();
        for(Guild guild : event.getJDA().getGuilds()) {
            serverConfigs.put(guild.getId(), new ServerConfig(event.getJDA(), guild));
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        serverConfigs.put(event.getGuild().getId(), new ServerConfig(event.getJDA(), event.getGuild()));
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        serverConfigs.remove(event.getGuild().getId());
    }

    @Override
    public void onInviteReceived(InviteReceivedEvent event) {
        if(event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo())
                && event.getJDA().getGuildById(event.getInvite().getGuildId()) == null) {
            InviteUtil.join(event.getInvite(), event.getJDA());
            String text = "Joined Guild! Server owner should probably configure me via the config command\nDefault prefix is: " + ServerConfig.DEFAULT_PREFIX;
            if(event.getMessage().isPrivate()) {
                event.getJDA().getPrivateChannelById(event.getMessage().getChannelId()).sendMessage(text);
            } else {
                event.getJDA().getTextChannelById(event.getMessage().getChannelId()).sendMessage(text);
            }
        }
    }

    public static Map<String, ServerConfig> getServerConfigs() {
        return serverConfigs;
    }
}
