package me.gravityio.itemio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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
    public static Predicate<Slot> PLAYER_SLOTS_ONLY = slot -> (slot.inventory instanceof PlayerInventory);

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

    /**
     * If an Inventory has an 'Output Slot' it will check if it has any item in that slot and return it
     *
     * @param handler the screen handler to search for the output slot
     * @param type    the type of inventory to search for (TOP or BOTTOM)
     * @return the ID of the output slot, or -1 if no output slot is found
     */
    public static int getOutputSlotID(ScreenHandler handler, InventoryType type) {
        List<Slot> slots = switch (type) {
            case PLAYER -> getPredicateSlots(handler, PLAYER_SLOTS_ONLY);
            case OTHER -> getPredicateSlots(handler, CONTAINER_SLOTS_ONLY);
        };
        for (Slot slot : slots) {
            if (slot.getStack().isEmpty() || !isOutputSlot(slot)) continue;
            return slot.id;
        }
        return -1;
    }

    private static List<Slot> getEmptySlots(ScreenHandler handler, InventoryType type) {
        return switch (type) {
            case PLAYER -> getPredicateSlots(handler, slot -> PLAYER_SLOTS_ONLY.test(slot) && !slot.hasStack());
            case OTHER -> getPredicateSlots(handler, slot -> CONTAINER_SLOTS_ONLY.test(slot) && !slot.hasStack());
        };
    }

    private static List<Slot> getEmptySlots(ScreenHandler handler, InventoryType type, Predicate<Slot> predicate) {
        return switch (type) {
            case PLAYER -> getPredicateSlots(handler, slot -> PLAYER_SLOTS_ONLY.test(slot) && !slot.hasStack() && predicate.test(slot));
            case OTHER -> getPredicateSlots(handler, slot -> CONTAINER_SLOTS_ONLY.test(slot) && !slot.hasStack() && predicate.test(slot));
        };
    }

    /**
     * Returns the ID of an empty slot in the specified screen handler and inventory type.
     *
     * @param handler the screen handler to search for empty slots
     * @param type    the inventory type to filter the slots
     * @return the ID of the first empty slot found, or -1 if no empty slot is found
     */
    private static int getEmptySlotID(ScreenHandler handler, InventoryType type) {
        List<Slot> slots = switch (type) {
            case PLAYER -> getPredicateSlots(handler, slot -> slot.inventory instanceof PlayerInventory);
            case OTHER -> getPredicateSlots(handler, slot -> !(slot.inventory instanceof PlayerInventory));
        };

        for (Slot slot : slots) {
            if (!slot.getStack().isEmpty()) continue;
            return slot.id;
        }
        return -1;
    }

    /**
     * Retrieves the ID of the first non-empty slot in the specified screen handler
     * and inventory type.
     *
     * @param handler The screen handler to search for non-empty slots.
     * @param type    The inventory type (TOP or BOTTOM) to filter the slots.
     * @return The ID of the first non-empty slot, or -1 if no non-empty slots are found.
     */
    public static int getNonEmptySlotID(ScreenHandler handler, InventoryType type) {
        List<Slot> slots = switch (type) {
            case PLAYER -> getPredicateSlots(handler, PLAYER_SLOTS_ONLY);
            case OTHER -> getPredicateSlots(handler, CONTAINER_SLOTS_ONLY);
        };

        for (Slot slot : slots) {
            if (slot.getStack().isEmpty()) continue;
            return slot.id;
        }
        return -1;
    }

    /**
     * Finds the slot ID using a slot index in a screen handler.
     *
     * @param slotIndex the index of the slot
     * @param handler   the screen handler
     * @param inventory the inventory
     * @return the slot ID, or -1 if not found
     */
    public static int findSlotID(int slotIndex, ScreenHandler handler, Inventory inventory) {
        for (Slot slot : handler.slots) {
            if (slot.inventory == inventory && slot.getIndex() == slotIndex) {
                return slot.id;
            }
        }
        return -1;
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
        List<Slot> slots = new ArrayList<>();
        if (type == InventoryType.OTHER) {
            slots = getPredicateSlots(handler, slot -> !(slot.inventory instanceof PlayerInventory));
        } else if (type == InventoryType.PLAYER) {
            slots = getPredicateSlots(handler, slot -> slot.inventory instanceof PlayerInventory);
        }

        for (Slot slot : slots) {
            if (!equalPredicate.test(slot.getStack(), searchStack)) continue;
            return slot.id;
        }
        return -1;
    }


    public static void splitStackSpam(ScreenHandler handler, ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int outputSlotId, int newSize) {
        int count = handler.getSlot(splitSlotId).getStack().getCount();
        if (count < newSize) return;
        if (count == newSize) {
            Helper.leftClickSlot(manager, player, splitSlotId);
            Helper.leftClickSlot(manager, player, outputSlotId);
            return;
        }

        Helper.leftClickSlot(manager, player, splitSlotId);
        if (newSize > 32) {
            int distance = count - newSize;
            for (int i = 0; i < distance; i++) {
                Helper.rightClickSlot(manager, player, splitSlotId);
            }
            Helper.leftClickSlot(manager, player, outputSlotId);

        } else {
            for (int i = 0; i < newSize; i++) {
                Helper.rightClickSlot(manager, player, outputSlotId);
            }
            Helper.leftClickSlot(manager, player, splitSlotId);

        }
    }

    public static void splitStackSpamShift(ScreenHandler handler, ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int newSize) {
        int count = handler.getSlot(splitSlotId).getStack().getCount();
        if (count < newSize) return;
        if (count == newSize) {
            Helper.shiftClickSlot(manager, player, splitSlotId);
            return;
        }

        Helper.leftClickSlot(manager, player, splitSlotId);
        for (int i = 0; i < newSize; i++) {
            Helper.rightClickSlot(manager, player, splitSlotId);
        }
        Helper.shiftClickSlot(manager, player, splitSlotId);
        Helper.leftClickSlot(manager, player, splitSlotId);

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

//    public static void splitAny(ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int newSize) {
//        var handler = player.currentScreenHandler;
//        int count = getCountAt(handler, splitSlotId);
//        int splits = (int) Math.floor((double) count / newSize);
//        List<Slot> freeList = getEmptySlots(handler, InventoryType.PLAYER, (slot) -> slot.getIndex() >= 0 && slot.getIndex() < 36);
//        if (freeList.size() >= splits) {
//            Slot[] freeNeeded = new Slot[splits];
//            for (int i = 0; i < freeNeeded.length; i++) {
//                freeNeeded[i] = freeList.get(i);
//            }
//            splitStackQuickCraft(manager, player, splitSlotId, freeNeeded, splits);
//        } else {
//            splitStackShift();
//        }
//    }

//    public static void splitStackQuickCraft(ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, Slot[] freeSlots, int splits) {
//        if (freeSlots.length < splits) return;
//
//        Slot[] freeNeeded = new Slot[splits];
//        for (int i = 0; i < freeNeeded.length; i++) {
//            freeNeeded[i] = freeSlots.get(i);
//        }
//
//        Helper.leftClickSlot(manager, player, splitSlotId);
//        Helper.quickcraftSlots(manager, player, freeNeeded, GLFW.GLFW_MOUSE_BUTTON_1);
//        Helper.leftClickSlot(manager, player, splitSlotId);
//    }

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

        int availableSlot = getEmptySlotID(handler, InventoryType.PLAYER);
        for (int i = 0; i < 10; i++) {
            if (shouldSplit(player, splitSlotId, newSize)) {
                Helper.rightClickSlot(manager, player, splitSlotId);
                Helper.leftClickSlot(manager, player, availableSlot);

            } else {
                break;
            }
        }
        count = getCountAt(handler, splitSlotId);

        int distance = count - newSize;
        if (distance > 0) {
            Helper.leftClickSlot(manager, player, splitSlotId);
            for (int i = 0; i < distance; i++) {
                Helper.rightClickSlot(manager, player, availableSlot);
            }
            Helper.leftClickSlot(manager, player, splitSlotId);
        } else {
            Helper.leftClickSlot(manager, player, availableSlot);
            for (int i = 0; i > distance; i--) {
                Helper.rightClickSlot(manager, player, splitSlotId);
            }
            Helper.leftClickSlot(manager, player, availableSlot);
        }

        Helper.shiftClickSlot(manager, player, splitSlotId);
        Helper.leftClickSlot(manager, player, availableSlot);
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

        var splitCount = Helper.simulateRightClick(player, clickSlotId).getLeft().getCount();
        int a = Math.abs(splitCount - target);
        int b = Math.abs(stackCount - target);
        return a < b;
    }

