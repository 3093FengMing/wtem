package me.fengming.wtem.mc1214.core;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

/**
 * @author FengMing
 */
public class Utils {

    public static String literal2Translatable(String literal) {
        if (literal.isEmpty()) return literal;
        MutableComponent original = Component.Serializer.fromJsonLenient(literal, RegistryAccess.EMPTY);
        return Component.Serializer.toJson(literal2Translatable(original), RegistryAccess.EMPTY);
    }

    public static Component literal2Translatable(Component original) {
        if (original == null || original.getContents().type() != PlainTextContents.TYPE) return original;

        PlainTextContents plain = (PlainTextContents) original.getContents();
        MutableComponent translatable = Component.translatable(plain.text());
        translatable.setStyle(original.getStyle());
        return translatable;
    }

    /**
     * Handle StringTag in the incoming path of the incoming compound tag.
     * Allow the use of '.' as a path separator.
     * @param compound the compound tag
     * @param path the path of string tag
     */
    public static void handleString(CompoundTag compound, String path) {
        String[] paths = path.split("\\.");
        for (String s : paths) {
            path = s;
            if (!compound.contains(path) || compound.getTagType(path) == Tag.TAG_STRING) break;
            compound = compound.getCompound(path);
        }
        compound.putString(path, literal2Translatable(compound.getString(path)));
    }
}
