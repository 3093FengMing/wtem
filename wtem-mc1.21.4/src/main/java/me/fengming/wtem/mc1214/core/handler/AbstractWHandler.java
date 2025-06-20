package me.fengming.wtem.mc1214.core.handler;

import net.minecraft.nbt.Tag;

/**
 * @author FengMing
 */
public abstract class AbstractWHandler<T extends Tag> {
    /**
     * @param tag a tag to handle.
     * @return true is the tag has been changed; otherwise, false.
     */
    public abstract boolean handle(T tag);
}
