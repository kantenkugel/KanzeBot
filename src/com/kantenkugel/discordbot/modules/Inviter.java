package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.PermissionUtil;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Inviter extends Module {


    @Override
    public String getName() {
        return "inviter";
    }

    @Override
    public boolean availableInPms() {
        return false;
    }

    @Override
    public void init(JDA jda, ServerConfig cfg) {
    }

    @Override
    public void configure(String cfgString, MessageReceivedEvent event, ServerConfig cfg) {
        MessageUtil.reply(event, "This module can not be configured");
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("invite", new CommandWrapper("Invites a bot to this Guild.\n**Usage:** `invite application_id`", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 3);
            if(args.length != 2) {
                MessageUtil.reply(e, "Invalid syntax!");
                return;
            }
            try {
                Long.parseLong(args[1]);
            } catch(NumberFormatException ex) {
                MessageUtil.reply(e, "Given argument is not a valid application-id");
                return;
            }

            if(PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_SERVER, e.getGuild())) {
                ((JDAImpl) e.getJDA()).getRequester().post("https://discordapp.com/api/oauth2/authorize?client_id=" + args[1] + "&scope=bot",
                        new JSONObject().put("guild_id", e.getGuild().getId()).put("permissions", 0).put("authorize", true));
            } else {
                MessageUtil.reply(e, "This Bot is missing the MANAGE_SERVER permission to do this!");
            }
        }));
        return commands;
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject();
    }

    @Override
    public void fromJson(JSONObject cfg) {

    }
}
