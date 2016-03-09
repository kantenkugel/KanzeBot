package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.Main;
import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoRespond extends Module {
    private static final Pattern addPattern = Pattern.compile("^(\\w+)\\s\\[([^\\]]+)\\]\\s(.+)$");
    private final Map<String, Pair<Set<String>, String>> responses = new HashMap<>(); //name ->
    private final Set<String> channels = new HashSet<>();
    private ServerConfig servercfg;

    private static final SimpleLog respondLog = SimpleLog.getLog("Responder");

    @Override
    public String getName() {
        return "responder";
    }

    @Override
    public boolean availableInPms() {
        return false;
    }

    @Override
    public void init(JDA jda, ServerConfig cfg) {
        this.servercfg = cfg;
    }

    @Override
    public void configure(String cfgString, MessageReceivedEvent event, ServerConfig cfg) {
        MessageUtil.reply(event, intConfig(cfgString, cfg, event));
    }

    @Override
    public Map<String, Command> getCommands() {
        HashMap<String, Command> commands = new HashMap<>();
        commands.put("responder", new CommandWrapper("Used to configure the responder module.", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            MessageUtil.reply(e, intConfig(args.length == 1 ? null : args[1], cfg, e));
        }).acceptPrivate(false).acceptPriv(Command.Priv.MOD));
        return commands;
    }

    @Override
    public JSONObject toJson() {
        JSONArray r = new JSONArray();
        responses.entrySet().forEach(e -> {
            JSONArray keys = new JSONArray();
            e.getValue().getKey().forEach(keys::put);
            r.put(new JSONObject().put("name", e.getKey()).put("keys", keys).put("response", e.getValue().getValue()));
        });
        JSONArray channels = new JSONArray();
        this.channels.forEach(channels::put);
        return new JSONObject().put("channels", channels).put("responses", r);
    }

    @Override
    public void fromJson(JSONObject cfg) {
        if(!cfg.has("channels")) {
            cfg.put("channels", new JSONArray());
        }
        if(!cfg.has("responses")) {
            cfg.put("responses", new JSONArray());
        }
        channels.clear();
        JSONArray arr = cfg.getJSONArray("channels");
        for(int i = 0; i < arr.length(); i++) {
            channels.add(arr.getString(i));
        }
        responses.clear();
        arr = cfg.getJSONArray("responses");
        for(int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            Set<String> keys = new HashSet<>();
            JSONArray keyarr = obj.getJSONArray("keys");
            for(int j = 0; j < keyarr.length(); j++) {
                keys.add(keyarr.getString(j));
            }
            responses.put(obj.getString("name"), new ImmutablePair<>(keys, obj.getString("response")));
        }
    }

    @Override
    public boolean handle(MessageReceivedEvent event) {
        if(event.getAuthor() == event.getJDA().getSelfInfo() || (!channels.isEmpty() && !channels.contains(event.getTextChannel().getId()))
                || event.getMessage().getContent().startsWith(servercfg.getPrefix())) {
            return false;
        }
        String content = event.getMessage().getContent().toLowerCase();
        Optional<String> response = responses.values().parallelStream().filter(r -> r.getLeft().parallelStream()
                .allMatch(k -> {
                    int i = content.indexOf(k);
                    return i >= 0                                               //exists
                            && (i == 0 || content.charAt(i - 1) == ' ')         //at beginning or after space
                            && (i + k.length() == content.length()              //et end or no alphanumeric
                                || !Character.isLetterOrDigit(content.charAt(i + k.length())));
                }))
                .map(Pair::getRight).findAny();
        if(response.isPresent()) {
            respondLog.info(String.format("[%s][%s] %s: %s\n\t->%s", event.getGuild().getName(), event.getTextChannel().getName(),
                    event.getAuthor().getUsername(), event.getMessage().getContent(), response.get()));
            MessageUtil.reply(event, response.get());
        }
        return false;
    }

    private String intConfig(String cfgString, ServerConfig cfg, MessageReceivedEvent event) {
        if(cfgString == null) {
            return getUsage();
        }
        String[] split = cfgString.split("\\s+", 2);
        if(split.length < 2) {
            return getUsage();
        }
        switch(split[0].toLowerCase()) {
            case "add":
                Matcher matcher = addPattern.matcher(split[1]);
                if(matcher.matches() && matcher.group(2).trim().length() > 0) {
                    Set<String> keys = new HashSet<>();
                    Collections.addAll(keys, matcher.group(2).toLowerCase().split("\\s+"));
                    responses.put(matcher.group(1).toLowerCase(), new ImmutablePair<>(keys, matcher.group(3)));
                    cfg.save();
                    return "Response " + matcher.group(1).toLowerCase() + " added.";
                }
                break;
            case "remove":
            case "del":
                if(responses.containsKey(split[1].toLowerCase())) {
                    responses.remove(split[1].toLowerCase());
                    cfg.save();
                    return "Response " + split[1].toLowerCase() + " removed.";
                } else {
                    return "Response " + split[1].toLowerCase() + " not found!";
                }
            case "channels":
                String[] split2 = split[1].split("\\s+", 2);
                if(split2.length == 2) {
                    switch(split2[0].toLowerCase()) {
                        case "add":
                            if(event.getMessage().getMentionedChannels().size() > 0) {
                                event.getMessage().getMentionedChannels().forEach(c -> channels.add(c.getId()));
                                cfg.save();
                                return "Channel(s) added.";
                            }
                            Optional<TextChannel> any = event.getGuild().getTextChannels().parallelStream().filter(tc -> tc.getName().equals(split2[1])).findAny();
                            if(any.isPresent()) {
                                channels.add(any.get().getId());
                                cfg.save();
                                return "Channel " + split2[1] + " added.";
                            } else {
                                return "Channel " + split2[1] + " not found!";
                            }
                        case "del":
                        case "remove":
                            if(event.getMessage().getMentionedChannels().size() > 0) {
                                event.getMessage().getMentionedChannels().forEach(c -> channels.remove(c.getId()));
                                cfg.save();
                                return "Channel(s) removed.";
                            }
                            Optional<TextChannel> any2 = event.getGuild().getTextChannels().parallelStream().filter(tc -> tc.getName().equals(split2[1])).findAny();
                            if(any2.isPresent()) {
                                if(channels.contains(any2.get().getId())) {
                                    channels.remove(any2.get().getId());
                                    cfg.save();
                                    return "Channel " + split2[1] + " removed.";
                                } else {
                                    return "Channel " + split2[1] + " was not added!";
                                }
                            } else {
                                return "Channel " + split2[1] + " not found!";
                            }
                    }
                }
                break;
        }
        return getUsage();
    }

    private String getUsage() {
        return "**Usage:**\n`add NAME [WORD WORD ...] RESPONSE` (include brackets)\n**Or:**\n`remove NAME`\n**Or:**\n`channels add/remove CHANNEL [CHANNEL ...]`\n\n" +
                "Registered: " + (responses.isEmpty() ? "None!" : StringUtils.join(responses.keySet(), ", "))
                + "\nChannels: " + (channels.isEmpty() ? "All" : channels.stream().map(id -> Main.api.getTextChannelById(id) == null ? null : Main.api.getTextChannelById(id).getName())
                .filter(s -> s != null).reduce((s1, s2) -> s1 + ", " + s2).get());
    }

}
