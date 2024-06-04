package me.gravityio.itemio;

import net.minecraft.block.WallSignBlock;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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
        for (int i = 0; i < 4; i++) {
            bytes[i] = getByteAt(value, left ? 3 - i : i);
        }
        return bytes;
    }

    public static boolean isInventory(World world, HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        var blockHit = (BlockHitResult) hitResult;
        return isInventory(world, blockHit.getBlockPos());
    }

    public static boolean isInventory(World world, BlockPos blockPos) {
        var blockState = world.getBlockState(blockPos);
        if (blockState.getBlock() instanceof WallSignBlock) {
            return isInventory(world, blockPos.offset(blockState.get(Properties.HORIZONTAL_FACING).getOpposite(), 1));
        }
        return world.getBlockEntity(blockPos) instanceof Inventory;
    }

    public static BlockPos getInventory(World world, HitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return null;
        var blockHit = (BlockHitResult) hitResult;
        return getInventory(world, blockHit.getBlockPos());
    }

    public static BlockPos getInventory(World world, BlockPos pos) {
        var blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof WallSignBlock) {
            return getInventory(world, pos.offset(blockState.get(Properties.HORIZONTAL_FACING).getOpposite(), 1));
        }
        return world.getBlockEntity(pos) == null ? null : pos;
    }

    /**
     * Executes a left click on a slot in the player's current screen,
     * and returns the clicked item stack and the cursor item stack.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param clickSlotId the ID of the clicked slot
     * @return the clicked item stack and the cursor item stack
     */
    public static ClickData leftClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        manager.clickSlot(screen.syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, SlotActionType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getStack().copy();
        var cursor = screen.getCursorStack().copy();
        return new ClickData(click, cursor);
    }

    public static void quickcraftSlots(ClientPlayerInteractionManager manager, PlayerEntity player, Slot[] clickSlots, int button) {
//					this.onMouseClick(slot2, slot2.id, ScreenHandler.packQuickCraftData(1, this.heldButtonType), SlotActionType.QUICK_CRAFT);
        var screen = player.currentScreenHandler;
        manager.clickSlot(screen.syncId, -999, ScreenHandler.packQuickCraftData(0, button), SlotActionType.QUICK_CRAFT, player);
        for (Slot clickSlot : clickSlots) {
            manager.clickSlot(screen.syncId, clickSlot.id, ScreenHandler.packQuickCraftData(1, button), SlotActionType.QUICK_CRAFT, player);
        }
        manager.clickSlot(screen.syncId, -999, ScreenHandler.packQuickCraftData(2, button), SlotActionType.QUICK_CRAFT, player);
    }

    /**
     * Executes a right click on a slot in the player's current screen,
     * and returns the clicked item stack and the cursor item stack.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param clickSlotId the ID of the clicked slot
     * @return the clicked item stack and the cursor item stack
     */
    public static ClickData rightClickSlot(ClientPlayerInteractionManager manager, PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        manager.clickSlot(screen.syncId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_2, SlotActionType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getStack().copy();
        var cursor = screen.getCursorStack().copy();
        return new ClickData(click, cursor);
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

    public static ClickData simulateLeftClick(PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        var click = screen.getSlot(clickSlotId).getStack();
        var cursor = screen.getCursorStack();

        ItemStack clickOut;
        ItemStack cursorOut;
        if (cursor.isEmpty()) {
            clickOut = ItemStack.EMPTY;
            cursorOut = click.copy();
        } else {
            clickOut = click.copy();
            cursorOut = cursor.copy();
            if (ItemStack.canCombine(click, cursor)) {
                int total = clickOut.getCount() + cursorOut.getCount();
                int countClick = Math.min(total, clickOut.getMaxCount());
                int countCursor = Math.max(total - clickOut.getMaxCount(), 0);

                clickOut.setCount(countClick);
                cursorOut.setCount(countCursor);
            } else {
                var temp = clickOut;
                clickOut = cursorOut;
                cursorOut = temp;
            }
        }
        return new ClickData(clickOut, cursorOut);
    }

    /**
     * Simulates a right click action by a player.
     *
     * @param player      the player entity performing the right click
     * @param clickSlotId the slot ID of the clicked item
     * @return the item stack obtained from the clicked slot
     */
    public static ClickData simulateRightClick(PlayerEntity player, int clickSlotId) {
        var screen = player.currentScreenHandler;
        var click = screen.getSlot(clickSlotId).getStack();
        var cursor = screen.getCursorStack();

        ItemStack clickOut;
        ItemStack cursorOut;
        if (cursor.isEmpty()) {
            clickOut = click.copy();
            cursorOut = click.copy();
            clickOut.setCount(click.getCount() / 2);
            cursorOut.setCount(click.getCount() / 2 + 1);
        } else {
            clickOut = click.copy();
            cursorOut = cursor.copy();
            if (ItemStack.canCombine(click, cursor)) {
                cursorOut.decrement(1);
                clickOut.increment(1);
            }
        }
        return new ClickData(clickOut, cursorOut);
    }

    /**
     * A record containing the stack you clicked and the stack in your cursor
     */
    public record ClickData(ItemStack click, ItemStack cursor) {}

}
