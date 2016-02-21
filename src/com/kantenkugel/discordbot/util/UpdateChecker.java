package com.kantenkugel.discordbot.util;

public class UpdateChecker extends Thread {
    private static final int MAX_TIME = 30*1000;
    private static UpdateChecker instance;
    public static synchronized UpdateChecker getInstance() {
        if(instance == null) {
            instance = new UpdateChecker();
        }
        return instance;
    }

    private UpdateChecker() {
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
        System.exit(UpdateWatcher.REVERT_CODE);
    }
}
