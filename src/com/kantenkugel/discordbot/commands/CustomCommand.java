package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;

import java.util.function.BiConsumer;

public class CustomCommand extends CommandWrapper {
    public CustomCommand(String desc, BiConsumer<MessageEvent, ServerConfig> biFunction) {
        super(desc, biFunction);
    }
}
