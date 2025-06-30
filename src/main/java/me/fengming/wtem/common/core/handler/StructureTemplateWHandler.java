package me.fengming.wtem.common.core.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * @author FengMing
 */
public class StructureTemplateWHandler extends AbstractWHandler<CompoundTag> {

    @Override
    protected String getKey(CompoundTag tag) {
        return "structure";
    }

    public CompoundTag handle(StructureTemplate structure) {
        if (structure == null) return new CompoundTag();
        CompoundTag compound = structure.save(new CompoundTag());
        handle(compound);
        return compound;
    }

    @Override
    protected boolean innerHandle(CompoundTag tag) {
        BlockEntityWHandler beHandler = new BlockEntityWHandler();
        ListTag blocks = tag.getList("blocks", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i);
            if (!block.contains("nbt")) continue;
            beHandler.handle(block.getCompound("nbt"));
        }
        return true;
    }
}
