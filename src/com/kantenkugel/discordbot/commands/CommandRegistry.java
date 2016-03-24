package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.*;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;
import net.dv8tion.jda.events.InviteReceivedEvent;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.BlockedException;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.utils.InviteUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandRegistry extends ListenerAdapter {
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<String, ServerConfig> serverConfigs = new HashMap<>();
    private static final Set<String> blacklist = new HashSet<>();

    private static final SimpleLog pmLog = SimpleLog.getLog("PM");
    private static final SimpleLog mentionLog = SimpleLog.getLog("Mention");
    private static final SimpleLog commandLog = SimpleLog.getLog("Command");

    private static long msgCount = 0;
    private static int cmdCount = 0;

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");

    public static void init() {
        JSONArray blacklistArr = BotConfig.get("blacklist", new JSONArray());
        for(int i = 0; i < blacklistArr.length(); i++) {
            blacklist.add(blacklistArr.getString(i));
        }

        loadCommands();
        engine.put("commands", commands);
        engine.put("rng", new Random());
        engine.put("finder", new FinderUtil());
        engine.put("Command", CustomCommand.class);
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");
        } catch(ScriptException e) {
            e.printStackTrace();
        }
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
                    reply(m, cfg, "Command " + args[1] + " is reserved");
                } else {
                    Map<String, String> textCommands = cfg.getTextCommands();
                    if(textCommands.containsKey(args[1].toLowerCase())) {
                        reply(m, cfg, "Command " + args[1] + " is already defined, edit it with !editcom");
                    } else {
                        textCommands.put(args[1].toLowerCase(), args[2]);
                        cfg.save();
                        reply(m, cfg, "Command " + args[1].toLowerCase() + " was created!");
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
                    reply(m, cfg, "Command " + args[1].toLowerCase() + " was edited!");
                    return;
                }
                reply(m, cfg, "Command " + args[1] + " is not defined");
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
                    reply(m, cfg, "Command " + args[1].toLowerCase() + " was removed!");
                    return;
                }
                reply(m, cfg, "Command " + args[1] + " is not defined");
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
                    reply(m, cfg, "Provided Command does not exist or is not available to you!");
                } else {
                    reply(m, cfg, "Help for " + args[1].toLowerCase() + ":\n" + command.getDescription(), false);
                }
                return;
            }
            Map<Command.Priv, Set<String>> availCommands = new HashMap<>();
            commands.entrySet().parallelStream().filter(entry -> entry.getValue().isAvailable(m, cfg)).sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).sequential().forEach(c -> {
                if(!availCommands.containsKey(c.getValue().getPriv())) {
                    availCommands.put(c.getValue().getPriv(), new HashSet<>());
                }
                availCommands.get(c.getValue().getPriv()).add('`' + cfg.getPrefix() + c.getKey() + '`');
            });
            String returned;
            if(!availCommands.isEmpty()) {
                if(availCommands.size() == 1) {
                    returned = StringUtils.join(availCommands.values().stream().findAny().get(), "\n") + "\n";
                } else {
                    returned = "";
                    for(Command.Priv priv : Command.Priv.values()) {
                        if(availCommands.containsKey(priv)) {
                            returned += "Commands for: **" + priv.getRepr() + "**\n\t" + StringUtils.join(availCommands.get(priv), "\n\t") + "\n";
                        }
                    }
                }
            } else {
                returned = "No normal commands available!\n";
            }
            Optional<String> reduce = cfg.getCommands().entrySet().stream().filter(entry -> entry.getValue().isAvailable(m, cfg)).map(entry -> '`' + cfg.getPrefix() + entry.getKey() + '`').reduce((s1, s2) -> s1 + "\n\t" + s2);
            if(reduce.isPresent()) {
                returned += "Available through modules:\n\t" + reduce.get() + "\n";
            }
            try {
                m.getAuthor().getPrivateChannel().sendMessage("Commands available for " + (m.isPrivate() ? "PM" : "Guild " + m.getGuild().getName())
                        + ":\n\n" + returned + "\n**NOTE**: you can type `help COMMAND` to get more detailed info about a specific command.");
                if(m.isPrivate()) {
                    reply(m, cfg, "In Guilds, my commands may be prefixed differently (standard prefix in guilds is -kb instead of !)\n" +
                            "There are also 2 special commands: `-kbreset` resets the guild-prefix to default (-kb) and `-kbprefix` prints the current prefix of the guild. " +
                            "These special commands work in every guild, independent of the configured prefix.");
                } else {
                    reply(m, cfg, "Help sent via PM.");
                }
            } catch(BlockedException ex) {
                reply(m, cfg, "Sorry, but you are blocking my PMs!");
            }
        }));
        commands.put("texts", new CommandWrapper("Shows all available text-commands. (to add/edit/remove them, call addcom/editcom/delcom [requires mod-status])", (m, cfg) -> {
            Optional<String> reduce = cfg.getTextCommands().keySet().stream()
                    .map(key -> cfg.getPrefix() + key).sorted().reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                reply(m, cfg, "Defined Text-Commands: " + reduce.get());
            } else {
                reply(m, cfg, "No Text-Commands defined for this guild");
            }
        }).acceptCustom((m, cfg) -> !m.isPrivate() && (!cfg.isRestrictTexts() || cfg.isMod(m.getAuthor()))));
        commands.put("clear", new CommandWrapper("Clears messages newer than given time.\n" +
                "Usage: `clear [xh][ym][zs] [@Mention] [@Mention]` with x,y,z being integers and defining the hours, minutes and seconds to clear (at least one of those must be present)" +
                " and optional Mentions to specify that only messages of these users should be cleared.\nThis will clear a max of 1000 messages at once.", (m, cfg) -> {
            if(!m.getTextChannel().checkPermission(m.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE)) {
                reply(m, cfg, "I do not have permissions!");
                return;
            }
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length < 2) {
                reply(m, cfg, "Please provide a time-span to delete!");
                return;
            }
            OffsetDateTime clearTo = MiscUtil.getOffsettedTime(args[1]);
            if(clearTo == null) {
                reply(m, cfg, "Incorrect time range!");
                return;
            }
            String msg = "Deleting messages sent within the last " + args[1] + "... ";
            List<User> mentioned = m.getMessage().getMentionedUsers();
            if(!TaskHelper.start("clear" + m.getTextChannel().getId(), new ClearRunner(m.getTextChannel(), clearTo, mentioned, msg))) {
                reply(m, cfg, "There is already a clear-task running for this Channel!");
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("kick", new CommandWrapper("Kicks one or more Users from this Guild.\n" +
                "Usage: `kick @mention [@mention ...]`", (e, cfg) -> {
            if(!PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.KICK_MEMBERS, e.getGuild())) {
                reply(e, cfg, "I do not have permissions!");
                return;
            }
            if(e.getMessage().getMentionedUsers().size() == 0) {
                reply(e, cfg, "Please add the user(s) to kick via mentions");
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
            reply(e, cfg, notKicked.isEmpty() ? "User(s) kicked" : "Following user(s) could not be kicked (mod): " + notKicked.substring(0, notKicked.length() - 2));
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        commands.put("ban", new CommandWrapper("Bans one or more Users from this Guild.\n" +
                "Usage: `ban @mention [@mention ...]`", (e, cfg) -> {
            if(!PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS, e.getGuild())) {
                reply(e, cfg, "I do not have permissions!");
                return;
            }
            if(e.getMessage().getMentionedUsers().size() == 0) {
                reply(e, cfg, "Please add the user(s) to ban via mentions");
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
            reply(e, cfg, notBanned.isEmpty() ? "User(s) banned" : "Following user(s) could not be banned (admin): " + notBanned.substring(0, notBanned.length() - 2));
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("unban", new CommandWrapper("Unbans one or more Users from this Guild. You need their Id to do that (use command \"`bans`\" to look them up).\n" +
                "Usage: `unban Id [Id ...]`", (e, cfg) -> {
            if(!PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS, e.getGuild())) {
                reply(e, cfg, "I do not have permissions!");
                return;
            }
            String[] args = MessageUtil.getArgs(e, cfg);
            if(args.length < 3) {
                reply(e, cfg, "Please provide the Ids of users to unban (get them by calling the bans command)");
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
            reply(e, cfg, "Following users got unbanned: " + (unbanned.isEmpty() ? "None! (did you provide Ids?)" : StringUtils.join(unbanned, ", ")));
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("bans", new CommandWrapper("Prints out all Users that were banned in this guild (with their id).", (e, cfg) -> {
            if(!PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.BAN_MEMBERS, e.getGuild())) {
                reply(e, cfg, "I do not have permissions!");
                return;
            }
            Optional<String> bans = e.getGuild().getManager().getBans().stream().map(u -> u.getUsername() + '(' + u.getId() + ')').reduce((s1, s2) -> s1 + ", " + s2);
            if(bans.isPresent()) {
                reply(e, cfg, "Banned users: " + bans.get());
            } else {
                reply(e, cfg, "No Bans found for this Guild");
            }
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        commands.put("eval", new CommandWrapper("Evaluates javascript code through the Rhino-interpreter. This will respond with whatever was __returned__ from the eval-script." +
                " Injected variables are: **api** (the jda object), **event** (the MessageReceivedEvent), **rng** (a global Random object), **config** (the internal ServerConfig object [see github])" +
                " and **commands** (the commands-map; use `new Command.static(String help, BiConsumer<MessageReceivedEvent, ServerConfig>)` to create new commands).", (m, cfg) -> {
            Message msg;
            engine.put("event", m);
            engine.put("config", cfg);
            try {
                Object out = engine.eval("(function(){ with(imports) {"
                        + m.getMessage().getContent().substring(cfg.getPrefix().length() + 5)
                        + "} })();");
                msg = new MessageBuilder().appendString(out == null ? "Done!" : MessageUtil.strip(out.toString()), MessageBuilder.Formatting.BLOCK).build();
            } catch(Exception e) {
                msg = new MessageBuilder().appendString(e.getMessage(), MessageBuilder.Formatting.BLOCK).build();
            }
            reply(m, msg);
        }).acceptPriv(Command.Priv.BOTADMIN));
        commands.put("uptime", new CommandWrapper("Prints the uptime... DUH!", (event, cfg) -> {
            reply(event, cfg, "Running for " + MiscUtil.getUptime());
        }));
        commands.put("shutdown", new CommandWrapper("Shuts down this bot. Be careful or Kantenkugel will kill you!", (msg, cfg) -> {
            reply(msg, cfg, "OK, Bye!");
            MiscUtil.await(msg.getJDA(), MiscUtil::shutdown);
            msg.getJDA().shutdown();
        }).acceptPriv(Command.Priv.BOTADMIN));
        commands.put("restart", new CommandWrapper("Restarts this bot.", (msg, cfg) -> {
            reply(msg, cfg, "OK, BRB!");
            MiscUtil.await(msg.getJDA(), MiscUtil::restart);
            msg.getJDA().shutdown();
        }).acceptPriv(Command.Priv.BOTADMIN));
        commands.put("update", new CommandWrapper("Updates this bot.", (msg, cfg) -> {
            reply(msg, cfg, "OK, BRB!");
            MiscUtil.await(msg.getJDA(), MiscUtil::update);
            msg.getJDA().shutdown();
        }).acceptPriv(Command.Priv.BOTADMIN));
        commands.put("info", new CommandWrapper("Prints minimalistic info about your User, the TextChannel and the Guild.", (event, cfg) -> {
            String text = String.format("```\nUser:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n" +
                    "Channel:\n\t%-15s%s\n\t%-15s%s\n" +
                    "Guild:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n```",
                    "Name", event.getAuthor().getUsername(), "ID", event.getAuthor().getId(), "Discriminator", event.getAuthor().getDiscriminator(),
                    "Avatar", event.getAuthor().getAvatarUrl()==null?"None":event.getAuthor().getAvatarUrl(),
                    "Name", event.getTextChannel().getName(), "ID", event.getTextChannel().getId(),
                    "Name", event.getGuild().getName(), "ID", event.getGuild().getId(), "Icon", event.getGuild().getIconUrl()==null?"None":event.getGuild().getIconUrl());
            reply(event, cfg, text, false);
        }).acceptPrivate(false));
        commands.put("mentioned", new CommandWrapper("Looks for the last message in this Channel where you got mentioned.", (e, cfg) -> {
            MessageHistory messageHistory = new MessageHistory(e.getJDA(), e.getTextChannel());
            User user = e.getMessage().getMentionedUsers().size() > 0 ? e.getMessage().getMentionedUsers().get(0) : e.getAuthor();
            for(int i = 0; i < 5; i++) {
                List<Message> msgs = messageHistory.retrieve();
                if(msgs == null) {
                    reply(e, cfg, "You have never been mentioned in this channel before!");
                    return;
                }
                for(Message msg : msgs) {
                    if((msg.getMentionedUsers().contains(user) || msg.mentionsEveryone()) && !msg.getContent().startsWith(cfg.getPrefix())) {
                        reply(e, cfg, "Last mention of " + user.getUsername() + " was at " + msg.getTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)) + " from "
                                + (msg.getAuthor() == null ? "-" : msg.getAuthor().getUsername()) + "\nContent: " + msg.getContent());
                        return;
                    }
                }
                if(msgs.size() < 100) {
                    reply(e, cfg, "You have never been mentioned in this channel before!");
                    return;
                }
            }
            reply(e, cfg, "Last mention is older than 500 messages!");
        }).acceptPrivate(false));
        commands.put("rip", new CommandWrapper("Rest in Pieces", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 1) {
                reply(e, cfg, "https://cdn.discordapp.com/attachments/116705171312082950/120787560988540929/rip2.png", false);
                return;
            }
            if(!e.isPrivate() && !e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MESSAGE_ATTACH_FILES)) {
                reply(e, cfg, "I cannot upload files here!");
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
                e.getChannel().sendFileAsync(tmpFile, null, mess -> tmpFile.delete());
            } catch(IOException e1) {
                reply(e, cfg, "I made a Boo Boo!");
            }
        }));
        commands.put("silence", new CommandWrapper("Silences a given user in this channel (denies write-permission).\nUsage: `silence @Mention [@Mention]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_PERMISSIONS)) {
                reply(e, cfg, "I do not have permissions to modify other peoples Permissions in this channel!");
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()) {
                reply(e, cfg, "Please mention at least one user");
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
            reply(e, cfg, "Let there be Silence! :speak_no_evil:", false);
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        commands.put("unsilence", new CommandWrapper("Unsilences a previously silenced user in this channel (reallows write-ermission).\nUsage: `unsilence @Mention [@Mention]`", (e, cfg) -> {
            if(!e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_PERMISSIONS)) {
                reply(e, cfg, "I do not have permissions to modify other peoples Permissions in this channel!");
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            if(mentioned.isEmpty()) {
                reply(e, cfg, "Please mention at least one user");
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
            reply(e, cfg, "HAH! Just joking... you can talk again... :speech_balloon:", false);
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        commands.put("stats", new CommandWrapper("Displays some stats about KanzeBot", (e, cfg) -> {
            String stats = String.format("%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n\n%s\n%s",
                    "Guilds:", e.getJDA().getGuilds().size(),
                    "Users (Unique):", e.getJDA().getGuilds().stream().map(g -> g.getUsers().size()).reduce(0, (s1, s2) -> s1 + s2) + " (" + e.getJDA().getUsers().size() + ')',
                    "Uptime:", MiscUtil.getUptime(),
                    "Messages seen:", msgCount,
                    "Commands seen:", cmdCount,
                    "Version rev:", Statics.VERSION,
                    "Changes of current version:", Statics.CHANGES);
            reply(e, new MessageBuilder().appendString("Stats for KanzeBot:\n")
                    .appendCodeBlock(stats, "").build());
        }));
        commands.put("blacklist", new CommandWrapper("Blocks users from accessing features of this bot.\n" +
                "Usage: `blacklist add|remove|del @Mention [@Mention]`\nOr: `blacklist add|remove|del userid`\n" +
                "Or: `blacklist show`", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 3);
            if(args.length == 1) {
                reply(e, cfg, commands.get("blacklist").getDescription());
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            switch(args[1].toLowerCase()) {
                case "add":
                    if(mentioned.isEmpty()) {
                        if(args.length == 3) {
                            blacklist.add(args[2]);
                        } else {
                            reply(e, cfg, commands.get("blacklist").getDescription());
                            return;
                        }
                    } else {
                        mentioned.stream().filter(u -> !MessageUtil.isGlobalAdmin(u)).forEach(u -> blacklist.add(u.getId()));
                    }
                    break;
                case "del":
                case "remove":
                    if(mentioned.isEmpty()) {
                        if(args.length == 3) {
                            blacklist.remove(args[2]);
                        } else {
                            reply(e, cfg, commands.get("blacklist").getDescription());
                            return;
                        }
                    } else {
                        mentioned.forEach(u -> blacklist.remove(u.getId()));
                    }
                    break;
                case "show":
                case "list":
                    Optional<String> black = blacklist.stream().map(b -> {
                        User userById = e.getJDA().getUserById(b);
                        if(userById == null)
                            return b;
                        return userById.getUsername() + '(' + b + ')';
                    }).reduce((s1, s2) -> s1 + ", " + s2);
                    if(black.isPresent()) {
                        reply(e, cfg, "Current blacklist: " + black.get(), false);
                    } else {
                        reply(e, cfg, "Blacklist is empty", false);
                    }
                default:
                    return;
            }
            JSONArray blacklistArr = new JSONArray();
            blacklist.forEach(blacklistArr::put);
            BotConfig.set("blacklist", blacklistArr);

            reply(e, cfg, "User(s) added/removed from blacklist!");
        }).acceptPriv(Command.Priv.BOTADMIN));
        commands.put("about", new CommandWrapper("Shows some basic info about this Bot", (e, cfg) -> {
            reply(e, cfg, String.format("```\n" + e.getJDA().getSelfInfo().getUsername() + " info:" +
                            "\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%s\n```",
                    "Bot-ID", e.getJDA().getSelfInfo().getId(), "Owner", Statics.botOwner.getUsername() + '#' + Statics.botOwner.getDiscriminator(),
                    "Owner-ID", Statics.botOwner.getId(), "Version-rev.", Statics.VERSION,
                    "Library", "JDA (v" + Statics.JDAVERSION + ")",
                    "If you need help, or have suggestions, feel free to join my discord server: https://discord.gg/0tYwGhNHaw5MZjJn"));
        }));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        msgCount++;
        if(blacklist.contains(event.getAuthor().getId())) {
            return;
        }
        ServerConfig cfg;
        if(!event.isPrivate()) {
            cfg = serverConfigs.get(event.getTextChannel().getGuild().getId());
            if(event.getMessage().getContent().equals("-kbreset") && cfg.isOwner(event.getAuthor())) {
                cfg.setPrefix(ServerConfig.DEFAULT_PREFIX);
                reply(event, cfg, "Prefix was reset to default (" + ServerConfig.DEFAULT_PREFIX + ")");
                return;
            }
            if(event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()) || event.getMessage().getMentionedUsers().contains(Statics.botOwner)) {
                mentionLog.info(String.format("[%s][%s] %s:%s", event.getGuild().getName(), event.getTextChannel().getName(),
                        event.getAuthor().getUsername(), event.getMessage().getContent()));
            }
        } else {
            cfg = ServerConfig.PMConfig.getInstance(event.getJDA());
            if(event.getMessage().getContent().equalsIgnoreCase("help")) {
                commands.get("help").accept(event, cfg);
                return;
            }
            if(event.getAuthor() != event.getJDA().getSelfInfo())
                pmLog.info(event.getAuthor().getUsername() + ": " + event.getMessage().getContent());
        }

        if(event.getMessage().getContent().equals("-kbprefix")) {
            reply(event, cfg, "Current command-prefix is: `" + cfg.getPrefix() + '`');
            return;
        }

        if(cfg.getModules().values().stream().map(m -> m.handle(event, cfg)).anyMatch(b -> b)) {
            return;
        }

        if(event.getMessage().getContent().startsWith(cfg.getPrefix())) {
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

    @Override
    public void onReady(ReadyEvent event) {
        initVars(event.getJDA());
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        initVars(event.getJDA());
    }

    private void initVars(JDA jda) {
        jda.getAccountManager().setGame("JDA");
        serverConfigs.clear();
        for(Guild guild : jda.getGuilds()) {
            serverConfigs.put(guild.getId(), new ServerConfig(jda, guild));
        }
        Statics.botOwner = jda.getUserById(BotConfig.get("ownerId"));
        updateCarbon();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        Statics.LOG.info("Joined Guild " + event.getGuild().getName());
        serverConfigs.put(event.getGuild().getId(), new ServerConfig(event.getJDA(), event.getGuild()));
        updateCarbon();
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        Statics.LOG.info("Left Guild " + event.getGuild().getName());
        serverConfigs.remove(event.getGuild().getId());
        updateCarbon();
    }

    @Override
    public void onInviteReceived(InviteReceivedEvent event) {
        if(blacklist.contains(event.getAuthor().getId())) {
            return;
        }
        MessageChannel channel = event.isPrivate() ? event.getJDA().getPrivateChannelById(event.getMessage().getChannelId()) :
                event.getJDA().getTextChannelById(event.getMessage().getChannelId());
        if((event.isPrivate() || event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfInfo()))) {
            if(event.getJDA().getGuildById(event.getInvite().getGuildId()) != null) {
                try {
                    channel.sendMessage("Already in that Server!");
                } catch (RuntimeException ignored) {} //no write perms or blocked pm
                return;
            }
            Statics.LOG.info("Joining Guild " + event.getInvite().getGuildName() + " via invite of " + event.getAuthor().getUsername());
            InviteUtil.join(event.getInvite(), event.getJDA(), guild -> {
                String text = "Joined Guild " + guild.getName() + "! Server owner should probably configure me via the config command\nDefault command-prefix is: "
                        + ServerConfig.DEFAULT_PREFIX + "\nThe owner can reset it by calling -kbreset";
                try {
                    channel.sendMessage(text);
                } catch(RuntimeException ignored) {} //no write perms or blocked pm
                text = "Joined Guild via invite of " + event.getAuthor().getUsername() + " (ID: " + event.getAuthor().getId() +
                        ")! Server owner should probably configure me via the config command\n" +
                        "Default command-prefix is: " + ServerConfig.DEFAULT_PREFIX + "\nThe owner can reset it by calling -kbreset";
                if(guild.getPublicChannel().checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE)) {
                    guild.getPublicChannel().sendMessageAsync(text, null);
                } else {
                    Optional<TextChannel> first = guild.getTextChannels().parallelStream().filter(tc -> tc.checkPermission(event.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE))
                            .sorted((c1, c2) -> Integer.compare(c1.getPosition(), c2.getPosition())).findFirst();
                    if(first.isPresent()) {
                        first.get().sendMessage(text);
                    }
                }
            });
        }
    }

    private static void config(MessageReceivedEvent event, ServerConfig cfg) {
        String[] args = MessageUtil.getArgs(event, cfg);
        if(args.length == 1) {
            reply(event, cfg, "Available subcommands: prefix, restrictTexts, leave, admins, mods, modules, allowEveryone\nTo get more details, run " + cfg.getPrefix() + args[0] + " SUBCOMMAND");
        } else if(args.length > 1) {
            String key = null;
            if(args.length > 2) {
                key = args[2].toLowerCase();
            }
            switch(args[1].toLowerCase()) {
                case "prefix":
                    if(args.length == 2) {
                        reply(event, cfg, "This command modifies the prefix used to call commands of this bot." +
                                "\nCurrent Prefix: `" + cfg.getPrefix() +
                                "`\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " PREFIX");
                    } else {
                        String prefix = MessageUtil.getArgs(event, cfg, 3)[2].toLowerCase();
                        cfg.setPrefix(prefix);
                        reply(event, cfg, "Prefix changed to `" + prefix + '`');
                    }
                    break;
                case "restricttexts":
                    if(args.length == 2) {
                        reply(event, cfg, "This command changes the behavior of text-commands." +
                                "\nIf restrictTexts is set to true, only mods can call the text-commands" +
                                "\nIf set to false, everyone can (default)" +
                                "\nrestrictTexts is currently set to: " + cfg.isRestrictTexts() +
                                "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false");
                    } else {
                        cfg.setRestrictTexts(Boolean.parseBoolean(args[2]));
                        reply(event, cfg, "restrictTexts changed to " + cfg.isRestrictTexts());
                    }
                    break;
                case "alloweveryone":
                    if(args.length == 2) {
                        reply(event, "This config changes if the bot can ping @everyone in this guild " +
                                "(this affects primarily text-responses created by `addcom` and responses from the responder module)." +
                                "\nIf allowEveryone is set to true, this bot can do @everyone." +
                                "\nIf set to false, @everyone will always get escaped." +
                                "\nallowEveryone is currently set to: " + cfg.isAllowEveryone() +
                                "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false", true, true);
                    } else {
                        cfg.setAllowEveryone(Boolean.parseBoolean(args[2]));
                        reply(event, cfg, "allowEveryone changed to " + cfg.isAllowEveryone());
                    }
                    break;
                case "leave":
                    if(args.length == 2) {
                        reply(event, cfg, "This will make the bot leave this server!" +
                                "\nTo leave, call " + cfg.getPrefix() + args[0] + " " + args[1] + " YES");
                    } else if(args[2].equals("YES")) {
                        event.getGuild().getManager().leave();
                    }
                    break;
                case "admins":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove Users and/or Roles to the admin-set" +
                                "\nAdmins have access to everything mods can, + access to the clear command (may change)" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        reply(event, "Current Admins:\n\tUsers: "
                                + (cfg.getAdmins().size() == 0 ? "None" : cfg.getAdmins().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getAdminRoles().size() == 0 ? "None" :
                                MessageUtil.strip(cfg.getAdminRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get())), true, true);
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addAdmin);
                                reply(event, cfg, "User(s) added as admin(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeAdmin);
                                reply(event, cfg, "User(s) removed from admin(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addAdminRole(any.get());
                                    reply(event, "Role " + any.get().getName() + " added as admin role", true, true);
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeAdminRole(anyremove.get());
                                    reply(event, "Role " + anyremove.get().getName() + " removed from admin roles", true, true);
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                case "mods":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove Users and/or Roles to the mods-set" +
                                "\nMods have access to adding, removing and editing text-commands, and also calling them, when they were locked via the restrictTexts config" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        reply(event, "Current Mods:\n\tUsers: "
                                + (cfg.getMods().size() == 0 ? "None" : cfg.getMods().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getModRoles().size() == 0 ? "None" :
                                MessageUtil.strip(cfg.getModRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get())), true, true);
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addMod);
                                reply(event, cfg, "User(s) added as mod(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeMod);
                                reply(event, cfg, "User(s) removed from mod(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addModRole(any.get());
                                    reply(event, "Role " + any.get().getName() + " added as mod role", true, true);
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeModRole(anyremove.get());
                                    reply(event, "Role " + anyremove.get().getName() + " removed from mod roles", true, true);
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                case "modules":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove/configure Modules on this Guild" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " enable/disable MODULE" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " configure MODULE\n");
                        reply(event, cfg, "Currently enabled Modules:\n\t"
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
                                        reply(event, cfg, "Module enabled");
                                    } else {
                                        reply(event, cfg, "Module was already enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            case "disable":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        cfg.removeModule(val);
                                        reply(event, cfg, "Module disabled");
                                    } else {
                                        reply(event, cfg, "Module was not enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            case "configure":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        String cfgString = args.length > 4 ? StringUtils.join(args, ' ', 4, args.length) : null;
                                        cfg.getModules().get(val).configure(cfgString, event, cfg);
                                    } else {
                                        reply(event, cfg, "Module was not enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                default:
                    reply(event, cfg, "Invalid syntax");
            }
        }
    }

    private static void updateCarbon() {
        String carbonKey = BotConfig.get("carbonKey");
        if(carbonKey == null || carbonKey.trim().isEmpty()) {
            return;
        }
        try {
            Unirest.post("https://www.carbonitex.net/discord/data/botdata.php").field("key", carbonKey).field("servercount", Statics.jdaInstance.getGuilds().size()).asString();
        } catch(UnirestException e) {
            e.printStackTrace();
        }
    }

    private static class ClearRunner implements Runnable {
        private final TextChannel channel;
        private final MessageHistory history;
        private final OffsetDateTime time;
        private final List<User> mentioned;
        private Message message;

        public ClearRunner(TextChannel channel, OffsetDateTime time, List<User> mentioned, String msg) {
            this.channel = channel;
            this.history = new MessageHistory(channel.getJDA(), channel);
            this.time = time;
            this.mentioned = mentioned.isEmpty() ? null : mentioned;
            this.message = new MessageBuilder().appendString(msg).build();
        }

        @Override
        public void run() {
            int sets = 0;
            List<Message> messages = history.retrieve();
            message = channel.checkPermission(channel.getJDA().getSelfInfo(), Permission.MESSAGE_WRITE) ? channel.sendMessage(message) : null;
            while(messages != null) {
                if(sets++ > 10) {
                    try {
                        if(message != null)
                            message.updateMessage(message.getRawContent() + "Reached cap (1000 Messages)!");
                    } catch(Exception ignored) {}
                    return;
                }
                for(Message message : messages) {
                    if(message.getTime().isAfter(time)) {
                        if(mentioned == null || mentioned.contains(message.getAuthor())) {
                            message.deleteMessage();
                        }
                    } else {
                        messages = null;
                        break;
                    }
                }
                sets++;
                if(messages != null)
                    messages = history.retrieve();
            }
            try {
                if(message != null)
                    message.updateMessage(message.getRawContent() + "done!");
            } catch(Exception ignored) {}
        }
    }
}
