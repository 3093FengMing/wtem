package me.fengming.wtem.mc1214.core.extractor;

import net.minecraft.nbt.CompoundTag;

public class BlockEntityWHandler extends AbstractWHandler<CompoundTag> {
    public static BlockEntityWHandler getHandler(String blockId) {
        return new BlockEntityWHandler();
    }

    @Override
    public void handle(CompoundTag tag) {
        
    }
}
