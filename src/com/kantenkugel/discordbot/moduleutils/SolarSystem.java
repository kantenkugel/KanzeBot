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

package com.kantenkugel.discordbot.moduleutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class SolarSystem {
    public static Set<SolarSystem> hubs;
    private static Map<Long, SolarSystem> idTable;
    private static SearchTST<SolarSystem> nameTrie;

    public static void init() {
        if(idTable != null) {
            return;
        }
        idTable = new HashMap<>();
        nameTrie = new SearchTST<>();
        hubs = new HashSet<>();
        try {
            for(String line : Files.readAllLines(Paths.get("solarsystems.txt"))) {
                String[] split = line.split("\\s+", 2);
                SolarSystem sys = new SolarSystem(Long.parseLong(split[0]), split[1]);
                idTable.put(sys.id, sys);
                nameTrie.put(sys.name.toLowerCase(), sys);
            }
            hubs.addAll(Arrays.asList("jita", "hek", "amarr", "rens", "dodixie").stream().map(SolarSystem::get).collect(Collectors.toList()));
        } catch(IOException e) {
            e.printStackTrace();
            throw new NullPointerException();
        }
    }

    public static SolarSystem get(long id) {
        return idTable.get(id);
    }

    public static SolarSystem get(String name) {
        SolarSystem sys = nameTrie.get(name.toLowerCase());
        if(sys == null) {
            Set<SolarSystem> all = getAll(name);
            if(all.size() == 1) {
                return all.iterator().next();
            }
            return null;
        }
        return sys;
    }

    public static Set<SolarSystem> getAll(String prefix) {
        Set<SolarSystem> out = new HashSet<>();
        for(String sysname : nameTrie.collect(prefix.toLowerCase())) {
            out.add(nameTrie.get(sysname));
        }
        return out;
    }

    public final long id;
    public final String name;

    private SolarSystem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
