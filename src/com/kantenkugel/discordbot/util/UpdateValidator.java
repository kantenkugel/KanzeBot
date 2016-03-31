package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.Statics;

public class UpdateValidator extends Thread {
    private static final int MAX_TIME = 30*1000;
    private static UpdateValidator instance;
    public static synchronized UpdateValidator getInstance() {
        if(instance == null) {
            instance = new UpdateValidator();
        }
        return instance;
    }

    private UpdateValidator() {
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(MAX_TIME);
        } catch(InterruptedException e) {
            return;
        }
        System.out.println("Failed to start, reverting to old version");
        System.out.flush();
        MiscUtil.shutdown(Statics.REVERT_EXIT_CODE);
    }
}
