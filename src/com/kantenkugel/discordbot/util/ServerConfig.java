/**
 * Copyright 2015 Austin Keener & Michael Ritter
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
package com.kantenkugel.discordbot.util;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ServerConfig {
    public static final String DEFAULT_PREFIX = "-kb";
    private static final Path config_folder = Paths.get("configs");
    private static final int CURR_VERSION = 1;

    private final JDA api;
    private final Guild guild;
    private final Set<User> admins = new HashSet<>();
    private final Set<Role> adminRoles = new HashSet<>();
    private final Set<User> mods = new HashSet<>();
    private final Set<Role> modRoles = new HashSet<>();
    private final Map<String, Map<String, String>> textCommands = new HashMap<>();
    private String prefix = DEFAULT_PREFIX;
    private boolean restrictTexts = false;

    public ServerConfig(JDA api, Guild guild) {
        this.api = api;
        this.guild = guild;
        if(guild != null)
            load();
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isRestrictTexts() {
        return restrictTexts;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public void setRestrictTexts(boolean restrictTexts) {
        this.restrictTexts = restrictTexts;
        save();
    }

    public boolean isOwner(User u) {
        return MessageUtil.isGlobalAdmin(u) || guild.getOwnerId().equals(u.getId());
    }

    public boolean isAdmin(User u) {
        if(isOwner(u) || admins.contains(u)) {
            return true;
        }
        List<Role> rolesForUser = guild.getRolesForUser(u);
        return adminRoles.stream().anyMatch(rolesForUser::contains);
    }

    public boolean isMod(User u) {
        if(isAdmin(u) || mods.contains(u)) {
            return true;
        }
        List<Role> rolesForUser = guild.getRolesForUser(u);
        return modRoles.stream().anyMatch(rolesForUser::contains);
    }

    public Map<String, Map<String, String>> getTextCommands() {
        return textCommands;
    }

    public void addAdmin(User u) {
        admins.add(u);
        save();
    }

    public void removeAdmin(User u) {
        admins.remove(u);
        save();
    }

    public void addAdminRole(Role role) {
        adminRoles.add(role);
        save();
    }

    public void removeAdminRole(Role role) {
        adminRoles.remove(role);
        save();
    }

    public void addMod(User u) {
        mods.add(u);
        save();
    }

    public void removeMod(User u) {
        mods.remove(u);
        save();
    }

    public void addModRole(Role role) {
        modRoles.add(role);
        save();
    }

    public void removeModRole(Role role) {
        modRoles.remove(role);
        save();
    }

    public void save() {
        JSONObject config = new JSONObject()
                .put("prefix", prefix)
                .put("restrictTexts", restrictTexts);

        JSONArray arr = new JSONArray();
        for(User admin : admins) {
            arr.put(admin.getId());
        }
        config.put("admins", arr);

        arr = new JSONArray();
        for(Role adminRole : adminRoles) {
            arr.put(adminRole.getName().toLowerCase());
        }
        config.put("adminRoles", arr);

        arr = new JSONArray();
        for(User mod : mods) {
            arr.put(mod.getId());
        }
        config.put("mods", arr);

        arr = new JSONArray();
        for(Role modRole : modRoles) {
            arr.put(modRole.getName().toLowerCase());
        }
        config.put("modRoles", arr);

        JSONObject cmdObject = new JSONObject();
        for(String chanId : textCommands.keySet()) {
            Map<String, String> commandMap = textCommands.get(chanId);
            JSONObject channelObject = new JSONObject();
            for(Map.Entry<String, String> cmdEntry : commandMap.entrySet()) {
                channelObject.put(cmdEntry.getKey(), cmdEntry.getValue());
            }
            cmdObject.put(chanId, channelObject);
        }
        config.put("commands", cmdObject);
        writeConfig(guild.getId(), config);
    }

    private void load() {
        JSONObject config = getConfig(guild.getId());
        if(config == null) {
            return;
        }
        admins.clear();
        JSONArray adminArr = config.getJSONArray("admins");
        for(int i = 0; i < adminArr.length(); i++) {
            User admin = api.getUserById(adminArr.getString(i));
            if(admin != null) {
                admins.add(admin);
            }
        }
        adminRoles.clear();
        JSONArray adminRoleArr = config.getJSONArray("adminRoles");
        for(int i = 0; i < adminRoleArr.length(); i++) {
            String name = adminRoleArr.getString(i);
            Optional<Role> any = guild.getRoles().stream().filter(role -> role.getName().toLowerCase().equals(name)).findAny();
            if(any.isPresent()) {
                adminRoles.add(any.get());
            }
        }
        mods.clear();
        JSONArray modArr = config.getJSONArray("mods");
        for(int i = 0; i < modArr.length(); i++) {
            User mod = api.getUserById(modArr.getString(i));
            if(mod != null) {
                mods.add(mod);
            }
        }
        modRoles.clear();
        JSONArray modeRoleArr = config.getJSONArray("adminRoles");
        for(int i = 0; i < modeRoleArr.length(); i++) {
            String name = modeRoleArr.getString(i);
            Optional<Role> any = guild.getRoles().stream().filter(role -> role.getName().toLowerCase().equals(name)).findAny();
            if(any.isPresent()) {
                modRoles.add(any.get());
            }
        }

        textCommands.clear();
        JSONObject commands = config.getJSONObject("commands");
        for(String chanId : commands.keySet()) {
            JSONObject o = commands.getJSONObject(chanId);
            Map<String, String> cmds = new HashMap<>();
            for(String s : o.keySet()) {
                cmds.put(s, o.getString(s));
            }
            textCommands.put(chanId, cmds);
        }

        prefix = config.getString("prefix");
        restrictTexts = config.getBoolean("restrictTexts");
    }

    private static JSONObject getConfig(String id) {
        try {
            if(!Files.exists(config_folder)) {
                Files.createDirectories(config_folder);
            }
            Path config = config_folder.resolve("./" + id + ".json");
            if(!Files.exists(config)) {
                Files.createFile(config);
                JSONObject o = new JSONObject()
                        .put("admins", new JSONArray())
                        .put("adminRoles", new JSONArray())
                        .put("mods", new JSONArray())
                        .put("modRoles", new JSONArray())
                        .put("prefix", DEFAULT_PREFIX)
                        .put("restrictTexts", false)
                        .put("commands", new JSONObject());
                writeConfig(id, o);
            }
            JSONObject conf = new JSONObject(new String(Files.readAllBytes(config), StandardCharsets.UTF_8));
            return conf;
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static void writeConfig(String id, JSONObject conf) {
        try {
            Path config = config_folder.resolve("./" + id + ".json");
            Files.write(config, conf.put("version", CURR_VERSION).toString(4).getBytes(StandardCharsets.UTF_8));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static class PMConfig extends ServerConfig {
        public PMConfig() {
            super(null, null);
        }

        @Override
        public String getPrefix() {
            return "!";
        }

        @Override
        public boolean isRestrictTexts() {
            return true;
        }

        @Override
        public boolean isOwner(User u) {
            return false;
        }

        @Override
        public boolean isAdmin(User u) {
            return false;
        }

        @Override
        public boolean isMod(User u) {
            return false;
        }
    }
}
