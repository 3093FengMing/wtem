package me.fengming.wtem.common;

import com.mojang.logging.LogUtils;
import me.fengming.wtem.common.core.datapack.*;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

/**
 * @author FengMing
 */
public class Wtem implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        ResourceHandlers.addHandler(SimpleJsonHandler.AdvancementHandlerSimple.FACTORY);
        ResourceHandlers.addHandler(SimpleJsonHandler.EnchantmentHandlerSimple.FACTORY);
        ResourceHandlers.addHandler(ItemModifierHandler.FACTORY);
        ResourceHandlers.addHandler(LootTableHandler.FACTORY);
        ResourceHandlers.addHandler(PredicateHandler.FACTORY);
        ResourceHandlers.addHandler(FunctionHandler.FACTORY);
        ResourceHandlers.addHandler(StructureHandler.FACTORY);
    }
}
