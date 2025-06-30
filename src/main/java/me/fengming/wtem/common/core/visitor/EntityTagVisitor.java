package me.fengming.wtem.common.core.visitor;

import me.fengming.wtem.common.core.TranslationContext;
import me.fengming.wtem.common.core.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.List;

/**
 * @author FengMing
 */
public class EntityTagVisitor implements SimpleTagVisitor {

    @Override
    public void visitCompound(CompoundTag tag) {
        Utils.handleString(tag, "CustomName");
        Utils.handleString(tag, "text");
        ListTag passengers = tag.getList("Passengers", Tag.TAG_COMPOUND);
        for (int i = 0; i < passengers.size(); i++) {
            TranslationContext.append("passengers." + i);
            visitCompound(passengers.getCompound(i));
        }

        ItemTagVisitor itemVisitor = new ItemTagVisitor();

        TranslationContext.revert();
        CompoundTag equipment = tag.getCompound("equipment");
        for (String key : equipment.getAllKeys()) {
            TranslationContext.append(key);
            equipment.getCompound(key).accept(itemVisitor);
        }

        TranslationContext.revert();
        for (String s : List.of("Items", "Inventory", "ArmorItems", "HandItems")) {
            TranslationContext.append(s);
            ListTag items = tag.getList(s, Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                items.getCompound(i).accept(itemVisitor);
            }
        }

        TranslationContext.revert();
        for (String s : List.of("Item", "item", "FireworksItem")) {
            TranslationContext.append(s);
            CompoundTag item = tag.getCompound(s);
            item.accept(itemVisitor);
        }

        TranslationContext.revert();
        ListTag recipes = tag.getCompound("Offers").getList("Recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < recipes.size(); i++) {
            CompoundTag recipe = recipes.getCompound(i);
            for (String s : List.of("buy", "buyB", "sell")) {
                TranslationContext.append("Offers." + s);
                recipe.getCompound(s).accept(itemVisitor);
            }
        }
    }
}
