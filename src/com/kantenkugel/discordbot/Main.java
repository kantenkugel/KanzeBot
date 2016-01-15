package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.commands.CommandRegistry;
import com.kantenkugel.discordbot.modules.Eve;
import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.UpdateWatcher;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;

import javax.security.auth.login.LoginException;

/**
 * Created by Michael Ritter on 05.12.2015.
 */
public class Main {
    public static JDA api;

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Missing arguments!");
            return;
        }

        if(args.length > 2) {
            CommandRegistry.START_TIME = Long.parseLong(args[2]);
        }

        CommandRegistry.init();
        Module.register(Eve.class);
        try {
            JDABuilder jdaBuilder = new JDABuilder(args[0], args[1]).addListener(new CommandRegistry());
            if(args.length == 4) {
                boolean success = Boolean.parseBoolean(args[3]);
                jdaBuilder.addListener(new UpdatePrintListener(success));
            }
            api = jdaBuilder.build();
            CommandRegistry.setJDA(api);
            new UpdateWatcher(api);
        } catch(LoginException e) {
            e.printStackTrace();
        }
    }

    private static class UpdatePrintListener implements EventListener {
        private final boolean success;
        private UpdatePrintListener(boolean success) {
            this.success = success;
        }
        @Override
        public void onEvent(Event event) {
            if(event instanceof ReadyEvent) {
                UpdateWatcher.getChannel(event.getJDA()).sendMessage("Update was " + (success ? "successful" : "unsuccessful") + "!");
                event.getJDA().removeEventListener(this);
            }
        }
    }

}
