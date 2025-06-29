package me.fengming.wtem.common.core.datapack;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * @author FengMing
 */
@FunctionalInterface
public interface HandlerFactory {
    ResourceHandler newHandler(Function<ResourceLocation, Path> filePath, ResourceHandler.Context context);
}
