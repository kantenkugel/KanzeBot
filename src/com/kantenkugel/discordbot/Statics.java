package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.config.BotConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDAInfo;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;

public class Statics {
    public static final SimpleLog LOG = SimpleLog.getLog(BotConfig.get("logname", "KanzeBot"));
    public static long START_TIME = System.currentTimeMillis();
    public static String OAUTH_ID = BotConfig.get("oauthAppId", "");

    public static User botOwner;

    public static JDA jdaInstance;

    public static int VERSION;
    public static String CHANGES;

    public static final String JDAVERSION = JDAInfo.VERSION;

    public static final int UPDATE_EXIT_CODE = 20;
    public static final int NORMAL_EXIT_CODE = 21;
    public static final int RESTART_EXIT_CODE = 22;
    public static final int REVERT_EXIT_CODE = 23;

    private Statics(){}
}
