package me.gravityio.itemio;

import com.mojang.blaze3d.systems.RenderSystem;
import me.gravityio.itemio.lib.keybind.KeybindManager;
import me.gravityio.itemio.lib.keybind.KeybindWrapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ItemIO implements ClientModInitializer {
    public static final String MOD_ID = "itemio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ParticleEffect STORE_PARTICLE = ParticleTypes.HAPPY_VILLAGER;
    public static final ParticleEffect ADD_BLOCK_PARTICLE = ParticleTypes.GLOW;
    public static final ParticleEffect REMOVE_BLOCK_PARTICLE = ParticleTypes.SMALL_FLAME;

    public static final String FAR_INVENTORY = "messages.quickstore.far_inventory";

    private static final KeybindWrapper STORE = KeybindWrapper.of("key.quickstore.store", GLFW.GLFW_KEY_V, "category.gravityio.name");
    private static final KeybindWrapper INCREMENT = KeybindWrapper.of("key.quickstore.increment", GLFW.GLFW_KEY_LEFT_SHIFT, "category.gravityio.name");

    public static boolean IS_DEBUG;

    public ItemStack heldStack = ItemStack.EMPTY;
    public BlockRec currentInventoryBlock;
    public Set<BlockRec> inventoryBlocks = new HashSet<>();
    public Iterator<BlockRec> inventoryBlockIterator;
    public boolean waiting;
    public boolean increment;
    public int slot;
    private int splitCount;

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

        ModConfig.HANDLER.load();

        var client = MinecraftClient.getInstance();
        ClientTickEvents.END_WORLD_TICK.register(w -> this.onTick(client));
        ModEvents.ON_SCREEN_FULLY_OPENED.register(handler -> this.onScreenFullyOpened(client, handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> this.clear());

        STORE.whilePressed(() -> this.whileStorePressed(client));
        STORE.onRelease(() -> this.onReleaseStore(client));

        WorldRenderEvents.END.register((a) -> this.onRender(client));
    }

    private void onRender(MinecraftClient client) {

        if (this.inventoryBlocks.isEmpty()) return;
        if (client.player == null) return;
        if (this.waiting) return;

        var hit = client.getCameraEntity().raycast(5, 0, false);
        if (!Helper.isInventory(client.world, hit)) {
            return;
        }

        ItemStack item = client.player.getMainHandStack();
        int split = (int) Math.floor((double) item.getCount() / this.inventoryBlocks.size());

        Camera camera = client.gameRenderer.getCamera();
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        VertexConsumerProvider.Immediate vc1 = client.getBufferBuilders().getEffectVertexConsumers();

        byte[] rgba = Helper.getBytes(ModConfig.HANDLER.instance().rgba_outline_color, true);
        int r, g, b, a;
        r = rgba[0] & 0xFF;
        g = rgba[1] & 0xFF;
        b = rgba[2] & 0xFF;
        a = rgba[3] & 0xFF;

        if (a != 0) {
            int al = Math.max(a, 30);
            int ah = Math.min(a + 50, 255);
            double p = (Math.sin(System.currentTimeMillis() / 250d) + 1) / 2;
            a = (int) (al + ((ah - al) * p));
        }

        for (BlockRec block : this.inventoryBlocks) {
            Vec3d targetPosition = block.getParticlePosition();
            Vec3d targetPosition1 = block.pos().toCenterPos();
            Vec3d transPosition = targetPosition.subtract(camera.getPos());
            Vec3d transPosition1 = targetPosition1.subtract(camera.getPos());

            RenderSystem.enableDepthTest();

            VertexConsumer v = vc1.getBuffer(RenderLayer.getTextBackgroundSeeThrough());
            matrices.push();
            matrices.translate(transPosition1.x, transPosition1.y, transPosition1.z);

            RenderHelper.renderCube(v, matrices.peek().getPositionMatrix(), 0.51f, 0.51f, 0.51f, r, g, b, a, 0xF000F0);
            matrices.pop();

            matrices.push();
            matrices.translate(transPosition.x, transPosition.y, transPosition.z);

            if (!item.isEmpty()) {
                float s = (float) Math.sin((double) System.currentTimeMillis() / 1000 + block.pos().hashCode()) * 0.1f;
                matrices.translate(0, s, 0);

                matrices.push();
                matrices.scale(0.25f, 0.25f, 0.25f);
                matrices.multiply(RenderHelper.getBillboard(camera, Vec2f.ZERO, RenderHelper.Billboard.VERTICAL));
                RenderHelper.renderItem(client, vc1, matrices, client.world, item, 0, 0, 0);
                matrices.pop();

                matrices.push();
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.multiply(camera.getRotation());
                RenderHelper.renderText(matrices, client.textRenderer, vc1, Text.literal(String.valueOf(split)), 0.5f, 0xffffffff);
                matrices.pop();
            }

            matrices.pop();
            vc1.draw();
            RenderSystem.disableDepthTest();
        }
    }

    private void onTick(MinecraftClient client) {
        KeybindManager.tick(client);
        if (this.waiting) {
            client.player.setSneaking(false);
        }
    }

    private void whileStorePressed(MinecraftClient client) {
        var hit = client.getCameraEntity().raycast(5, 0, false);
        this.showFarInventories(client.player);

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
        client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, client.world.random.nextFloat() + 0.5f);
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
            if (!blockRec.isTooFar(player)) continue;
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

    private void showFarInventories(PlayerEntity player) {
        for (BlockRec blockRec : this.inventoryBlocks) {
            if (!blockRec.isTooFar(player)) continue;
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
