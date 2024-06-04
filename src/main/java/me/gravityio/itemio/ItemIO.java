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
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
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

    private static final KeybindWrapper STORE = KeybindWrapper.of("key.itemio.store", GLFW.GLFW_KEY_V, "category.itemio.name");
    private static final KeybindWrapper INCREMENT = KeybindWrapper.of("key.itemio.increment", GLFW.GLFW_KEY_LEFT_SHIFT, "category.itemio.name");

    public static boolean IS_DEBUG;

    public static ItemIO INSTANCE;

    public ItemStack heldStack = ItemStack.EMPTY;
    public BlockRec currentInventoryBlock;
    public Set<BlockRec> inventoryBlocks = new HashSet<>();
    public Iterator<BlockRec> inventoryBlockIterator;
    public boolean waiting;
    public boolean increment;
    public int slotIndex;
    private int splitCount;
    private Iterator<Integer> splitSlotIndexArray;

    // TODO: We render on the side that was looked at as the bind was down, what this means is that with chests
    //  placed next to each other, and you hover from chest a to chest b, you can click their sides on the way there, which renders the item inside a chest.
    //  Fix: Detect when the block on a side is not air (or 1x1x1) and fallback to the opposite of the facing direction of the player?

    // TODO: Add an option to restock the item you just put in an inventory?

    public static void DEBUG(String message, Object... args) {
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

        ModConfig.HANDLER.load();

        var client = MinecraftClient.getInstance();
        ClientTickEvents.END_WORLD_TICK.register(w -> this.onTick(client));
        ModEvents.ON_SCREEN_FULLY_OPENED.register(handler -> this.onScreenFullyOpened(client, handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> this.clear());

        STORE.whilePressed(() -> this.whileStorePressed(client));
        STORE.onRelease(() -> this.onReleaseStore(client));

        ModEvents.ON_KEY.register((key, scancode, action, modifiers) -> {
            if (this.inventoryBlocks.isEmpty() || key != GLFW.GLFW_KEY_ESCAPE) return false;
            this.clear();
            return true;
        });

        WorldRenderEvents.END.register((a) -> this.onRender(client));
    }

    private void onRender(MinecraftClient client) {
        if (this.inventoryBlocks.isEmpty()) return;
        if (client.player == null) return;
        if (this.waiting) return;

        if (ModConfig.HANDLER.instance().need_look_at_container) {
            var hit = client.getCameraEntity().raycast(5, 0, false);
            if (!Helper.isInventory(client.world, hit)) return;
        }

        ItemStack item = client.player.getMainHandStack();
        int split = (int) Math.floor((double) item.getCount() / this.inventoryBlocks.size());

        Camera camera = client.gameRenderer.getCamera();
        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        VertexConsumerProvider.Immediate vc = client.getBufferBuilders().getEffectVertexConsumers();

        byte[] rgba = Helper.getBytes(ModConfig.HANDLER.instance().rgba_outline_color, true);
        int r, g, b, a;
        r = rgba[0] & 0xFF;
        g = rgba[1] & 0xFF;
        b = rgba[2] & 0xFF;
        a = rgba[3] & 0xFF;

        if (ModConfig.HANDLER.instance().animate_opacity && a != 0) {
            int al = Math.max(a, 30);
            int ah = Math.min(a + 50, 255);
            double p = (Math.sin(System.currentTimeMillis() / 250d) + 1) / 2;
            a = (int) (al + ((ah - al) * p));
        }

        for (BlockRec block : this.inventoryBlocks) {
            int tempG = block.isTooFar(client.player) ? 0 : g;
            int tempB = block.isTooFar(client.player) ? 0 : b;

            Vec3d targetPosition = block.getParticlePosition();
            Vec3d targetPosition1 = block.pos().toCenterPos();
            Vec3d transPosition = targetPosition.subtract(camera.getPos());
            Vec3d transPosition1 = targetPosition1.subtract(camera.getPos());

            RenderSystem.enableDepthTest();

            VertexConsumer v = vc.getBuffer(RenderLayer.getTextBackgroundSeeThrough());
            matrices.push();
            matrices.translate(transPosition1.x, transPosition1.y, transPosition1.z);
            RenderHelper.renderCube(v, matrices.peek().getPositionMatrix(), 0.51f, 0.51f, 0.51f, r, tempG, tempB, a, 0xF000F0);
            matrices.pop();

            matrices.push();
            matrices.translate(transPosition.x, transPosition.y, transPosition.z);

            if (!item.isEmpty()) {

                if (ModConfig.HANDLER.instance().animate_item) {
                    float s = (float) Math.sin((double) System.currentTimeMillis() / 1000 + block.pos().hashCode()) * 0.1f;
                    matrices.translate(0, s, 0);
                }

                matrices.push();
                matrices.multiply(RenderHelper.getBillboard(camera, Vec2f.ZERO, RenderHelper.Billboard.VERTICAL));
                matrices.scale(0.25f, 0.25f, 0.25f);
                RenderHelper.renderItem(client, vc, matrices, client.world, item, 0, 0, 0);
                matrices.pop();

                matrices.push();
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.multiply(camera.getRotation());
                RenderHelper.renderText(matrices, client.textRenderer, vc, Text.literal(String.valueOf(split)), 0.5f, 0xffffffff);
                matrices.pop();
            }

            matrices.pop();
            vc.draw();
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
        var hit = client.getCameraEntity().raycast(5, client.getTickDelta(), false);

        BlockPos blockPos = Helper.getInventory(client.world, hit);
        if (this.waiting || blockPos == null) {
            return;
        }

        var blockHit = (BlockHitResult) hit;
        var blockRec = new BlockRec(blockPos, blockHit.getSide());
        if (this.inventoryBlocks.contains(blockRec)) return;
        DEBUG("Adding '{}'", blockPos.toShortString());
        var pos = blockRec.getParticlePosition();
        client.world.addParticle(ADD_BLOCK_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        this.inventoryBlocks.add(blockRec);
        client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, client.world.random.nextFloat() + 0.5f);
    }

    private void onReleaseStore(MinecraftClient client) {
        if (this.waiting) {
            return;
        }

        if (ModConfig.HANDLER.instance().need_look_at_container) {
            var hit = client.getCameraEntity().raycast(5, 0, false);
            if (!Helper.isInventory(client.world, hit)) {
                this.clear();
                return;
            }
        }

        this.removeFarOnes(client.player);
        this.heldStack = client.player.getMainHandStack().copy();
        this.inventoryBlockIterator = this.inventoryBlocks.iterator();
        this.waiting = true;
        this.slotIndex = client.player.getInventory().selectedSlot;
        this.splitCount = (int) Math.floor((double) this.heldStack.getCount() / this.inventoryBlocks.size());
        this.increment = INCREMENT.isPressed();

        client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        DEBUG("Item: '{}', Count: '{}', Inventories: '{}', Split: '{}'", this.heldStack, this.heldStack.getCount(), this.inventoryBlocks.size(), this.splitCount);

        if (!this.heldStack.isEmpty() && !this.inventoryBlocks.isEmpty()) {
            int slotId = ScreenHandlerHelper.findIndexSlotID(this.slotIndex, client.player.currentScreenHandler, ScreenHandlerHelper.InventoryType.PLAYER);
            Slot[] slots = ScreenHandlerHelper.splitStackQuickCraft(client.interactionManager, client.player, slotId, splitCount);
            if (slots != null) {
                this.splitSlotIndexArray = Arrays.stream(slots).mapToInt(Slot::getIndex).iterator();
            }
        }

        if (this.nextInventoryBlock()) {
            this.sendOpenScreenPacket(client, this.currentInventoryBlock);
        } else {
            this.clear();
        }
    }

    private void clear() {
        this.waiting = false;
        this.increment = false;
        this.inventoryBlocks.clear();
        this.inventoryBlockIterator = null;
        this.currentInventoryBlock = null;
        this.heldStack = null;
        this.splitSlotIndexArray = null;
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

        var slotId = ScreenHandlerHelper.findIndexSlotID(this.slotIndex, handler, ScreenHandlerHelper.InventoryType.PLAYER);
        var outputSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.OTHER);
        if (outputSlotId != -1 && (this.heldStack.isEmpty() || ItemStack.canCombine(handler.getSlot(outputSlotId).getStack(), this.heldStack))) {
            ScreenHandlerHelper.moveToOrShift(client, outputSlotId, slotId);
        } else {
            if (this.heldStack.isEmpty()) {
                int nonEmptySlotID = ScreenHandlerHelper.getNonEmptySlotID(handler, ScreenHandlerHelper.InventoryType.OTHER);
                if (nonEmptySlotID != -1) {
                    ScreenHandlerHelper.moveToOrShift(client, nonEmptySlotID, slotId);
                }
            } else {
                if (this.splitSlotIndexArray != null) {
                    int splitSlotIndex = this.splitSlotIndexArray.next();
                    var splitSlotId = ScreenHandlerHelper.findIndexSlotID(splitSlotIndex, handler, ScreenHandlerHelper.InventoryType.PLAYER);
                    Helper.shiftClickSlot(client.interactionManager, client.player, splitSlotId);
                } else {
                    ScreenHandlerHelper.splitStackShift(client.interactionManager, client.player, slotId, this.splitCount);
                }
            }
        }

        client.player.closeHandledScreen();

        var pos = this.currentInventoryBlock.getParticlePosition();
        client.world.addParticle(STORE_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        if (!this.nextInventoryBlock()) {
            if (!ItemStack.areEqual(this.heldStack, client.player.getMainHandStack())) {
                client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 1);
            }
            if (INCREMENT.isPressed()) {
                client.player.getInventory().scrollInHotbar(-1);
            }
//            if (client.player.getMainHandStack().isEmpty()) {
//                int foundSlotId = ScreenHandlerHelper.findSlotID(this.heldStack, client.player.currentScreenHandler, ScreenHandlerHelper.InventoryType.PLAYER, ItemStack::canCombine);
//                DEBUG("Found slot of item {} at {}", this.heldStack, foundSlotId);
//                if (foundSlotId != -1) {
//                    Helper.swapSlot(client.interactionManager, client.player, foundSlotId, this.slotIndex);
//                }
//            }
            this.clear();
        } else {
            this.sendOpenScreenPacket(client, this.currentInventoryBlock);
        }
    }

}
