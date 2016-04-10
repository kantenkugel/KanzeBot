package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.commands.Command;
import com.kantenkugel.discordbot.commands.CommandWrapper;
import com.kantenkugel.discordbot.config.ServerConfig;
import com.kantenkugel.discordbot.listener.MessageEvent;
import com.kantenkugel.discordbot.modules.Module;
import com.kantenkugel.discordbot.util.MessageUtil;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

import static com.kantenkugel.discordbot.util.MessageUtil.reply;

public class ConfigCommand implements CommandSection {
    @Override
    public void register(Map<String, Command> registry, JDA api) {
        registry.put("config", new CommandWrapper("Allows the server-owner (you) to configure different parts of this bot. To see more detailed help, call it without arguments"
                , (e, cfg) -> ConfigCommand.config(e, cfg, MessageUtil.getArgs(e, cfg)))
                .acceptPrivate(false).acceptPriv(Command.Priv.OWNER));
        registry.put("module", new CommandWrapper("Shortcut to `config modules`", (e, cfg) -> {
            String[] args = ("config modules" + e.getMessage().getContent().substring(cfg.getPrefix().length() + 6)).split("\\s+");
            ConfigCommand.config(e, cfg, args);
        }).acceptPriv(Command.Priv.OWNER));
    }

    private static void config(MessageEvent event, ServerConfig cfg, String[] args) {
        if(args.length == 1) {
            reply(event, cfg, "Available subcommands: prefix, restrictTexts, leave, admins, mods, modules, allowEveryone\nTo get more details, run " + cfg.getPrefix() + args[0] + " SUBCOMMAND");
        } else if(args.length > 1) {
            String key = null;
            if(args.length > 2) {
                key = args[2].toLowerCase();
            }
            switch(args[1].toLowerCase()) {
                case "prefix":
                    if(args.length == 2) {
                        reply(event, cfg, "This command modifies the prefix used to call commands of this bot." +
                                "\nCurrent Prefix: `" + cfg.getPrefix() +
                                "`\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " PREFIX");
                    } else {
                        String prefix = MessageUtil.getArgs(event, cfg, 3)[2].toLowerCase();
                        cfg.setPrefix(prefix);
                        reply(event, cfg, "Prefix changed to `" + prefix + '`');
                    }
                    break;
                case "restricttexts":
                    if(args.length == 2) {
                        reply(event, cfg, "This command changes the behavior of text-commands." +
                                "\nIf restrictTexts is set to true, only mods can call the text-commands" +
                                "\nIf set to false, everyone can (default)" +
                                "\nrestrictTexts is currently set to: " + cfg.isRestrictTexts() +
                                "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false");
                    } else {
                        cfg.setRestrictTexts(Boolean.parseBoolean(args[2]));
                        reply(event, cfg, "restrictTexts changed to " + cfg.isRestrictTexts());
                    }
                    break;
                case "alloweveryone":
                    if(args.length == 2) {
                        reply(event, cfg, MessageUtil.strip("This config changes if the bot can ping @everyone in this guild " +
                                "(this affects primarily text-responses created by `addcom` and responses from the responder module)." +
                                "\nIf allowEveryone is set to true, this bot can do @everyone." +
                                "\nIf set to false, @everyone will always get escaped." +
                                "\nallowEveryone is currently set to: " + cfg.isAllowEveryone() +
                                "\nTo change, call " + cfg.getPrefix() + args[0] + " " + args[1] + " true/false"));
                    } else {
                        cfg.setAllowEveryone(Boolean.parseBoolean(args[2]));
                        reply(event, cfg, "allowEveryone changed to " + cfg.isAllowEveryone());
                    }
                    break;
                case "leave":
                    if(args.length == 2) {
                        reply(event, cfg, "This will make the bot leave this server!" +
                                "\nTo leave, call " + cfg.getPrefix() + args[0] + " " + args[1] + " YES");
                    } else if(args[2].equals("YES")) {
                        event.getGuild().getManager().leave();
                    }
                    break;
                case "admins":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove Users and/or Roles to the admin-set" +
                                "\nAdmins have access to everything mods can, + access to the clear command (may change)" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        reply(event, cfg, MessageUtil.strip("Current Admins:\n\tUsers: "
                                + (cfg.getAdmins().size() == 0 ? "None" : cfg.getAdmins().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getAdminRoles().size() == 0 ? "None" :
                                MessageUtil.strip(cfg.getAdminRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get()))));
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addAdmin);
                                reply(event, cfg, "User(s) added as admin(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeAdmin);
                                reply(event, cfg, "User(s) removed from admin(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addAdminRole(any.get());
                                    reply(event, cfg, MessageUtil.strip("Role " + any.get().getName() + " added as admin role"));
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeAdminRole(anyremove.get());
                                    reply(event, cfg, MessageUtil.strip("Role " + anyremove.get().getName() + " removed from admin roles"));
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                case "mods":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove Users and/or Roles to the mods-set" +
                                "\nMods have access to adding, removing and editing text-commands, and also calling them, when they were locked via the restrictTexts config" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " addUser/removeUser @MENTION" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " addRole/removeRole ROLENAME");
                        reply(event, cfg, MessageUtil.strip("Current Mods:\n\tUsers: "
                                + (cfg.getMods().size() == 0 ? "None" : cfg.getMods().stream().map(User::getUsername).reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\n\tRoles: " + (cfg.getModRoles().size() == 0 ? "None" :
                                MessageUtil.strip(cfg.getModRoles().stream().map(Role::getName).reduce((s1, s2) -> s1 + ", " + s2).get()))));
                    } else {
                        switch(key) {
                            case "adduser":
                                event.getMessage().getMentionedUsers().forEach(cfg::addMod);
                                reply(event, cfg, "User(s) added as mod(s)");
                                break;
                            case "removeuser":
                                event.getMessage().getMentionedUsers().forEach(cfg::removeMod);
                                reply(event, cfg, "User(s) removed from mod(s)");
                                break;
                            case "addrole":
                                String name = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> any = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
                                if(any.isPresent()) {
                                    cfg.addModRole(any.get());
                                    reply(event, cfg, MessageUtil.strip("Role " + any.get().getName() + " added as mod role"));
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            case "removerole":
                                String name2 = StringUtils.join(args, ' ', 3, args.length);
                                Optional<Role> anyremove = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(name2)).findAny();
                                if(anyremove.isPresent()) {
                                    cfg.removeModRole(anyremove.get());
                                    reply(event, cfg, MessageUtil.strip("Role " + anyremove.get().getName() + " removed from mod roles"));
                                } else {
                                    reply(event, cfg, "No role matching given name found");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                case "modules":
                    if(args.length < 4) {
                        reply(event, cfg, "This will add/remove/configure Modules on this Guild" +
                                "\nUsage: " + cfg.getPrefix() + args[0] + " " + args[1] + " enable/disable MODULE" +
                                "\nOr: " + cfg.getPrefix() + args[0] + " " + args[1] + " configure MODULE\n");
                        reply(event, cfg, "Currently enabled Modules:\n\t"
                                + (cfg.getModules().size() == 0 ? "None" : cfg.getModules().keySet().stream().reduce((s1, s2) -> s1 + ", " + s2).get())
                                + "\nAvailable Modules:\n\t"
                                + (Module.getModuleList().size() == 0 ? "None" : Module.getModuleList().stream().reduce((s1, s2) -> s1 + ", " + s2).get()));
                    } else {
                        String val = null;
                        if(args.length > 3) {
                            val = args[3].toLowerCase();
                        }
                        switch(key) {
                            case "enable":
                                if(Module.getModules().containsKey(val)) {
                                    if(!cfg.getModules().containsKey(val)) {
                                        cfg.addModule(val);
                                        reply(event, cfg, "Module enabled");
                                    } else {
                                        reply(event, cfg, "Module was already enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            case "disable":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        cfg.removeModule(val);
                                        reply(event, cfg, "Module disabled");
                                    } else {
                                        reply(event, cfg, "Module was not enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            case "configure":
                                if(Module.getModules().containsKey(val)) {
                                    if(cfg.getModules().containsKey(val)) {
                                        String cfgString = args.length > 4 ? StringUtils.join(args, ' ', 4, args.length) : null;
                                        cfg.getModules().get(val).configure(cfgString, event, cfg);
                                    } else {
                                        reply(event, cfg, "Module was not enabled!");
                                    }
                                } else {
                                    reply(event, cfg, "Module does not exist");
                                }
                                break;
                            default:
                                reply(event, cfg, "Invalid syntax");
                        }
                    }
                    break;
                default:
                    reply(event, cfg, "Invalid syntax");
            }
        }
    }
}
