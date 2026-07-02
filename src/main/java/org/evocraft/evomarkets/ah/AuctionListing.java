package org.evocraft.evomarkets.ah;

import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class AuctionListing {
    // Campuri publice pentru acces usor
    public final UUID id;
    public final UUID sellerUUID;
    public final String sellerName;
    public final ItemStack itemStack;
    public final double price;
    public Long timestamp;

    public AuctionListing(UUID sellerUUID, String sellerName, ItemStack itemStack, double price) {
        this.id = UUID.randomUUID();
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }
}