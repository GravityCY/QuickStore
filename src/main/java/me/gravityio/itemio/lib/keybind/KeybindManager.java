package me.gravityio.itemio.lib.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
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

    public static void tick(Minecraft client) {
        var handle = client.getWindow().getWindow();
        for (KeybindWrapper bind : binds) {
            if (!bind.workInScreen && client.screen != null) continue;

            boolean prevDown = bind.down;
            bind.down = InputConstants.isKeyDown(handle, KeyBindingHelper.getBoundKeyOf(bind.bind).getValue());
            if (bind.down)
                bind.internalWhilePressed();
            if (bind.down && !prevDown)
                bind.internalOnPressed();

            if (prevDown && !bind.down)
                bind.internalOnRelease();
        }
    }
}
