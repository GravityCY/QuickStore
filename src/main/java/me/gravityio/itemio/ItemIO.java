package me.gravityio.itemio;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.gravityio.itemio.helper.Helper;
import me.gravityio.itemio.helper.RenderHelper;
import me.gravityio.itemio.helper.ScreenHandlerHelper;
import me.gravityio.itemio.lib.keybind.KeybindManager;
import me.gravityio.itemio.lib.keybind.KeybindWrapper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ItemIO implements ClientModInitializer {
    public static final String MOD_ID = "itemio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SimpleParticleType STORE_PARTICLE = ParticleTypes.HAPPY_VILLAGER;
    private static final SimpleParticleType ADD_BLOCK_PARTICLE = ParticleTypes.GLOW;
    private static final SimpleParticleType REMOVE_BLOCK_PARTICLE = ParticleTypes.SMALL_FLAME;

    private static final String FAR_INVENTORY_KEY = "messages.itemio.far_inventory";
    private static final String TOGGLE_KEY = "messages.itemio.toggle";

    private static final KeybindWrapper STORE = Util.make(
            KeybindWrapper.of("key.itemio.store", GLFW.GLFW_KEY_V, "category.itemio.name"),
            bind -> bind.setWorkInScreen(true)
    );

    private static final int INCREMENT_MODIFIER_KEY = GLFW.GLFW_KEY_LEFT_SHIFT;
    private static final int RESTOCK_MODIFIER_KEY = GLFW.GLFW_KEY_LEFT_CONTROL;

    private static final int TIMEOUT = 2000;

    public static boolean IS_DEV;
    public static ItemIO INSTANCE;

    private final Set<BlockRec> inventoryBlocks = new HashSet<>();
    private ItemStack heldStack = ItemStack.EMPTY;
    private BlockRec currentInventoryBlock;
    private Iterator<BlockRec> inventoryBlockIterator;
    private Iterator<Integer> splitSlotIndexIterator;

    private int slotIndex;
    private int splitCount;

    public boolean waiting;
    private boolean fromScreen;
    private boolean anyInvalid;
    private boolean isStoreDown;

    private long startWaiting;

    public static ResourceLocation getId(String path) {
        //? if >=1.21 {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?} else {
        /*return new ResourceLocation(MOD_ID, path);
        *///?}
    }

    /**
     * Whether is in a screen that should run inventory operations
     */
    public static boolean isValidScreen(Minecraft client) {
        return client.screen instanceof AbstractContainerScreen<?> && client.player.containerMenu == client.player.inventoryMenu;
    }

    public static boolean isKeyPressed(int keycode) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keycode);
    }

    // TODO: Compatibility: IRIS Mod Compatibility shaders

    public static void DEBUG(String message, Object... args) {
        if (!IS_DEV) {
            return;
        }

        LOGGER.info(message, args);
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
        KeybindManager.init();

        ModConfig.HANDLER.load();
        ModConfig.INSTANCE = ModConfig.HANDLER.instance();

        var client = Minecraft.getInstance();
        ClientTickEvents.START_WORLD_TICK.register(w -> {
            KeybindManager.tick(client);
        });
        ClientTickEvents.END_WORLD_TICK.register(w -> {
            if (!ModConfig.INSTANCE.enable_mod) return;
            this.onTick(client);
        });
        ModEvents.ON_SCREEN_FULLY_OPENED.register(handler -> {
            if (!ModConfig.INSTANCE.enable_mod || handler == client.player.inventoryMenu) return;
            this.onScreenFullyOpened(client, handler);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client1) -> {
            this.clear();
        });

        STORE.onPressed(() -> {
            if (!ModConfig.INSTANCE.enable_mod) return;

            if (client.screen != null) {
                if (ModConfig.INSTANCE.inventory_operations && !this.isStoreDown && isValidScreen(client)) {
                    this.tickItemScreenIO(client);
                    return;
                }
                return;
            }

            if (ModConfig.INSTANCE.toggle_bind) {
                this.toggleStoreDown(client);
            } else {
                this.isStoreDown = true;
            }
        });

        STORE.onRelease(() -> {
            if (!ModConfig.INSTANCE.enable_mod) return;
            if (ModConfig.INSTANCE.toggle_bind) return;

            this.isStoreDown = false;
            this.onReleaseIO(client);
        });

        ModEvents.ON_KEY.register((key, scancode, action, modifiers) -> {
            if (!ModConfig.INSTANCE.enable_mod) return false;
            if (client.player == null || !this.isStoreDown || key != GLFW.GLFW_KEY_ESCAPE) return false;

            client.player.displayClientMessage(Component.translatable("messages.itemio.cancel").withStyle(ChatFormatting.RED), true);
            this.isStoreDown = false;
            this.clear();
            return true;
        });
        WorldRenderEvents.END.register((a) -> this.onRender(client));
    }

    private void toggleStoreDown(Minecraft client) {
        if (!this.isStoreDown) {
            client.player.displayClientMessage(Component.translatable(TOGGLE_KEY), true);
        }
        this.isStoreDown = !this.isStoreDown;
        if (!this.isStoreDown) {
            this.onReleaseIO(client);
        }
    }

    private void onRender(Minecraft client) {
        if (!ModConfig.INSTANCE.enable_mod) return;
        if (this.inventoryBlocks.isEmpty()) return;
        if (client.player == null) return;
        if (this.waiting) return;

        if (ModConfig.INSTANCE.need_look_at_container) {
            if (Helper.getLookingAtInventory(client) == null) return;
        }

        ItemStack item;
        if (this.fromScreen) {
            item = client.player.getInventory().items.get(this.slotIndex);
        } else {
            item = client.player.getMainHandItem();
        }
        int split = (int) Math.floor((double) item.getCount() / this.inventoryBlocks.size());

        Camera camera = client.gameRenderer.getMainCamera();
        PoseStack matrices = new PoseStack();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        MultiBufferSource.BufferSource vc = client.renderBuffers().bufferSource();

        byte[] rgba = Helper.getBytes(ModConfig.INSTANCE.rgba_outline_color, 4, true);
        int r, g, b, a;
        r = rgba[0] & 0xFF;
        g = rgba[1] & 0xFF;
        b = rgba[2] & 0xFF;
        a = rgba[3] & 0xFF;

        if (ModConfig.INSTANCE.animate_opacity && a != 0) {
            int al = Math.max(a, 30);
            int ah = Math.min(a + 50, 255);
            double p = (Math.sin(System.currentTimeMillis() / 250d) + 1) / 2;
            a = (int) Mth.lerp(p, al, ah);
        }

        for (BlockRec block : this.inventoryBlocks) {
            int tempG = block.isTooFar(client.player) ? 0 : g;
            int tempB = block.isTooFar(client.player) ? 0 : b;

            Vec3 targetPosition = block.getParticlePosition();
            Vec3 targetPosition1 = block.pos().getCenter();
            Vec3 textPosition = targetPosition.subtract(camera.getPosition());
            Vec3 cubePosition = targetPosition1.subtract(camera.getPosition());

            VertexConsumer v = vc.getBuffer(RenderType.textBackgroundSeeThrough());
            matrices.pushPose();
            matrices.translate(cubePosition.x, cubePosition.y, cubePosition.z);
            RenderHelper.renderCube(v, matrices.last().pose(), 0.51f, 0.51f, 0.51f, r, tempG, tempB, a, 0xF000F0);
            matrices.popPose();

            matrices.pushPose();
            matrices.translate(textPosition.x, textPosition.y, textPosition.z);

            if (!item.isEmpty()) {

                if (ModConfig.INSTANCE.animate_item) {
                    float s = (float) Math.sin((double) System.currentTimeMillis() / 1000 + block.pos().hashCode()) * 0.1f;
                    matrices.translate(0, s, 0);
                }

                matrices.pushPose();
                matrices.mulPose(RenderHelper.getBillboard(camera, Vec2.ZERO, RenderHelper.Billboard.VERTICAL));
                matrices.scale(0.25f, 0.25f, 0.25f);
                RenderHelper.renderItem(client, vc, matrices, client.level, item, 0, 0, 0);
                matrices.popPose();

                matrices.pushPose();
                matrices.mulPose(camera.rotation());
                //? if >=1.21 {
                matrices.mulPose(Axis.YP.rotationDegrees(180));
                //?}
                matrices.scale(0.5f, 0.5f, 0.5f);
                RenderHelper.renderText(matrices, client.font, vc, Component.literal(String.valueOf(split)), 0.5f, 0xffffffff);
                matrices.popPose();
            }
            matrices.popPose();
        }

        RenderSystem.enableDepthTest();
        vc.endBatch();
        RenderSystem.disableDepthTest();

    }

    private void onTick(Minecraft client) {
        if (this.isStoreDown) {
            this.tickItemIO(client);
        }
        if (this.waiting) {
            this.tickWaiting();
            if (client.player.isShiftKeyDown()) {
                client.player.setShiftKeyDown(false);
            }
        }
    }

    private void tickWaiting() {
        if ((System.currentTimeMillis() - this.startWaiting) > TIMEOUT) {
            this.clear();
        }
    }

    private void tickItemScreenIO(Minecraft client) {
        var data = Helper.getHoverStack(client);
        if (data == null) return;
        if (data.slotIndex() >= client.player.getInventory().items.size()) return;
        if (data.slot().container != client.player.getInventory()) return;

        DEBUG("Ticking ItemScreenIO, slot {}", data.slotIndex());
        if (ModConfig.INSTANCE.toggle_bind) {
            this.toggleStoreDown(client);
        } else {
            this.isStoreDown = true;
        }
        this.fromScreen = true;
        this.slotIndex = data.slotIndex();
        client.player.closeContainer();
    }

    private void tickItemIO(Minecraft client) {
        var hit = Helper.getLookingAtInventory(client);

        if (this.getInvalidBlocks(client.level, client.player).isEmpty()) {
            this.anyInvalid = false;
        } else {
            if (!this.anyInvalid) {
                client.player.displayClientMessage(Component.translatable(FAR_INVENTORY_KEY), true);
                this.anyInvalid = true;
            }
        }

        if (this.waiting || hit == null) {
            return;
        }

        var blockRec = BlockRec.of(client.level, client.player, hit.getBlockPos(), hit.getDirection());
        if (this.inventoryBlocks.contains(blockRec)) return;
        DEBUG("Adding '{}'", hit.getBlockPos().toShortString());
        var pos = blockRec.getParticlePosition();
        client.level.addParticle(ADD_BLOCK_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        this.inventoryBlocks.add(blockRec);
        client.player.playSound(SoundEvents.ITEM_PICKUP, 1, client.level.random.nextFloat() + 0.5f);
    }

    private void onReleaseIO(Minecraft client) {
        if (this.waiting || this.inventoryBlocks.isEmpty()) {
            return;
        }

        if (ModConfig.INSTANCE.need_look_at_container) {
            BlockHitResult hit = Helper.getLookingAtInventory(client);
            if (hit == null) {
                this.clear();
                return;
            }
        }

        this.startWaiting = System.currentTimeMillis();
        this.removeInvalid(client.level, client.player);
        if (!this.fromScreen) {
            this.heldStack = client.player.getMainHandItem().copy();
            this.slotIndex = client.player.getInventory().selected;
        } else {
            this.heldStack = client.player.getInventory().items.get(this.slotIndex).copy();
        }
        this.inventoryBlockIterator = this.inventoryBlocks.iterator();
        this.waiting = true;
        this.splitCount = (int) Math.floor((double) this.heldStack.getCount() / this.inventoryBlocks.size());

        client.getConnection().send(new ServerboundPlayerCommandPacket(client.player, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        DEBUG("Item: '{}', Count: '{}', Inventories: '{}', Split: '{}'", this.heldStack, this.heldStack.getCount(), this.inventoryBlocks.size(), this.splitCount);

        if (this.nextInventoryBlock()) {
            this.sendOpenScreenPacket(client, this.currentInventoryBlock);
        } else {
            this.clear();
        }
    }

    private void clear() {
        this.heldStack = null;
        this.currentInventoryBlock = null;
        this.inventoryBlocks.clear();
        this.inventoryBlockIterator = null;
        this.splitSlotIndexIterator = null;

        this.fromScreen = false;
        this.waiting = false;
    }

    private boolean nextInventoryBlock() {
        if (!this.inventoryBlockIterator.hasNext()) return false;
        this.currentInventoryBlock = this.inventoryBlockIterator.next();
        return true;
    }

    private void sendOpenScreenPacket(Minecraft client, BlockRec rec) {
        if (client.getConnection() == null) return;
        DEBUG("Sending open screen packet for '{}'", rec.pos().toShortString());
        client.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, rec.toBlockHitResult(), 6969));
    }

    private void onScreenFullyOpened(Minecraft client, AbstractContainerMenu handler) {
        if (!this.waiting) return;

        if (this.splitSlotIndexIterator == null && !this.heldStack.isEmpty()) {
            int slotId = ScreenHandlerHelper.findIndexSlotID(this.slotIndex, client.player.containerMenu, ScreenHandlerHelper.InventoryType.PLAYER);
            Slot[] slots = ScreenHandlerHelper.splitStackQuickCraft(client.gameMode, client.player, slotId, splitCount);
            if (slots != null) {
                StringBuilder sb = new StringBuilder(20);
                for (Slot slot : slots) {
                    sb.append(slot.getContainerSlot()).append(", ");
                }
                DEBUG("Splitting stacks: {}", sb.toString());
                this.splitSlotIndexIterator = Arrays.stream(slots).mapToInt(Slot::getContainerSlot).iterator();
            }
        }

        DEBUG("Screen {} fully opened.", handler);
        var slotId = ScreenHandlerHelper.findIndexSlotID(this.slotIndex, handler, ScreenHandlerHelper.InventoryType.PLAYER);
        var outputSlotId = ScreenHandlerHelper.getFullOutputSlotID(handler, ScreenHandlerHelper.InventoryType.OTHER);
        if (outputSlotId != -1 && (this.heldStack.isEmpty() || Helper.isExactlyTheSame(handler.getSlot(outputSlotId).getItem(), this.heldStack))) {
            DEBUG("Moving items found in the output slot into our selected slot.");
            ScreenHandlerHelper.moveToOrShift(client, outputSlotId, slotId);
        } else {
            if (this.heldStack.isEmpty()) {
                DEBUG("Moving first found stack to our selected slot.");
                int nonEmptySlotID = ScreenHandlerHelper.getNonEmptySlotID(handler, ScreenHandlerHelper.InventoryType.OTHER, false);
                if (nonEmptySlotID != -1) {
                    ScreenHandlerHelper.moveToOrShift(client, nonEmptySlotID, slotId);
                }
            } else {
                if (this.splitSlotIndexIterator != null) {
                    int splitSlotIndex = this.splitSlotIndexIterator.next();
                    var splitSlotId = ScreenHandlerHelper.findIndexSlotID(splitSlotIndex, handler, ScreenHandlerHelper.InventoryType.PLAYER);
                    DEBUG("Quick moving stack we split before at slot index {} and slot id {}.", splitSlotIndex, splitSlotId);
                    Helper.shiftClickSlot(client.gameMode, client.player, splitSlotId);
                } else {
                    DEBUG("Quick moving stack by splitting iteratively.");
                    ScreenHandlerHelper.splitStackShift(client.gameMode, client.player, slotId, this.splitCount);
                }
            }
        }

        client.player.closeContainer();

        var pos = this.currentInventoryBlock.getParticlePosition();
        client.level.addParticle(STORE_PARTICLE, pos.x, pos.y, pos.z, 0, 0, 0);
        if (!this.nextInventoryBlock()) {
            DEBUG("All screens have been processed.");
            if (!ItemStack.matches(this.heldStack, client.player.getMainHandItem())) {
                client.player.playSound(SoundEvents.ITEM_PICKUP, 1, 1);
            }

            if (this.doScroll()) {
                DEBUG("Scrolling hotbar");
                client.player.getInventory().swapPaint(-1);
            }

            if (this.doRestock() && client.player.getMainHandItem().isEmpty()) {
                int foundSlotId = ScreenHandlerHelper.findSlotID(this.heldStack, client.player.containerMenu, ScreenHandlerHelper.InventoryType.PLAYER, Helper::isExactlyTheSame);
                DEBUG("Restocking item {} found at {}", this.heldStack, foundSlotId);
                if (foundSlotId != -1) {
                    Helper.swapSlot(client.gameMode, client.player, foundSlotId, this.slotIndex);
                }
            }
            this.clear();
        } else {
            this.sendOpenScreenPacket(client, this.currentInventoryBlock);
        }
    }

    /**
     * Returns a list of blocks that are 'invalid', meaning they are too far away from the player.
     */
    private List<BlockRec> getInvalidBlocks(Level world, Player player) {
        List<BlockRec> invalid = new ArrayList<>();
        for (BlockRec blockRec : this.inventoryBlocks) {
            BlockPos pos = blockRec.pos();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!blockRec.isTooFar(player) && blockEntity instanceof Container) continue;
            invalid.add(blockRec);
        }
        return invalid;
    }

    /**
     * Removes all blocks that are too far away from the player.
     */
    private void removeInvalid(Level world, Player player) {
        var hadSomeTooFar = false;
        for (BlockRec blockRec : this.getInvalidBlocks(world, player)) {
            this.inventoryBlocks.remove(blockRec);
            Vec3 particlePos = blockRec.getParticlePosition();
            player.level().addParticle(REMOVE_BLOCK_PARTICLE, particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
            DEBUG("Removing '{}'", blockRec.pos().toShortString());
            hadSomeTooFar = true;
        }
        if (hadSomeTooFar) {
            player.displayClientMessage(Component.translatable(FAR_INVENTORY_KEY), true);
        }
    }

    private boolean doRestock() {
        return isKeyPressed(RESTOCK_MODIFIER_KEY);
    }

    private boolean doScroll() {
        return isKeyPressed(INCREMENT_MODIFIER_KEY);
    }

}
