package me.gravityio.itemio;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemIO implements ClientModInitializer {
    public static final String MOD_ID = "quickstore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final KeyBinding STORE = new KeyBinding("key.quickstore.store", GLFW.GLFW_KEY_V, "category.quickstore.name");
    private static final KeyBinding INCREMENT = new KeyBinding("key.quickstore.increment", GLFW.GLFW_KEY_LEFT_SHIFT, "category.quickstore.name");
    private static final KeyBinding RESTOCK = new KeyBinding("key.quickstore.restock", GLFW.GLFW_KEY_LEFT_ALT, "category.quickstore.name");
    private static final KeyBinding DEBUG = new KeyBinding("key.quickstore.debug", GLFW.GLFW_KEY_O, "category.quickstore.name");
    public static boolean IS_DEBUG;
    public static ItemIO INSTANCE;
    public boolean waiting = false;
    public int slot = -1;
    public ItemStack stack = ItemStack.EMPTY;
    public Box interactBox = null;
    public long startTime = -1;
    private boolean canSneak = true;
    private boolean increment = false;
    private boolean restock = false;
    private Vec3d blockPos;


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

        KeyBindingHelper.registerKeyBinding(STORE);
        KeyBindingHelper.registerKeyBinding(INCREMENT);
        KeyBindingHelper.registerKeyBinding(RESTOCK);
        KeyBindingHelper.registerKeyBinding(DEBUG);

        var client = MinecraftClient.getInstance();

        ClientTickEvents.END_WORLD_TICK.register(w -> this.onTick(client));
        ModEvents.ON_PLAY_SOUND.register((pos, sound, category, volume, pitch) -> this.onPlaySoundShouldYou(client, pos, sound, category, volume, pitch));
        ModEvents.ON_SCREEN_FULLY_OPENED.register(handler -> this.onScreenFullyOpened(client, handler));
    }

    private void onPressQuickStore(MinecraftClient client) {
        var type = client.crosshairTarget.getType();
        if (type == HitResult.Type.MISS
                || this.waiting) return;

        if (type == HitResult.Type.BLOCK) {
            var blockHit = (BlockHitResult) client.crosshairTarget;
            var blockPos = blockHit.getBlockPos();
            var blockEntity = client.world.getBlockEntity(blockPos);
            if (!(blockEntity instanceof Inventory)) return;
            this.interactBox = Box.of(blockPos.toCenterPos(), 3, 3, 3);
            this.blockPos = blockHit.getBlockPos().toCenterPos().offset(blockHit.getSide().getOpposite(), -0.75f);
            this.stack = client.player.getMainHandStack().copy();
            this.startTime = System.currentTimeMillis();
            this.waiting = true;
            this.canSneak = true;
            this.increment = InputUtil.isKeyPressed(client.getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(INCREMENT).getCode());
            this.restock = InputUtil.isKeyPressed(client.getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(RESTOCK).getCode());
            for (ItemStack handItem : client.player.getHandItems()) {
                if (handItem.isEmpty()) continue;
                this.canSneak = false;
                break;
            }
            this.slot = client.player.getInventory().selectedSlot;
            if (!this.canSneak) {
                client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            client.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, blockHit, 6969));
            DEBUG("Opening Screen and Selecting Slot {}", client.player.getInventory().selectedSlot);
        }

    }

    private void onTick(MinecraftClient client) {
        if (!this.canSneak && this.waiting) {
            client.player.setSneaking(false);
        }
        while (STORE.wasPressed()) this.onPressQuickStore(client);
    }

    private boolean onPlaySoundShouldYou(MinecraftClient client, Vec3d pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (this.startTime == -1
                || System.currentTimeMillis() - this.startTime > 50
                || (this.interactBox != null && !this.interactBox.contains(pos)
                && !client.player.getPos().equals(pos))) return true;
        DEBUG("Preventing Sound Event '{}' from playing", sound.getId());
        return false;
    }

    private void onScreenFullyOpened(MinecraftClient client, ScreenHandler handler) {
        if (!this.waiting) return;

        if (this.stack.isEmpty()) {
            int fromSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
            if (fromSlotId != -1) {
                DEBUG("Swapping with found Output Slot, ID: {}", fromSlotId);
            } else {
                fromSlotId = ScreenHandlerHelper.getNonEmptySlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
                if (fromSlotId != -1) {
                    DEBUG("Swapping with found non empty slot, ID: {}", fromSlotId);
                }
            }

            if (fromSlotId != -1) {
                int toSlotIndex = client.player.getInventory().selectedSlot;
                Helper.swapSlot(client.interactionManager, client.player, handler.syncId, fromSlotId, toSlotIndex);
            }

        } else {
            int clickSlotId = ScreenHandlerHelper.getOutputSlotID(handler, ScreenHandlerHelper.InventoryType.TOP);
            if (clickSlotId != -1) {
                if (!ItemStack.canCombine(handler.slots.get(clickSlotId).getStack(), this.stack))
                    clickSlotId = -1;
            }

            if (clickSlotId != -1) {
                DEBUG("Quick Moving from found Output Slot, ID: {}", clickSlotId);
            } else {
                clickSlotId = ScreenHandlerHelper.toHandlerID(handler, this.slot, ScreenHandlerHelper.InventoryType.BOTTOM);
                DEBUG("Quick Moving from found Selected Slot, ID: {}", clickSlotId);
            }

            Helper.shiftClickSlot(client.interactionManager, client.player, handler.syncId, clickSlotId);
        }

        client.player.closeHandledScreen();
        if (!ItemStack.areEqual(this.stack, client.player.getMainHandStack())) {
            client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 1);
            client.world.addParticle(ParticleTypes.GLOW, this.blockPos.x, this.blockPos.y, this.blockPos.z, 0, 0, 0);

            if (this.increment) {
                DEBUG("Incrementing hotbar slot");
                client.player.getInventory().scrollInHotbar(-1);
            }

            if (this.restock) {
                DEBUG("Restocking selected slot");
                int foundId = ScreenHandlerHelper.findSlotID(this.stack, client.player.playerScreenHandler, ScreenHandlerHelper.InventoryType.BOTTOM, ItemStack::canCombine);
                DEBUG("Found item ID: {}", foundId);
                if (foundId != -1) {
                    int outputId = ScreenHandlerHelper.findSlotID(this.slot, client.player.playerScreenHandler, client.player.getInventory());
                    ScreenHandlerHelper.splitStack(client.player.playerScreenHandler, client.interactionManager, client.player, foundId, outputId, this.stack.getCount());
                }
            }

        }

        this.waiting = false;
        this.slot = -1;
        this.stack = null;
    }
}
