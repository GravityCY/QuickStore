package me.gravityio.itemio.lib.keybind;

import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.Nullable;

/**
 * A Keybind wrapper responsible for wrapping the Original Keybind class, and providing
 * some useful callbacks etc...
 */
public class KeybindWrapper {
    public final KeyBinding bind;
    private Runnable whileRunnable;
    private Runnable pressedRunnable;
    private Runnable releasedRunnable;
    protected boolean workInScreen = false;
    protected boolean down;

    public KeybindWrapper(KeyBinding bind) {
        this(bind, null, null);
    }

    public KeybindWrapper(KeyBinding bind, Runnable whileRunnable) {
        this(bind, whileRunnable, null);
    }

    public KeybindWrapper(KeyBinding bind, Runnable whilePressed, Runnable pressedVisitor) {
        this(bind, whilePressed, pressedVisitor, null);
    }

    public KeybindWrapper(KeyBinding bind, @Nullable Runnable whileRunnable, @Nullable Runnable pressedRunnable, @Nullable Runnable releasedRunnable) {
        this.bind = bind;
        this.whileRunnable = whileRunnable;
        this.pressedRunnable = pressedRunnable;
        this.releasedRunnable = releasedRunnable;

        KeybindManager.add(this);
    }

    public void whilePressed(Runnable visitor) {
        this.whileRunnable = visitor;
    }

    public void onPressed(Runnable runnable) {
        this.pressedRunnable = runnable;
    }

    public void onRelease(Runnable runnable) {
        this.releasedRunnable = runnable;
    }

    public boolean getWorkInScreen() {
        return workInScreen;
    }

    public void setWorkInScreen(boolean workInScreen) {
        this.workInScreen = workInScreen;
    }

    protected void internalWhilePressed() {
        if (this.whileRunnable == null) return;
        this.whileRunnable.run();
    }

    protected void internalOnPressed() {
        if (this.pressedRunnable == null) return;
        this.pressedRunnable.run();
    }

    protected void internalOnRelease() {
        if (this.releasedRunnable == null) return;
        this.releasedRunnable.run();
    }

    public boolean isPressed() {
        return this.down;
    }

    public boolean wasPressed() {
        return this.bind.wasPressed();
    }

    public static KeybindWrapper of(String translationKey, int code, String category) {
        return of(translationKey, code, category, null, null);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable onPressed) {
        return of(translationKey, code, category, onPressed, null);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable onPressed, Runnable isPressed) {
        return new KeybindWrapper(new KeyBinding(translationKey, code, category), onPressed, isPressed);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable whilePressed, Runnable onPressed, Runnable onRelease) {
        return new KeybindWrapper(new KeyBinding(translationKey, code, category), whilePressed, onPressed, onRelease);
    }

}