//    public static void splitStack(ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int outputSlotId, int newSize) {
//        var stack = player.playerScreenHandler.getSlot(splitSlotId).getStack();
//        int count = stack.getCount();
//
//        var splitSteps = stepsToTarget(count, newSize);
//
//        QuickStoreMod.DEBUG("Times to split into target of {} is {}: ", newSize, splitSteps);
//
//        if (splitSteps < newSize) {
//            Helper.rightClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//            Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//            for (int i = 0; i < splitSteps - 1; i++) {
//                Helper.rightClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//                Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//            }
//            var manual = newSize - split(count, splitSteps);
//            QuickStoreMod.DEBUG("Now manually clicking {} times", manual);
//            if (manual > 0) {
//                Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//                for (int i = 0; i < manual; i++) {
//                    Helper.rightClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//                }
//                Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//            } else {
//                Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//                for (int i = 0; i > manual; i--) {
//                    Helper.rightClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//                }
//                Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//            }
//
//        } else {
//            Helper.leftClickSlot(manager, player, player.playerScreenHandler.syncId, splitSlotId);
//            for (int i = 0; i < newSize; i++) {
//                Helper.rightClickSlot(manager, player, player.playerScreenHandler.syncId, outputSlotId);
//            }
//        }
//    }

//    private static int split(int num, int times) {
//        for (int i = 0; i < times; i++) {
//            num = num / 2;
//        }
//        return num;
//    }
//
//    private static int stepsToTarget(int count, int target) {
//        int steps = 0;
//        int distance = 100000; // 9
//
//        while (true) {
//            count = (count + 1) / 2;
//            var temp = Math.abs(target - count);
//            if (temp < distance) {
//                distance = temp;
//            } else {
//                return steps;
//            }
//            steps++;
//        }
//    }


    public enum InventoryType {
        PLAYER, OTHER
    }

}
