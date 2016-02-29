package com.kantenkugel.discordbot.modules;

import com.kantenkugel.discordbot.Main;
import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.util.ClassEnumerator;
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

    public static void init() {
        ClassEnumerator.getClassesForPackage(Module.class.getPackage()).stream().filter(aClass -> Module.class.isAssignableFrom(aClass) && !aClass.equals(Module.class)).forEach(aClass -> {
            @SuppressWarnings("unchecked")
            Class<? extends Module> module = (Class<? extends Module>) aClass;
            register(module);
        });
    }

    /**
     * Used to manually register Modules. (Modules in the com.kantenkugel.discordbot.modules package are automatically loaded when init is called)
     * @param moduleClass
     *      the module to register
     */
    public static void register(Class<? extends Module> moduleClass) {
        try {
            Module module = moduleClass.newInstance();
            modules.putIfAbsent(module.getName().toLowerCase(), moduleClass);
            if(module.availableInPms()) {
                ServerConfig.PMConfig.registerModule(module.getName().toLowerCase());
            }
            Main.LOG.info("Registered module " + module.getName().toLowerCase());
        } catch(InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the name of this module. The name should be all lowercase.
     * This name is used as key for enabling/disabling/configuring the module
     *
     * @return
     *      this modules name
     */
    public abstract String getName();

    /**
     * Defines if this modules is available in PMs
     *
     * @return
     *      whether or not this should be available in pms
     */
    public abstract boolean availableInPms();

    /**
     * Initializes this module. This is called after {@link #fromJson(JSONObject)}.
     *
     * @param jda
     *      the JDA object
     * @param cfg
     *      the ServerConfig object
     */
    public abstract void init(JDA jda, ServerConfig cfg);

    /**
     * Pass-through of EVERY messageReceivedEvent
     * @param event
     *      the pass-through event
     * @return
     *      true, if further handling via commands should be stopped
     */
    public boolean handle(MessageReceivedEvent event) { return false; }

    /**
     * Called when the Guild-owner tries to configure this module.
     * If changes were made to the configuration, you should call cfg.save()
     *
     * @param cfgString
     *      null if no config string is supplied (help), or the string supplied to configure
     * @param event
     *      the complete MessageReceivedEvent
     * @param cfg
     *      the ServerConfig Object
     */
    public abstract void configure(String cfgString, MessageReceivedEvent event, ServerConfig cfg);

    /**
     * This method should return all Commands available via this Module.
     * This is only called when the list of modules in a guild is refreshed and can therefore recalc the Map each time without major performance impact
     *
     * @return
     *      the Map of all available commands (with the key being the command name)
     */
    public abstract Map<String, Command> getCommands();

    /**
     * Should return the Configuration-Object for this module.
     * This method is either called when the server-config is saved, or when this module is first added to a server.
     * Therefore this method should return the default Configuration Object, if this module didn't go through the fromJson+init progress already
     *
     * @return
     *      the current Configuration, or default Configuration, if module didn't get initialized
     */
    public abstract JSONObject toJson();

    /**
     * This is getting called when this module is loaded.
     * The given JsonObject is the current Current configuration Object of this module in the server.
     * This is always called before {@link #init(JDA, ServerConfig)}
     *
     * @param cfg
     *      the current Configuration
     */
    public abstract void fromJson(JSONObject cfg);

    /**
     * Currently unused, but should unload resources from this module (called once this module is removed from the last guild)
     */
    public void unload() {
    }
}
