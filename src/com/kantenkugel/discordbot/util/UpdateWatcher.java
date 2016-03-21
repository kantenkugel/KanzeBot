package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.Statics;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UpdateWatcher extends Thread {
    private final JDA api;

    public UpdateWatcher(JDA api) {
        this.api = api;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                String cmd = reader.readLine();
                int code = Integer.parseInt(cmd);
                cmd = null;
                switch(code) {
                    case Statics.UPDATE_EXIT_CODE:
                        cmd = "update";
                    case Statics.NORMAL_EXIT_CODE:
                        if(cmd == null)
                            cmd = "shutdown";
                    case Statics.RESTART_EXIT_CODE:
                        if(cmd == null)
                            cmd = "restart";
                        getChannel(api).sendMessage("Wrapper requested to " + cmd + ". Doing so now...");
                        MiscUtil.await(api, () -> System.exit(code));
                        api.shutdown();
                        return;
                    default:
                        System.out.println("UpdateWatcher got unknown command-code " + code + "... ignoring");
                }
            } catch(IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static MessageChannel getChannel(JDA api) {
        return Statics.botOwner.getPrivateChannel();
    }
}
