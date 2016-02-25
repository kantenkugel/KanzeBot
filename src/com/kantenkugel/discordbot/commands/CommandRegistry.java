package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import com.kantenkugel.discordbot.util.TaskHelper;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.InviteReceivedEvent;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.utils.InviteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.List;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandRegistry extends ListenerAdapter {
    public static long START_TIME = System.currentTimeMillis();
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<String, ServerConfig> serverConfigs = new HashMap<>();

    private static User kantenkugel;

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");

    public static void init() {
        loadCommands();
        engine.put("commands", commands);
        engine.put("Command", CustomCommand.class);
    }

    public static void setJDA(JDA jda) {
        engine.put("api", jda);
    }

    private static void loadCommands() {
        commands.clear();
        commands.put("config", new CommandWrapper("Allows the server-owner (you) to configure different parts of this bot. To see more detailed help, call it without arguments"
                ,CommandRegistry::config)
                .acceptPrivate(false).acceptPriv(Command.Priv.OWNER));
        commands.put("addcom", new CommandWrapper("Adds a new text-command (see command \"`texts`\").\n" +
                "Usage: `addcom NAME TEXT` with NAME being the name/key of the command and TEXT being the response.", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 3) {
                if(commands.containsKey(args[1].toLowerCase())) {
                    MessageUtil.reply(m, "Command " + args[1] + " is reserved");
                } else {
                    Map<String, String> textCommands = cfg.getTextCommands();
                    if(textCommands.containsKey(args[1].toLowerCase())) {
                        MessageUtil.reply(m, "Command " + args[1] + " is already defined, edit it with !editcom");
                    } else {
                        textCommands.put(args[1].toLowerCase(), args[2]);
                        cfg.save();
                        MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was created!");
                    }
                }
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("editcom", new CommandWrapper("Edits a already existing text-command (see command \"`texts`\").\n" +
                "Usage: `editcom NAME NEW_TEXT`", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 3) {
                Map<String, String> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(args[1].toLowerCase())) {
                    textCommands.put(args[1].toLowerCase(), args[2]);
                    cfg.save();
                    MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was edited!");
                    return;
                }
                MessageUtil.reply(m, "Command " + args[1] + " is not defined");
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("delcom", new CommandWrapper("Deletes a already existing text-command (see command \"`texts`\").\n" +
                "Usage: `delcom NAME`", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 2);
            if(args.length == 2) {
                Map<String, String> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(args[1].toLowerCase())) {
                    textCommands.remove(args[1].toLowerCase());
                    cfg.save();
                    MessageUtil.reply(m, "Command " + args[1].toLowerCase() + " was removed!");
                    return;
                }
                MessageUtil.reply(m, "Command " + args[1] + " is not defined");
            }
        }).acceptPriv(Command.Priv.MOD).acceptPrivate(false));
        commands.put("help", new CommandWrapper("HELP ME WITH HELP", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length > 1) {
                Command command = commands.get(args[1].toLowerCase());
                if(command == null) {
                    command = cfg.getCommands().get(args[1].toLowerCase());
                }
                if(command == null || !command.isAvailable(m, cfg)) {
                    MessageUtil.reply(m, "Provided Command does not exist or is not available to you!");
                } else {
                    MessageUtil.reply(m, "Help for " + args[1].toLowerCase() + ":\n" + command.getDescription(), false);
                }
                return;
            }
            String returned;
            Optional<String> reduce = commands.keySet().stream().filter(key -> commands.get(key).isAvailable(m, cfg)).sorted().map(orig -> cfg.getPrefix() + orig).reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                returned = "Available Commands: " + reduce.get();
            } else {
                returned = "No Commands available to you at this time in this chat";
            }
            reduce = cfg.getCommands().entrySet().stream().filter(entry -> entry.getValue().isAvailable(m, cfg)).map(entry -> cfg.getPrefix() + entry.getKey()).reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                returned += "\nAvailable through modules: " + reduce.get();
            }
            MessageUtil.reply(m, returned);
            if(m.isPrivate()) {
                MessageUtil.reply(m, "In Guilds, my commands may be prefixed differently (standard prefix in guilds is -kb instead of !)\n" +
                        "There are also 2 special commands: `-kbreset` resets the guild-prefix to default (-kb) and `-kbprefix` prints the current prefix of the guild. " +
                        "These special commands work in every guild, independent of the configured prefix.");
            }
        }));
        commands.put("texts", new CommandWrapper("Shows all available text-commands. (to add/edit/remove them, call addcom/editcom/delcom [requires mod-status])", (m, cfg) -> {
            Optional<String> reduce = cfg.getTextCommands().keySet().stream()
                    .map(key -> cfg.getPrefix() + key).sorted().reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                MessageUtil.reply(m, "Defined Text-Commands: " + reduce.get());
            } else {
                MessageUtil.reply(m, "No Text-Commands defined for this guild");
            }
        }).acceptCustom((m, cfg) -> !m.isPrivate() && (!cfg.isRestrictTexts() || cfg.isMod(m.getAuthor()))));
        commands.put("clear", new CommandWrapper("Clears either the whole chat in this channel, or only messages newer than given time.\n" +
                "Usage: `clear` to clear everything, or \n" +
                "`clear [xh][ym][zs] [@Mention] [@Mention]` with x,y,z being integers and defining the hours, minutes and seconds to clear (at least one of those must be present)" +
                " and optional Mentions to specify that only messages of these users should be cleared.", (m, cfg) -> {
            if(!m.getTextChannel().checkPermission(m.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE)) {
                MessageUtil.reply(m, "I do not have permissions!");
                return;
            }
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 1) {
                MessageHistory history = new MessageHistory(m.getJDA(), m.getTextChannel());
                TaskHelper.start("clear" + m.getTextChannel().getId(), new ClearRunner(history, null, null));
            } else {
                OffsetDateTime clearTo = MiscUtil.getOffsettedTime(args[1]);
                if(clearTo != null) {
                    List<User> mentioned = m.getMessage().getMentionedUsers();
                    MessageHistory history = new MessageHistory(m.getJDA(), m.getTextChannel());
                    if(!TaskHelper.start("clear" + m.getTextChannel().getId(), new ClearRunner(history, clearTo, mentioned))) {
                        MessageUtil.reply(m, "There is already a clear-task running for this Channel!");
                    }
                }
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("kick", new CommandWrapper("Kicks one or more Users from this Guild.\n" +
                "Usage: `kick @mention [@mention ...]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.KICK_MEMBERS)) {
                MessageUtil.reply(e, "I do not have permissions!");
                return;
            }
            if(e.getMessage().getMentionedUsers().size() == 0) {
                MessageUtil.reply(e, "Please add the user(s) to kick via mentions");
                return;
            }
            String notKicked = "";
            for(User user : e.getMessage().getMentionedUsers()) {
                if(cfg.isMod(user) || user == e.getJDA().getSelfInfo()) {
                    notKicked += user.getUsername() + ", ";
                } else {
                    e.getGuild().getManager().kick(user);
                }
            }
            MessageUtil.reply(e, notKicked.isEmpty() ? "User(s) kicked" : "Following user(s) could not be kicked (mod): " + notKicked.substring(0, notKicked.length() - 2));
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        commands.put("ban", new CommandWrapper("Bans one or more Users from this Guild.\n" +
                "Usage: `ban @mention [@mention ...]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS)) {
                MessageUtil.reply(e, "I do not have permissions!");
                return;
            }
            if(e.getMessage().getMentionedUsers().size() == 0) {
                MessageUtil.reply(e, "Please add the user(s) to ban via mentions");
                return;
            }
            String notBanned = "";
            for(User user : e.getMessage().getMentionedUsers()) {
                if(cfg.isAdmin(user) || user == e.getJDA().getSelfInfo()) {
                    notBanned += user.getUsername() + ", ";
                } else {
                    e.getGuild().getManager().ban(user, 0);
                }
            }
            MessageUtil.reply(e, notBanned.isEmpty() ? "User(s) banned" : "Following user(s) could not be banned (admin): " + notBanned.substring(0, notBanned.length() - 2));
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("unban", new CommandWrapper("Unbans one or more Users from this Guild. You need their Id to do that (use command \"`bans`\" to look them up).\n" +
                "Usage: `unban Id [Id ...]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS)) {
                MessageUtil.reply(e, "I do not have permissions!");
                return;
            }
            String[] args = MessageUtil.getArgs(e, cfg);
            if(args.length < 3) {
                MessageUtil.reply(e, "Please provide the Ids of users to unban (get them by calling the bans command)");
            }
            GuildManager manager = e.getGuild().getManager();
            Map<String, User> idMap = new HashMap<>();
            manager.getBans().forEach(u -> idMap.put(u.getId(), u));
            Set<String> unbanned = new HashSet<>();
            for(int i = 2; i < args.length; i++) {
                User user = idMap.get(args[i]);
                if(user != null) {
                    manager.unBan(user);
                    unbanned.add(user.getUsername());
                }
            }
            MessageUtil.reply(e, "Following users got unbanned: " + (unbanned.isEmpty() ? "None! (did you provide Ids?)" : StringUtils.join(unbanned, ", ")));
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("bans", new CommandWrapper("Prints out all Users that were banned in this guild (with their id).", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS)) {
                MessageUtil.reply(e, "I do not have permissions!");
                return;
            }
            Optional<String> bans = e.getGuild().getManager().getBans().stream().map(u -> u.getUsername() + '(' + u.getId() + ')').reduce((s1, s2) -> s1 + ", " + s2);
            if(bans.isPresent()) {
                MessageUtil.reply(e, "Banned users: " + bans.get());
            } else {
                MessageUtil.reply(e, "No Bans found for this Guild");
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("eval", new CommandWrapper("Evaluates javascript code through the Rhino-interpreter. This will respond with whatever was __returned__ from the eval-script." +
                " Injected variables are: **api** (the jda object), **event** (the MessageReceivedEvent), **config** (the internal ServerConfig object [see github])" +
                " and **commands** (the commands-map; use `new Command.static(String help, BiConsumer<MessageReceivedEvent, ServerConfig>)` to create new commands).", (m, cfg) -> {
            Message msg;
            engine.put("event", m);
            engine.put("config", cfg);
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
        commands.put("uptime", new CommandWrapper("Prints the uptime... DUH!", (event, cfg) -> {
            long diff = System.currentTimeMillis()-START_TIME;
            diff = diff/1000; //to s
            long days = diff/86400;
            long hrs = (diff%86400)/3600;
            long mins = (diff%3600)/60;
            long secs = diff%60;
            MessageUtil.reply(event, String.format("Running for %dd %dh %dm %ds", days, hrs, mins, secs));
        }));
        commands.put("shutdown", new CommandWrapper("Shuts down this bot. Be careful or Kantenkugel will kill you!", (msg, cfg) -> {
            MessageUtil.reply(msg, "OK, Bye!");
            msg.getJDA().shutdown();
            MiscUtil.await(msg.getJDA(), MiscUtil::shutdown);
        }).acceptCustom((event, cfg) -> MessageUtil.isGlobalAdmin(event.getAuthor())));
        commands.put("restart", new CommandWrapper("Restarts this bot.", (msg, cfg) -> {
            MessageUtil.reply(msg, "OK, BRB!");
            msg.getJDA().shutdown();
            MiscUtil.await(msg.getJDA(), MiscUtil::restart);
        }).acceptCustom((event, cfg) -> MessageUtil.isGlobalAdmin(event.getAuthor())));
        commands.put("info", new CommandWrapper("Prints minimalistic info about your User, the TextChannel and the Guild.", (event, cfg) -> {
            String text = String.format("```\nUser:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n" +
                    "Channel:\n\t%-15s%s\n\t%-15s%s\n" +
                    "Guild:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n```",
                    "Name", event.getAuthor().getUsername(), "ID", event.getAuthor().getId(), "Discriminator", event.getAuthor().getDiscriminator(),
                    "Avatar", event.getAuthor().getAvatarUrl()==null?"None":event.getAuthor().getAvatarUrl(),
                    "Name", event.getTextChannel().getName(), "ID", event.getTextChannel().getId(),
                    "Name", event.getGuild().getName(), "ID", event.getGuild().getId(), "Icon", event.getGuild().getIconUrl()==null?"None":event.getGuild().getIconUrl());
            MessageUtil.reply(event, text, false);
        }).acceptPrivate(false));
        commands.put("mentioned", new CommandWrapper("Looks for the last message in this Channel where you got mentioned.", (e, cfg) -> {
            MessageHistory messageHistory = new MessageHistory(e.getJDA(), e.getTextChannel());
            User user = e.getMessage().getMentionedUsers().size() > 0 ? e.getMessage().getMentionedUsers().get(0) : e.getAuthor();
            for(int i = 0; i < 5; i++) {
                List<Message> msgs = messageHistory.retrieve();
                if(msgs == null) {
                    MessageUtil.reply(e, "You have never been mentioned in this channel before!");
                    return;
                }
                for(Message msg : msgs) {
                    if((msg.getMentionedUsers().contains(user) || msg.mentionsEveryone()) && !msg.getContent().startsWith(cfg.getPrefix())) {
                        MessageUtil.reply(e, "Last mention of " + user.getUsername() + " was at " + msg.getTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + " from "
                                + (msg.getAuthor() == null ? "-" : msg.getAuthor().getUsername()) + "\nContent: " + msg.getContent());
                        return;
                    }
                }
                if(msgs.size() < 100) {
                    MessageUtil.reply(e, "You have never been mentioned in this channel before!");
                    return;
                }
            }
            MessageUtil.reply(e, "Last mention is older than 500 messages!");
        }).acceptPrivate(false));
        commands.put("rip", new CommandWrapper("Rest in Pieces", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 1) {
                e.getTextChannel().sendMessage("https://cdn.discordapp.com/attachments/116705171312082950/120787560988540929/rip2.png");
                return;
            }
            String text;
            BufferedImage avatar = null;
            if(e.getMessage().getMentionedUsers().size() > 0) {
                text = e.getMessage().getMentionedUsers().get(0).getUsername();
                try {
                    InputStream stream = MiscUtil.getDataStream(e.getMessage().getMentionedUsers().get(0).getAvatarUrl());
                    if(stream != null) {
                        avatar = ImageIO.read(stream);
                    }
                } catch(IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                text = args[1];
            }
            try {
                //left edge at 30, right one at 210 => 180 width
                //y = 200
                BufferedImage read = ImageIO.read(CommandRegistry.class.getClassLoader().getResourceAsStream("rip.png"));
                BufferedImage image = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.drawImage(read, 0, 0, null);
                g.setColor(Color.black);
                Font font = new Font("Arial Black", Font.BOLD, 50);
                FontMetrics m = g.getFontMetrics(font);
                while(m.stringWidth(text) > 180) {
                    font = new Font("Arial Black", Font.BOLD, font.getSize() - 1);
                    m = g.getFontMetrics(font);
                }
                g.setFont(font);
                g.drawString(text, 30 + (180 - m.stringWidth(text)) / 2, 190);
                if(avatar != null) {
                    g.drawImage(avatar, 90, 200, 60, 60, null);
                }
                g.dispose();
                File tmpFile = new File("rip_" + e.getResponseNumber() + ".png");
                ImageIO.write(image, "png", tmpFile);
                e.getTextChannel().sendFileAsync(tmpFile, null, mess -> tmpFile.delete());
            } catch(IOException e1) {
                MessageUtil.reply(e, "I made a Boo Boo!");
            }
        }).acceptPrivate(false));
        commands.put("silence", new CommandWrapper("Silences a given user in this channel (denies write-permission).\nUsage: `silence @Mention [@Mention]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_PERMISSIONS)) {
                MessageUtil.reply(e, "I do not have permissions to modify other peoples Permissions in this channel!");
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()) {
                MessageUtil.reply(e, "Please mention at least one user");
                return;
            }
            mentioned.stream().filter(user -> !cfg.isMod(user)).forEach(user -> {
                PermissionOverrideManager man;
                if(e.getTextChannel().getOverrideForUser(user) != null) {
                    man = e.getTextChannel().getOverrideForUser(user).getManager();
                } else {
                    man =e.getTextChannel().createPermissionOverride(user);
                }
                man.deny(Permission.MESSAGE_WRITE).update();
            });
            MessageUtil.reply(e, "Let there be Silence! :speak_no_evil:", false);
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        commands.put("unsilence", new CommandWrapper("Unsilences a previously silenced user in this channel (reallows write-ermission).\nUsage: `unsilence @Mention [@Mention]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_PERMISSIONS)) {
                MessageUtil.reply(e, "I do not have permissions to modify other peoples Permissions in this channel!");
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()) {
                MessageUtil.reply(e, "Please mention at least one user");
                return;
            }
            mentioned.stream().filter(user -> !cfg.isMod(user)).forEach(user -> {
                PermissionOverride override = e.getTextChannel().getOverrideForUser(user);
                if(override != null) {
                    PermissionOverrideManager man = override.getManager();
                    if(override.getAllowedRaw() == 0 && override.getDenied().size() == 1 && override.getDenied().get(0) == Permission.MESSAGE_WRITE) {
                        man.delete();
                    } else {
                        man.reset(Permission.MESSAGE_WRITE).update();
                    }
                }
            });
            MessageUtil.reply(e, "HAH! Just joking... you can talk again... :speech_balloon:", false);
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        ServerConfig cfg;
        if(!event.isPrivate()) {
            cfg = serverConfigs.get(event.getTextChannel().getGuild().getId());
            if(event.getMessage().getContent().equals("-kbreset") && cfg.isOwner(event.getAuthor())) {
                cfg.setPrefix(ServerConfig.DEFAULT_PREFIX);
                MessageUtil.reply(event, "Prefix was reset to default (" + ServerConfig.DEFAULT_PREFIX + ")");
                return;
            }
            if(event.getMessage().getContent().equals("-kbprefix")) {
                MessageUtil.reply(event, "Current prefix is: `" + cfg.getPrefix() + '`');
                return;
            }
            if(event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()) || event.getMessage().getMentionedUsers().contains(kantenkugel)) {
                System.out.printf("\t@[%s][%s] %s:%s\n", event.getGuild().getName(), event.getTextChannel().getName(),
                        event.getAuthor().getUsername(), event.getMessage().getContent());
            }
        } else {
            cfg = ServerConfig.PMConfig.getInstance(event.getJDA());
            if(event.getAuthor() != event.getJDA().getSelfInfo())
                System.out.println("\tP[" + event.getAuthor().getUsername() + "]: " + event.getMessage().getContent());
        }

        if(cfg.getModules().values().stream().map(m -> m.handle(event)).anyMatch(b -> b)) {
            return;
        }

        if(event.getMessage().getContent().startsWith(cfg.getPrefix())) {
            String[] args = MessageUtil.getArgs(event, cfg);
            if(commands.containsKey(args[0])) {
                if(commands.get(args[0]).isAvailable(event, cfg)) {
                    commands.get(args[0]).accept(event, cfg);
                }

            } else if(cfg.getCommands().containsKey(args[0])) {
                if(cfg.getCommands().get(args[0]).isAvailable(event, cfg)) {
                    cfg.getCommands().get(args[0]).accept(event, cfg);
                }
            } else if(!event.isPrivate()) {
                if(cfg.isRestrictTexts() && !cfg.isMod(event.getAuthor())) {
                    //texts only available to mods
                    return;
                }
                Map<String, String> textCommands = cfg.getTextCommands();
                if(textCommands.containsKey(args[0])) {
                    MessageUtil.reply(event, textCommands.get(args[0]));
                }
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().getAccountManager().setGame("JDA");
        for(Guild guild : event.getJDA().getGuilds()) {
            serverConfigs.put(guild.getId(), new ServerConfig(event.getJDA(), guild));
        }
        kantenkugel = event.getJDA().getUserById("122758889815932930");
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
        if((event.getMessage().isPrivate() || event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()))
                && event.getJDA().getGuildById(event.getInvite().getGuildId()) == null) {
            InviteUtil.join(event.getInvite(), event.getJDA(), null);
            String text = "Joined Guild! Server owner should probably configure me via the config command\nDefault prefix is: "
                    + ServerConfig.DEFAULT_PREFIX + "\nThe owner can reset it by calling -kbreset";
            System.out.println("Joining Guild " + event.getInvite().getGuildName() + " via invite of " + event.getAuthor().getUsername());
            if(event.getMessage().isPrivate()) {
                event.getJDA().getPrivateChannelById(event.getMessage().getChannelId()).sendMessage(text);
            } else {
                event.getJDA().getTextChannelById(event.getMessage().getChannelId()).sendMessage(text);
            }
        }
    }

    private static void config(MessageReceivedEvent event, ServerConfig cfg) {
        String[] args = MessageUtil.getArgs(event, cfg);
        if(args.length == 1) {
            MessageUtil.reply(event, "Available subcommands: prefix, restrictTexts, leave, admins, mods, modules\nTo get more details, run " + cfg.getPrefix() + args[0] + " SUBCOMMAND");
        } else if(args.length > 1) {
            String key = null;
            if(args.length > 2) {
                key = args[2].toLowerCase();
            }
            switch(args[1].toLowerCase()) {
                case "prefix":
                    if(args.length == 2) {
                        MessageUtil.reply(event, "This command modifies the prefix used to call commands of this bot." +
                                "\nCurrent Prefix: `" + cfg.getPrefix() +
                                "`\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " PREFIX");
                    } else {
                        String prefix = MessageUtil.getArgs(event, cfg, 3)[2].toLowerCase();
                        cfg.setPrefix(prefix);
                        MessageUtil.reply(event, "Prefix changed to `" + prefix + '`');
                    }
                    break;
                case "restricttexts":
                    if(args.length == 2) {
                        MessageUtil.reply(event, "This command changes the behavior of text-commands." +
                                "\nIf restrictTexts is set to true, only mods can call the text-commands" +
                                "\nIf set to false, everyone can (default)" +
                                "\nrestrictTexts is currently set to: " + cfg.isRestrictTexts() +
                                "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false");
                    } else {
                        cfg.setRestrictTexts(Boolean.parseBoolean(args[2]));
                        MessageUtil.reply(event, "restrictTexts changed to " + cfg.isRestrictTexts());
                    }
                    break;
                case "leave":
                    if(args.length == 2) {
                        MessageUtil.reply(event, "This will make the bot leave this server!" +
                                "\nTo leave, call " + cfg.getPrefix() + args[0] + " " + args[1] + " YES");
                    } else if(args[2].equals("YES")) {
                        event.getGuild().getManager().leave();
                    }
                    break;
                case "admins":
                    if(args.length < 4) {
                        MessageUtil.reply(event, "This will add/remove Users and/or Roles to the admin-set" +
                                "\nAdmins have access to everything mods can, + access to the clear command (may change)" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        MessageUtil.reply(event, "Current Admins:\n\tUsers: "
                                + (cfg.getAdmins().size() == 0 ? "None" : cfg.getAdmins().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getAdminRoles().size() == 0 ? "None" : cfg.getAdminRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get()));
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addAdmin);
                                MessageUtil.reply(event, "User(s) added as admin(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeAdmin);
                                MessageUtil.reply(event, "User(s) removed from admin(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addAdminRole(any.get());
                                    MessageUtil.reply(event, "Role " + any.get().getName() + " added as admin role");
                                } else {
                                    MessageUtil.reply(event, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeAdminRole(anyremove.get());
                                    MessageUtil.reply(event, "Role " + anyremove.get().getName() + " removed from admin roles");
                                } else {
                                    MessageUtil.reply(event, "No role matching given name found");
                                }
                                break;
                            default:
                                MessageUtil.reply(event, "Invalid syntax");
                        }
                    }
                    break;
                case "mods":
                    if(args.length < 4) {
                        MessageUtil.reply(event, "This will add/remove Users and/or Roles to the mods-set" +
                                "\nMods have access to adding, removing and editing text-commands, and also calling them, when they were locked via the restrictTexts config" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        MessageUtil.reply(event, "Current Mods:\n\tUsers: "
                                + (cfg.getMods().size() == 0 ? "None" : cfg.getMods().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getModRoles().size() == 0 ? "None" : cfg.getModRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get()));
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addMod);
                                MessageUtil.reply(event, "User(s) added as mod(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeMod);
                                MessageUtil.reply(event, "User(s) removed from mod(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addModRole(any.get());
                                    MessageUtil.reply(event, "Role " + any.get().getName() + " added as mod role");
                                } else {
                                    MessageUtil.reply(event, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeModRole(anyremove.get());
                                    MessageUtil.reply(event, "Role " + anyremove.get().getName() + " removed from mod roles");
                                } else {
                                    MessageUtil.reply(event, "No role matching given name found");
                                }
                                break;
                            default:
                                MessageUtil.reply(event, "Invalid syntax");
                        }
                    }
                    break;
                case "modules":
                    if(args.length < 4) {
                        MessageUtil.reply(event, "This will add/remove/configure Modules on this Guild" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " enable/disable MODULE" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " configure MODULE\n");
                        MessageUtil.reply(event, "Currently enabled Modules:\n\t"
                                + (cfg.getModules().size() == 0 ? "None" : cfg.getModules().keySet().stream().reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\nAvailable Modules:\n\t"
                                + (Module.getModules().size() == 0 ? "None" : Module.getModules().keySet().stream().reduce((s1, s2) -> s1 + ", " + s2).get()));
                    } else {
                        String val = null;
                        if(args.length > 3) {
                            val = args[3].toLowerCase();
                        }
                        switch(key) {
                            case "enable":
                                if(Module.getModules().containsKey(val)) {
                                    if(!cfg.getModules().containsKey(val)) {
                                        cfg.addModule(val);
                                        MessageUtil.reply(event, "Module enabled");
                                    } else {
                                        MessageUtil.reply(event, "Module was already enabled!");
                                    }
                                } else {
                                    MessageUtil.reply(event, "Module does not exist");
                                }
                                break;
                            case "disable":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        cfg.removeModule(val);
                                        MessageUtil.reply(event, "Module disabled");
                                    } else {
                                        MessageUtil.reply(event, "Module was not enabled!");
                                    }
                                } else {
                                    MessageUtil.reply(event, "Module does not exist");
                                }
                                break;
                            case "configure":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        String cfgString = args.length > 4 ? StringUtils.join(args, ' ', 4, args.length) : null;
                                        cfg.getModules().get(val).configure(cfgString, event, cfg);
                                    } else {
                                        MessageUtil.reply(event, "Module was not enabled!");
                                    }
                                } else {
                                    MessageUtil.reply(event, "Module does not exist");
                                }
                                break;
                            default:
                                MessageUtil.reply(event, "Invalid syntax");
                        }
                    }
                    break;
                default:
                    MessageUtil.reply(event, "Invalid syntax");
            }
        }
    }

    private static class ClearRunner implements Runnable {
        private final MessageHistory history;
        private final OffsetDateTime time;
        private final List<User> mentioned;

        public ClearRunner(MessageHistory histo, OffsetDateTime time, List<User> mentioned) {
            this.history = histo;
            this.time = time;
            this.mentioned = mentioned.isEmpty() ? null : mentioned;
        }

        @Override
        public void run() {
            if(time == null) {
                List<Message> messages = history.retrieve();
                while(messages != null) {
                    messages.stream().filter(m -> mentioned == null || mentioned.contains(m.getAuthor())).forEach(Message::deleteMessage);
                    messages = history.retrieve();
                }
            } else {
                List<Message> messages = history.retrieve();
                while(messages != null) {
                    for(Message message : messages) {
                        if(message.getTime().isAfter(time)) {
                            if(mentioned == null || mentioned.contains(message.getAuthor())) {
                                message.deleteMessage();
                            }
                        } else {
                            return;
                        }
                    }
                    messages = history.retrieve();
                }
            }
        }
    }
}
