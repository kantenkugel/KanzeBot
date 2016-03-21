package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.util.BotConfig;
import com.kantenkugel.discordbot.util.MessageUtil;
import com.kantenkugel.discordbot.util.ServerConfig;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator extends Module {
    private static final Pattern responsePattern = Pattern.compile("^<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">(.*)</string>$");
    private static String clientId = null;
    private static String clientSecret = null;
    private static boolean initialized = false;
    private static Token token = null;

    @Override
    public String getName() {
        return "translate";
    }

    @Override
    public boolean availableInPms() {
        return true;
    }

    @Override
    public void init(JDA jda, ServerConfig cfg) {
        if(!initialized) {
            initialized = true;
            JSONObject c = BotConfig.get("translate", new JSONObject().put("clientId", "").put("clientSecret", ""));
            clientId = c.getString("clientId");
            clientSecret = c.getString("clientSecret");
        }
    }

    @Override
    public void configure(String cfgString, MessageReceivedEvent event, ServerConfig cfg) {
        MessageUtil.reply(event, cfg, "This module has no Configuration options!");
    }

    @Override
    public Map<String, Command> getCommands() {
        Map<String, Command> commands = new HashMap<>();
        commands.put("translate", new CommandWrapper("Translates a given string into another language\n" +
                "Usage: `translate [-LN] TEXT` where ln is the destination language (optional) and text is the text you want to translate.", (e, cfg) -> {
            String[] args = MessageUtil.getArgs(e, cfg, 3);
            if(args.length > 1) {
                String language = "en";
                String text;
                if(args.length > 2 && args[1].startsWith("-")) {
                    language = args[1].substring(1);
                    text = args[2];
                } else {
                    text = args[1] + (args.length > 2 ? " " + args[2] : "");
                }
                String translated = translate(language, text);
                if(translated != null) {
                    MessageUtil.reply(e, cfg, "Translated text: " + translated, false);
                } else {
                    MessageUtil.reply(e, cfg, "Error translating!");
                }
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

    private static String translate(String language, String text) {
        if(token == null || token.expired()) {
            if(!getToken()) {
                return null;
            }
        }
        try {
            String body = Unirest.get("http://api.microsofttranslator.com/v2/Http.svc/Translate?Text=" + URLEncoder.encode(text, "UTF-8") + "&To=" + URLEncoder.encode(language, "UTF-8"))
                    .header("Authorization", token.getAuthString())
                    .asString().getBody();
            Matcher m = responsePattern.matcher(body);
            if(m.matches()) {
                return m.group(1);
            }
        } catch(UnirestException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean getToken() {
        if(clientId == null || clientSecret == null || clientId.isEmpty() || clientSecret.isEmpty()) {
            return false;
        }
        try {
            String body = Unirest.post("https://datamarket.accesscontrol.windows.net/v2/OAuth2-13")
                    .field("grant_type", "client_credentials")
                    .field("client_id", clientId)
                    .field("client_secret", clientSecret)
                    .field("scope", "http://api.microsofttranslator.com/")
                    .asString().getBody();
            JSONObject o = new JSONObject(body);
            if(!o.has("access_token")) {
                System.err.println("Error logging into Translation service");
            } else {
                token = new Token(o.getString("access_token"), Integer.parseInt(o.getString("expires_in")));
                return true;
            }
        } catch(UnirestException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static class Token {
        private final long expires;
        private final String key;

        public Token(String key, int expires) {
            this.key = key;
            this.expires = System.currentTimeMillis() + 1000 * expires;
        }

        public boolean expired() {
            return System.currentTimeMillis() > expires;
        }

        public String getAuthString() {
            return "Bearer " + key;
        }
    }
}
