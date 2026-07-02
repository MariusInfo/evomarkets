package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.*;

public class PlaneShopManager {
    private static final File SAVE_FILE = FMLPaths.CONFIGDIR.get().resolve("evomarkets_planes.json").toFile();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class PlanePart {
        public String uuid;
        public String id;
        public String nbt;
        public String name;
        public double price;

        public PlanePart(String uuid, String id, String nbt, String name, double price) {
            this.uuid = uuid; this.id = id; this.nbt = nbt; this.name = name; this.price = price;
        }
    }

    public static Map<String, List<PlanePart>> shopData = new HashMap<>();

    static {
        shopData.put("BODY", new ArrayList<>());
        shopData.put("ENGINE", new ArrayList<>());
        shopData.put("ATTACHMENTS", new ArrayList<>());
    }

    public static void load() {
        if (!SAVE_FILE.exists()) return;
        try (Reader reader = new FileReader(SAVE_FILE)) {
            Map<String, List<PlanePart>> loaded = GSON.fromJson(reader, new TypeToken<Map<String, List<PlanePart>>>(){}.getType());
            if (loaded != null) shopData = loaded;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try (Writer writer = new FileWriter(SAVE_FILE)) { GSON.toJson(shopData, writer); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addPart(String category, String uuid, String itemId, String nbt, String name, double price) {
        String catUpper = category.toUpperCase();
        if (shopData.containsKey(catUpper)) {
            shopData.get(catUpper).add(new PlanePart(uuid, itemId, nbt, name, price));
            save();
        }
    }

    public static boolean removePart(String category, String name) {
        String catUpper = category.toUpperCase();
        if (shopData.containsKey(catUpper)) {
            List<PlanePart> parts = shopData.get(catUpper);
            boolean removed = parts.removeIf(part -> part.name.equalsIgnoreCase(name));
            if (removed) {
                save();
                return true;
            }
        }
        return false;
    }
}