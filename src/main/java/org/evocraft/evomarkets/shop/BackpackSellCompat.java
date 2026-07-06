package org.evocraft.evomarkets.shop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BackpackSellCompat {
    private static final String SOPHISTICATED_BACKPACKS_MODID = "sophisticatedbackpacks";
    private static final String CURIOS_MODID = "curios";

    private BackpackSellCompat() {
    }

    public static int countSellableItems(Player player, ItemStack target) {
        if (!isAvailable(player, target)) return 0;

        int count = 0;
        for (ItemStack backpack : findBackpacks(player)) {
            count += countInBackpack(backpack, target);
        }
        return count;
    }

    public static int removeSellableItems(Player player, ItemStack target, int amount) {
        if (!isAvailable(player, target) || amount <= 0) return 0;

        int removed = 0;
        for (ItemStack backpack : findBackpacks(player)) {
            int remaining = amount - removed;
            if (remaining <= 0) break;
            removed += removeFromBackpack(backpack, target, remaining);
        }
        return removed;
    }

    private static boolean isAvailable(Player player, ItemStack target) {
        return player != null
                && target != null
                && !target.isEmpty()
                && ModList.get().isLoaded(SOPHISTICATED_BACKPACKS_MODID);
    }

    private static List<ItemStack> findBackpacks(Player player) {
        List<ItemStack> backpacks = new ArrayList<>();

        for (ItemStack stack : player.getInventory().items) {
            addIfBackpack(backpacks, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            addIfBackpack(backpacks, stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            addIfBackpack(backpacks, stack);
        }

        addCuriosBackpacks(player, backpacks);
        return backpacks;
    }

    private static void addIfBackpack(List<ItemStack> backpacks, ItemStack stack) {
        if (isSophisticatedBackpack(stack)) {
            backpacks.add(stack);
        }
    }

    private static boolean isSophisticatedBackpack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !SOPHISTICATED_BACKPACKS_MODID.equals(key.getNamespace())) return false;
        if (!key.getPath().contains("backpack")) return false;
        return stack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().isPresent();
    }

    private static int countInBackpack(ItemStack backpack, ItemStack target) {
        Optional<IItemHandler> optional = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (optional.isEmpty()) return 0;

        IItemHandler handler = optional.get();
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (isSellTarget(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int removeFromBackpack(ItemStack backpack, ItemStack target, int amount) {
        Optional<IItemHandler> optional = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (optional.isEmpty()) return 0;

        IItemHandler handler = optional.get();
        int removed = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            int remaining = amount - removed;
            if (remaining <= 0) break;

            ItemStack stack = handler.getStackInSlot(slot);
            if (!isSellTarget(stack, target)) continue;

            ItemStack extracted = handler.extractItem(slot, remaining, false);
            removed += extracted.getCount();
        }
        return removed;
    }

    private static boolean isSellTarget(ItemStack stack, ItemStack target) {
        return stack != null && !stack.isEmpty() && target != null && !target.isEmpty() && stack.getItem() == target.getItem();
    }

    private static void addCuriosBackpacks(Player player, List<ItemStack> backpacks) {
        if (!ModList.get().isLoaded(CURIOS_MODID)) return;

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInventory = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            getCuriosInventory.setAccessible(true);
            Object inventoryOptional = getCuriosInventory.invoke(null, player);
            Object curiosInventory = unwrapOptionalLike(inventoryOptional);
            if (curiosInventory == null) return;

            Method getCurios = curiosInventory.getClass().getMethod("getCurios");
            getCurios.setAccessible(true);
            Object curiosMapObject = getCurios.invoke(curiosInventory);
            if (!(curiosMapObject instanceof Map<?, ?> curiosMap)) return;

            for (Object stacksHandler : curiosMap.values()) {
                if (stacksHandler == null) continue;

                Method getStacks = stacksHandler.getClass().getMethod("getStacks");
                getStacks.setAccessible(true);
                Object stacks = getStacks.invoke(stacksHandler);
                if (!(stacks instanceof IItemHandler itemHandler)) continue;

                for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
                    addIfBackpack(backpacks, itemHandler.getStackInSlot(slot));
                }
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Curios is optional. If its API is absent or changed, the shop simply sells from vanilla inventory.
        }
    }

    private static Object unwrapOptionalLike(Object optionalLike) throws ReflectiveOperationException {
        if (optionalLike == null) return null;
        if (optionalLike instanceof Optional<?> optional) {
            return optional.orElse(null);
        }

        try {
            Method resolve = optionalLike.getClass().getMethod("resolve");
            resolve.setAccessible(true);
            Object resolved = resolve.invoke(optionalLike);
            if (resolved instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
        } catch (NoSuchMethodException ignored) {
            // Try the common Optional-style method below.
        }

        try {
            Method orElse = optionalLike.getClass().getMethod("orElse", Object.class);
            orElse.setAccessible(true);
            return orElse.invoke(optionalLike, new Object[]{null});
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
