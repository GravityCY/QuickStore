package me.gravityio.itemio.mixins.mod;

import me.gravityio.itemio.ItemIO;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class CancelSoundMixin {
    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"), cancellable = true)
    private void itemio$cancelPlaySound(Player player, Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float f, float g, long l, CallbackInfo ci) {
        if (!ItemIO.INSTANCE.waiting || soundSource != SoundSource.BLOCKS) return;
        ci.cancel();
    }
}
