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
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.impl.JDAImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Todo extends Module {
    private static final Pattern msgPattern = Pattern.compile("^\\d+\\)\\s(.+)$");
    private String channel = null;
    private JDA api;
    private ServerConfig cfg;
    private final LinkedList<String> todoMessage = new LinkedList<>();
    private final LinkedList<String> todoEntries = new LinkedList<>();

    @Override
    public String getName() {
        return "todo";
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
            MessageUtil.reply(event, cfg, "**Config:** `channel channelname` to set the channel where the dodo-list should be built");
        } else {
            if(cfgString.startsWith("channel ")) {
                if(event.getMessage().getMentionedChannels().size() > 0) {
                    TextChannel channel = event.getMessage().getMentionedChannels().get(0);
                    if(!channel.checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) {
                        MessageUtil.reply(event, cfg, "I do not have WRITE-Permission for that channel!");
                        return;
                    }
                    changeChannel(channel.getId());
                    MessageUtil.reply(event, cfg, "Set todo channel to " + channel.getName());
                    return;
                }
                Optional<TextChannel> any = event.getGuild().getTextChannels().parallelStream().filter(c -> c.getName().equals(cfgString.substring(8))).findAny();
                if(any.isPresent()) {
                    if(!any.get().checkPermission(api.getSelfInfo(), Permission.MESSAGE_WRITE)) {
                        MessageUtil.reply(event, cfg, "I do not have WRITE-Permission for that channel!");
                        return;
                    }
                    changeChannel(any.get().getId());
                    MessageUtil.reply(event, cfg, "Set todo channel to " + any.get().getName());
                } else {
                    MessageUtil.reply(event, cfg, "Channel not found!");
                }
            }
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        HashMap<String, Command> cmds = new HashMap<>();
        cmds.put("todo", new CommandWrapper(getUsage(), (e, cfg) -> {
            if(channel == null) {
                MessageUtil.reply(e, cfg, "Please configure a channel first (via `config` command - available only to Guild owner)");
                return;
            }
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 2) {
                if(args[1].charAt(0) == '-') {
                    try {
                        int num = Integer.parseInt(args[1].substring(1));
                        if(!toggle(num)) {
                            MessageUtil.reply(e, cfg, "The todo-entry with that id doesn't exist!");
                        } else {
                            if(e.getTextChannel().checkPermission(e.getJDA().getSelfInfo(), Permission.MESSAGE_MANAGE)) {
                                e.getMessage().deleteMessage();
                            }
                        }
                    } catch(NumberFormatException ex) {
                        MessageUtil.reply(e, cfg, args[1].substring(1) + " is not a number!");
                    }
                } else if(args[1].equalsIgnoreCase("clear")) {
                    clear();
                } else {
                    todoEntries.add(args[1]);
                    updateMessage(todoEntries.size());
                }
                return;
            }
            MessageUtil.reply(e, cfg, getUsage());
        }).acceptPrivate(false).acceptPriv(Command.Priv.ADMIN));
        return cmds;
    }

    @Override
    public JSONObject toJson() {
        JSONArray arr = new JSONArray();
        todoMessage.forEach(arr::put);
        return new JSONObject().put("channelId", channel==null?JSONObject.NULL:channel).put("messages", arr);
    }

    @Override
    public void fromJson(JSONObject cfg) {
        channel = null;
        todoMessage.clear();
        if(cfg.has("channelId") && !cfg.isNull("channelId")) {
            channel = cfg.getString("channelId");
            if(cfg.has("messages")) {
                JSONArray messages = cfg.getJSONArray("messages");
                for(int i = 0; i < messages.length(); i++) {
                    todoMessage.add(messages.getString(i));
                }
            }
        }
    }

    private void changeChannel(String newChannelId) {
        if(newChannelId.equals(channel)) {
            return;
        }
        if(!todoMessage.isEmpty()) {
            todoMessage.forEach(this::delete);
            todoMessage.clear();
        }
        this.channel = newChannelId;
        for(int i=1; i<= todoEntries.size(); i+=10) {
            updateMessage(i);
        }
        cfg.save();
    }

    private boolean toggle(int entryId) {
        if(todoEntries.size() < entryId) {
            return false;
        }
        String curr = todoEntries.get(entryId - 1);
        if(curr.startsWith("~~") && curr.endsWith("~~")) {
            curr = curr.substring(2, curr.length() - 2);
        } else {
            curr = "~~" + curr + "~~";
        }
        todoEntries.set(entryId - 1, curr);
        updateMessage(entryId);
        return true;
    }

    private void clear() {
        Iterator<String> iterator = todoEntries.iterator();
        while(iterator.hasNext()) {
            String next = iterator.next();
            if(next.startsWith("~~") && next.endsWith("~~")) {
                iterator.remove();
            }
        }
        todoMessage.forEach(this::delete);
        todoMessage.clear();
        for(int i=1; i<= todoEntries.size(); i+=10) {
            updateMessage(i);
        }
    }

    private void update(String msgId, String content) {
        try {
            ((JDAImpl) api).getRequester().patch("https://discordapp.com/api/channels/" + channel + "/messages/" + msgId, new JSONObject().put("content", content));
        } catch(Exception ignored) {
        }
    }

    private void delete(String msgId) {
        try {
            ((JDAImpl) api).getRequester().delete("https://discordapp.com/api/channels/" + channel + "/messages/" + msgId);
        } catch(Exception ignored) {
        }
    }

    private void updateMessage(int entryId) {
        int msgNum = (entryId - 1) / 10;
        StringBuilder builder = new StringBuilder();
        for(int i = msgNum*10; i<(msgNum+1)*10 && i<todoEntries.size(); i++) {
            builder.append((i+1)).append(") ").append(todoEntries.get(i)).append('\n');
        }
        builder.setLength(builder.length() - 1);
        if(msgNum < todoMessage.size()) {
            update(todoMessage.get(msgNum), builder.toString());
        } else {
            Message message = api.getTextChannelById(channel).sendMessage(builder.toString());
            todoMessage.add(message.getId());
            cfg.save();
        }
    }

    private void fetchMessages() {
        todoEntries.clear();
        if(channel == null) {
            return;
        }
        Map<String, List<String>> messages = new HashMap<>();
        List<String> toFind = new ArrayList<>(todoMessage);
        MessageHistory history = new MessageHistory(api.getTextChannelById(channel));
        while(!toFind.isEmpty()) {
            List<Message> retrieve = history.retrieve();
            if(retrieve == null) {
                return;
            }
            retrieve.forEach(m -> {
                if(toFind.contains(m.getId())) {
                    toFind.remove(m.getId());
                    List<String> tmp = new LinkedList<>();
                    for(String line : m.getRawContent().split("\n")) {
                        Matcher matcher = msgPattern.matcher(line);
                        if(matcher.matches()) {
                            tmp.add(matcher.group(1));
                        }
                    }
                    messages.put(m.getId(), tmp);
                }
            });
        }
        todoMessage.forEach(i -> messages.get(i).forEach(todoEntries::add));
    }

    private static String getUsage() {
        return "Adds or removes todo-entries.\n**Usage:** `todo TEXT` to add a todo-item\n**Or:** `todo -N` to check a item as done\n**Or:** `todo clear` to clear all done entries";
    }
}
