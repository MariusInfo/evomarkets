package org.evocraft.evomarkets.ah;

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

public class AuctionStationManager {
    private static AuctionStationManager INSTANCE;
    private final Map<String, Set<BlockPos>> stations = new HashMap<>();
    private final File saveFile;
    private final Gson gson;

    public AuctionStationManager() {
        this.saveFile = FMLPaths.CONFIGDIR.get().resolve("evo_markets_auction_stations.json").toFile();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new AuctionStationManager();
    }

    public static AuctionStationManager get() {
        if (INSTANCE == null) INSTANCE = new AuctionStationManager();
        return INSTANCE;
    }

    public void addStation(Level level, BlockPos pos) {
        String dim = level.dimension().location().toString();
        stations.computeIfAbsent(dim, k -> new HashSet<>()).add(pos);
        save();
    }

    public void removeStation(Level level, BlockPos pos) {
        String dim = level.dimension().location().toString();
        if (stations.containsKey(dim)) {
            stations.get(dim).remove(pos);
            save();
        }
    }

    public boolean isStation(Level level, BlockPos pos) {
        String dim = level.dimension().location().toString();
        return stations.containsKey(dim) && stations.get(dim).contains(pos);
    }

    public void save() {
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(stations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            Type type = new TypeToken<Map<String, Set<BlockPos>>>(){}.getType();
            Map<String, Set<BlockPos>> loaded = gson.fromJson(reader, type);
            if (loaded != null) stations.putAll(loaded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}