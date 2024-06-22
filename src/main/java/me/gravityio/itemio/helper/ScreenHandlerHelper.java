package me.gravityio.itemio.helper;

import com.google.common.base.Predicates;
import me.gravityio.itemio.lib.ListIterator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Helper class for handling screen handlers.
 */
public class ScreenHandlerHelper {

    public static Predicate<Slot> CONTAINER_SLOTS_ONLY = slot -> !(slot.inventory instanceof PlayerInventory);
    public static Predicate<Slot> PLAYER_SLOTS_ONLY = slot -> slot.inventory instanceof PlayerInventory;

    /**
     * Returns a predicate based on the given inventory type.
     *
     * @param type the inventory type (PLAYER or OTHER)
     * @return a predicate that filters slots based on the inventory type
     */
    public static Predicate<Slot> getPredicate(InventoryType type) {
        return switch (type) {
            case PLAYER -> PLAYER_SLOTS_ONLY;
            case OTHER -> CONTAINER_SLOTS_ONLY;
            case ANY -> Predicates.alwaysTrue();
        };
    }

    /*
      When you send the slot the player clicked to the server, you
      don't actually send the index of the slot that starts from hotbar -> player inventory -> opened inventory.<br>
      It's actually kind of dependent on each screen.<br><br>
      <p>
      What you're sending is actually the id of the slot, and the ID of the slot is handled
      by the implementation of each ScreenHandler, so for instance the GenericContainerScreenHandler,
      adds the slots from top to bottom, so first all the slots of the GenericContainer, then the PlayerInventory
      and then the Hotbar, so 0 would be top left of the GenericContainer
      <p>
     */

    /**
     * Determines if the given slot is an output slot.
     *
     * @param slot the slot to check
     * @return true if the slot is an output slot, false otherwise
     */
    public static boolean isOutputSlot(Slot slot) {
        return slot instanceof FurnaceOutputSlot
                || slot instanceof CraftingResultSlot;
    }

