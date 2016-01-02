package com.kantenkugel.discordbot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michael Ritter on 06.12.2015.
 */
public class Item {
    private static Map<Long, Item> idTable;
    private static SearchTST<Item> nameTrie;

    public static void init() {
        if(idTable != null) {
            return;
        }
        idTable = new HashMap<>();
        nameTrie = new SearchTST<>();
        try {
            for(String line : Files.readAllLines(Paths.get("items.txt"))) {
                String[] split = line.split("\\s+", 2);
                Item item = new Item(Long.parseLong(split[0]), split[1]);
                idTable.put(item.id, item);
                nameTrie.put(item.name.toLowerCase(), item);
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new NullPointerException();
        }
    }

    public static Item get(long id) {
        return idTable.get(id);
    }

    public static Item get(String name) {
        Item item = nameTrie.get(name.toLowerCase());
        if(item == null) {
            Set<Item> all = getAll(name);
            if(all.size() == 1) {
                return all.iterator().next();
            }
            return null;
        }
        return item;
    }

    public static Set<Item> getAll(String prefix) {
        Set<Item> out = new HashSet<>();
        for(String itemname : nameTrie.collect(prefix.toLowerCase())) {
            out.add(nameTrie.get(itemname));
        }
        return out;
    }

    public final long id;
    public final String name;

    private Item(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
