package me.gravityio.itemio.lib.keybind;

import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.Nullable;

/**
 * A Keybind wrapper responsible for wrapping the Original Keybind class, and providing
 * some useful callbacks etc...
 */
public class KeybindWrapper {
    public final KeyMapping bind;
    private Runnable whileRunnable;
    private Runnable pressedRunnable;
    private Runnable releasedRunnable;
    protected boolean workInScreen = false;
    protected boolean down;

    public KeybindWrapper(KeyMapping bind) {
        this(bind, null, null);
    }

    public KeybindWrapper(KeyMapping bind, Runnable whileRunnable) {
        this(bind, whileRunnable, null);
    }

    public KeybindWrapper(KeyMapping bind, Runnable whilePressed, Runnable pressedVisitor) {
        this(bind, whilePressed, pressedVisitor, null);
    }

    public KeybindWrapper(KeyMapping bind, @Nullable Runnable whileRunnable, @Nullable Runnable pressedRunnable, @Nullable Runnable releasedRunnable) {
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
        return this.bind.consumeClick();
    }

    public static KeybindWrapper of(String translationKey, int code, String category) {
        return of(translationKey, code, category, null, null);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable onPressed) {
        return of(translationKey, code, category, onPressed, null);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable onPressed, Runnable isPressed) {
        return new KeybindWrapper(new KeyMapping(translationKey, code, category), onPressed, isPressed);
    }

    public static KeybindWrapper of(String translationKey, int code, String category, Runnable whilePressed, Runnable onPressed, Runnable onRelease) {
        return new KeybindWrapper(new KeyMapping(translationKey, code, category), whilePressed, onPressed, onRelease);
    }

}
