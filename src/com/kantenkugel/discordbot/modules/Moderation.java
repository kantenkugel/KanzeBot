package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.exceptions.PermissionException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class Moderation extends Module {
    private JSONObject config = null;
    private final Set<String> blacklisted = new HashSet<>();
    private ServerConfig servercfg;
    private int maxWarns = 2;
    private Map<User, Warns> warns = new HashMap<>();

    @Override
    public String getName() {
        return "moderation";
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
        if(cfgString == null) {
            MessageUtil.reply(event, "To add blacklisted words, use `add name [name]`, otherwise use `remove`\nTo change warnings before kick (current: "+maxWarns+"), use `warns NUM`\n" +
                    "Blacklisted: " + (blacklisted.isEmpty() ? "-" : blacklisted.stream().reduce((s1, s2) -> s1 + ", " + s2).get()), false);
        }
        else {
            String[] parts = cfgString.toLowerCase().split("\\s+");
            switch(parts[0]) {
                case "add":
                    blacklisted.addAll(Arrays.asList(parts).subList(1, parts.length));
                    MessageUtil.reply(event, "word(s) added");
                    cfg.save();
                    break;
                case "remove":
                    for(int i = 1; i < parts.length; i++) {
                        blacklisted.remove(parts[i]);
                    }
                    MessageUtil.reply(event, "word(s) removed");
                    cfg.save();
                    break;
                case "warns":
                    try {
                        maxWarns = Integer.parseInt(parts[1]);
                        MessageUtil.reply(event, "Warnings edited");
                        cfg.save();
                    } catch(NumberFormatException ex) {
                        MessageUtil.reply(event, "Misformatted number!");
                    }
                    break;
                default:
                    MessageUtil.reply(event, "Invalid Syntax");
            }
        }
    }

    @Override
    public Map<String, Command> getCommands() {
        return new HashMap<>();
    }

    @Override
    public JSONObject toJson() {
        JSONArray b = new JSONArray();
        blacklisted.forEach(b::put);
        return new JSONObject().put("blacklist", b)
                .put("warns", maxWarns);
    }

    @Override
    public void fromJson(JSONObject cfg) {
        this.config = cfg;
        if(!config.has("blacklist")) {
            config.put("blacklist", new JSONArray());
        }
        JSONArray blacklist = config.getJSONArray("blacklist");
        for(int i = 0; i < blacklist.length(); i++) {
            blacklisted.add(blacklist.getString(i));
        }
        if(!config.has("warns")) {
            config.put("warns", 2);
        }
        maxWarns = config.getInt("warns");
    }

    @Override
    public boolean handle(MessageReceivedEvent event) {
        if(event.getAuthor() == event.getJDA().getSelfInfo() || servercfg.isMod(event.getAuthor())) {
            return false;
        }
        String msg = event.getMessage().getContent().toLowerCase();
        if(blacklisted.stream().anyMatch(msg::contains)) {
            try {
                event.getMessage().deleteMessage();
            } catch(PermissionException ignored) {
            }
            if(!warns.containsKey(event.getAuthor())) {
                warns.put(event.getAuthor(), new Warns());
            }
            Warns warns = this.warns.get(event.getAuthor());
            if(warns.inc() > maxWarns) {
                try {
                    event.getGuild().getManager().ban(event.getAuthor(), 0);
                    MessageUtil.reply(event, event.getAuthor().getUsername() + " got banned for using to many restricted words!", false);
                } catch(PermissionException ignored) {
                    MessageUtil.reply(event, "I would ban you if i could! (" + warns.get() + " warnings!)");
                }
            } else {
                MessageUtil.reply(event, "Oh no you didnt just write that! (warning " + warns.get() + ")");
            }
            return true;
        }
        return false;
    }

    private static class Warns {
        private int w = 0;

        public int inc() {
            return ++w;
        }

        public int get() {
            return w;
        }
    }

}
