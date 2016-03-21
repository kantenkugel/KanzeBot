package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.modules.Module;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.impl.GuildImpl;
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
    private static final int CURR_VERSION = 5;

    private final JDA api;
    private final Guild guild;
    private final Set<User> admins = new HashSet<>();
    private final Set<Role> adminRoles = new HashSet<>();
    private final Set<User> mods = new HashSet<>();
    private final Set<Role> modRoles = new HashSet<>();
    private final Map<String, Module> enabledModules = new HashMap<>();
    private final Map<String, String> textCommands = new HashMap<>();
    private final Map<String, Command> commands = new HashMap<>();
    private JSONObject moduleConfig;
    private String prefix = DEFAULT_PREFIX;
    private boolean restrictTexts = false;
    private boolean allowEveryone = false;

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

    public boolean isAllowEveryone() {
        return allowEveryone;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        save();
    }

    public void setRestrictTexts(boolean restrictTexts) {
        this.restrictTexts = restrictTexts;
        save();
    }

    public void setAllowEveryone(boolean allowEveryone) {
        this.allowEveryone = allowEveryone;
        save();
    }

    public boolean isOwner(User u) {
        return MessageUtil.isGlobalAdmin(u) || guild.getOwnerId().equals(u.getId());
    }

    public boolean isAdmin(User u) {
        if(isOwner(u) || admins.contains(u) || adminRoles.contains(guild.getPublicRole())) {
            return true;
        }
        return guild.getRolesForUser(u).stream().anyMatch(adminRoles::contains);
    }

    public boolean isMod(User u) {
        if(isAdmin(u) || mods.contains(u) || modRoles.contains(guild.getPublicRole())) {
            return true;
        }
        return guild.getRolesForUser(u).stream().anyMatch(modRoles::contains);
    }

    public Map<String, String> getTextCommands() {
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

    public Set<User> getAdmins() {
        return admins;
    }

    public Set<Role> getAdminRoles() {
        return adminRoles;
    }

    public Set<User> getMods() {
        return mods;
    }

    public Set<Role> getModRoles() {
        return modRoles;
    }

    public void addModule(String moduleName) {
        Class<? extends Module> moduleClass = Module.getModules().get(moduleName.toLowerCase());
        if(moduleClass == null) {
            return;
        }
        try {
            Module module = moduleClass.newInstance();
            module.fromJson(getConfigForModule(module));
            module.init(api, this);
            enabledModules.put(moduleName.toLowerCase(), module);
            recalcCommands();
            save();
        } catch(InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void removeModule(String moduleName) {
        if(enabledModules.containsKey(moduleName.toLowerCase())) {
            enabledModules.remove(moduleName.toLowerCase());
            recalcCommands();
            save();
        }
    }

    public Map<String, Module> getModules() {
        return enabledModules;
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    private void recalcCommands() {
        commands.clear();
        enabledModules.values().forEach(m -> commands.putAll(m.getCommands()));
    }

    public void save() {
        JSONObject config = new JSONObject()
                .put("prefix", prefix)
                .put("restrictTexts", restrictTexts)
                .put("allowEveryone", allowEveryone);

        JSONArray arr = new JSONArray();
        for(User admin : admins) {
            arr.put(admin.getId());
        }
        config.put("admins", arr);

        arr = new JSONArray();
        for(Role adminRole : adminRoles) {
            arr.put(adminRole.getId());
        }
        config.put("adminRoles", arr);

        arr = new JSONArray();
        for(User mod : mods) {
            arr.put(mod.getId());
        }
        config.put("mods", arr);

        arr = new JSONArray();
        for(Role modRole : modRoles) {
            arr.put(modRole.getId());
        }
        config.put("modRoles", arr);

        JSONObject cmdObject = new JSONObject();
        for(String cmdkey : textCommands.keySet()) {
            cmdObject.put(cmdkey, textCommands.get(cmdkey));
        }
        config.put("commands", cmdObject);

        JSONArray moduleArr = new JSONArray();
        for(Module module : enabledModules.values()) {
            moduleArr.put(module.getName());
            moduleConfig.put(module.getName(), module.toJson());
        }
        config.put("enabledModules", moduleArr);
        config.put("moduleConfigs", moduleConfig);

        writeConfig(guild.getId(), config);
    }

    private void load() {
        JSONObject config = getConfig(guild);
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

        GuildImpl guildImpl = (GuildImpl) this.guild;
        adminRoles.clear();
        JSONArray adminRoleArr = config.getJSONArray("adminRoles");
        for(int i = 0; i < adminRoleArr.length(); i++) {
            Role role = guildImpl.getRolesMap().get(adminRoleArr.getString(i));
            if(role != null) {
                adminRoles.add(role);
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
        JSONArray modeRoleArr = config.getJSONArray("modRoles");
        for(int i = 0; i < modeRoleArr.length(); i++) {
            Role role = guildImpl.getRolesMap().get(modeRoleArr.getString(i));
            if(role != null) {
                modRoles.add(role);
            }
        }

        textCommands.clear();
        JSONObject commands = config.getJSONObject("commands");
        for(String cmdkey : commands.keySet()) {
            textCommands.put(cmdkey, commands.getString(cmdkey));
        }

        prefix = config.getString("prefix");
        restrictTexts = config.getBoolean("restrictTexts");
        allowEveryone = config.getBoolean("allowEveryone");

        moduleConfig = config.getJSONObject("moduleConfigs");
        enabledModules.clear();
        JSONArray moduleArr = config.getJSONArray("enabledModules");
        for(int i = 0; i < moduleArr.length(); i++) {
            addModule(moduleArr.getString(i));
        }
    }

    protected JSONObject getConfigForModule(Module mod) {
        if(moduleConfig.has(mod.getName())) {
            return moduleConfig.getJSONObject(mod.getName());
        } else {
            JSONObject modCfg = mod.toJson();
            moduleConfig.put(mod.getName(), modCfg);
            save();
            return modCfg;
        }
    }

    private static JSONObject getConfig(Guild g) {
        try {
            if(!Files.exists(config_folder)) {
                Files.createDirectories(config_folder);
            }
            Path config = config_folder.resolve("./" + g.getId() + ".json");
            if(!Files.exists(config)) {
                Files.createFile(config);
                JSONObject o = new JSONObject()
                        .put("admins", new JSONArray())
                        .put("adminRoles", new JSONArray())
                        .put("mods", new JSONArray())
                        .put("modRoles", new JSONArray())
                        .put("prefix", DEFAULT_PREFIX)
                        .put("restrictTexts", false)
                        .put("allowEveryone", false)
                        .put("commands", new JSONObject())
                        .put("enabledModules", new JSONArray())
                        .put("moduleConfigs", new JSONObject());
                writeConfig(g.getId(), o);
            }
            JSONObject conf = new JSONObject(new String(Files.readAllBytes(config), StandardCharsets.UTF_8));
            switch(conf.getInt("version")) {
                case 1:
                    conf.put("commands", new JSONObject());
                case 2:
                    conf
                        .put("moduleConfigs", new JSONObject())
                        .put("enabledModules", new JSONArray());
                case 3:
                    JSONArray adminRoles = conf.getJSONArray("adminRoles");
                    JSONArray newRoles = new JSONArray();
                    for(int i = 0; i < adminRoles.length(); i++) {
                        String name = adminRoles.getString(i);
                        Optional<Role> any = g.getRoles().stream().filter(r -> r.getName().equals(name)).findAny();
                        if(any.isPresent()) {
                            newRoles.put(any.get().getId());
                        }
                    }
                    conf.put("adminRoles", newRoles);
                    JSONArray modRoles = conf.getJSONArray("modRoles");
                    newRoles = new JSONArray();
                    for(int i = 0; i < modRoles.length(); i++) {
                        String name = modRoles.getString(i);
                        Optional<Role> any = g.getRoles().stream().filter(r -> r.getName().equals(name)).findAny();
                        if(any.isPresent()) {
                            newRoles.put(any.get().getId());
                        }
                    }
                    conf.put("modRoles", newRoles);
                case 4:
                    conf.put("allowEveryone", false);

                    writeConfig(g.getId(), conf);
                    break;
            }
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
        private static final Map<JDA, PMConfig> instances = new HashMap<>();
        private static final Set<String> enabledModules = new HashSet<>();

        public synchronized static PMConfig getInstance(JDA jda) {
            if(!instances.containsKey(jda)) {
                instances.put(jda, new PMConfig(jda));
            }
            return instances.get(jda);
        }

        public synchronized static void registerModule(String name) {
            enabledModules.add(name);
            instances.values().forEach(i -> i.addModule(name));
        }

        private PMConfig(JDA api) {
            super(api, null);
            enabledModules.forEach(super::addModule);
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

        @Override
        public void save() {}

        @Override
        protected JSONObject getConfigForModule(Module mod) {
            return new JSONObject();
        }
    }
}
