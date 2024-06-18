package me.gravityio.itemio.mixins.impl;

import me.gravityio.itemio.ModEvents;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {

    @Inject(method = "updateSlotStacks", at = @At("TAIL"))
    private void itemio$onInit(CallbackInfo ci) {
        ModEvents.ON_SCREEN_FULLY_OPENED.invoker().onOpened((ScreenHandler) (Object) this);
    }
}
