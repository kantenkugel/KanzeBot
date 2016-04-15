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

package com.kantenkugel.discordbot.util;

import com.kantenkugel.discordbot.Statics;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.MessageChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UpdateWatcher extends Thread {
    private final JDA api;

    public UpdateWatcher(JDA api) {
        this.api = api;
        setDaemon(true);
        start();
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                String cmd = reader.readLine();
                int code = Integer.parseInt(cmd);
                cmd = null;
                switch(code) {
                    case Statics.UPDATE_EXIT_CODE:
                        cmd = "update";
                    case Statics.NORMAL_EXIT_CODE:
                        if(cmd == null)
                            cmd = "shutdown";
                    case Statics.RESTART_EXIT_CODE:
                        if(cmd == null)
                            cmd = "restart";
                        getChannel(api).sendMessage("Wrapper requested to " + cmd + ". Doing so now...");
                        MiscUtil.shutdown(code);
                        return;
                    default:
                        System.out.println("UpdateWatcher got unknown command-code " + code + "... ignoring");
                }
            } catch(IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    public static MessageChannel getChannel(JDA api) {
        return Statics.botOwner.getPrivateChannel();
    }
}
