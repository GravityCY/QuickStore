package me.gravityio.quickstore;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public class ModEvents {

    /**
     * When a screen has opened and been initialized with its contents
     */
    public static Event<OnScreenFullyOpened> ON_SCREEN_FULLY_OPENED = EventFactory.createArrayBacked(OnScreenFullyOpened.class,
            (listeners) -> screen -> {
                for (OnScreenFullyOpened listener : listeners) {
                    listener.onOpened(screen);
                }
            }
    );

    public static Event<OnPlaySound> ON_PLAY_SOUND = EventFactory.createArrayBacked(OnPlaySound.class,
            (listeners) -> (pos, sound, category, volume, pitch) -> {
                boolean isCancelled = false;
                for (OnPlaySound listener : listeners) {
                    if (listener.onPlaySound(pos, sound, category, volume, pitch)) continue;
                    isCancelled = true;
                }
                return !isCancelled;
            }
    );

    public interface OnScreenFullyOpened {
        void onOpened(ScreenHandler screen);
    }

    public interface OnPlaySound {
        boolean onPlaySound(Vec3d pos, SoundEvent sound, SoundCategory category, float volume, float pitch);
    }

}
