package me.gravityio.itemio;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
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
import java.util.function.Supplier;

public class ModConfig {
    public static final String TITLE = "yacl.itemio.title";

    public static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("itemio.json5");

    public static final ConfigClassHandler<ModConfig> HANDLER = ConfigClassHandler
            .createBuilder(ModConfig.class)
            .id(new Identifier(ItemIO.MOD_ID))
            .serializer(serializer ->
                    GsonConfigSerializerBuilder.create(serializer)
                            .setPath(PATH)
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                            .setJson5(true)
                            .build())
            .build();

    private static Option<Integer>[] getRGBAOptions(int def, Supplier<Integer> rgbaGetter, Consumer<Integer> rgbaSetter) {
        byte[] rgbaDefaults = Helper.getBytes(def, true);
        char[] rgbaLabels = {'r','g','b','a'};

        Option<Integer>[] opts = new Option[4];
        for (int i = 0; i < 4; i++) {
            int byteIndex = 3 - i;
            char label = rgbaLabels[i];
            int defaultValue = (int) ((rgbaDefaults[i] & 0xFF) / 255f * 100);

            Text name = Text.translatable("yacl.itemio.color." + label + ".label");
            Text desc = Text.translatable("yacl.itemio.color." + label + ".desc");

            Supplier<Integer> getter = () -> (int) ((Helper.getByteAt(rgbaGetter.get(), byteIndex) & 0xFF) / 255f * 100);
            Consumer<Integer> setter = v -> rgbaSetter.accept(Helper.setByteAt(rgbaGetter.get(), (byte) (v / 100f * 255), byteIndex));

            opts[i] = Option.<Integer>createBuilder()
                    .name(name)
                    .description(OptionDescription.of(desc))
                    .controller(opt -> IntegerSliderControllerBuilder.create(opt).step(1).range(0, 100))
                    .binding(defaultValue, getter, setter)
                    .build();
        }
        return opts;
    }

    @SerialEntry
    public int rgba_outline_color = 0xffffff40;

    public static Screen getScreen(Screen parent) {
        return YetAnotherConfigLib.create(HANDLER, (defaults, config, builder) -> {
            builder.title(Text.translatable(TITLE));

            Option<Integer>[] outlineColors = getRGBAOptions(defaults.rgba_outline_color, () -> config.rgba_outline_color, (v) -> config.rgba_outline_color = v);

            OptionGroup.Builder group = OptionGroup.createBuilder()
                    .name(Text.translatable("yacl.itemio.group.outline_color.label"))
                    .description(OptionDescription.of(Text.translatable("yacl.itemio.group.outline_color.desc")))
                    .options(List.of(outlineColors));

            var main = ConfigCategory.createBuilder()
                    .name(Text.translatable(TITLE))
                    .group(group.build());

            builder.category(main.build());

            return builder;
        }).generateScreen(parent);
    }
}
