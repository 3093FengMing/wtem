package me.fengming.wtem.common.core.datapack;

import me.fengming.wtem.common.core.TranslationContext;
import me.fengming.wtem.common.core.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class SimpleJsonHandler extends ResourceHandler {
    public SimpleJsonHandler(String path, Function<ResourceLocation, Path> filePath, Context context) {
        super(path, filePath, context);
    }

    @Override
    protected void innerHandle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        Utils.writeLines(getFilePath(rl), processJsonFile(supplier, this.context.list()));
    }

    protected String processJsonFile(IoSupplier<InputStream> supplier, List<String> list) {
        if (list == null) return "";
        var jsonObj = Utils.getJson(supplier, "").getAsJsonObject();
        for (String s : list) {
            Utils.handleJsonElement(jsonObj, s);
            TranslationContext.revert();
        }
        return GSON.toJson(jsonObj);
    }

    public static class AdvancementHandlerSimple extends SimpleJsonHandler {
        public static final HandlerFactory FACTORY = AdvancementHandlerSimple::new;

        public AdvancementHandlerSimple(Function<ResourceLocation, Path> filePath, Context context) {
            super("advancement", filePath, context.set(List.of("display.title", "display.description"), null));
        }

        @Override
        protected String processJsonFile(IoSupplier<InputStream> supplier, List<String> list) {
            var s = super.processJsonFile(supplier, list);
            return s;
        }
    }

    public static class EnchantmentHandlerSimple extends SimpleJsonHandler {
        public static final HandlerFactory FACTORY = EnchantmentHandlerSimple::new;

        public EnchantmentHandlerSimple(Function<ResourceLocation, Path> filePath, Context context) {
            super("enchantment", filePath, context.set(List.of("description"), null));
        }
    }
}
