package me.fengming.wtem.common.core.visitor;

import me.fengming.wtem.common.core.TranslationContext;
import me.fengming.wtem.common.core.Utils;
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
        TranslationContext.setKey("item." + Utils.getId(tag.getString("id")));
        CompoundTag components = tag.getCompound("components");

        Utils.handleString(components, "minecraft:custom_name");
        TranslationContext.revertAndAppend("lore");
        components.getList("minecraft:lore", Tag.TAG_STRING).accept(INSTANCE);

        TranslationContext.revert();
        handleBook(components, "minecraft:written_book_content");
    }

    @Override
    public void visitList(ListTag tag) {
        if (tag.getElementType() != Tag.TAG_COMPOUND) SimpleTagVisitor.super.visitList(tag);
        for (int i = 0; i < tag.size(); i++) {
            TranslationContext.append(String.valueOf(i));
            visitCompound(tag.getCompound(i));
        }
    }

    private static void handleBook(CompoundTag components, String path) {
        if (!components.contains(path)) return;
        CompoundTag book = components.getCompound(path);

        // translate title by using custom name
        String title = book.getCompound("title").getString("raw");
        TranslationContext.append("title");
        components.putString("minecraft:custom_name", Utils.literal2Translatable(title, true));

        TranslationContext.revertAndAppend("pages");
        ListTag pages = book.getList("pages", Tag.TAG_COMPOUND);
        for (int i = 0; i < pages.size(); i++) {
            TranslationContext.revertAndAppend(String.valueOf(i));
            Utils.handleString(pages.getCompound(i), "raw");
        }
    }
}
