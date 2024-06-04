package me.gravityio.itemio.mixins.impl;

import me.gravityio.itemio.ModEvents;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class OnKeyMixin {
    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getHandle()J", shift = At.Shift.AFTER), cancellable = true)
    private void itemio$onKeyEvent(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (!ModEvents.ON_KEY.invoker().onKey(key, scancode, action, modifiers)) return;
        ci.cancel();
    }
}
