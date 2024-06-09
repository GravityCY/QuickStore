package me.gravityio.itemio.mixins.impl;

import me.gravityio.itemio.ItemIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class OpenScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void itemio$cancelSetScreen(Screen screen, CallbackInfo ci) {
        if (!ItemIO.INSTANCE.waiting || !(screen instanceof HandledScreen<?>)) return;
        ci.cancel();
    }

}
