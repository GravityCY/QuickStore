package me.gravityio.itemio;

import me.gravityio.itemio.lib.keybind.KeybindManager;
import me.gravityio.itemio.lib.keybind.KeybindWrapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.telemetry.WorldUnloadedEvent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MinecartItem;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ItemIO implements ClientModInitializer {
    public static final String MOD_ID = "quickstore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final double MAX_BREAK_SQUARED_DISTANCE = 36;
    private static final KeybindWrapper STORE = KeybindWrapper.of("key.quickstore.store",GLFW.GLFW_KEY_V, "category.quickstore.name");
    public static boolean IS_DEBUG;
    public static ItemIO INSTANCE;
    public boolean waiting = false;
    public int slot = -1;
    public Set<BlockRec> blocks = new HashSet<>();
    public BlockRec current;
    public Iterator<BlockRec> blockIterator;
    public ItemStack stack = ItemStack.EMPTY;
    private boolean canSneak = true;

    public static void DEBUG(String message, Object...args) {
        if (!IS_DEBUG) {
            return;
        }

        LOGGER.info(message, args);
    }



    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        IS_DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();
        KeybindManager.init();

        var client = MinecraftClient.getInstance();
        ClientTickEvents.END_WORLD_TICK.register(w -> this.onTick(client));
        ModEvents.ON_SCREEN_FULLY_OPENED.register(handler -> this.onScreenFullyOpened(client, handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> this.clear());

        STORE.setWhilePressedCallback(() -> this.whileStorePressed(client));
        STORE.setOnReleaseCallback(() -> this.onReleaseStore(client));
    }

    private void onTick(MinecraftClient client) {
        KeybindManager.tick(client.getWindow().getHandle());
        if (!this.canSneak && this.waiting) {
            client.player.setSneaking(false);
        }
    }

    private void clear() {
        this.waiting = false;
        this.blocks.clear();
        this.blockIterator = null;
        this.current = null;
        this.stack = null;
        this.slot = -1;
    }

    private void whileStorePressed(MinecraftClient client) {
        if (this.waiting || !Helper.isInventory(client.world, client.crosshairTarget)) {
            return;
        }

        var blockHit = (BlockHitResult) client.crosshairTarget;
        var blockRec = new BlockRec(blockHit.getBlockPos(), blockHit.getSide());
        if (this.blocks.contains(blockRec)) return;
        DEBUG("Adding '{}'", blockHit.getBlockPos().toShortString());
        var pos = blockRec.pos().toCenterPos().offset(blockRec.side(), 0.75f);
        client.world.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 0, 0, 0);
        this.blocks.add(blockRec);
    }

    private void onReleaseStore(MinecraftClient client) {
        if (this.waiting) {
            return;
        }
        if (!Helper.isInventory(client.world, client.crosshairTarget) ) {
            this.clear();
            return;
        }

        this.stack = client.player.getMainHandStack().copy();
        this.blockIterator = this.blocks.iterator();
        var sizePerInv = this.stack.getCount() / this.blocks.size();
        DEBUG("Item: '{}', Count: '{}', Inventories: '{}', Split: '{}'", this.stack, this.stack.getCount(), this.blocks.size(), sizePerInv);
        this.waiting = true;
        this.canSneak = true;
        for (ItemStack handItem : client.player.getHandItems()) {
            if (handItem.isEmpty()) continue;
            this.canSneak = false;
            break;
        }
        this.slot = client.player.getInventory().selectedSlot;
        if (!this.canSneak) {
            client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        this.sendOpenScreenPacket(client);
    }

    private boolean sendOpenScreenPacket(MinecraftClient client) {
        if (!this.blockIterator.hasNext()) return false;

        this.current = this.blockIterator.next();
        this.sendOpenScreenPacket(client, this.current);
        return true;
    }

    private void sendOpenScreenPacket(MinecraftClient client, BlockRec rec) {
        DEBUG("Sending open screen packet for '{}'", rec.pos().toShortString());
        var center = rec.pos().toCenterPos();

        var blockHit = new BlockHitResult(center, rec.side(), rec.pos(), false);
        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHit, 6969));
    }

    private void onScreenFullyOpened(MinecraftClient client, ScreenHandler handler) {
        if (!this.waiting) return;

        var slotId = ScreenHandlerHelper.findSlotID(this.slot, handler, client.player.getInventory());
        boolean success = false;
        if (this.stack.isEmpty()) {
            var clickSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
            if (clickSlotId == -1) {
                clickSlotId = ScreenHandlerHelper.getNonEmptySlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
            }
            if (clickSlotId != -1) {
                Helper.shiftClickSlot(client.interactionManager, client.player, handler.syncId, clickSlotId);
                success = true;
            }
        } else {
            var clickSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
            if (clickSlotId != -1 && ItemStack.canCombine(handler.getSlot(clickSlotId).getStack(), this.stack)) {
                Helper.shiftClickSlot(client.interactionManager, client.player, handler.syncId, clickSlotId);
            } else {
                var split = this.stack.getCount() / this.blocks.size();
                ScreenHandlerHelper.splitStackShiftTest(client.interactionManager, client.player, slotId, split);
            }

        }
        client.player.closeHandledScreen();
        if (success || !ItemStack.areEqual(this.stack, client.player.getMainHandStack())) {
            client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 1);
            var pos = this.current.pos().toCenterPos().offset(this.current.side(), 0.75f);
            client.world.addParticle(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 0, 0, 0);
        }
        if (!this.sendOpenScreenPacket(client)) {
            this.clear();
        }
    }

}
