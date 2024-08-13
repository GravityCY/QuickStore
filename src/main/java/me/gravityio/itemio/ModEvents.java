package me.gravityio.itemio;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.inventory.AbstractContainerMenu;

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

    /**
     * When a key has been pressed at the top level close to the GLFW callbacks.<br><br>
     *
     * returning true will cancel the minecraft key
     */
    public static Event<OnKey> ON_KEY = EventFactory.createArrayBacked(OnKey.class,
        (listeners) -> (key, scancode, action, modifiers) -> {
            boolean cancel = false;
            for (OnKey listener : listeners) {
                if (listener.onKey(key, scancode, action, modifiers))
                    cancel = true;
            }
            return cancel;
        }
    );

    public interface OnScreenFullyOpened {
        void onOpened(AbstractContainerMenu menu);
    }

    public interface OnKey {
        boolean onKey(int key, int scancode, int action, int modifiers);
    }

}
