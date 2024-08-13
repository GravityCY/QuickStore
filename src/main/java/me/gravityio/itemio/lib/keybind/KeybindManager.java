package me.gravityio.itemio.lib.keybind;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A Keybind manager<br><br>
 * <p>
 * Needs to be called every tick to update the keybinds
 */
public class KeybindManager {
    private static final List<KeybindWrapper> binds = new ArrayList<>();

    public static void add(@NotNull KeybindWrapper bind) {
        binds.add(bind);
    }

    public static void init() {
        for (KeybindWrapper bind : binds) {
            KeyBindingHelper.registerKeyBinding(bind.bind);
        }
    }

    public static void tick(MinecraftClient client) {
        var handle = client.getWindow().getHandle();
        for (KeybindWrapper bind : binds) {
            if (!bind.workInScreen && client.currentScreen != null) continue;

            boolean prevDown = bind.down;
            bind.down = InputUtil.isKeyPressed(handle, KeyBindingHelper.getBoundKeyOf(bind.bind).getCode());
            if (bind.down)
                bind.internalWhilePressed();
            if (bind.down && !prevDown)
                bind.internalOnPressed();

            if (prevDown && !bind.down)
                bind.internalOnRelease();
        }
    }
}
