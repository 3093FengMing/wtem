package me.fengming.wtem.common.core.handler;

import me.fengming.wtem.common.core.visitor.EntityTagVisitor;
import net.minecraft.nbt.CompoundTag;

/**
 * @author FengMing
 */
public class EntityWHandler extends AbstractWHandler<CompoundTag> {

    @Override
    public boolean handle(CompoundTag tag) {
        tag.accept(new EntityTagVisitor());
        return true;
    }
}
