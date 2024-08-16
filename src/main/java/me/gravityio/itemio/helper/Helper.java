package me.gravityio.itemio.helper;

import me.gravityio.itemio.lib.PredicateRaycastContext;
import me.gravityio.itemio.mixins.mod.HandledAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class Helper {
    public static boolean isExactlyTheSame(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        return ItemStack.isSameItemSameComponents(a, b);
        //?} else {
        /*return ItemStack.matches(a, b);
        *///?}
    }

    public static byte getByteAt(int value, int index, int size, boolean left) {
        index = left ? size - 1 - index  : index;
        return (byte) ((value >> (index * 8)) & 0xFF);
    }

    public static int setByteAt(int value, byte b, int index, int size, boolean left) {
        index = left ? size - 1 - index  : index;
        int cleared = value & ~(0xFF << (index * 8));
        return cleared | ((b & 0xFF) << (index * 8));
    }

    public static int getInt(byte[] bytes, int size, boolean left) {
        int ret = 0;
        for (int i = 0; i < size; i++) {
            ret = setByteAt(ret, bytes[i], i, size, left);
        }
        return ret;
    }

    /**
     * Gives you a new integer with re-ordered bytes according to the indices provided by `indexArray`
     */
    public static int reorder(int value, int... indexArray) {
        int ret = 0;

        for (int i = 0; i < indexArray.length; i++) {
            ret = setByteAt(ret, getByteAt(value, i, indexArray.length, false), indexArray[i], indexArray.length, false);
        }

        return ret;
    }

    public static byte[] getBytes(int value, int... indexArray) {
        byte[] bytes = new byte[indexArray.length];

        for (int i = 0; i < indexArray.length; i++) {
            bytes[i] = getByteAt(value, indexArray[i], indexArray.length, false);
        }
        return bytes;
    }

    /**
     * If left is true returns highest to lowest else returns lowest to highest
     */
    public static byte[] getBytes(int value, int size, boolean left) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = getByteAt(value, i, size, left);
        }
        return bytes;
    }

    public static BlockHitResult raycast(Entity entity, float tickDelta, float maxDistance) {
        Level world = entity.level();
        Vec3 cameraPos = entity.getEyePosition(tickDelta);
        Vec3 cameraForward = entity.getViewVector(tickDelta);


        Vec3 cameraEnd = cameraPos.add(cameraForward.x * maxDistance, cameraForward.y * maxDistance, cameraForward.z * maxDistance);
        return world.clip(new PredicateRaycastContext(
                cameraPos, cameraEnd,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                entity,
                (w, p) -> w.getBlockState(p).getBlock() instanceof WallSignBlock
        ));
    }

    /**
     * Gets the inventory the player is looking at
     */
    public static BlockHitResult getLookingAtInventory(Minecraft client) {
        //? if >=1.21 {
        var hit = Helper.raycast(client.cameraEntity, client.getTimer().getGameTimeDeltaPartialTick(true), (float) client.player.blockInteractionRange());
        //?} elif >=1.20.5 {
        /*var hit = Helper.raycast(client.cameraEntity, client.getDeltaFrameTime(), (float) client.player.blockInteractionRange());
        *///?} else {
        /*var hit = Helper.raycast(client.cameraEntity, client.getDeltaFrameTime(), client.gameMode.getPickRange());
        *///?}
        if (!Helper.isInventory(client.level, hit)) {
            return null;
        }
        return hit;
    }

    public static boolean isInventory(Level world, BlockHitResult hitResult) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
        return blockEntity instanceof MenuProvider && blockEntity instanceof Container;
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
    public static ClickData leftClickSlot(MultiPlayerGameMode manager, Player player, int clickSlotId) {
        var screen = player.containerMenu;
        manager.handleInventoryMouseClick(screen.containerId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, ClickType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getItem().copy();
        var cursor = screen.getCarried().copy();
        return new ClickData(click, cursor);
    }

    public static void quickcraftSlots(MultiPlayerGameMode manager, Player player, Slot[] clickSlots, int button) {
//					this.onMouseClick(slot2, slot2.id, AbstractContainerMenu.getQuickCraftMask(1, this.heldButtonType), ClickType.QUICK_CRAFT);
        var screen = player.containerMenu;
        manager.handleInventoryMouseClick(screen.containerId, -999, AbstractContainerMenu.getQuickcraftMask(0, button), ClickType.QUICK_CRAFT, player);
        for (Slot clickSlot : clickSlots) {
            manager.handleInventoryMouseClick(screen.containerId, clickSlot.index, AbstractContainerMenu.getQuickcraftMask(1, button), ClickType.QUICK_CRAFT, player);
        }
        manager.handleInventoryMouseClick(screen.containerId, -999, AbstractContainerMenu.getQuickcraftMask(2, button), ClickType.QUICK_CRAFT, player);
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
    public static ClickData rightClickSlot(MultiPlayerGameMode manager, Player player, int clickSlotId) {
        var screen = player.containerMenu;
        manager.handleInventoryMouseClick(screen.containerId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_2, ClickType.PICKUP, player);
        var click = screen.getSlot(clickSlotId).getItem().copy();
        var cursor = screen.getCarried().copy();
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
    public static ItemStack shiftClickSlot(MultiPlayerGameMode manager, Player player, int clickSlotId) {
        manager.handleInventoryMouseClick(player.containerMenu.containerId, clickSlotId, GLFW.GLFW_MOUSE_BUTTON_1, ClickType.QUICK_MOVE, player);
        return player.containerMenu.getSlot(clickSlotId).getItem();
    }

    /**
     * Swaps two slots in the player's inventory.
     *
     * @param manager     the client player interaction manager
     * @param player      the player entity
     * @param fromSlotId  the ID of the slot to swap from (The actual ID of the slot in the AbstractContainerMenu)
     * @param toSlotIndex the index of the slot to swap to (the slot in the player's inventory)
     */
    public static void swapSlot(MultiPlayerGameMode manager, Player player, int fromSlotId, int toSlotIndex) {
        manager.handleInventoryMouseClick(player.containerMenu.containerId, fromSlotId, toSlotIndex, ClickType.SWAP, player);
    }

    public static ClickData simulateLeftClick(Player player, int clickSlotId) {
        var screen = player.containerMenu;
        var click = screen.getSlot(clickSlotId).getItem();
        var cursor = screen.getCarried();

        ItemStack clickOut;
        ItemStack cursorOut;
        if (cursor.isEmpty()) {
            clickOut = ItemStack.EMPTY;
            cursorOut = click.copy();
        } else {
            clickOut = click.copy();
            cursorOut = cursor.copy();
            if (Helper.isExactlyTheSame(click, cursor)) {
                int total = clickOut.getCount() + cursorOut.getCount();
                int countClick = Math.min(total, clickOut.getMaxStackSize());
                int countCursor = Math.max(total - clickOut.getMaxStackSize(), 0);

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
    public static ClickData simulateRightClick(Player player, int clickSlotId) {
        var screen = player.containerMenu;
        var click = screen.getSlot(clickSlotId).getItem();
        var cursor = screen.getCarried();

        ItemStack clickOut;
        ItemStack cursorOut;
        if (cursor.isEmpty()) {
            clickOut = click.copy();
            cursorOut = click.copy();
            clickOut.setCount(click.getCount() / 2);
            cursorOut.setCount((int) Math.ceil(click.getCount() / 2f));
        } else {
            clickOut = click.copy();
            cursorOut = cursor.copy();
            if (Helper.isExactlyTheSame(click, cursor)) {
                cursorOut.shrink(1);
                clickOut.grow(1);
            }
        }
        return new ClickData(clickOut, cursorOut);
    }

    public static HoverData getHoverStack(Minecraft client) {
        if (!(client.screen instanceof AbstractContainerScreen<?> handled)) return null;
        HandledAccessor accessor = (HandledAccessor) handled;
        HoverData data = null;

        int mx = (int)(client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getWidth());
        int my = (int)(client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getHeight());

        AbstractContainerMenu handler = handled.getMenu();
        for (Slot slot : handler.slots) {
            if (accessor.itemio$isPointOverSlot(slot, mx, my)) {
                data = new HoverData(handler, handled, slot, slot.index, slot.getContainerSlot(), slot.getItem(), mx, my);
                break;
            }
        }
        return data;
    }

    public record HoverData(AbstractContainerMenu handler, AbstractContainerScreen<?> screen, Slot slot, int slotId, int slotIndex, ItemStack stack, int x, int y) {}

    /**
     * A record containing the stack you clicked and the stack in your cursor
     */
    public record ClickData(ItemStack click, ItemStack cursor) {
    }

}
