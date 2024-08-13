package me.gravityio.itemio.mixins.impl;

import me.gravityio.itemio.ModEvents;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class OnKeyMixin {
    @Inject(method = "keyPress", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWindow()J", shift = At.Shift.AFTER), cancellable = true)
    private void itemio$onKeyEvent(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (!ModEvents.ON_KEY.invoker().onKey(key, scancode, action, modifiers)) return;
        ci.cancel();
    }
}
