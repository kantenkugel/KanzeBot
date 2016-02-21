package com.kantenkugel.discordbot.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TaskHelper {
    private static Map<String, Thread> tasks = new HashMap<>();

    public static boolean start(String name, Runnable runnable) {
        if(tasks.containsKey(name) && tasks.get(name).isAlive()) {
            return false;
        }
        Thread t = new Thread(runnable);
        tasks.put(name, t);
        t.start();
        return true;
    }

    public static boolean startTimed(String name, long timeout, Runnable runnable) {
        return start(name, () -> {
            while(!Thread.interrupted()) {
                runnable.run();
                try {
                    Thread.sleep(timeout);
                } catch(InterruptedException ex) {
                    break;
                }
            }
        });
    }

    public static void stop(String name) {
        tasks.get(name).interrupt();
    }

    public static void stopAll() {
        tasks.values().forEach(Thread::interrupt);
    }

    public static Set<String> getTasks() {
        return tasks.keySet();
    }
}
