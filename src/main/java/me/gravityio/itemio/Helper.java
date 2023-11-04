package me.gravityio.itemio;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.ItemSteerable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class Helper {

    public static boolean isInventory(World world, HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        var blockHit = (BlockHitResult) hitResult;
        var blockPos = blockHit.getBlockPos();
        var blockEntity = world.getBlockEntity(blockPos);
        return blockEntity instanceof Inventory;
    }

    public static void leftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.PICKUP, player);
    }

    public static void rightClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_2, SlotActionType.PICKUP, player);
    }

    public static Pair<ItemStack, ItemStack> rightClickSlotInfo(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        rightClickSlot(manager, player, syncId, clickSlotId);
        var screen = player.currentScreenHandler;
        var click = screen.getSlot(clickSlotId).getStack().copy();
        var cursor = screen.getCursorStack().copy();
        return new Pair<>(click, cursor);
    }

    public static void shiftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int clickSlotId) {
        manager.clickSlot(syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.QUICK_MOVE, player);
    }

    public static void swapSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int syncId, int fromSlotId, int toSlotId) {
        manager.clickSlot(syncId, fromSlotId, toSlotId, SlotActionType.SWAP, player);
    }

    public static Pair<ItemStack, ItemStack> simulateRightClick(PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        var click = screen.getSlot(clickSlotId).getStack();
        var cursor = screen.getCursorStack();

        ItemStack clickOut;
        ItemStack cursorOut;
        if (cursor.isEmpty()) {
            clickOut = click.copy();
            cursorOut = click.copy();
            clickOut.setCount(click.getCount() / 2);
            cursorOut.setCount((click.getCount() + 1) / 2);
        } else {
            clickOut = click.copy();
            cursorOut = cursor.copy();
            if (ItemStack.canCombine(screen.getSlot(clickSlotId).getStack(), cursor)) {
                cursorOut.decrement(1);
                clickOut.increment(1);
            }
        }
        return new Pair<>(clickOut, cursorOut);
    }

}
