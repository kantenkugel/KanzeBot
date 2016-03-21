package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.commands.CommandRegistry;
import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.UpdateValidator;
import com.kantenkugel.discordbot.util.UpdateWatcher;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.hooks.EventListener;
import net.dv8tion.jda.utils.SimpleLog;
import org.apache.commons.lang3.StringUtils;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;

/**
 * Created by Michael Ritter on 05.12.2015.
 */
public class Main {
    public static UpdateValidator checker = null;

    /*
    Args:
        0  email,
        1  password,
        2  system-time of wrapper-start (for uptime),
        3  success-indicator (true/false/"-")
        4  version-number
        5+ MULTIPLE/NONE strings describing the changelog of this version
    */
    public static void main(String[] args) {
//        Requester.LOG.setLevel(SimpleLog.Level.TRACE);
        if(args.length < 5) {
            System.out.println("Missing arguments!");
            return;
        }

        try {
            SimpleLog.addFileLogs(new File("logs/main.txt"), new File("logs/err.txt"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        Statics.START_TIME = Long.parseLong(args[2]);
        Statics.VERSION = Integer.parseInt(args[4]);

        if(args.length > 5) {
            Statics.CHANGES = StringUtils.join(args, '\n', 5, args.length);
        } else {
            Statics.CHANGES = null;
        }

        CommandRegistry.init();
        Module.init();
        try {
            JDABuilder jdaBuilder;
            if(args[1].equals("-")) {
                jdaBuilder = new JDABuilder(args[0]);
            } else {
                jdaBuilder = new JDABuilder(args[0], args[1]);
            }
            jdaBuilder.setAudioEnabled(false).addListener(new CommandRegistry());
            if(!args[3].equals("-")) {
                boolean success = Boolean.parseBoolean(args[3]);
                if(success) {
                    checker = UpdateValidator.getInstance();
                    checker.start();
                }
                jdaBuilder.addListener(new UpdatePrintListener(success));
            }
            Statics.jdaInstance = jdaBuilder.buildAsync();
            CommandRegistry.setJDA(Statics.jdaInstance);
            new UpdateWatcher(Statics.jdaInstance);
        } catch(LoginException e) {
            Statics.LOG.fatal("Login informations were incorrect!");
            System.err.flush();
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
                UpdateWatcher.getChannel(event.getJDA()).sendMessage("Update was " + (success ? "" : "**NOT**") + "successful!\nCurrent revision: " + Statics.VERSION);
                if(Statics.CHANGES != null) {
                    UpdateWatcher.getChannel(event.getJDA()).sendMessage("Changes for this revision:\n" + Statics.CHANGES);
                }
                event.getJDA().removeEventListener(this);
            }
        }
    }

}
