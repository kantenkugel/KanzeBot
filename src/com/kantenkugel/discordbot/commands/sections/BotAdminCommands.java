package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.DbEngine;
import com.kantenkugel.discordbot.Statics;
import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.commands.CustomCommand;
import com.kantenkugel.discordbot.config.BlackList;
import com.kantenkugel.discordbot.util.FinderUtil;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.MiscUtil;
import com.kantenkugel.discordbot.util.TaskHelper;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.User;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

public class BotAdminCommands implements CommandSection {

    private static final ScriptEngine engine = new ScriptEngineManager().getEngineByName("Nashorn");

    @Override
    public void register(Map<String, Command> registry, JDA api) {
        //eval
        engine.put("api", api);
        engine.put("commands", registry);
        engine.put("rng", new Random());
        engine.put("finder", new FinderUtil());
        engine.put("Command", CustomCommand.class);
        engine.put("TaskHelper", TaskHelper.class);
        try {
            engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");
        } catch(ScriptException e) {
            e.printStackTrace();
        }
        registry.put("eval", new CommandWrapper("Evaluates javascript code through the Rhino-interpreter. This will respond with whatever was __returned__ from the eval-script." +
                " Injected variables are: **api** (the jda object), **event** (the MessageReceivedEvent), **rng** (a global Random object), **config** (the internal ServerConfig object [see github])" +
                " and **commands** (the commands-map; use `new Command.static(String help, BiConsumer<MessageReceivedEvent, ServerConfig>)` to create new commands).", (m, cfg) -> {
            Message msg;
            engine.put("event", m);
            engine.put("config", cfg);
            try {
                Object out = engine.eval("(function(){ with(imports) {"
                        + m.getContent().substring(cfg.getPrefix().length() + 5)
                        + "} })();");
                msg = new MessageBuilder().appendString(out == null ? "Done!" : MessageUtil.strip(out.toString()), MessageBuilder.Formatting.BLOCK).build();
            } catch(Exception e) {
                msg = new MessageBuilder().appendString(e.getMessage(), MessageBuilder.Formatting.BLOCK).build();
            }
            reply(m, msg);
        }).acceptPriv(Command.Priv.BOTADMIN));

        //Control-commands
        registry.put("shutdown", new CommandWrapper("Shuts down this bot. Be careful or Kantenkugel will kill you!", (msg, cfg) -> {
            MessageUtil.replySync(msg, cfg, "OK, Bye!");
            MiscUtil.shutdown(Statics.NORMAL_EXIT_CODE);
        }).acceptPriv(Command.Priv.BOTADMIN));
        registry.put("restart", new CommandWrapper("Restarts this bot.", (msg, cfg) -> {
            MessageUtil.replySync(msg, cfg, "OK, BRB!");
            MiscUtil.shutdown(Statics.RESTART_EXIT_CODE);
        }).acceptPriv(Command.Priv.BOTADMIN));
        registry.put("update", new CommandWrapper("Updates this bot.", (msg, cfg) -> {
            MessageUtil.replySync(msg, cfg, "OK, BRB!");
            MiscUtil.shutdown(Statics.UPDATE_EXIT_CODE);
        }).acceptPriv(Command.Priv.BOTADMIN));

        //Blacklist
        registry.put("blacklist", new CommandWrapper("Blocks users from accessing features of this bot.\n" +
                "Usage: `blacklist add|remove|del @Mention [@Mention]`\nOr: `blacklist add|remove|del userid`\n" +
                "Or: `blacklist show`", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 3);
            if(args.length == 1) {
                reply(e, cfg, registry.get("blacklist").getDescription());
                return;
            }
            List<User> mentioned = e.getMessage().getMentionedUsers();
            switch(args[1].toLowerCase()) {
                case "add":
                    if(mentioned.isEmpty()) {
                        if(args.length == 3) {
                            if(!MessageUtil.getGlobalAdmins().contains(args[2]))
                                BlackList.add(args[2]);
                        } else {
                            reply(e, cfg, registry.get("blacklist").getDescription());
                            return;
                        }
                    } else {
                        mentioned.stream().filter(u -> !MessageUtil.isGlobalAdmin(u)).forEach(BlackList::add);
                    }
                    break;
                case "del":
                case "remove":
                    if(mentioned.isEmpty()) {
                        if(args.length == 3) {
                            BlackList.remove(args[2]);
                        } else {
                            reply(e, cfg, registry.get("blacklist").getDescription());
                            return;
                        }
                    } else {
                        mentioned.forEach(BlackList::remove);
                    }
                    break;
                case "show":
                case "list":
                    Optional<String> black = BlackList.get().stream().map(b -> {
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
            reply(e, cfg, "User(s) added/removed from blacklist!");
        }).acceptPriv(Command.Priv.BOTADMIN));

        registry.put("delay", new CommandWrapper("Gets the delay between a message create and KanzeBot reading it", (e, cfg) -> {
            long until = e.getMessage().getTime().until(OffsetDateTime.now(), ChronoUnit.MILLIS);
            reply(e, cfg, "Delay to KanzeBot: " + until + "ms");
        }).acceptPriv(Command.Priv.BOTADMIN));

        registry.put("sql", new CommandWrapper("Executes SQL on the db", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 2);
            if(args.length == 1) {
                reply(e, cfg, "Please provide a SQL string");
            } else if(!args[1].substring(0, 6).toLowerCase().equals("select")) {
                reply(e, cfg, "Only SELECT allowed, sry!");
            } else {
                try {
                    String response = "Response:\n`" + DbEngine.stringify(DbEngine.query(args[1])) + "`";
                    if(response.length() > 2000) {
                        reply(e, cfg, "Sry, the response is to big!");
                    } else {
                        reply(e, cfg, response);
                    }
                } catch(SQLException ex) {
                    reply(e, cfg, "There was an error running the query:\n" + ex.getMessage());
                }
            }
        }).acceptPriv(Command.Priv.BOTADMIN));
    }
}
