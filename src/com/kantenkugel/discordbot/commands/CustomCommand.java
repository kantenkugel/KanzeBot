package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.util.function.BiConsumer;

public class CustomCommand extends CommandWrapper {
    public CustomCommand(String desc, BiConsumer<MessageReceivedEvent, ServerConfig> biFunction) {
        super(desc, biFunction);
    }
}
