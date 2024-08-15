package me.gravityio.itemio.mixins.mod;

import me.gravityio.itemio.ModEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void itemio$onInit(CallbackInfo ci) {
        ModEvents.ON_SCREEN_FULLY_OPENED.invoker().onOpened((AbstractContainerMenu) (Object) this);
    }
}
