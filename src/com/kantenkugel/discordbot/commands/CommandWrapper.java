package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandWrapper extends Command {
    private final Consumer<MessageReceivedEvent> function;
    private final BiConsumer<MessageReceivedEvent, ServerConfig> biFunction;
    private final String description;

    public CommandWrapper(String desc, Consumer<MessageReceivedEvent> function) {
        this.description = desc;
        this.function = function;
        this.biFunction = null;
    }

    public CommandWrapper(String desc, BiConsumer<MessageReceivedEvent, ServerConfig> biFunction) {
        this.description = desc;
        this.biFunction = biFunction;
        this.function = null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void accept(MessageReceivedEvent event, ServerConfig cfg) {
        if(biFunction != null) {
            biFunction.accept(event, cfg);
        }
        if(function != null) {
            function.accept(event);
        }
    }
}
