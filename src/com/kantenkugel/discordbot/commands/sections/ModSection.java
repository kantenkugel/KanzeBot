package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import com.kantenkugel.discordbot.util.TaskHelper;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.PermissionOverride;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.managers.GuildManager;
import net.dv8tion.jda.managers.PermissionOverrideManager;
import net.dv8tion.jda.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

public class ModSection implements CommandSection {
    @Override
    public void register(Map<String, Command> registry, JDA api) {
        registerTxtCommands(registry);
        registerKickBan(registry);
        registerMisc(registry);
    }

    private void registerTxtCommands(Map<String, Command> registry) {
        registry.put("addcom", new CommandWrapper("Adds a new text-command (see command \"`texts`\").\n" +
                "Usage: `addcom NAME TEXT` with NAME being the name/key of the command and TEXT being the response.", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length == 3) {
                if(registry.containsKey(args[1].toLowerCase())) {
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

        registry.put("editcom", new CommandWrapper("Edits a already existing text-command (see command \"`texts`\").\n" +
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

        registry.put("delcom", new CommandWrapper("Deletes a already existing text-command (see command \"`texts`\").\n" +
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

        registry.put("texts", new CommandWrapper("Shows all available text-commands. (to add/edit/remove them, call addcom/editcom/delcom [requires mod-status])", (m, cfg) -> {
            Optional<String> reduce = cfg.getTextCommands().keySet().stream()
                    .map(key -> cfg.getPrefix() + key).sorted().reduce((s1, s2) -> s1 + ", " + s2);
            if(reduce.isPresent()) {
                reply(m, cfg, "Defined Text-Commands: " + reduce.get());
            } else {
                reply(m, cfg, "No Text-Commands defined for this guild");
            }
        }).acceptCustom((m, cfg) -> !m.isPrivate() && (!cfg.isRestrictTexts() || cfg.isMod(m.getAuthor()))));
    }

    private void registerKickBan(Map<String, Command> registry) {
        registry.put("kick", new CommandWrapper("Kicks one or more Users from this Guild.\n" +
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

        registry.put("ban", new CommandWrapper("Bans one or more Users from this Guild.\n" +
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

        registry.put("unban", new CommandWrapper("Unbans one or more Users from this Guild. You need their Id to do that (use command \"`bans`\" to look them up).\n" +
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

        registry.put("bans", new CommandWrapper("Prints out all Users that were banned in this guild (with their id).", (e, cfg) -> {
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
    }

    private void registerMisc(Map<String, Command> registry) {
        registry.put("silence", new CommandWrapper("Silences a given user in this channel (denies write-permission).\nUsage: `silence @Mention [@Mention]`", (e, cfg) -> {
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

        registry.put("unsilence", new CommandWrapper("Unsilences a previously silenced user in this channel (reallows write-ermission).\nUsage: `unsilence @Mention [@Mention]`", (e, cfg) -> {
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

        registry.put("clear", new CommandWrapper("Clears messages newer than given time.\n" +
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
