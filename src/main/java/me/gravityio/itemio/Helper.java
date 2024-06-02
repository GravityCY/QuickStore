package me.gravityio.itemio;

import net.minecraft.client.network.ClientPlayerInteractionManager;
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

    public static byte getByteAt(int value, int index) {
        return (byte) ((value >> (index * 8)) & 0xFF);
    }

    public static int setByteAt(int value, byte b, int index) {
        int cleared = value & ~(0xFF << (index * 8));
        return cleared | ((b & 0xFF) << (index * 8));
    }

    public static int getInt(byte[] bytes) {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            ret = setByteAt(ret, bytes[i], i);
        }
        return ret;
    }

    public static int shift(int value, int r, int g, int b, int a) {
        int ret = 0;

        ret = setByteAt(ret, getByteAt(value, 0), r);
        ret = setByteAt(ret, getByteAt(value, 1), g);
        ret = setByteAt(ret, getByteAt(value, 2), b);
        ret = setByteAt(ret, getByteAt(value, 3), a);

        return ret;
    }

    public static byte[] getBytes(int value, int r, int g, int b, int a) {
        byte[] bytes = new byte[4];
        bytes[0] = getByteAt(value, r);
        bytes[1] = getByteAt(value, g);
        bytes[2] = getByteAt(value, b);
        bytes[3] = getByteAt(value, a);
        return bytes;
    }

    /**
     * If left is true returns highest to lowest else returns lowest to highest
     */
    public static byte[] getBytes(int value, boolean left) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 3; i++) {
            bytes[left ? 3 - i : i] = getByteAt(value, i);
        }
        return bytes;
    }

    /**
     * Checks if the given hit result is an inventory block in the world.
     *
     * @param world     the world in which the hit result occurred
     * @param hitResult the hit result to check
     * @return true if the hit result corresponds to an inventory block, false otherwise
     */
    public static boolean isInventory(World world, HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        var blockHit = (BlockHitResult) hitResult;
        var blockPos = blockHit.getBlockPos();
        var blockEntity = world.getBlockEntity(blockPos);
        return blockEntity instanceof Inventory;
    }

    /**
     * Executes a left click on a slot in the player's current screen,
     * and returns the clicked item stack and the cursor item stack.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param clickSlotId the ID of the clicked slot
     * @return a pair containing the clicked item stack and the cursor item stack
     */
    public static Pair<Object, Object> leftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        manager.clickSlot(screen.syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getStack().copy();
        var cursor = screen.getCursorStack().copy();
        return new Pair<>(click, cursor);
    }

    /**
     * Executes a right click on a slot in the player's current screen,
     * and returns the clicked item stack and the cursor item stack.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param clickSlotId the ID of the clicked slot
     * @return a Pair object containing the clicked ItemStack and the cursor ItemStack
     */
    public static Pair<ItemStack, ItemStack> rightClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        manager.clickSlot(screen.syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_2, SlotActionType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getStack().copy();
        var cursor = screen.getCursorStack().copy();
        return new Pair<>(click, cursor);
    }

    /**
     * Shift-clicks the slot.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param clickSlotId the ID of the clicked slot
     * @return the stack obtained from the clicked slot
     */
    public static ItemStack shiftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int clickSlotId) {
        manager.clickSlot(player.currentScreenHandler.syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.QUICK_MOVE, player);
        return player.currentScreenHandler.getSlot(clickSlotId).getStack();
    }

    /**
     * Swaps two slots in the player's inventory.
     *
     * @param manager    the client player interaction manager
     * @param player     the player entity
     * @param fromSlotId the ID of the slot to swap from
     * @param toSlotId   the ID of the slot to swap to
     */
    public static void swapSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int fromSlotId, int toSlotId) {
        manager.clickSlot(player.currentScreenHandler.syncId, fromSlotId, toSlotId, SlotActionType.SWAP, player);
    }

    /**
     * Simulates a right click action by a player.
     *
     * @param player      the player entity performing the right click
     * @param clickSlotId the slot ID of the clicked item
     * @return a pair of ItemStacks representing the modified click output and cursor output
     */
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
