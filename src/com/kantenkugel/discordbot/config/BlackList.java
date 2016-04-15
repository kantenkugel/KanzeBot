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

import net.dv8tion.jda.entities.User;
import org.json.JSONArray;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BlackList {
    private static final Set<String> blacklist = new HashSet<>();
    private static boolean initialized = false;

    public static void add(String id) {
        if(blacklist.add(id))
            save();
    }

    public static void add(User user) {
        add(user.getId());
    }

    public static void remove(String id) {
        if(blacklist.remove(id))
            save();
    }

    public static void remove(User user) {
        remove(user.getId());
    }

    public static boolean contains(String id) {
        return blacklist.contains(id);
    }

    public static boolean contains(User user) {
        return contains(user.getId());
    }

    public static Set<String> get() {
        return Collections.unmodifiableSet(blacklist);
    }

    private static void save() {
        JSONArray blacklistArr = new JSONArray();
        blacklist.forEach(blacklistArr::put);
        BotConfig.set("blacklist", blacklistArr);
    }

    private static void load() {
        JSONArray blacklistArr = BotConfig.get("blacklist", new JSONArray());
        for(int i = 0; i < blacklistArr.length(); i++) {
            blacklist.add(blacklistArr.getString(i));
        }
    }

    public static synchronized void init() {
        if(initialized) {
            return;
        }
        initialized = true;
        load();
    }
}
