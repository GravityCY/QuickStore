package me.gravityio.itemio;

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

    public interface OnScreenFullyOpened {
        void onOpened(ScreenHandler screen);
    }

}
