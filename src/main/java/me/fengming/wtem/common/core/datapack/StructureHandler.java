package me.fengming.wtem.common.core.datapack;

import me.fengming.wtem.common.core.Utils;
import me.fengming.wtem.common.core.handler.StructureTemplateWHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class StructureHandler extends ResourceHandler {
    private static final FileToIdConverter STRUCTURE_CONVERTOR = new FileToIdConverter("structure", ".nbt");

    public static final HandlerFactory FACTORY = StructureHandler::new;

    public StructureHandler(Function<ResourceLocation, Path> filePath, Context context) {
        super("structure", filePath, context);
    }

    @Override
    public void handle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        StructureTemplateManager m = this.context.structureManager();
        if (m == null) return;
        StructureTemplate structure = m.get(STRUCTURE_CONVERTOR.fileToId(rl)).orElse(null);
        CompoundTag modified = new StructureTemplateWHandler().handle(structure);
        Utils.writeNbt(getFilePath(rl), modified);
    }
}
