package me.gravityio.quickstore.mixins.impl;

import me.gravityio.quickstore.ModEvents;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(method = "updateSlotStacks", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ModEvents.ON_SCREEN_FULLY_OPENED.invoker().onOpened((ScreenHandler) (Object) this);
    }
}
