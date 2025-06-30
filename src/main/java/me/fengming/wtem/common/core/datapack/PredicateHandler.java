package me.fengming.wtem.common.core.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
public class PredicateHandler extends NonExtraResourceHandler {
    public static final HandlerFactory FACTORY = PredicateHandler::new;

    public PredicateHandler(Function<ResourceLocation, Path> filePath, Context context) {
        super("predicate", filePath);
    }

    @Override
    protected void innerHandle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        var json = Utils.getJson(supplier, "");
        if (json.isJsonObject()) {
            json = processPredicate(json.getAsJsonObject());
        } else if (json.isJsonArray()) {
            json = processPredicates(json.getAsJsonArray());
        }
        Utils.writeLines(getFilePath(rl), GSON.toJson(json));
    }

    public static JsonObject processPredicate(JsonObject predicate) {
        String condition = predicate.get("condition").getAsString();
        switch (condition) {
            case "all_of", "any_of" -> {
                var array = processPredicates(predicate.getAsJsonArray("terms"));
                predicate.remove("terms");
                predicate.add("terms", array);
            }
            case "inverted" -> {
                var object = predicate.getAsJsonObject("term");
                predicate.remove("term");
                predicate.add("term", object);
            }
            case "match_tool" -> {
                var components = predicate.getAsJsonObject("predicate").getAsJsonObject("components");
                var compound = Utils.json2Compound(components);
                compound.accept(new ItemTagVisitor());
                predicate.getAsJsonObject("predicate").add("components", Utils.compound2Json(compound));
            }
        }
        return predicate;
    }

    public static JsonArray processPredicates(JsonArray predicates) {
        var array = new JsonArray();
        for (JsonElement predicate : predicates) {
            array.add(processPredicate(predicate.getAsJsonObject()));
        }
        return array;
    }
}
