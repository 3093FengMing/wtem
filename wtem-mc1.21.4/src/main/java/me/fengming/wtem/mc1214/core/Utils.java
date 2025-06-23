package me.fengming.wtem.mc1214.core;

import me.fengming.wtem.mc1214.Wtem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    public static String component2String(Component component) {
        if (component == null) return "";
        return Component.Serializer.toJson(component, RegistryAccess.EMPTY);
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
        String s = compound.getString(path);
        if (s.isEmpty()) return;
        compound.putString(path, literal2Translatable(s));
    }

    public static CompoundTag getCompound(CompoundTag compound, String path) {
        String[] paths = path.split("\\.");
        for (String s : paths) {
            path = s;
            if (!compound.contains(path) || compound.getTagType(path) == Tag.TAG_STRING) break;
            compound = compound.getCompound(path);
        }
        return compound.getCompound(path);
    }

    public static List<String> readLines(IoSupplier<InputStream> supplier, ResourceLocation rl) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8))) {
            return bufferedReader.lines().toList();
        } catch (IOException e) {
            Wtem.LOGGER.error("Failed to read lines from {}", rl, e);
        }
        return List.of();
    }

    public static void writeLines(Path path, String lines) {
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            }
            Files.writeString(path, lines);
        } catch (IOException e) {
            Wtem.LOGGER.error("Failed to write lines to {}", path, e);
        }
    }
}
