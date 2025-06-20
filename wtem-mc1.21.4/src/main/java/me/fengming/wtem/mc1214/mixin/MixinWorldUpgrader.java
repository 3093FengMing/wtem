package me.fengming.wtem.mc1214.mixin;

import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * @author FengMing
 */
@Mixin(WorldUpgrader.class)
public interface MixinWorldUpgrader {
    @Accessor("finished")
    void setFinished(boolean finished);

    @Accessor("overworldDataStorage")
    DimensionDataStorage getOverworldDataStorage();
}
