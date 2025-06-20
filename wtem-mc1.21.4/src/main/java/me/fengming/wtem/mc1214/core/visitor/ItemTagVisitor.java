package me.fengming.wtem.mc1214.core.visitor;

import me.fengming.wtem.mc1214.core.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * @author FengMing
 */
public class ItemTagVisitor implements SimpleTagVisitor {
    @Override
    public void visitCompound(CompoundTag tag) {
        if (tag.isEmpty()) return;
        CompoundTag components = tag.getCompound("components");

        Utils.handleString(components, "minecraft:custom_name");
        components.getList("minecraft:lore", Tag.TAG_STRING).accept(INSTANCE);

        handleBook(components, "minecraft:written_book_content");
    }

    @Override
    public void visitList(ListTag tag) {
        if (tag.getElementType() != Tag.TAG_COMPOUND) SimpleTagVisitor.super.visitList(tag);
        for (int i = 0; i < tag.size(); i++) {
            visitCompound(tag.getCompound(i));
        }
    }

    private static void handleBook(CompoundTag components, String path) {
        if (!components.contains(path)) return;
        CompoundTag book = components.getCompound(path);

        // translate title by using custom name
        String title = components.getCompound("title").getString("raw");
        components.putString("minecraft:custom_name", Utils.literal2Translatable(title));

        ListTag pages = book.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < pages.size(); i++) {
            Utils.handleString(pages.getCompound(i), "raw");
        }
    }
}
