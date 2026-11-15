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

    public static final Map<String, List<PlanePart>> shopData = new HashMap<>();

    static {
        ensureDefaultCategories(shopData);
    }

    private static void ensureDefaultCategories(Map<String, List<PlanePart>> map) {
        map.computeIfAbsent("BODY", k -> new ArrayList<>());
        map.computeIfAbsent("ENGINE", k -> new ArrayList<>());
        map.computeIfAbsent("ATTACHMENTS", k -> new ArrayList<>());
    }

    public static synchronized void load() {
        if (!SAVE_FILE.exists()) return;
        try (Reader reader = new FileReader(SAVE_FILE)) {
            Map<String, List<PlanePart>> loaded = GSON.fromJson(reader, new TypeToken<Map<String, List<PlanePart>>>(){}.getType());
            ensureDefaultCategories(shopData);
            if (loaded != null) {
                for (Map.Entry<String, List<PlanePart>> entry : loaded.entrySet()) {
                    String key = entry.getKey() != null ? entry.getKey().toUpperCase(Locale.ROOT) : "";
                    if (!shopData.containsKey(key)) continue;
                    List<PlanePart> clean = new ArrayList<>();
                    if (entry.getValue() != null) {
                        for (PlanePart part : entry.getValue()) {
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

    public static synchronized PlanePart findPart(String category, String uuid) {
        if (category == null || uuid == null || uuid.isEmpty() || "none".equals(uuid)) return null;
        List<PlanePart> parts = shopData.get(category.toUpperCase(Locale.ROOT));
        if (parts == null) return null;
        for (PlanePart part : parts) {
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
            shopData.get(catUpper).add(new PlanePart(safeUuid, itemId, nbt != null ? nbt : "", safeName, price));
            save();
        }
    }

    public static synchronized boolean removePart(String category, String name) {
        if (category == null || name == null) return false;
        String catUpper = category.toUpperCase(Locale.ROOT);
        if (shopData.containsKey(catUpper)) {
            List<PlanePart> parts = shopData.get(catUpper);
            boolean removed = parts.removeIf(part -> part != null && part.name != null && part.name.equalsIgnoreCase(name));
            if (removed) {
                save();
                return true;
            }
        }
        return false;
    }
}