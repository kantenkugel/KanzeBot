package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public abstract class Command implements BiConsumer<MessageReceivedEvent, ServerConfig> {
    protected Boolean requiresPrivate = null;
    protected Priv priv = Priv.ALL;
    protected BiPredicate<MessageReceivedEvent, ServerConfig> customFunction = null;

    public boolean isAvailable(MessageReceivedEvent event, ServerConfig cfg) {
        if(customFunction != null) {
            return customFunction.test(event, cfg);
        }
        switch(priv) {
            case OWNER:
                if(!cfg.isOwner(event.getAuthor()))
                    return false;
                break;
            case ADMIN:
                if(!cfg.isAdmin(event.getAuthor()))
                    return false;
                break;
            case MOD:
                if(!cfg.isMod(event.getAuthor()))
                    return false;
                break;
        }
        if(requiresPrivate != null) {
            if(requiresPrivate != event.isPrivate()) {
                return false;
            }
        }
        return true;
    }

    public Command acceptPrivate(boolean priv) {
        this.requiresPrivate = priv;
        return this;
    }

    public Command acceptCustom(BiPredicate<MessageReceivedEvent, ServerConfig> customFunction) {
        this.customFunction = customFunction;
        return this;
    }

    public Command acceptPriv(Priv priv) {
        this.priv = priv;
        return this;
    }

    public enum Priv {
        ALL, OWNER, ADMIN, MOD
    }
}
