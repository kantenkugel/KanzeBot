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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TaskHelper {
    private static Map<String, Thread> tasks = new HashMap<>();

    public static boolean start(String name, Runnable runnable) {
        if(tasks.containsKey(name) && tasks.get(name).isAlive()) {
            return false;
        }
        Thread t = new Thread(()-> {
            runnable.run();
            tasks.remove(name);
        });
        tasks.put(name, t);
        t.start();
        return true;
    }

    public static boolean startTimed(String name, long timeout, Runnable runnable) {
        return start(name, () -> {
            while(!Thread.interrupted()) {
                runnable.run();
                try {
                    Thread.sleep(timeout);
                } catch(InterruptedException ex) {
                    break;
                }
            }
        });
    }

    public static void stop(String name) {
        tasks.get(name).interrupt();
    }

    public static void stopAll() {
        tasks.values().forEach(Thread::interrupt);
    }

    public static Set<String> getTasks() {
        return tasks.keySet();
    }
}
