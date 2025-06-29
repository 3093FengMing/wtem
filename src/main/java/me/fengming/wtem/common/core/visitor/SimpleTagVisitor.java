package me.fengming.wtem.common.core.visitor;

import me.fengming.wtem.common.core.Utils;
import net.minecraft.nbt.*;

/**
 * @author FengMing
 */
@FunctionalInterface
public interface SimpleTagVisitor extends TagVisitor {
    SimpleTagVisitor INSTANCE = (tag) -> {};

    /**
     * Processing a CompoundTag used to represent an object, such as a block entity or entity.
     * The incoming tag should include the key of "id".
     * @param tag the root tag.
     */
    @Override
    void visitCompound(CompoundTag tag);

    @Override
    default void visitString(StringTag tag) {}

    @Override
    default void visitByte(ByteTag tag) {}

    @Override
    default void visitShort(ShortTag tag) {}

    @Override
    default void visitInt(IntTag tag) {}

    @Override
    default void visitLong(LongTag tag) {}

    @Override
    default void visitFloat(FloatTag tag) {}

    @Override
    default void visitDouble(DoubleTag tag) {}

    @Override
    default void visitByteArray(ByteArrayTag tag) {}

    @Override
    default void visitIntArray(IntArrayTag tag) {}

    @Override
    default void visitLongArray(LongArrayTag tag) {}

    @Override
    default void visitList(ListTag tag) {
        if (tag.isEmpty()) return;
        for (int i = 0; i < tag.size(); i++) {
            tag.setTag(i, StringTag.valueOf(Utils.literal2Translatable(tag.getString(i))));
        }
    }

    @Override
    default void visitEnd(EndTag tag) {}
}
