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

package com.kantenkugel.discordbot;

import com.kantenkugel.discordbot.config.BotConfig;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDAInfo;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.utils.SimpleLog;

import java.util.HashSet;
import java.util.Set;

public class Statics {
    public static final SimpleLog LOG = SimpleLog.getLog(BotConfig.get("logname", "KanzeBot"));
    public static long START_TIME = System.currentTimeMillis();
    public static String OAUTH_ID = BotConfig.get("oauthAppId", "");

    public static User botOwner;

    public static JDA jdaInstance;

    public static int VERSION;
    public static String CHANGES;

    public static final Set<String> GLOBAL_ADMINS = new HashSet<>();
    static {
        GLOBAL_ADMINS.add("122758889815932930");
        GLOBAL_ADMINS.add("107562988810027008");
        GLOBAL_ADMINS.add("107490111414882304");
    }

    public static final String JDAVERSION = JDAInfo.VERSION;

    public static final int UPDATE_EXIT_CODE = 20;
    public static final int NORMAL_EXIT_CODE = 21;
    public static final int RESTART_EXIT_CODE = 22;
    public static final int REVERT_EXIT_CODE = 23;

    private Statics(){}
}
