package me.fengming.wtem.common.core.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.fengming.wtem.common.core.TranslationContext;
import me.fengming.wtem.common.core.Utils;
import me.fengming.wtem.common.core.visitor.ItemTagVisitor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class ItemModifierHandler extends NonExtraResourceHandler {
    public static final HandlerFactory FACTORY = ItemModifierHandler::new;

    public ItemModifierHandler(Function<ResourceLocation, Path> filePath, Context context) {
        super("item_modifier", filePath);
    }

    @Override
    protected void innerHandle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        var json = Utils.getJson(supplier, "");
        if (json.isJsonObject()) {
            json = processItemModifier(json.getAsJsonObject());
        } else if (json.isJsonArray()) {
            json = processItemModifiers(json.getAsJsonArray());
        }
        Utils.writeLines(getFilePath(rl), GSON.toJson(json));
    }


    public static JsonArray processItemModifiers(JsonArray modifiers) {
        JsonArray array = new JsonArray();
        for (JsonElement element : modifiers) {
            array.add(processItemModifier(element.getAsJsonObject()));
        }
        return array;
    }

    public static JsonObject processItemModifier(JsonObject modifier) {
        String function = modifier.get("function").getAsString();
        TranslationContext.revertAndAppend(Utils.getId(function));
        switch (function) {
            case "minecraft:set_lore" -> {
                var lore = modifier.get("lore").getAsJsonArray();
                var array = new JsonArray();
                for (JsonElement element : lore) {
                    array.add(Utils.literal2Translatable(element));
                }
                modifier.remove("lore");
                modifier.add("lore", array);
            }
            case "minecraft:set_name" -> {
                var json = modifier.get("name");
                modifier.remove("name");
                modifier.add("name", Utils.literal2Translatable(json));
            }
            case "minecraft:set_components" -> {
                var compound = Utils.json2Compound(modifier.getAsJsonObject("components"));
                compound.accept(new ItemTagVisitor());
            }
            case "minecraft:set_contents" -> {
                var array = new JsonArray();
                var entries = modifier.get("entries").getAsJsonArray();
                for (JsonElement entry : entries) {
                    array.add(LootTableHandler.processLootEntry(entry.getAsJsonObject()));
                }
                modifier.remove("entries");
                modifier.add("entries", array);
            }
        }
        return modifier;
    }
}
