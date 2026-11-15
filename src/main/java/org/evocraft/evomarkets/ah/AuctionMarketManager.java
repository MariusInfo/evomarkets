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

    public static synchronized void initialize() {
        if (INSTANCE == null) INSTANCE = new AuctionMarketManager();
    }

    public static synchronized AuctionMarketManager get() {
        if (INSTANCE == null) INSTANCE = new AuctionMarketManager();
        return INSTANCE;
    }

    // --- OPERAȚII ---

    public synchronized void addListing(AuctionListing listing) {
        if (listing == null || listing.id == null || listing.sellerUUID == null || listing.itemStack == null || listing.itemStack.isEmpty()) return;
        listings.add(listing);
        save();
        // SINCRONIZARE INSTANTĂ: Notificăm toți jucătorii că s-a adăugat ceva nou
        broadcastUpdate();
    }

    public synchronized boolean removeListing(UUID listingId) {
        if (listingId == null) return false;
        boolean removed = listings.removeIf(l -> l != null && listingId.equals(l.id));
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

    public synchronized List<AuctionListing> getAllListings() {
        return new ArrayList<>(listings);
    }

    public synchronized List<AuctionListing> getListingsByPlayer(UUID playerUUID) {
        if (playerUUID == null) return new ArrayList<>();
        return listings.stream()
                .filter(l -> l != null && playerUUID.equals(l.sellerUUID))
                .collect(Collectors.toList());
    }

    public synchronized AuctionListing getListing(UUID id) {
        if (id == null) return null;
        return listings.stream()
                .filter(l -> l != null && id.equals(l.id))
                .findFirst()
                .orElse(null);
    }

    // --- SALVARE / ÎNCĂRCARE ---

    public synchronized void save() {
        File parent = saveFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (Writer writer = new FileWriter(saveFile)) {
            gson.toJson(listings, writer);
        } catch (IOException e) {
            LOGGER.error("Error saving auctions: ", e);
        }
    }

    public synchronized void load() {
        if (!saveFile.exists()) return;
        try (Reader reader = new FileReader(saveFile)) {
            AuctionListing[] loaded = gson.fromJson(reader, AuctionListing[].class);
            if (loaded != null) {
                listings.clear();
                for (AuctionListing l : loaded) {
                    if (l != null && l.id != null && l.sellerUUID != null
                            && l.itemStack != null && !l.itemStack.isEmpty()
                            && Double.isFinite(l.price) && l.price >= 1.0D) {
                        listings.add(l);
                    }
                }
            }
        } catch (Exception e) {
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
