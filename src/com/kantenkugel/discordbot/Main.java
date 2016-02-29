package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.commands.CommandRegistry;
import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.UpdateChecker;
import com.kantenkugel.discordbot.util.UpdateWatcher;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;

import javax.security.auth.login.LoginException;

/**
 * Created by Michael Ritter on 05.12.2015.
 */
public class Main {
    public static int VERSION;
    public static JDA api;
    public static UpdateChecker checker = null;

    public static final SimpleLog LOG = SimpleLog.getLog("KanzeBot");

    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Missing arguments!");
            return;
        }

        if(args.length > 2) {
            CommandRegistry.START_TIME = Long.parseLong(args[2]);
        }

        if(args.length > 3) {
            VERSION = Integer.parseInt(args[3]);
        } else {
            VERSION = 0;
        }

        CommandRegistry.init();
        Module.init();
        try {
            JDABuilder jdaBuilder = new JDABuilder(args[0], args[1]).addListener(new CommandRegistry());
            if(args.length == 5) {
                boolean success = Boolean.parseBoolean(args[4]);
                if(success) {
                    checker = UpdateChecker.getInstance();
                    checker.start();
                }
                jdaBuilder.addListener(new UpdatePrintListener(success));
            }
            api = jdaBuilder.buildAsync();
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
                if(checker != null) {
                    checker.interrupt();
                }
                UpdateWatcher.getChannel(event.getJDA()).sendMessage("Update was " + (success ? "successful" : "unsuccessful") + "!");
                event.getJDA().removeEventListener(this);
            }
        }
    }

}
