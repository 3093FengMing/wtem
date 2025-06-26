package me.fengming.wtem.mc1214.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author FengMing
 */
@Mixin(StructureTemplateManager.class)
public interface MixinStructureTemplateManager {
    @Accessor("generatedDir")
    Path getGeneratedDir();

    @Invoker("listGenerated")
    Stream<ResourceLocation> invokeListGenerated();
}
