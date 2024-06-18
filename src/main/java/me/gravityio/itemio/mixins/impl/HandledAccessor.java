package me.gravityio.itemio.mixins.impl;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledAccessor {
    @Invoker("isPointOverSlot")
    boolean itemio$isPointOverSlot(Slot slot, double pointX, double pointY);
}
