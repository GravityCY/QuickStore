package me.gravityio.itemio;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class Helper {

    public static void leftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.PICKUP, player);
    }

    public static void rightClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_2, SlotActionType.PICKUP, player);
    }

    public static void shiftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.QUICK_MOVE, player);
    }

    public static void swapSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int fromSlotId, int toSlotId) {
        manager.clickSlot(syncId, fromSlotId, toSlotId, SlotActionType.SWAP, player);
    }

}
