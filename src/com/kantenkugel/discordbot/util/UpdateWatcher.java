package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UpdateWatcher extends Thread {
    public static final int UPDATE_EXIT_CODE = 20;
    public static final int NORMAL_EXIT_CODE = 21;
    public static final int RESTART_CODE = 22;
    public static final int REVERT_CODE = 23;

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
                    case UPDATE_EXIT_CODE:
                        cmd = "update";
                    case NORMAL_EXIT_CODE:
                        if(cmd == null)
                            cmd = "shutdown";
                    case RESTART_CODE:
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
        return api.getUserById("122758889815932930").getPrivateChannel();
    }
}
