package me.fengming.wtem.mc1214.core.handler;

import me.fengming.wtem.mc1214.core.visitor.EntityTagVisitor;
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