    /**
     * Retrieves a list of slots from the given screen handler that satisfy the specified predicates.
     *
     * @param handler      the screen handler from which to retrieve the slots
     * @param predicate    the predicate used to filter the slots to be added to the list
     * @return a list of slots that satisfy the predicates
     */
    public static List<Slot> getPredicateSlots(ScreenHandler handler, Predicate<Slot> predicate, boolean reverse) {
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : ListIterator.of(handler.slots, reverse)) {
            if (!predicate.test(slot)) continue;
            slots.add(slot);
        }
        return slots;
    }

    /**
     * Retrieves the first slot from the given screen handler that satisfies the specified predicate.
     *
     * @param handler   the screen handler from which to retrieve the slot
     * @param predicate the predicate used to filter the slot
     * @return the first slot that satisfies the predicate
     */
    public static Slot getPredicateSlot(ScreenHandler handler, Predicate<Slot> predicate, boolean reverse) {
        for (Slot slot : ListIterator.of(handler.slots, reverse)) {
            if (!predicate.test(slot)) continue;
            return slot;
        }
        return null;
    }
    /**
     * If an Inventory has an 'Output Slot' it will check if it has any item in that slot and return it
     *
     * @param handler the screen handler to search for the output slot
     * @param type    the type of inventory to search for (TOP or BOTTOM)
     * @return the ID of the output slot, or -1 if no output slot is found
     */
    public static int getFullOutputSlotID(ScreenHandler handler, InventoryType type) {
        var predicate = getPredicate(type).and(s -> !s.getStack().isEmpty() && isOutputSlot(s));
        var slot = getPredicateSlot(handler, predicate, false);
        if (slot == null) return -1;
        return slot.id;
    }

    /**
     * Returns a list of empty slots in the specified screen handler and inventory type.
     *
     * @param handler the screen handler to search for empty slots
     * @param type    the inventory type to filter the slots
     * @return a list of empty slots
     */
    private static List<Slot> getEmptySlots(ScreenHandler handler, InventoryType type) {
        return getEmptySlots(handler, type, Predicates.alwaysTrue());
    }

    /**
     * Returns a list of empty slots in the specified screen handler and inventory type.
     *
     * @param handler   the screen handler to search for empty slots
     * @param type      the inventory type to filter the slots
     * @param predicate the predicate used to filter the slots
     * @return a list of empty slots
     */
    private static List<Slot> getEmptySlots(ScreenHandler handler, InventoryType type, Predicate<Slot> predicate) {
        var merged = getPredicate(type).and(slot -> !slot.hasStack() && predicate.test(slot));
        return getPredicateSlots(handler, merged, type == InventoryType.PLAYER);
    }

    /**
     * Returns the ID of an empty slot in the specified screen handler and inventory type.
     *
     * @param handler the screen handler to search for empty slots
     * @param type    the inventory type to filter the slots
     * @return the ID of the first empty slot found, or -1 if no empty slot is found
     */
    private static int getEmptySlotID(ScreenHandler handler, InventoryType type, boolean reverse) {
        var predicate = getPredicate(type).and(s -> s.getStack().isEmpty());
        var slot = getPredicateSlot(handler, predicate, reverse);
        if (slot == null) return -1;
        return slot.id;
    }

    /**
     * Retrieves the ID of the first non-empty slot in the specified screen handler
     * and inventory type.
     *
     * @param handler The screen handler to search for non-empty slots.
     * @param type    The inventory type (TOP or BOTTOM) to filter the slots.
     * @return The ID of the first non-empty slot, or -1 if no non-empty slots are found.
     */
    public static int getNonEmptySlotID(ScreenHandler handler, InventoryType type, boolean reverse) {
        var predicate = getPredicate(type).and(slot -> !slot.getStack().isEmpty());
        Slot ret = getPredicateSlot(handler, predicate, reverse);
        return ret != null ? ret.id : -1;
    }

    /**
     * Finds the slot ID using a slot index in a screen handler.
     *
     * @param slotIndex the index of the slot
     * @param handler   the screen handler
     * @param type      the type of inventory to search in (TOP or BOTTOM)
     * @return the slot ID, or -1 if not found
     */
    public static int findIndexSlotID(int slotIndex, ScreenHandler handler, InventoryType type) {
        var predicate = getPredicate(type).and(slot -> slot.getIndex() == slotIndex);
        Slot ret = getPredicateSlot(handler, predicate, false);
        return ret != null ? ret.id : -1;
    }

    public static int findSlotID(int slotID, ScreenHandler handler, InventoryType type) {
        Predicate<Slot> predicate = getPredicate(type).and(slot -> slot.id == slotID);
        Slot found = getPredicateSlot(handler, predicate, false);
        return found != null ? found.id : -1;
    }

    /**
     * Finds the slot ID of the given searchStack in the specified ScreenHandler.
     *
     * @param searchStack the ItemStack to search for
     * @param handler     the ScreenHandler to search in
     * @param type        the type of inventory to search in (TOP or BOTTOM)
     * @return the slot ID of the found ItemStack, or -1 if not found
     */
    public static int findSlotID(ItemStack searchStack, ScreenHandler handler, InventoryType type) {
        return findSlotID(searchStack, handler, type, ItemStack::areEqual);
    }

    /**
     * Finds the slot ID of the given searchStack in the specified ScreenHandler.
     *
     * @param searchStack    the ItemStack to search for
     * @param handler        the ScreenHandler to search in
     * @param type           the type of inventory to search in (TOP or BOTTOM)
     * @param equalPredicate the predicate to compare ItemStacks for equality
     * @return the slot ID of the found ItemStack, or -1 if not found
     */
    public static int findSlotID(ItemStack searchStack, ScreenHandler handler, InventoryType type, BiPredicate<ItemStack, ItemStack> equalPredicate) {
        List<Slot> slots = getPredicateSlots(handler, getPredicate(type), type == InventoryType.PLAYER);

        for (Slot slot : slots) {
            if (!equalPredicate.test(slot.getStack(), searchStack)) continue;
            return slot.id;
        }
        return -1;
    }

    /**
     * Retrieves the count at the specified slot ID in the given screen handler.
     *
     * @param handler the screen handler to retrieve the count from
     * @param slotId  the ID of the slot to retrieve the count from
     * @return the count at the specified slot ID
     */
    public static int getCountAt(ScreenHandler handler, int slotId) {
        return handler.getSlot(slotId).getStack().getCount();
    }

    /**
     * Moves an item stack from one slot to another, or shifts it if the destination slot is not empty.
     *
     * @param client     the Minecraft client instance
     * @param fromSlotId the ID of the slot to move the item stack from
     * @param toSlotId   the ID of the slot to move the item stack to
     */
    public static void moveToOrShift(MinecraftClient client, int fromSlotId, int toSlotId) {
        var handler = client.player.currentScreenHandler;
        var from = handler.getSlot(fromSlotId).getStack();
        var to = handler.getSlot(toSlotId).getStack();
        if (from.isEmpty()) {
            return;
        }

        if (to.isEmpty()) {
            Helper.leftClickSlot(client.interactionManager, client.player, fromSlotId);
            Helper.leftClickSlot(client.interactionManager, client.player, toSlotId);
        } else {
            Helper.shiftClickSlot(client.interactionManager, client.player, fromSlotId);
        }
    }

    /**
     * Splits the stack in a player's inventory at a specified slot into smaller stacks of a given size.
     *
     * @param manager     the client player interaction manager
     * @param player      the player whose inventory is being modified
     * @param splitSlotId the ID of the slot containing the stack to be split
     * @param newSize     the desired size of each smaller stack
     * @return an array of slots containing the smaller stacks, or null if there are not enough empty slots
     */
    public static Slot[] splitStackQuickCraft(ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int newSize) {
        var handler = player.currentScreenHandler;
        int count = getCountAt(handler, splitSlotId);
        int splits = (int) Math.floor((double) count / newSize);
        List<Slot> free = getEmptySlots(handler, InventoryType.PLAYER, (slot) -> slot.getIndex() >= 0 && slot.getIndex() < 36);
        if (free.size() < splits) return null;

        Slot[] freeNeeded = new Slot[splits];
        for (int i = 0; i < freeNeeded.length; i++) {
            freeNeeded[i] = free.get(i);
        }

        Helper.leftClickSlot(manager, player, splitSlotId);
        Helper.quickcraftSlots(manager, player, freeNeeded, GLFW.GLFW_MOUSE_BUTTON_1);
        Helper.leftClickSlot(manager, player, splitSlotId);

        return freeNeeded;
    }

    public static void splitStackShift(ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int newSize) {
        var handler = player.currentScreenHandler;
        int count = getCountAt(handler, splitSlotId);
        if (count < newSize) {
            return;
        }
        if (count == newSize) {
            Helper.shiftClickSlot(manager, player, splitSlotId);
            return;
        }

        int availableSlot = getEmptySlotID(handler, InventoryType.ANY, false);
        if (availableSlot == -1) {
            return;
        }
        while (shouldSplit(player, splitSlotId, newSize)) {
            Helper.rightClickSlot(manager, player, splitSlotId);
            Helper.leftClickSlot(manager, player, availableSlot);
        }
        count = getCountAt(handler, splitSlotId);

        int distance = count - newSize;
        int absDistance = Math.abs(distance);
        int left = distance > 0 ? splitSlotId : availableSlot;
        int right = distance > 0 ? availableSlot : splitSlotId;

        if (absDistance > 0) {
            Helper.leftClickSlot(manager, player, left);
            for (int i = 0; i < absDistance; i++) {
                Helper.rightClickSlot(manager, player, right);
            }
            Helper.leftClickSlot(manager, player, left);
        }

        Helper.leftClickSlot(manager, player, availableSlot);
        Helper.shiftClickSlot(manager, player, splitSlotId);
        Helper.leftClickSlot(manager, player, splitSlotId);
    }

    /**
     * Determines whether the given player should split the stack based on the click slot ID and the target count.
     *
     * @param player      the player entity
     * @param clickSlotId the ID of the slot that was clicked
     * @param target      the target count for the stack
     * @return true if the player should split the stack, false otherwise
     */
    private static boolean shouldSplit(PlayerEntity player, int clickSlotId, int target) {
        var stackCount = getCountAt(player.currentScreenHandler, clickSlotId);
        if (stackCount == target) return false;

        var splitCount = Helper.simulateRightClick(player, clickSlotId).click().getCount();
        int a = Math.abs(splitCount - target);
        int b = Math.abs(stackCount - target);
        return a < b;
    }

    public enum InventoryType {
        PLAYER, OTHER, ANY
    }

}
