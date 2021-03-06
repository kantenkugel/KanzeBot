/*
 * Copyright 2016 Michael Ritter (Kantenkugel)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kantenkugel.discordbot.commands;

import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.util.MessageUtil;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public abstract class Command implements BiConsumer<MessageEvent, ServerConfig> {
    protected Boolean requiresPrivate = null;
    protected Priv priv = Priv.ALL;
    protected BiPredicate<MessageEvent, ServerConfig> customFunction = null;

    public boolean isAvailable(MessageEvent event, ServerConfig cfg) {
        if(customFunction != null) {
            return customFunction.test(event, cfg);
        }
        switch(priv) {
            case BOTADMIN:
                if(!MessageUtil.isGlobalAdmin(event.getAuthor())) {
                    return false;
                }
                break;
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

    public abstract String getDescription();

    public Command acceptPrivate(boolean priv) {
        this.requiresPrivate = priv;
        return this;
    }

    public Boolean doesAcceptPrivate() {
        return requiresPrivate;
    }

    public Command acceptCustom(BiPredicate<MessageEvent, ServerConfig> customFunction) {
        this.customFunction = customFunction;
        return this;
    }

    public Command acceptPriv(Priv priv) {
        this.priv = priv;
        return this;
    }

    public Priv getPriv() {
        return priv;
    }

    public enum Priv {
        BOTADMIN("Bot Admin"), OWNER("Guild Owner"), ADMIN("Guild Admin"), MOD("Guild Mod"), ALL("Other");

        private final String repr;

        Priv(String repr) {
            this.repr = repr;
        }

        public String getRepr() {
            return repr;
        }
    }
}
