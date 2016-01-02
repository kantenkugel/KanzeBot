package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.Item;
import com.kantenkugel.discordbot.SolarSystem;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.entities.impl.JDAImpl;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class EveCommands {
    private static final List<String> eveChats = Arrays.asList("eve-online");
    private static final boolean enabled = false;

    public static void init(Map<String, Command> registry) {
        if(!enabled) {
            return;
        }
        registry.put("route", setAccess(new CommandWrapper((msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg);
            if(split.length > 2) {
                SolarSystem start = SolarSystem.get(split[1]);
                SolarSystem end = SolarSystem.get(split[2]);
                boolean unique = true;
                if(start == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "Start System not found!");
                    } else {
                        MessageUtil.reply(msg, "Start System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(end == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[2]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "End System not found!");
                    } else {
                        MessageUtil.reply(msg, "End System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(unique) {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/route/from/" + start.id + "/to/" + end.id);
                    MessageUtil.reply(msg, "There are " + array.length() + " jumps between " + start.name + " and " + end.name);
                }
            }
        })));
        registry.put("nexthub", setAccess(new CommandWrapper((msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg);
            if(split.length > 1) {
                SolarSystem start = SolarSystem.get(split[1]);
                if(start == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "System not found!");
                    } else {
                        MessageUtil.reply(msg, "System not unique... Possible: " + getSystemList(all));
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
                        MessageUtil.reply(msg, "Closest Hub to " + start.name + " is " + closest.name + " with " + closestJumps + " jumps distance");
                    }
                }
            }
        })));
        registry.put("price", setAccess(new CommandWrapper((msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg, 2);
            if(split.length > 1) {
                Item i = Item.get(split[1]);
                if(i == null) {
                    Set<Item> all = Item.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "Item not found!");
                    } else {
                        MessageUtil.reply(msg, "Item not unique... Possible: " + getItemList(all));
                    }
                } else {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/marketstat/json?typeid=" + i.id + "&usesystem=" + SolarSystem.get("jita").id);
                    double maxbuy = array.getJSONObject(0).getJSONObject("buy").getDouble("max");
                    double minsell = array.getJSONObject(0).getJSONObject("sell").getDouble("min");
                    MessageUtil.reply(msg, String.format("Stats for %s in Jita: Sell: %,.2f; Buy: %,.2f", i.name, minsell, maxbuy));
                }
            }
        })));
        registry.put("pricein", setAccess(new CommandWrapper((msg, cfg) -> {
            String[] split = MessageUtil.getArgs(msg, cfg, 3);
            if(split.length > 2) {
                boolean unique = true;
                Item i = Item.get(split[1]);
                if(i == null) {
                    Set<Item> all = Item.getAll(split[1]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "Item not found!");
                    } else {
                        MessageUtil.reply(msg, "Item not unique... Possible: " + getItemList(all));
                    }
                    unique = false;
                }
                SolarSystem sys = SolarSystem.get(split[2]);
                if(sys == null) {
                    Set<SolarSystem> all = SolarSystem.getAll(split[2]);
                    if(all.size() == 0) {
                        MessageUtil.reply(msg, "System not found!");
                    } else {
                        MessageUtil.reply(msg, "System not unique... Possible: " + getSystemList(all));
                    }
                    unique = false;
                }
                if(unique) {
                    JSONArray array = makeGetRequest("http://api.eve-central.com/api/marketstat/json?typeid=" + i.id + "&usesystem=" + sys.id);
                    double maxbuy = array.getJSONObject(0).getJSONObject("buy").getDouble("max");
                    double minsell = array.getJSONObject(0).getJSONObject("sell").getDouble("min");
                    MessageUtil.reply(msg, String.format("Stats for %s in %s: Sell: %,.2f; Buy: %,.2f", i.name, sys.name, minsell, maxbuy));
                }
            }
        })));
        registry.put("eve", new CommandWrapper(m -> {
            m.getAuthor().getPrivateChannel().sendMessage("Eve commands are only available via private chat. Type !help here for a list of available commands");
            m.getMessage().deleteMessage();
        }).acceptCustom((event, cfg) -> !event.isPrivate() && !eveChats.contains(event.getTextChannel().getName().toLowerCase())));
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

    private static JSONArray makeGetRequest(String url) {
        return new JDAImpl().getRequester().getA(url);
    }

    private static Command setAccess(Command wrapper) {
        wrapper.acceptPrivate(true);
//        eveChats.forEach(wrapper::acceptChat);
        return wrapper;
    }
}
