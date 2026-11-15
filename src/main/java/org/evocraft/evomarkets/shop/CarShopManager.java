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

    public static final Map<String, List<CarPart>> shopData = new HashMap<>();

    static {
        ensureDefaultCategories(shopData);
    }

    private static void ensureDefaultCategories(Map<String, List<CarPart>> map) {
        map.computeIfAbsent("BODY", k -> new ArrayList<>());
        map.computeIfAbsent("WHEELS", k -> new ArrayList<>());
        map.computeIfAbsent("ENGINE", k -> new ArrayList<>());
        map.computeIfAbsent("ATTACHMENTS", k -> new ArrayList<>());
    }

    public static synchronized void load() {
        if (!SAVE_FILE.exists()) return;
        try (Reader reader = new FileReader(SAVE_FILE)) {
            Map<String, List<CarPart>> loaded = GSON.fromJson(reader, new TypeToken<Map<String, List<CarPart>>>(){}.getType());
            ensureDefaultCategories(shopData);
            if (loaded != null) {
                for (Map.Entry<String, List<CarPart>> entry : loaded.entrySet()) {
                    String key = entry.getKey() != null ? entry.getKey().toUpperCase(Locale.ROOT) : "";
                    if (!shopData.containsKey(key)) continue;
                    List<CarPart> clean = new ArrayList<>();
                    if (entry.getValue() != null) {
                        for (CarPart part : entry.getValue()) {
                            if (part == null || part.id == null || !Double.isFinite(part.price) || part.price < 0) continue;
                            if (part.uuid == null || part.uuid.isBlank()) part.uuid = UUID.randomUUID().toString();
                            if (part.name == null) part.name = "Part";
                            clean.add(part);
                        }
                    }
                    shopData.put(key, clean);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static synchronized void save() {
        try {
            File parent = SAVE_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (Writer writer = new FileWriter(SAVE_FILE)) { GSON.toJson(shopData, writer); }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static synchronized boolean hasCategory(String category) {
        return category != null && shopData.containsKey(category.toUpperCase(Locale.ROOT));
    }

    public static synchronized String toJson() {
        return GSON.toJson(shopData);
    }

    public static synchronized CarPart findPart(String category, String uuid) {
        if (category == null || uuid == null || uuid.isEmpty() || "none".equals(uuid)) return null;
        List<CarPart> parts = shopData.get(category.toUpperCase(Locale.ROOT));
        if (parts == null) return null;
        for (CarPart part : parts) {
            if (part != null && uuid.equals(part.uuid)) return part;
        }
        return null;
    }

    public static synchronized void addPart(String category, String uuid, String itemId, String nbt, String name, double price) {
        if (category == null || itemId == null || !Double.isFinite(price) || price < 0) return;
        String catUpper = category.toUpperCase(Locale.ROOT);
        if (shopData.containsKey(catUpper)) {
            String safeUuid = (uuid != null && !uuid.isBlank()) ? uuid : UUID.randomUUID().toString();
            String safeName = name != null ? name : "Part";
            shopData.get(catUpper).add(new CarPart(safeUuid, itemId, nbt != null ? nbt : "", safeName, price));
            save();
        }
    }

    // ADAUGAT: Metodă pentru a șterge o piesă din shop
    public static synchronized boolean removePart(String category, String name) {
        if (category == null || name == null) return false;
        String catUpper = category.toUpperCase(Locale.ROOT);
        if (shopData.containsKey(catUpper)) {
            List<CarPart> parts = shopData.get(catUpper);
            // Șterge piesa dacă numele corespunde ignorând literele mari/mici
            boolean removed = parts.removeIf(part -> part != null && part.name != null && part.name.equalsIgnoreCase(name));
            if (removed) {
                save(); // Salvăm modificările în JSON
                return true;
            }
        }
        return false;
    }
}