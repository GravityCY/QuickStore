package me.gravityio.itemio.mixins.mod;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface HandledAccessor {
    @Invoker("isHovering")
    boolean itemio$isPointOverSlot(net.minecraft.world.inventory.Slot slot, double pointX, double pointY);
}
