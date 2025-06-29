package me.fengming.wtem.common.core.datapack;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * @author FengMing
 */
public abstract class NonExtraResourceHandler extends ResourceHandler {
    public NonExtraResourceHandler(String path, Function<ResourceLocation, Path> filePath) {
        super(path, filePath, null);
    }
}
