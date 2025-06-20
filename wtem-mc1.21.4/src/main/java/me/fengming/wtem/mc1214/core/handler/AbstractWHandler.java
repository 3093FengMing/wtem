package me.fengming.wtem.mc1214.core.extractor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

public abstract class AbstractWHandler<T extends Tag> {
    public static String literalToTranslatable(String literal) {
        MutableComponent component = Component.Serializer.fromJsonLenient(literal, RegistryAccess.EMPTY);
        if (component == null || component.getContents().type() != PlainTextContents.TYPE) return literal;

        PlainTextContents plain = (PlainTextContents) component.getContents();
        String translatable = Component.Serializer.toJson(Component.translatable(plain.text()), RegistryAccess.EMPTY);

        return StringTag.quoteAndEscape(translatable);
    }

    public abstract void handle(T tag);
}
