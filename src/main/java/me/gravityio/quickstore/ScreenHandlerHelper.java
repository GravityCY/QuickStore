package me.gravityio.quickstore;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ScreenHandlerHelper {

    /**
     * When you send the slot the player clicked to the server, you
     * don't actually send the slot index that starts from hotbar -> player inventory -> opened inventory.<br>
     * It's actually kind of dependent on each screen.<br><br>
     *
     * What you're sending is actually the id of the slot, and the ID of the slot is handled
     * by the implementation of each ScreenHandler, so for instance the GenericContainerScreenHandler,
     * adds the slots from top to bottom, so first all the slots of the GenericContainer, then the PlayerInventory
     * and then the Hotbar, so 0 would be top left of the GenericContainer, and as said previously
     * if some ScreenHandler decides to add the Player Hotbar first it would completely
     * fuck everything up, and then index 0 would be the hotbar, for instance.<br><br>
     *
     * ALL OF IT IS DEPENDENT ON EACH SCREEN HANDLER, THIS IS A QUICK SOLUTION TO THAT PROBLEM,
     * THE BEST WAY TO CHECK IT SO ITERATE THROUGH ALL THE SLOTS AND FIND THE SAME ITEMSTACK <br><br>
     *
     * Fuck it imma search the whole inventory for the ItemStack
     */
    public static int toHandlerID(int handlerSize, int playerInvSize, int slot) {
        return handlerSize - playerInvSize + slot;
    }

    public static int toHandlerID(ScreenHandler handler, int index, InventoryType type) {
        if (type == InventoryType.TOP)
            return index;
        var slot = getPredicateSlot(handler, s -> s.inventory instanceof PlayerInventory && s.getIndex() == index);
        if (slot == null) return -1;
        return slot.id;
    }

    public static boolean isOutputSlot(Slot slot) {
        return slot instanceof FurnaceOutputSlot
                || slot instanceof CraftingResultSlot;
    }

    public static List<Slot> getPredicateSlots(ScreenHandler handler, Predicate<Slot> predicate) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (!predicate.test(slot)) continue;
            slots.add(slot);
        }
        return slots;
    }

    public static Slot getPredicateSlot(ScreenHandler handler, Predicate<Slot> predicate) {
        for (Slot slot : handler.slots) {
            if (!predicate.test(slot)) continue;
            return slot;
        }
        return null;
    }

    public static int getOutputSlotID(ScreenHandler handler, InventoryType type) {
        List<Slot> slots = new ArrayList<>();
        if (type == InventoryType.TOP) {
            slots = getPredicateSlots(handler, slot -> !(slot.inventory instanceof PlayerInventory));
        } else if (type == InventoryType.BOTTOM) {
            slots = getPredicateSlots(handler, slot -> slot.inventory instanceof PlayerInventory);
        }
        for (Slot slot : slots) {
            if (slot.getStack().isEmpty() || !isOutputSlot(slot)) continue;
            return slot.id;
        }
        return -1;
    }

    public static int getNonEmptySlotID(ScreenHandler handler, InventoryType type) {
        List<Slot> slots = new ArrayList<>();
        if (type == InventoryType.TOP) {
            slots = getPredicateSlots(handler, slot -> !(slot.inventory instanceof PlayerInventory));
        } else if (type == InventoryType.BOTTOM) {
            slots = getPredicateSlots(handler, slot -> slot.inventory instanceof PlayerInventory);
        }

        for (Slot slot : slots) {
            if (slot.getStack().isEmpty() || !isOutputSlot(slot)) continue;
            return slot.id;
        }

        for (Slot slot : slots) {
            if (slot.getStack().isEmpty()) continue;
            return slot.id;
        }
        return -1;
    }

    public static int getSlotID(ItemStack searchStack, ScreenHandler handler, InventoryType type) {
        return getSlotID(searchStack, handler, type, ItemStack::areEqual);
    }

    public static int getSlotID(ItemStack searchStack, ScreenHandler handler, InventoryType type, BiPredicate<ItemStack, ItemStack> equalPredicate) {
        List<Slot> slots = new ArrayList<>();
        if (type == InventoryType.TOP) {
            slots = getPredicateSlots(handler, slot -> !(slot.inventory instanceof PlayerInventory));
        } else if (type == InventoryType.BOTTOM) {
            slots = getPredicateSlots(handler, slot -> slot.inventory instanceof PlayerInventory);
        }

        for (Slot slot : slots) {
            if (!equalPredicate.test(slot.getStack(), searchStack)) continue;
            return slot.id;
        }
        return -1;
    }

    public enum InventoryType {
        BOTTOM, TOP
    }

}
