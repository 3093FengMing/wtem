package me.fengming.wtem.common.core.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.fengming.wtem.common.core.Utils;
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
    public void handle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
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
        if ("set_lore".equals(function)) Utils.handleJsonElement(modifier, "lore");
        if ("set_name".equals(function)) Utils.handleJsonElement(modifier, "name");
        return modifier;
    }
}
