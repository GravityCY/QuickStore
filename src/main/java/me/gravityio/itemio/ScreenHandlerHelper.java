package me.gravityio.itemio;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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
            if (slot.getStack().isEmpty()) continue;
            return slot.id;
        }
        return -1;
    }

    public static int getSlotID(int slotIndex, ScreenHandler handler, Inventory inventory) {
        for (Slot slot : handler.slots) {
            if (slot.inventory == inventory && slot.getIndex() == slotIndex) {
                return slot.id;
            }
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

    public static void splitStack(ScreenHandler handler, ClientPlayerInteractionManager manager, PlayerEntity player, int splitSlotId, int outputSlotId, int newSize) {
        int count = handler.getSlot(splitSlotId).getStack().getCount();
        if (count < newSize) return;

        Helper.leftClickSlot(manager, player, handler.syncId, splitSlotId);
        if (newSize > 32) {
            int distance = count - newSize;
            for (int i = 0; i < distance; i++) {
                Helper.rightClickSlot(manager, player, handler.syncId, splitSlotId);
            }
            Helper.leftClickSlot(manager, player, handler.syncId, outputSlotId);

        } else {
            for (int i = 0; i < newSize; i++) {
                Helper.rightClickSlot(manager, player, handler.syncId, outputSlotId);
            }
            Helper.leftClickSlot(manager, player, handler.syncId, splitSlotId);

        }

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
        BOTTOM, TOP
    }

}
