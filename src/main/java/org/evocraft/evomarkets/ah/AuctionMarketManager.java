package org.evocraft.evomarkets.ah;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler; // Reteaua noastra

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionMarketManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static AuctionMarketManager INSTANCE;
    private final List<AuctionListing> listings = new ArrayList<>();
    private final File saveFile;
    private final Gson gson;

    public AuctionMarketManager() {
        this.saveFile = FMLPaths.CONFIGDIR.get().resolve("evo_markets_auction_items.json").toFile();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .create();

        load();
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new AuctionMarketManager();
    }

    public static AuctionMarketManager get() {
        return INSTANCE;
    }

    // --- OPERAȚII ---

    public void addListing(AuctionListing listing) {
        listings.add(listing);
        save();
        // SINCRONIZARE INSTANTĂ: Notificăm toți jucătorii că s-a adăugat ceva nou
        broadcastUpdate();
    }

    public boolean removeListing(UUID listingId) {
        boolean removed = listings.removeIf(l -> l.id.equals(listingId));
        if (removed) {
            save();
            // SINCRONIZARE INSTANTĂ: Notificăm toți jucătorii că un item a dispărut
            broadcastUpdate();
        }
        return removed;
    }

    /**
     * Trimite un pachet către toți jucătorii de pe server pentru a forța reîmprospătarea listei AH.
     */
    private void broadcastUpdate() {
        EvoMarketsPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new EvoMarketsPacketHandler.S2C_AuctionGlobalUpdate());
    }

    public List<AuctionListing> getAllListings() {
        return new ArrayList<>(listings);
    }

    public List<AuctionListing> getListingsByPlayer(UUID playerUUID) {
        return listings.stream()
                .filter(l -> l.sellerUUID.equals(playerUUID))
                .collect(Collectors.toList());
    }

    public AuctionListing getListing(UUID id) {
        return listings.stream()
                .filter(l -> l.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    // --- SALVARE / ÎNCĂRCARE ---

    public void save() {
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(listings, writer);
        } catch (IOException e) {
            LOGGER.error("Error saving auctions: ", e);
        }
    }

    public void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            AuctionListing[] loaded = gson.fromJson(reader, AuctionListing[].class);
            if (loaded != null) {
                listings.clear();
                for (AuctionListing l : loaded) {
                    listings.add(l);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error loading auctions: ", e);
        }
    }

    // Adapter JSON pentru Itemele Minecraft
    private static class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
        @Override
        public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
            CompoundTag tag = new CompoundTag();
            src.save(tag);
            return new JsonPrimitive(tag.toString());
        }

        @Override
        public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                CompoundTag tag = TagParser.parseTag(json.getAsString());
                return ItemStack.of(tag);
            } catch (Exception e) {
                return ItemStack.EMPTY;
            }
        }
    }
}
