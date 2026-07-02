package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.*;

public class CarShopManager {
    private static final File SAVE_FILE = FMLPaths.CONFIGDIR.get().resolve("evomarkets_cars.json").toFile();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class CarPart {
        public String uuid;
        public String id;   // Ex: automobility:automobile_wheel
        public String nbt;  // Aici salvăm diferența dintre roți!
        public String name;
        public double price;

        public CarPart(String uuid, String id, String nbt, String name, double price) {
            this.uuid = uuid; this.id = id; this.nbt = nbt; this.name = name; this.price = price;
        }
    }

    public static Map<String, List<CarPart>> shopData = new HashMap<>();

    static {
        shopData.put("BODY", new ArrayList<>()); shopData.put("WHEELS", new ArrayList<>());
        shopData.put("ENGINE", new ArrayList<>()); shopData.put("ATTACHMENTS", new ArrayList<>());
    }

    public static void load() {
        if (!SAVE_FILE.exists()) return;
        try (Reader reader = new FileReader(SAVE_FILE)) {
            Map<String, List<CarPart>> loaded = GSON.fromJson(reader, new TypeToken<Map<String, List<CarPart>>>(){}.getType());
            if (loaded != null) shopData = loaded;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save() {
        try (Writer writer = new FileWriter(SAVE_FILE)) { GSON.toJson(shopData, writer); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void addPart(String category, String uuid, String itemId, String nbt, String name, double price) {
        String catUpper = category.toUpperCase();
        if (shopData.containsKey(catUpper)) {
            shopData.get(catUpper).add(new CarPart(uuid, itemId, nbt, name, price));
            save();
        }
    }

    // ADAUGAT: Metodă pentru a șterge o piesă din shop
    public static boolean removePart(String category, String name) {
        String catUpper = category.toUpperCase();
        if (shopData.containsKey(catUpper)) {
            List<CarPart> parts = shopData.get(catUpper);
            // Șterge piesa dacă numele corespunde ignorând literele mari/mici
            boolean removed = parts.removeIf(part -> part.name.equalsIgnoreCase(name));
            if (removed) {
                save(); // Salvăm modificările în JSON
                return true;
            }
        }
        return false;
    }
}