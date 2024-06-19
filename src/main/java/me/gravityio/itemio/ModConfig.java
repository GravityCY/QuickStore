package me.gravityio.itemio;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModConfig {
    public static ModConfig INSTANCE;
    public static final String TITLE = "yacl.itemio.title";

    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("itemio.json5");

    public static final ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler
            .createBuilder(ModConfig.class)
            .id(Identifier.of(ItemIO.MOD_ID, "config"))
            .serializer(serializer ->
                    GsonConfigSerializerBuilder.create(serializer)
                            .setPath(PATH)
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
            .build();

    private static <T> Option.Builder<T> getOption(String modid, String name, Function<Option<T>, ControllerBuilder<T>> controller, T def, Supplier<T> getter, Consumer<T> setter) {
        String labelKey = "yacl.%s.%s.label".formatted(modid, name);
        String descriptionKey = "yacl.%s.%s.desc".formatted(modid, name);

        return Option.<T>createBuilder()
                .name(Text.translatable(labelKey))
                .description(OptionDescription.of(Text.translatable(descriptionKey)))
                .controller(controller)
                .binding(def, getter, setter);
    }

    @SerialEntry(comment = "Whether to enable the mod")
    public boolean enable_mod = true;
    @SerialEntry(comment = "Whether to enable the functionality to start an operation in the inventory.")
    public boolean inventory_operations = true;
    @SerialEntry(comment = "Whether you need to look at a container to run an operation.")
    public boolean need_look_at_container = false;
    @SerialEntry(comment = "Whether to toggle the bind. Tip for Toggle Mode: You can cancel a selection with the `Escape` key")
    public boolean toggle_bind = false;
    @SerialEntry(comment = "Whether to animate item and text.")
    public boolean animate_item = true;
    @SerialEntry(comment = "Whether to animate the opacity of the selection.")
    public boolean animate_opacity = true;
    @SerialEntry(comment = "The color to overlay over blocks when storing or extracting from an container.")
    public int rgba_outline_color = 0xffffff40;

    public boolean getEnableMod() {
        return enable_mod;
    }

    public void setEnableMod(boolean enable_mod) {
        this.enable_mod = enable_mod;
    }

    public boolean getInventoryOperations() {
        return inventory_operations;
    }

    public void setInventoryOperations(boolean inventory_operations) {
        this.inventory_operations = inventory_operations;
    }

    public boolean getToggleBind() {
        return toggle_bind;
    }

    public void setToggleBind(boolean v) {
        this.toggle_bind = v;
    }

    public boolean getLookAtContainer() {
        return need_look_at_container;
    }

    public void setLookAtContainer(boolean needLookAtContainer) {
        this.need_look_at_container = needLookAtContainer;
    }

    public boolean getAnimateItem() {
        return animate_item;
    }

    public void setAnimateItem(boolean animateItem) {
        this.animate_item = animateItem;
    }

    public int getOutlineColor() {
        return rgba_outline_color;
    }

    public void setOutlineColor(int v) {
        this.rgba_outline_color = v;
    }

    public boolean getAnimateOpacity() {
        return this.animate_opacity;
    }

    public void setAnimateOpacity(boolean v) {
        this.animate_opacity = v;
    }

    public static Screen getScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> {
            builder.title(Text.translatable(TITLE));

            Function<Option<Boolean>, ControllerBuilder<Boolean>> onOffCont = opt -> BooleanControllerBuilder.create(opt).coloured(true).onOffFormatter();

            var enableModOpt = getOption(
                    ItemIO.MOD_ID, "enable_mod",
                    onOffCont,
                    defaults.enable_mod, config::getEnableMod, config::setEnableMod
            ).build();

            var inventoryOpsOpt = getOption(
                    ItemIO.MOD_ID, "inventory_operations",
                    onOffCont,
                    defaults.inventory_operations, config::getInventoryOperations, config::setInventoryOperations
            ).build();

            var animateOpt = getOption(
                    ItemIO.MOD_ID, "animate_opacity",
                    onOffCont,
                    defaults.animate_opacity, config::getAnimateOpacity, config::setAnimateOpacity
            ).build();

            var animateItem = getOption(
                    ItemIO.MOD_ID, "animate_item",
                    onOffCont,
                    defaults.animate_item, config::getAnimateItem, config::setAnimateItem
            ).build();

            var lookContainer = getOption(
                    ItemIO.MOD_ID, "look_container",
                    onOffCont,
                    defaults.need_look_at_container, config::getLookAtContainer, config::setLookAtContainer
            ).build();

            var toggleBind = getOption(
                    ItemIO.MOD_ID, "toggle_bind",
                    onOffCont,
                    defaults.toggle_bind, config::getToggleBind, config::setToggleBind
            ).build();

            var colorOpt = getOption(
                    ItemIO.MOD_ID, "colour",
                    option -> ColorControllerBuilder.create(option).allowAlpha(true),
                    new Color(Helper.reorder(defaults.rgba_outline_color, 3, 0, 1, 2), true), // ARGB
                    () -> new Color(Helper.reorder(config.rgba_outline_color, 3, 0, 1, 2), true), // ARGB
                    color -> config.rgba_outline_color = Helper.reorder(color.getRGB(), 1, 2, 3, 0) // RGBA
            ).build();

            var main = ConfigCategory.createBuilder()
                    .name(Text.translatable(TITLE))
                    .option(enableModOpt)
                    .option(inventoryOpsOpt)
                    .option(toggleBind)
                    .option(lookContainer)
                    .option(animateItem)
                    .option(animateOpt)
                    .option(colorOpt)
                    .build();

            builder.category(main);

            return builder;
        }).generateScreen(parent);
    }
}
