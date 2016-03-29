package com.kantenkugel.discordbot.commands.sections;

import com.kantenkugel.discordbot.commands.Command;
import net.dv8tion.jda.JDA;

import java.util.Map;

public interface CommandSection {
    void register(Map<String, Command> registry, JDA api);
}
