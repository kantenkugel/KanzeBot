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

package com.kantenkugel.discordbot.config;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class BotConfig {
    private static final Path configPath = Paths.get("kanzeBotConfig.json");
    private static JSONObject config;

    public static void set(@NotNull String key, @Nullable Object val) {
        if(val == null) {
            config.remove(key);
        } else {
            config.put(key, val);
        }
        save();
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(@NotNull String key) {
        if(config.has(key)) {
            try {
                return (T) config.get(key);
            } catch(JSONException | ClassCastException ignored) {}
        }
        return null;
    }

    public static <T> T get(@NotNull String key, @NotNull T def) {
        T obj = get(key);
        if(obj == null) {
            obj = def;
            config.put(key, def);
            save();
        }
        return obj;
    }

    public static void save() {
        try {
            Files.write(configPath, config.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch(IOException ignored) {
        }
    }

    public static boolean load() {
        boolean exists = Files.exists(configPath);
        try {
            JSONObject def = getDefault();
            if(!exists) {
                Files.write(configPath, def.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            } else {
                config = new JSONObject(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8));
                for(String key : def.keySet()) {
                    if(!config.has(key)) {
                        config.put(key, def.get(key));
                        exists = false;
                    }
                }
                if(!exists) {
                    save();
                }
            }
        } catch(IOException ex) {
            System.err.println("Error reading/writing config file: ");
            ex.printStackTrace();
            return false;
        }
        return exists;
    }

    private static JSONObject getDefault() {
        return new JSONObject()
                .put("ownerId", "")
                .put("logname", "KanzeBot")
                .put("carbonKey", "")
                .put("oauthAppId", "")
                .put("logToFiles", true)
                .put("db", new JSONObject()
                        .put("host", "")
                        .put("database", "")
                        .put("user", "")
                        .put("password", "")
                ).put("historyBase", "");       //eg http://vps.kantenkugel.com:3000/history/
    }

    private BotConfig() {
    }
}
