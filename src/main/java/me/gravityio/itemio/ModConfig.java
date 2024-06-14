package me.gravityio.itemio;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModConfig {
    public static final String TITLE = "yacl.itemio.title";

    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("itemio.json5");

    public static final ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler
            .createBuilder(ModConfig.class)
            .id(Identifier.of(ItemIO.MOD_ID))
            .serializer(serializer ->
                    GsonConfigSerializerBuilder.create(serializer)
                            .setPath(PATH)
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
            .build();

    private static Option<Integer>[] getRGBAOptions(String format, int def, Supplier<Integer> rgbaGetter, Consumer<Integer> rgbaSetter) {
        byte[] rgbaDefaults = Helper.getBytes(def, 4, true);
        char[] rgbaLabels = {'r', 'g', 'b', 'a'};

        Option<Integer>[] opts = new Option[4];
        for (int i = 0; i < 4; i++) {
            char label = rgbaLabels[i];
            int defaultValue = (int) ((rgbaDefaults[i] & 0xFF) / 255f * 100);

            String key = format.formatted(label);

            Text name = Text.translatable(key + ".label");
            Text desc = Text.translatable(key + ".desc");

            int byteIndex = i;
            Supplier<Integer> getter = () -> (int) ((Helper.getByteAt(rgbaGetter.get(), byteIndex, 4, true) & 0xFF) / 255f * 100);
            Consumer<Integer> setter = v -> rgbaSetter.accept(Helper.setByteAt(rgbaGetter.get(), (byte) (v / 100f * 255), byteIndex, 4, true));

            opts[i] = Option.<Integer>createBuilder()
                    .name(name)
                    .description(OptionDescription.of(desc))
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).step(1).range(0, 100))
                    .binding(defaultValue, getter, setter)
                    .build();
        }
        return opts;
    }

    private static <T> Option.Builder<T> getOption(String nameKey, String descKey, Function<Option<T>, ControllerBuilder<T>> controller, T def, Supplier<T> getter, Consumer<T> setter) {
        return Option.<T>createBuilder()
                .name(Text.translatable(nameKey))
                .description(OptionDescription.of(Text.translatable(descKey)))
                .controller(controller)
                .binding(def, getter, setter);
    }

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

            Option<Integer>[] outlineColors = getRGBAOptions("yacl.itemio.color.%s", defaults.rgba_outline_color, config::getOutlineColor, config::setOutlineColor);

            OptionGroup.Builder group = OptionGroup.createBuilder()
                    .name(Text.translatable("yacl.itemio.group.outline_color.label"))
                    .description(OptionDescription.of(Text.translatable("yacl.itemio.group.outline_color.desc")))
                    .options(List.of(outlineColors));

            Option.Builder<Boolean> animateOpt = getOption(
                    "yacl.itemio.animate_opacity.label", "yacl.itemio.animate_opacity.desc",
                    BooleanControllerBuilder::create,
                    defaults.animate_opacity, config::getAnimateOpacity, config::setAnimateOpacity);

            Option.Builder<Boolean> animateItem = getOption(
                    "yacl.itemio.animate_item.label", "yacl.itemio.animate_item.desc",
                    BooleanControllerBuilder::create,
                    defaults.animate_item, config::getAnimateItem, config::setAnimateItem);

            Option.Builder<Boolean> lookContainer = getOption(
                    "yacl.itemio.look_container.label", "yacl.itemio.look_container.desc",
                    BooleanControllerBuilder::create,
                    defaults.need_look_at_container, config::getLookAtContainer, config::setLookAtContainer);

            Option.Builder<Boolean> toggleBind = getOption(
                    "yacl.itemio.toggle_bind.label", "yacl.itemio.toggle_bind.desc",
                    BooleanControllerBuilder::create,
                    defaults.toggle_bind, config::getToggleBind, config::setToggleBind);

            var main = ConfigCategory.createBuilder()
                    .name(Text.translatable(TITLE))
                    .option(toggleBind.build())
                    .option(lookContainer.build())
                    .option(animateItem.build())
                    .option(animateOpt.build())
                    .group(group.build());

            builder.category(main.build());

            return builder;
        }).generateScreen(parent);
    }
}
