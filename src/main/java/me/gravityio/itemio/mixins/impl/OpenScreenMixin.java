package me.gravityio.itemio.mixins.impl;

import me.gravityio.itemio.ItemIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class OpenScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void itemio$cancelSetScreen(Screen screen, CallbackInfo ci) {
        if (!ItemIO.INSTANCE.waiting || !(screen instanceof AbstractContainerScreen<?>)) return;
        ci.cancel();
    }
}
