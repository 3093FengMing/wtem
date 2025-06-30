package me.fengming.wtem.common.core.handler;

import me.fengming.wtem.common.core.visitor.EntityTagVisitor;
import net.minecraft.nbt.CompoundTag;

/**
 * @author FengMing
 */
public class EntityWHandler extends AbstractWHandler<CompoundTag> {

    @Override
    protected String getKey(CompoundTag tag) {
        return "entity." + tag.getString("id").split(":", 2)[1];
    }

    @Override
    protected boolean innerHandle(CompoundTag tag) {
        tag.accept(new EntityTagVisitor());
        return true;
    }
}
