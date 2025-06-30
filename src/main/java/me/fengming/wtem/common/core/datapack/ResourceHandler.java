package me.fengming.wtem.common.core.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.fengming.wtem.common.core.TranslationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * @author FengMing
 */
public abstract class ResourceHandler {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final String path;
    private final Function<ResourceLocation, Path> filePath;
    protected final Context context;

    public ResourceHandler(String path, Function<ResourceLocation, Path> filePath, Context context) {
        this.path = path;
        this.filePath = filePath;
        this.context = context;
    }

    public String getPath() {
        return this.path;
    }

    public Path getFilePath(ResourceLocation rl) {
        return this.filePath.apply(rl);
    }

    public void handle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        String p = rl.getPath().split("\\.", 2)[0];
        String s = rl.getNamespace() + "." + p;
        s = s.replace("/", ".");
        TranslationContext.setKey("datapack." + s);
        innerHandle(rl, supplier);
    }

    protected abstract void innerHandle(ResourceLocation rl, IoSupplier<InputStream> supplier);

    public record Context(@Nullable List<String> list, @Nullable StructureTemplateManager structureManager) {
        public static Context of(List<String> list, StructureTemplateManager structureManager) {
            return new Context(list, structureManager);
        }

        public Context set(List<String> list, StructureTemplateManager structureManager) {
            if (this.list == null && this.structureManager == null) return new Context(list, structureManager);
            if (this.list != null && this.structureManager == null) return new Context(this.list, structureManager);
            if (this.list == null) return new Context(list, this.structureManager);
            return new Context(this.list, this.structureManager);
        }
    }
}
