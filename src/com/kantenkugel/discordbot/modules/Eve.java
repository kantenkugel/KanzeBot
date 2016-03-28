package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.moduleutils.Item;
import com.kantenkugel.discordbot.moduleutils.SolarSystem;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Channel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.impl.JDAImpl;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class Eve extends Module {
    private static boolean initialized = false;
    private final Set<String> availableChats = new HashSet<>();
    private JSONObject config = null;

    public Map<String, Command> getCommands() {
        Map<String, Command> registry = new HashMap<>();

        registry.put("route", setAccess(new CommandWrapper("Gets the amount of jumps between 2 Systems.\n" +
                "Usage: `route SYS1 SYS2` where sys1/sys2 are the system-names (or prefixes of them).", (msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg);
            if(split.length > 2) {
                SolarSystem start = SolarSystem.get(split[1]);
                SolarSystem end = SolarSystem.get(split[2]);
                boolean unique = true;
                if(start == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "Start System not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "Start System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(end == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[2]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "End System not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "End System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(unique) {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/route/from/" + start.id + "/to/" + end.id);
                    MessageUtil.reply(msg, cfg, "There are " + array.length() + " jumps between " + start.name + " and " + end.name);
                }
            }
        })));
        registry.put("nexthub", setAccess(new CommandWrapper("Gets the closest trade-hub to the provided System (and the jump count).\n" +
                "Usage: `nexthub SYS` where sys is the system-name (or a prefix of one).", (msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg);
            if(split.length > 1) {
                SolarSystem start = SolarSystem.get(split[1]);
                if(start == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "System not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "System not unique... Possible: " + getSystemList(all));
                    }
                } else {
                    SolarSystem closest = null;
                    int closestJumps = Integer.MAX_VALUE;
                    for(SolarSystem hub : SolarSystem.hubs) {
                        JSONArray array = makeGetRequest("http://api.eve-central.com/api/route/from/" + start.id + "/to/" + hub.id);
                        if(array.length() < closestJumps) {
                            closest = hub;
                            closestJumps = array.length();
                        }
                    }
                    if(closest != null) {
                        MessageUtil.reply(msg, cfg, "Closest Hub to " + start.name + " is " + closest.name + " with " + closestJumps + " jumps distance");
                    }
                }
            }
        })));
        registry.put("price", setAccess(new CommandWrapper("Gets the buy/sell price of provided item in Jita.\n" +
                "Usage: `price NAME` where name is the item name (or a prefix of it).", (msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg, 2);
            if(split.length > 1) {
                Item i = Item.get(split[1]);
                if(i == null) {
                    Set<Item> all = Item.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "Item not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "Item not unique... Possible: " + getItemList(all));
                    }
                } else {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/marketstat/json?typeid=" + i.id + "&usesystem=" + SolarSystem.get("jita").id);
                    double maxbuy = array.getJSONObject(0).getJSONObject("buy").getDouble("max");
                    double minsell = array.getJSONObject(0).getJSONObject("sell").getDouble("min");
                    MessageUtil.reply(msg, cfg, String.format("Stats for %s in Jita: Sell: %,.2f; Buy: %,.2f", i.name, minsell, maxbuy));
                }
            }
        })));
        registry.put("pricein", setAccess(new CommandWrapper("Gets the buy/sell price of provided item in the provided System.\n" +
                "Usage: `pricein NAME SYS` where name/sys are the item/system name (or a prefix of them).", (msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg, 3);
            if(split.length > 2) {
                boolean unique = true;
                Item i = Item.get(split[1]);
                if(i == null) {
                    Set<Item> all = Item.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "Item not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "Item not unique... Possible: " + getItemList(all));
                    }
                    unique = false;
                }
                SolarSystem sys = SolarSystem.get(split[2]);
                if(sys == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[2]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, cfg, "System not found!");
                    } else {
                        MessageUtil.reply(msg, cfg, "System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(unique) {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/marketstat/json?typeid=" + i.id + "&usesystem=" + sys.id);
                    double maxbuy = array.getJSONObject(0).getJSONObject("buy").getDouble("max");
                    double minsell = array.getJSONObject(0).getJSONObject("sell").getDouble("min");
                    MessageUtil.reply(msg, cfg, String.format("Stats for %s in %s: Sell: %,.2f; Buy: %,.2f", i.name, sys.name, minsell, maxbuy));
                }
            }
        })));
        return registry;
    }

    @Override
    public void configure(String cfgString, MessageEvent event, ServerConfig cfg) {
        if(cfgString == null) {
            Optional<String> chans = event.getGuild().getTextChannels().stream().filter(c -> availableChats.contains(c.getId())).map(Channel::getName).reduce((s1, s2) -> s1 + ", " + s2);
            MessageUtil.reply(event, cfg, "Use addChannel/removeChannel CHANNELNAME to add/remove channels to whitelist"
                    + "\nCurrent channels: " + (chans.isPresent() ? chans.get() : "All"));
            return;
        }
        String[] split = cfgString.toLowerCase().split("\\s+", 2);
        if(split.length != 2) {
            MessageUtil.reply(event, cfg, "Invalid Syntax");
        } else {
            Optional<TextChannel> chan = event.getGuild().getTextChannels().stream().filter(c -> c.getName().toLowerCase().equals(split[1])).findAny();
            if(split[0].equals("addchannel")) {
                if(chan.isPresent()) {
                    availableChats.add(chan.get().getId());
                    updateConfig();
                    cfg.save();
                    MessageUtil.reply(event, cfg, "Channel added");
                } else {
                    MessageUtil.reply(event, cfg, "Channel not found!");
                }
            } else if(split[0].equals("removechannel")) {
                if(chan.isPresent()) {
                    availableChats.remove(chan.get().getId());
                    updateConfig();
                    cfg.save();
                    MessageUtil.reply(event, cfg, "Channel removed");
                } else {
                    MessageUtil.reply(event, cfg, "Channel not found!");
                }
            } else {
                MessageUtil.reply(event, cfg, "Invalid Syntax");
            }
        }
    }

    @Override
    public void init(JDA jda, ServerConfig cfg) {
        synchronized(jda) {
            if(!initialized) {
                initialized = true;
                Item.init();
                SolarSystem.init();
            }
        }
    }

    @Override
    public JSONObject toJson() {
        if(config == null) {
            //Default config
            return new JSONObject().put("chats", new JSONArray());
        }
        return config;
    }

    @Override
    public void fromJson(JSONObject cfg) {
        this.config = cfg;
        if(cfg.has("chats")) {
            JSONArray chats = cfg.getJSONArray("chats");
            for(int i = 0; i < chats.length(); i++) {
                this.availableChats.add(chats.getString(i));
            }
        } else {
            config.put("chats", new JSONArray());
        }
    }

    @Override
    public String getName() {
        return "eve";
    }

    @Override
    public boolean availableInPms() {
        return true;
    }

    private static String getSystemList(Set<SolarSystem> systems) {
        StringBuilder b = new StringBuilder();
        for(SolarSystem system : systems) {
            b.append(system.name).append(", ");
        }
        b.setLength(b.length() - 2);
        return b.toString();
    }

    private static String getItemList(Set<Item> items) {
        StringBuilder b = new StringBuilder();
        for(Item item : items) {
            b.append(item.name).append(", ");
        }
        b.setLength(b.length() - 2);
        return b.toString();
    }

    private void updateConfig() {
        JSONArray chats = new JSONArray();
        availableChats.forEach(chats::put);
        config.put("chats", chats);
    }

    private static JSONArray makeGetRequest(String url) {
        return new JDAImpl(false).getRequester().getA(url);
    }

    private Command setAccess(Command wrapper) {
        wrapper.acceptCustom((e, cfg) -> e.isPrivate() || availableChats.isEmpty() || availableChats.contains(e.getTextChannel().getId()));
        return wrapper;
    }
}
