/**
 * Copyright 2015-2016 Austin Keener & Michael Ritter
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.util.ServerConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public abstract class Module {

    private static Map<String, Class<? extends Module>> modules = new HashMap<>();

    public static Map<String, Class<? extends Module>> getModules() {
        return modules;
    }

    public static void register(Class<? extends Module> moduleClass) {
        try {
            Module module = moduleClass.newInstance();
            modules.put(module.getName().toLowerCase(), moduleClass);
        } catch(InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public abstract String getName();

    public abstract void init(JDA jda, ServerConfig cfg);

    public abstract void configure(String cfgString, MessageReceivedEvent event, ServerConfig cfg);

    public abstract Map<String, Command> getCommands();

    public abstract JSONObject toJson();

    public abstract void fromJson(JSONObject cfg);

    public void unload() {
    }
}
