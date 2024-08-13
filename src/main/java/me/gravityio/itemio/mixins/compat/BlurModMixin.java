package me.gravityio.itemio.mixins.compat;

import me.gravityio.itemio.ItemIO;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class BlurModMixin {
    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void itemio$applyBlurToHandledScreen(float delta, CallbackInfo ci) {
        var screen = (Screen) (Object) this;
        if (!ItemIO.INSTANCE.waiting || !(screen instanceof HandledScreen<?>)) return;
        ci.cancel();
    }
}
