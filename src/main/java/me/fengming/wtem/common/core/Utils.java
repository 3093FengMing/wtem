package me.fengming.wtem.common.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import me.fengming.wtem.common.Wtem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author FengMing
 */
public class Utils {

    public static String literal2Translatable(String literal, boolean nonItalic) {
        if (literal.isBlank()) return literal;
        MutableComponent original = Component.Serializer.fromJsonLenient(literal, RegistryAccess.EMPTY);
        if (original != null && nonItalic) original = original.withStyle(Style.EMPTY.withItalic(false));
        return Component.Serializer.toJson(literal2Translatable(original), RegistryAccess.EMPTY);
    }

    public static Component literal2Translatable(Component original) {
        if (original == null || original.getContents().type() != PlainTextContents.TYPE) return original;

        PlainTextContents plain = (PlainTextContents) original.getContents();
        MutableComponent translatable = Component.translatable(generateKey(plain.text()));
        translatable.setStyle(original.getStyle());
        return translatable;
    }

    public static JsonElement literal2Translatable(JsonElement json) {
        var translatable = literal2Translatable(Component.Serializer.fromJson(json, RegistryAccess.EMPTY));
        return ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, translatable).getOrThrow();
    }

    public static String translatable2String(Component component) {
        if (component == null) return "";
        return Component.Serializer.toJson(literal2Translatable(component), RegistryAccess.EMPTY);
    }

    public static String generateKey(String plain) {
        String key = TranslationContext.nextKey();
        TranslationContext.addKey(key, plain);
        return key;
    }

    public static void handleString(CompoundTag compound, String path) {
        TranslationContext.append(getId(path));
        String[] paths = path.split("\\.");
        CompoundTag element = compound;
        for (String s : paths) {
            path = s;
            if (!compound.contains(path) || compound.getTagType(path) == Tag.TAG_STRING) break;
            element = compound.getCompound(path);
        }
        String s = element.getString(path);
        if (s.isEmpty()) return;
        element.remove(path);
        element.putString(path, literal2Translatable(s,false));
    }

    public static void handleJsonElement(JsonObject json, String path) {
        TranslationContext.append(path);
        String[] paths = path.split("\\.");
        if (paths.length == 0) return;
        JsonObject element = json;
        for (String s : paths) {
            path = s;
            if (!json.has(path)) break;
            element = json.getAsJsonObject(path);
        }
        var translatable = literal2Translatable(Component.Serializer.fromJson(element.get(path), RegistryAccess.EMPTY));
        element.remove(path);
        element.add(path, ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, translatable).getOrThrow());
    }

    public static JsonElement getJson(IoSupplier<InputStream> supplier, String path) {
        String[] paths = path.split("\\.");
        if (paths.length == 0) return new JsonObject();
        try (var br = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(br);
            JsonElement element = json;
            if (!json.isJsonObject() || path.isEmpty()) return element;
            json = json.getAsJsonObject();
            for (String s : paths) {
                path = s;
                if (!json.getAsJsonObject().has(path)) break;
                element = json.getAsJsonObject().get(path);
            }
            return element;
        } catch (Exception e) {
            Wtem.LOGGER.error("Failed to parse JSON", e);
        }
        return new JsonObject();
    }

    public static CompoundTag getCompound(CompoundTag compound, String path) {
        String[] paths = path.split("\\.");
        if (paths.length == 0) return compound;
        CompoundTag tag = new CompoundTag();
        for (String s : paths) {
            path = s;
            if (!compound.contains(path) || compound.getTagType(path) == Tag.TAG_STRING) break;
            tag = compound.getCompound(path);
        }
        return tag.getCompound(path);
    }

    public static CompoundTag json2Compound(JsonObject json) {
        var optional = CompoundTag.CODEC.parse(JsonOps.INSTANCE, json).result();
        if (optional.isEmpty()) {
            Wtem.LOGGER.warn("Couldn't parse JSON to a compound tag: {}", json);
            return new CompoundTag();
        }
        return optional.get();
    }

    public static JsonObject compound2Json(CompoundTag compound) {
        var optional = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, compound).result();
        if (optional.isEmpty()) {
            Wtem.LOGGER.warn("Couldn't parse a compound tag to JSON: {}", compound);
            return new JsonObject();
        }
        return optional.get().getAsJsonObject();
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

    public static void writeNbt(Path path, CompoundTag tag) {
        try {
            NbtIo.writeCompressed(tag, path);
        } catch (IOException e) {
            Wtem.LOGGER.error("Failed to write nbt to {}", path, e);
        }
    }

    public static String getId(String id) {
        String[] sp = id.split(":", 2);
        if (sp.length == 2) return sp[1];
        return id;
    }

    public static void logInfo(String key, ResourceLocation world, ListTag pos) {
        if (pos.size() != 3) return;
        StringBuilder sb = new StringBuilder("[");
        sb.append(pos.getInt(0)).append(pos.getInt(1)).append(pos.getInt(2)).append("]");
        Wtem.LOGGER.info("Key: {} {} In World {}", key, sb, world);
    }
}
