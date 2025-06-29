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
public class LootTableHandler extends NonExtraResourceHandler {
    public static final HandlerFactory FACTORY = LootTableHandler::new;

    public LootTableHandler(Function<ResourceLocation, Path> filePath, Context context) {
        super("loot_table", filePath);
    }

    @Override
    public void handle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        var array = new JsonArray();
        var pools = Utils.getJson(supplier, "pools").getAsJsonArray();
        for (JsonElement pool : pools) {
            var array1 = new JsonArray();
            var entries = pool.getAsJsonObject().getAsJsonArray("entries");
            for (JsonElement entry : entries) {
                array1.add(processLootEntry(entry.getAsJsonObject()));
            }
            pool.getAsJsonObject().add("entries", array1);
            array.add(pool);
        }
        Utils.writeLines(getFilePath(rl), GSON.toJson(array));
    }

    public static JsonObject processLootEntry(JsonObject entry) {
        var entryObj = entry.getAsJsonObject();
        String type = entryObj.get("type").getAsString();
        switch (type) {
            case "minecraft:item" -> {
                var modifiers = entryObj.getAsJsonArray("functions");
                entryObj.remove("functions");
                entryObj.add("functions", ItemModifierHandler.processItemModifiers(modifiers));
            }
            case "minecraft:group" -> {
                var array = new JsonArray();
                var children = entryObj.getAsJsonArray("children");
                for (JsonElement child : children) {
                    array.add(processLootEntry(child.getAsJsonObject()));
                }
                entryObj.remove("children");
                entryObj.add("children", array);
            }
            default -> {
                return entryObj;
            }
        }
        return entryObj;
    }
}
