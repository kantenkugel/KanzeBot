package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandRegistry;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.exceptions.BlockedException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

public class InfoCommands implements CommandSection {
    @Override
    public void register(Map<String, Command> registry, JDA api) {
        registry.put("about", new CommandWrapper("Shows some basic info about this Bot", (e, cfg) -> {
            reply(e, cfg, String.format("```\n" + e.getJDA().getSelfInfo().getUsername() + " info:" +
                            "\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%-16s: %s\n%s\n```",
                    "Bot-ID", e.getJDA().getSelfInfo().getId(), "Owner", Statics.botOwner.getUsername() + '#' + Statics.botOwner.getDiscriminator(),
                    "Owner-ID", Statics.botOwner.getId(), "Version-rev.", Statics.VERSION,
                    "Library", "JDA (v" + Statics.JDAVERSION + ")",
                    "If you need help, or have suggestions, feel free to join my discord server: https://discord.gg/0tYwGhNHaw5MZjJn"));
        }));

        registry.put("help", new CommandWrapper("HELP ME WITH HELP", (m, cfg) -> {
            String[] args = MessageUtil.getArgs(m, cfg, 3);
            if(args.length > 1) {
                Command command = registry.get(args[1].toLowerCase());
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
            registry.entrySet().parallelStream().filter(entry -> entry.getValue().isAvailable(m, cfg)).sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).sequential().forEach(c -> {
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
                m.getAuthor().getPrivateChannel().sendMessage("Commands available for **" + (m.isPrivate() ? "PM" : "Guild " + m.getGuild().getName())
                        + "**:\n\n" + returned + "\n**NOTE**: you can type `help COMMAND` to get more detailed info about a specific command." +
                        "\n**NOTE 2**: This command shows only the commands available to you for the Guild/Channel you called help from!" +
                        "\nTherefore calling help via PM does not show moderation-commands and other commands exclusively available in Guilds." +
                        "\nIt also only shows commands that you have the correct privileges for (Owner/Admin/Mod).");
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

        registry.put("info", new CommandWrapper("Prints minimalistic info about your User, the TextChannel and the Guild.", (event, cfg) -> {
            String text = String.format("```\nUser:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n" +
                            "Channel:\n\t%-15s%s\n\t%-15s%s\n" +
                            "Guild:\n\t%-15s%s\n\t%-15s%s\n\t%-15s%s\n```",
                    "Name", event.getAuthor().getUsername(), "ID", event.getAuthor().getId(), "Discriminator", event.getAuthor().getDiscriminator(),
                    "Avatar", event.getAuthor().getAvatarUrl() == null ? "None" : '<' + event.getAuthor().getAvatarUrl() + '>',
                    "Name", event.getTextChannel().getName(), "ID", event.getTextChannel().getId(),
                    "Name", event.getGuild().getName(), "ID", event.getGuild().getId(),
                    "Icon", event.getGuild().getIconUrl() == null ? "None" : '<' + event.getGuild().getIconUrl() + '>');
            reply(event, cfg, text, false);
        }).acceptPrivate(false));

        registry.put("stats", new CommandWrapper("Displays some stats about KanzeBot", (e, cfg) -> {
            String stats = String.format("%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n%-15s%s\n\n%s\n%s",
                    "Guilds:", e.getJDA().getGuilds().size(),
                    "Users (Unique):", e.getJDA().getGuilds().stream().map(g -> g.getUsers().size()).reduce(0, (s1, s2) -> s1 + s2) + " (" + e.getJDA().getUsers().size() + ')',
                    "Uptime:", MiscUtil.getUptime(),
                    "Messages seen:", CommandRegistry.getMessageCount(),
                    "Commands seen:", CommandRegistry.getCommandCount(),
                    "Version rev:", Statics.VERSION,
                    "Changes of current version:", Statics.CHANGES);
            reply(e, new MessageBuilder().appendString("Stats for KanzeBot:\n")
                    .appendCodeBlock(stats, "").build());
        }));

        registry.put("uptime", new CommandWrapper("Prints the uptime... DUH!", (event, cfg) -> {
            reply(event, cfg, "Running for " + MiscUtil.getUptime());
        }));
    }
}
