package me.gravityio.itemio;

import me.gravityio.itemio.lib.keybind.KeybindManager;
import me.gravityio.itemio.lib.keybind.KeybindWrapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ItemIO implements ClientModInitializer {
    public static final String MOD_ID = "quickstore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ParticleEffect STORE_PARTICLE = ParticleTypes.HAPPY_VILLAGER;
    public static final ParticleEffect ADD_BLOCK_PARTICLE = ParticleTypes.GLOW;
    public static final ParticleEffect REMOVE_BLOCK_PARTICLE = ParticleTypes.SMALL_FLAME;
    public static final String FAR_INVENTORY = "messages.quickstore.far_inventory";
    private static final KeybindWrapper STORE = KeybindWrapper.of("key.quickstore.store", GLFW.GLFW_KEY_V, "category.gravityio.name");
    private static final KeybindWrapper INCREMENT = KeybindWrapper.of("key.quickstore.increment", GLFW.GLFW_KEY_LEFT_SHIFT, "category.gravityio.name");
    public static boolean IS_DEBUG;
    // STUFF
    public ItemStack heldStack = ItemStack.EMPTY;
    public BlockRec currentInventoryBlock;
    public Set<BlockRec> inventoryBlocks = new HashSet<>();
    public Iterator<BlockRec> inventoryBlockIterator;
    public boolean waiting;
    public boolean increment;
    public int slot;
    private int splitCount;
    // END OF STUFF

    public static void DEBUG(String message, Object... args) {
        if (!IS_DEBUG) {
            return;
        }

        LOGGER.info(message, args);
    }

    @Override
    public void onInitializeClient() {
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
        if (this.waiting) {
            client.player.setSneaking(false);
        }
    }

    private void whileStorePressed(MinecraftClient client) {
        var hit = client.getCameraEntity().raycast(5, 0, false);
        this.removeFarOnesButDontRemoveThem(client.player);

        if (this.waiting || !Helper.isInventory(client.world, hit)) {
            return;
        }

        var blockHit = (BlockHitResult) hit;
        var blockRec = new BlockRec(blockHit.getBlockPos(), blockHit.getSide());
        if (this.inventoryBlocks.contains(blockRec)) return;
        DEBUG("Adding '{}'", blockHit.getBlockPos().toShortString());
        var pos = blockRec.getParticlePosition();
        client.world.addParticle(ADD_BLOCK_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        this.inventoryBlocks.add(blockRec);
    }

    private void onReleaseStore(MinecraftClient client) {
        if (this.waiting) {
            return;
        }

        var hit = client.getCameraEntity().raycast(5, 0, false);
        if (!Helper.isInventory(client.world, hit)) {
            this.clear();
            return;
        }

        this.removeFarOnes(client.player);
        this.heldStack = client.player.getMainHandStack().copy();
        this.inventoryBlockIterator = this.inventoryBlocks.iterator();
        this.waiting = true;
        this.slot = client.player.getInventory().selectedSlot;
        this.splitCount = this.heldStack.getCount() / this.inventoryBlocks.size();
        this.increment = INCREMENT.isPressed();

        client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        DEBUG("Item: '{}', Count: '{}', Inventories: '{}', Split: '{}'", this.heldStack, this.heldStack.getCount(), this.inventoryBlocks.size(), this.splitCount);
        this.nextInventoryBlock();
        this.sendOpenScreenPacket(client, this.currentInventoryBlock);
    }

    private void clear() {
        this.waiting = false;
        this.increment = false;
        this.inventoryBlocks.clear();
        this.inventoryBlockIterator = null;
        this.currentInventoryBlock = null;
        this.heldStack = null;
    }

    private void removeFarOnes(PlayerEntity player) {
        var iterator = this.inventoryBlocks.iterator();
        var hadSomeTooFar = false;
        while (iterator.hasNext()) {
            var blockRec = iterator.next();
            if (!blockRec.tooFar(player)) continue;
            var pos = blockRec.getParticlePosition();
            player.getWorld().addParticle(REMOVE_BLOCK_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
            DEBUG("Removing '{}'", blockRec.pos().toShortString());
            iterator.remove();
            hadSomeTooFar = true;
        }
        if (hadSomeTooFar) {
            player.sendMessage(Text.translatable(FAR_INVENTORY), true);
        }
    }

    private void removeFarOnesButDontRemoveThem(PlayerEntity player) {
        for (BlockRec blockRec : this.inventoryBlocks) {
            if (!blockRec.tooFar(player)) continue;
            var pos = blockRec.getParticlePosition();
            player.getWorld().addParticle(REMOVE_BLOCK_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    private boolean nextInventoryBlock() {
        if (!this.inventoryBlockIterator.hasNext()) return false;
        this.currentInventoryBlock = this.inventoryBlockIterator.next();
        return true;
    }

    private void sendOpenScreenPacket(MinecraftClient client, BlockRec rec) {
        DEBUG("Sending open screen packet for '{}'", rec.pos().toShortString());
        client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, rec.toRaycast(), 6969));
    }

    private void onScreenFullyOpened(MinecraftClient client, ScreenHandler handler) {
        if (!this.waiting) return;

        var slotId = ScreenHandlerHelper.findSlotID(this.slot, handler, client.player.getInventory());
        var outputSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
        if (outputSlotId != -1 && (this.heldStack.isEmpty() || ItemStack.canCombine(handler.getSlot(outputSlotId).getStack(), this.heldStack))) {
            ScreenHandlerHelper.moveToOrShift(client, outputSlotId, slotId);
        } else {
            if (this.heldStack.isEmpty()) {
                int clickSlotId = ScreenHandlerHelper.getNonEmptySlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
                if (clickSlotId != -1) {
                    ScreenHandlerHelper.moveToOrShift(client, clickSlotId, slotId);
                }
            } else {
                ScreenHandlerHelper.splitStackShift(client.interactionManager, client.player, slotId, this.splitCount);
            }
        }

        client.player.closeHandledScreen();
        var pos = this.currentInventoryBlock.getParticlePosition();
        client.world.addParticle(STORE_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        if (!this.nextInventoryBlock()) {
            if (!ItemStack.areEqual(this.heldStack, client.player.getMainHandStack())) {
                client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 1);
            }
            if (this.increment && this.inventoryBlocks.size() == 1) {
                client.player.getInventory().scrollInHotbar(-1);
            }
            this.clear();
        } else {
            this.sendOpenScreenPacket(client, this.currentInventoryBlock);
        }
    }

}
