package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.impl.JDAImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class ChannelQueue extends Module {
    private String channel = null;
    private JDA api;
    private ServerConfig cfg;
    private final Set<String> mods = new HashSet<>();
    private final LinkedList<String> messageIds = new LinkedList<>();       //message-ids
    private final Map<String, String> messages = new HashMap<>();           //message-id -> message-content
    private final Map<String, String> contained = new HashMap<>();          //user-id -> message-id

    @Override
    public String getName() {
        return "channelqueue";
    }

    @Override
    public boolean availableInPms() {
        return false;
    }

    @Override
    public void init(JDA jda, ServerConfig cfg) {
        this.api = jda;
        this.cfg = cfg;
        fetchMessages();
    }

    @Override
    public void configure(String cfgString, MessageEvent event, ServerConfig cfg) {
        if(cfgString == null) {
            MessageUtil.reply(event, cfg, "**Config:**\n`channel channelname` to set the channel where the queue should be built\n" +
                    "`mods add/remove ROLENAME [ROLENAME...]` to grant/revoke a role from clearing others from the queue.");
        } else {
            if(cfgString.startsWith("channel ")) {
                if(event.getMessage().getMentionedChannels().size() > 0) {
                    TextChannel channel = event.getMessage().getMentionedChannels().get(0);
                    if(!channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) {
                        MessageUtil.reply(event, cfg, "I do not have WRITE-Permission for that channel!");
                        return;
                    }
                    changeChannel(channel.getId());
                    MessageUtil.reply(event, cfg, "Set queue channel to " + channel.getName());
                    return;
                }
                Optional<TextChannel> any = event.getGuild().getTextChannels().parallelStream().filter(c -> c.getName().equals(cfgString.substring(8))).findAny();
                if(any.isPresent()) {
                    if(!any.get().checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) {
                        MessageUtil.reply(event, cfg, "I do not have WRITE-Permission for that channel!");
                        return;
                    }
                    changeChannel(any.get().getId());
                    MessageUtil.reply(event, cfg, "Set queue channel to " + any.get().getName());
                } else {
                    MessageUtil.reply(event, cfg, "Channel not found!");
                }
            } else if(cfgString.startsWith("mods ")) {
                String[] split = cfgString.split("\\s+");
                if(split.length < 3) {
                    MessageUtil.reply(event, cfg, "Invalid syntax!");
                    return;
                }
                List<Role> roles = event.getGuild().getRoles();
                String out = "";
                switch(split[1].toLowerCase()) {
                    case "add":
                        for(int i = 2; i < split.length; i++) {
                            String roleName = split[i];
                            Optional<Role> any = roles.parallelStream().filter(r -> r.getName().equals(roleName)).findAny();
                            if(any.isPresent()) {
                                mods.add(any.get().getId());
                                out += "Added Role " + any.get().getName() + " to queue mods!\n";
                            } else {
                                out += "Role " + roleName + " not found!\n";
                            }
                        }
                        break;
                    case "del":
                    case "delete":
                    case "remove":
                        for(int i = 2; i < split.length; i++) {
                            String roleName = split[i];
                            Optional<Role> any = roles.parallelStream().filter(r -> r.getName().equals(roleName)).findAny();
                            if(any.isPresent()) {
                                if(mods.contains(any.get().getId())) {
                                    mods.remove(any.get().getId());
                                    out += "Removed Role " + any.get().getName() + " from queue mods!\n";
                                } else {
                                    out += "Role " + any.get().getName() + " was not marked as queue mod!";
                                }
                            } else {
                                out += "Role " + roleName + " not found!\n";
                            }
                        }
                        break;
                    default:
                        MessageUtil.reply(event, cfg, "Invalid 2nd argument. Please provide add or remove!");
                        return;
                }
                cfg.save();
            }
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        HashMap<String, Command> cmds = new HashMap<>();
        cmds.put("sos", new CommandWrapper("Adds you to the queue. Usage: `sos TEXT`", (e, cfg) -> {
            if(channel == null) {
                MessageUtil.reply(e, cfg, "Please configure me first! (channel not configured)");
                return;
            }
            if(contained.containsKey(e.getAuthor().getId())) {
                MessageUtil.reply(e, cfg, "You are already queued! remove yourself first!");
                return;
            }
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length < 2) {
                MessageUtil.reply(e, cfg, "Please provide some text!");
                return;
            }
            String message = e.getAuthor().getAsMention() + ": " + MessageUtil.strip(args[1].replace('\n', ' '));
            if(messageIds.isEmpty()) {
                create(message);
            } else {
                String lastId = messageIds.getLast();
                String lastMsg = messages.get(lastId);
                if(lastMsg.length() + 1 + message.length() > 2000) {
                    create(message);
                } else {
                    lastMsg += '\n' + message;
                    messages.put(lastId, lastMsg);
                    contained.put(e.getAuthor().getId(), lastId);
                    update(lastId);
                }
            }
            MessageUtil.reply(e, cfg, "Done.");
        }));
        cmds.put("delsos", new CommandWrapper("Removes you from the queue.\nUsage: `delsos`" +
                "\nMods can also remove others: `delsos @Mention [@Mention...]`", (e, cfg) -> {
            if(channel == null) {
                MessageUtil.reply(e, cfg, "Please configure me first! (channel not configured)");
                return;
            }
            if(e.getMessage().getMentionedUsers().size() > 0) {
                boolean b = e.getGuild().getRolesForUser(e.getAuthor()).parallelStream().anyMatch(r -> mods.contains(r.getId()));
                if(b) {
                    for(User user : e.getMessage().getMentionedUsers()) {
                        remove(user.getId());
                    }
                } else {
                    MessageUtil.reply(e, cfg, "You have to be mod to remove others from the queue!");
                }
            } else {
                if(contained.containsKey(e.getAuthor().getId())) {
                    remove(e.getAuthor().getId());
                } else {
                    MessageUtil.reply(e, cfg, "You must queue first :)");
                }
            }
        }));
        cmds.put("sosclear", new CommandWrapper("Clears invalid sos-requests (user left server), or all sos-requests\n" +
                "`sosclear` will clear invalid requests, while `sosclear all` will clear all", (e, cfg) -> {
            if(e.getContent().equalsIgnoreCase("sosclear")) {
                clearInvalid();
            } else if(e.getContent().equalsIgnoreCase("sosclear all")) {
                messageIds.forEach(this::delete);
                messageIds.clear();
                messages.clear();
                contained.clear();
            }
        }).acceptCustom((e, cfg) -> e.getGuild().getRolesForUser(e.getAuthor()).parallelStream().anyMatch(r -> mods.contains(r.getId()))));
        return cmds;
    }

    @Override
    public JSONObject toJson() {
        JSONArray msgs = new JSONArray();
        messageIds.forEach(msgs::put);
        JSONArray modArr = new JSONArray();
        mods.forEach(modArr::put);
        return new JSONObject().put("channelId", channel==null?JSONObject.NULL:channel).put("messages", msgs).put("mods", modArr);
    }

    @Override
    public void fromJson(JSONObject cfg) {
        channel = null;
        mods.clear();
        messageIds.clear();
        if(cfg.has("channelId") && !cfg.isNull("channelId")) {
            channel = cfg.getString("channelId");
            if(cfg.has("messages")) {
                JSONArray messagesArr = cfg.getJSONArray("messages");
                for(int i = 0; i < messagesArr.length(); i++) {
                    messageIds.add(messagesArr.getString(i));
                }
            }
            if(cfg.has("mods")) {
                JSONArray modArr = cfg.getJSONArray("mods");
                for(int i = 0; i < modArr.length(); i++) {
                    mods.add(modArr.getString(i));
                }
            }
        }
    }

    private void changeChannel(String newChannelId) {
        if(newChannelId.equals(channel)) {
            return;
        }
        StringBuilder logBuilder = new StringBuilder();
        if(!messageIds.isEmpty()) {
            for(String messageId : messageIds) {
                logBuilder.append(messages.get(messageId)).append("\n");
            }
            messageIds.forEach(this::delete);
            messageIds.clear();
            messages.clear();
            contained.clear();
        }

        this.channel = newChannelId;
        if(logBuilder.length() > 0) {
            String log = logBuilder.toString();
            while(log.length() > 0) {
                String write = log.length() > 2000 ? log.substring(0, log.lastIndexOf('\n', 2000)) : log;
                create(write);
                log = log.substring(write.length() + 1);
            }
        }
        cfg.save();
    }

    private void clearInvalid() {
        Set<String> tmp = new HashSet<>(contained.keySet());
        tmp.forEach(userId -> {
            User userById = api.getUserById(userId);
            if(userById == null || !api.getTextChannelById(channel).getGuild().getUsers().contains(userById))
                remove(userId);
        });
    }

    private void remove(String userId) {
        String messageId = contained.get(userId);
        if(messageId == null) {
            return;
        }
        String mention = "<@" + userId + '>';
        String[] split = messages.get(messageId).split("\n");
        StringBuilder b = new StringBuilder();
        for(String s : split) {
            if(s.startsWith(mention)) {
                continue;
            }
            b.append(s).append('\n');
        }
        if(b.length() == 0) {
            delete(messageId);
        } else {
            b.setLength(b.length() - 1);
            String newMsg = b.toString();
            messages.put(messageId, newMsg);
            update(messageId);
        }
        contained.remove(userId);
    }

    private void update(String msgId) {
        try {
            ((JDAImpl) api).getRequester().patch("https://discordapp.com/api/channels/" + channel + "/messages/" + msgId, new JSONObject().put("content", messages.get(msgId)));
        } catch(Exception ignored) {
        }
    }

    private void delete(String msgId) {
        try {
            ((JDAImpl) api).getRequester().delete("https://discordapp.com/api/channels/" + channel + "/messages/" + msgId);
            messageIds.remove(msgId);
            messages.remove(msgId);
        } catch(Exception ignored) {
        }
    }

    private void create(String content) {
        api.getTextChannelById(channel).sendMessageAsync(content, message -> {
            messageIds.add(message.getId());
            messages.put(message.getId(), content);
            for(User user : message.getMentionedUsers()) {
                contained.put(user.getId(), message.getId());
            }
            cfg.save();
        });
    }

    private void fetchMessages() {
        messages.clear();
        contained.clear();
        if(channel == null) {
            return;
        }
        Set<String> toFind = new HashSet<>(messageIds);
        MessageHistory history = new MessageHistory(api, api.getTextChannelById(channel));
        while(!toFind.isEmpty()) {
            List<Message> retrieve = history.retrieve();
            if(retrieve == null) {
                break;
            }
            retrieve.forEach(m -> {
                if(toFind.contains(m.getId())) {
                    toFind.remove(m.getId());
                    messages.put(m.getId(), m.getRawContent());
                    for(User user : m.getMentionedUsers()) {
                        contained.put(user.getId(), m.getId());
                    }
                }
            });
        }
        if(!toFind.isEmpty()) {
            toFind.forEach(messageIds::remove);
            cfg.save();
        }
    }

    @Override
    public boolean hideModule() {
        return true;
    }
}
