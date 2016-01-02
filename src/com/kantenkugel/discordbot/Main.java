package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.commands.CommandRegistry;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;

/**
 * Created by Michael Ritter on 05.12.2015.
 */
public class Main {
    public static JDA api;
    private Main(String[] args) {
    }

    public static void main(String[] args) {
        if(args.length < 2) {
            System.err.println("Need email and password as arguments!");
            return;
        }
        SolarSystem.init();
        Item.init();
        CommandRegistry.init();
        try {
            api = new JDABuilder(args[0], args[1]).addListener(new CommandRegistry()).build();
            CommandRegistry.setJDA(api);
        } catch(LoginException e) {
            e.printStackTrace();
        }
    }
}
