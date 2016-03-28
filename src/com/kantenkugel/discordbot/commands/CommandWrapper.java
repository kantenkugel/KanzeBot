package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class CommandWrapper extends Command {
    private final Consumer<MessageEvent> function;
    private final BiConsumer<MessageEvent, ServerConfig> biFunction;
    private final String description;

    public CommandWrapper(String desc, Consumer<MessageEvent> function) {
        this.description = desc;
        this.function = function;
        this.biFunction = null;
    }

    public CommandWrapper(String desc, BiConsumer<MessageEvent, ServerConfig> biFunction) {
        this.description = desc;
        this.biFunction = biFunction;
        this.function = null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void accept(MessageEvent event, ServerConfig cfg) {
        if(biFunction != null) {
            biFunction.accept(event, cfg);
        }
        if(function != null) {
            function.accept(event);
        }
    }
}
