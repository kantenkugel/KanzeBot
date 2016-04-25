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

public class UpdateValidator extends Thread {
    private static final int MAX_TIME = 30*1000;
    private static UpdateValidator instance;
    public static synchronized UpdateValidator getInstance() {
        if(instance == null) {
            instance = new UpdateValidator();
        }
        return instance;
    }

    private UpdateValidator() {
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(MAX_TIME);
        } catch(InterruptedException e) {
            return;
        }
        Statics.LOG.fatal("Failed to start, reverting to old version");
        System.err.flush();
        MiscUtil.shutdown(Statics.REVERT_EXIT_CODE);
    }
}
