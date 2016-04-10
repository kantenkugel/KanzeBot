package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.JDA;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Inviter extends Module {
    private static final Pattern authPattern = Pattern.compile("\\bhttps://(?:www\\.)?discordapp\\.com/oauth2/authorize/?\\?(?:[^\\s]*&)?client_id=(\\d+)(?:&.+)?\\b");


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
    public void configure(String cfgString, MessageEvent event, ServerConfig cfg) {
        MessageUtil.reply(event, cfg, "This module can not be configured");
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("invite", new CommandWrapper("This command/modules is getting removed!", (e, cfg) -> {
            MessageUtil.reply(e, cfg, "This command/modules is getting removed... " +
                    "If you want, you can join the `KanzeBot Hangout` Server (invite in `about` command) to discuss its future!");
        }));
        /*
        commands.put("invite", new CommandWrapper("Invites a bot to this Guild.\n**Usage:** `invite application_id`\n" +
                "**Or:** `invite Oauth-URL`", (e, cfg) -> {
            if(!PermissionUtil.checkPermission(e.getJDA().getSelfInfo(), Permission.MANAGE_SERVER, e.getGuild())) {
                MessageUtil.reply(e, cfg, "This Bot is missing the MANAGE_SERVER permission to do this!");
                return;
            }
            String[] args = MessageUtil.getArgs(e, cfg, 3);
            if(args.length != 2) {
                MessageUtil.reply(e, cfg, "Invalid syntax!");
                return;
            }
            String app_id;
            Matcher matcher = authPattern.matcher(args[1]);
            if(matcher.matches()) {
                app_id = matcher.group(1);
            } else {
                try {
                    Long.parseLong(args[1]);
                    app_id = args[1];
                } catch(NumberFormatException ex) {
                    MessageUtil.reply(e, cfg, "Given argument is not a valid application-id");
                    return;
                }
            }

            JSONObject botInfo = ((JDAImpl) e.getJDA()).getRequester().get("https://discordapp.com/api/oauth2/authorize?client_id=" + app_id + "&scope=bot").getObject();
            if(botInfo == null || !botInfo.has("bot")) {
                MessageUtil.reply(e, cfg, "Could not resolve Bot-name... is this a valid application-id?");
                return;
            }
            botInfo = botInfo.getJSONObject("bot");

            User botUser = e.getJDA().getUserById(botInfo.getString("id"));
            if(botUser != null && e.getGuild().getUsers().contains(botUser)) {
                MessageUtil.reply(e, cfg, "Bot "+botUser.getUsername()+" is already member of this Server!");
                return;
            }

            Requester.Response response = ((JDAImpl) e.getJDA()).getRequester().post("https://discordapp.com/api/oauth2/authorize?client_id=" + app_id + "&scope=bot",
                    new JSONObject().put("guild_id", e.getGuild().getId()).put("permissions", 0).put("authorize", true));
            if(response.isOk()) {
                MessageUtil.reply(e, cfg, "Bot **" + botInfo.getString("username") + "** was invited to this Guild");
            } else {
                MessageUtil.reply(e, cfg, "Bot **" + botInfo.getString("username") + "** could not be invited!");
            }
        }));
        */
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
