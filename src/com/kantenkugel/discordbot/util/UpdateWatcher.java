package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.TextChannel;

import java.io.IOException;
import java.nio.file.*;

public class UpdateWatcher extends Thread {
    private static final int UPDATE_EXIT_CODE = 20;
    private static final Path folder = Paths.get("C:\\Files\\Dropbox\\IdeaProjects\\DiscordBotJDA\\out\\artifacts\\DiscordBotJDA_jar");

    private final JDA api;

    public UpdateWatcher(JDA api) {
        this.api = api;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        WatchService watcher;
        WatchKey key;
        try {
            watcher = FileSystems.getDefault().newWatchService();
            folder.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        while(true) {
            try {
                key = watcher.take();
            } catch(InterruptedException e) {
                System.out.println("File-Watcher is getting shutdown!");
                break;
            }
            for(WatchEvent<?> watchEvent : key.pollEvents()) {
                WatchEvent.Kind<?> kind = watchEvent.kind();
                if(kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                System.out.println("Version change registered... restarting");
                getChannel(api).sendMessage("Detected version-change... updating");
                api.shutdown();
                System.exit(UPDATE_EXIT_CODE);
            }
        }

    }

    public static TextChannel getChannel(JDA api) {
        return api.getGuildsByName("Java Discord API").get(0).getTextChannels().stream().filter(c -> c.getName().equalsIgnoreCase("testing")).findAny().get();
    }
}
