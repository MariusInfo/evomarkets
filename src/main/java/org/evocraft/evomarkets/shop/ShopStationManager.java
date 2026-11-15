package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShopStationManager {
    private static ShopStationManager INSTANCE;
    private final Map<String, Set<BlockPos>> stations = new HashMap<>();
    private final File saveFile;
    private final Gson gson;

    public ShopStationManager() {
        this.saveFile = FMLPaths.CONFIGDIR.get().resolve("evo_markets_shop_stations.json").toFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static synchronized void initialize() {
        if (INSTANCE == null) INSTANCE = new ShopStationManager();
    }

    // FIX ANTI-CRASH APLICAT AICI
    public static synchronized ShopStationManager get() {
        if (INSTANCE == null) INSTANCE = new ShopStationManager();
        return INSTANCE;
    }

    public synchronized void addStation(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        String dim = level.dimension().location().toString();
        stations.computeIfAbsent(dim, k -> new HashSet<>()).add(pos.immutable());
        save();
    }

    public synchronized void removeStation(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        String dim = level.dimension().location().toString();
        if (stations.containsKey(dim)) {
            stations.get(dim).remove(pos);
            save();
        }
    }

    public synchronized boolean isStation(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        String dim = level.dimension().location().toString();
        return stations.containsKey(dim) && stations.get(dim).contains(pos);
    }

    public synchronized void save() {
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(stations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            Type type = new TypeToken<Map<String, Set<BlockPos>>>(){}.getType();
            Map<String, Set<BlockPos>> loaded = gson.fromJson(reader, type);
            stations.clear();
            if (loaded != null) stations.putAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}