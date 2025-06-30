package me.fengming.wtem.common.core.handler;

import me.fengming.wtem.common.core.Utils;
import me.fengming.wtem.common.core.datapack.FunctionHandler;
import me.fengming.wtem.common.core.visitor.EntityTagVisitor;
import me.fengming.wtem.common.core.visitor.ItemTagVisitor;
import me.fengming.wtem.common.core.visitor.SimpleTagVisitor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagVisitor;

import java.util.List;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class BlockEntityWHandler extends AbstractWHandler<CompoundTag> {

    private static final Function<String, SimpleTagVisitor> CONTAINER_VISITOR = (path) -> (tag) -> {
        if (!tag.contains(path)) return;
        Utils.handleString(tag, "CustomName");
        tag.getList(path, Tag.TAG_COMPOUND).accept(new ItemTagVisitor());
    };

    @Override
    protected String getKey(CompoundTag tag) {
        return "block." + Utils.getId(tag.getString("id"));
    }

    @Override
    protected boolean innerHandle(CompoundTag compound) {
        String id = Utils.getId(compound.getString("id"));
        compound.accept(getVisitor(id));
        return true;
    }

    private TagVisitor getVisitor(String id) {
        EntityTagVisitor entityVisitor = new EntityTagVisitor();
        return switch (id) {
            case "barrel", "blast_furnace", "brewing_stand",
                 "campfire", "chest", "chiseled_bookshelf",
                 "crafter", "dispenser", "dropper",
                 "furnace", "hopper", "shulker_box",
                 "smoker", "trapped_chest" -> CONTAINER_VISITOR.apply("Items");
            case "jukebox" -> CONTAINER_VISITOR.apply("Records");
            case "lectern" -> CONTAINER_VISITOR.apply("Book");
            case "brushable_block", "decorated_pot" -> CONTAINER_VISITOR.apply("item");
            case "sign", "hanging_sign" -> (SimpleTagVisitor) tag -> {
                for (String s : List.of("front_text", "back_text")) {
                    tag.getCompound(s)
                            .getList("messages", Tag.TAG_STRING)
                            .accept(SimpleTagVisitor.INSTANCE);
                }
            };
            case "beehive", "bee_nest" -> (SimpleTagVisitor) tag -> {
                ListTag bees = tag.getList("bees", Tag.TAG_COMPOUND);
                for (int i = 0; i < bees.size(); ++i) {
                    Utils.getCompound(bees.getCompound(i), "entity_data.entity").accept(entityVisitor);
                }
            };
            case "mob_spawner" -> (SimpleTagVisitor) tag -> {
                tag.getCompound("SpawnData").getCompound("entity").accept(entityVisitor);
                ListTag potentials = tag.getList("SpawnPotentials", Tag.TAG_COMPOUND);
                for (int i = 0; i < potentials.size(); ++i) {
                    Utils.getCompound(potentials.getCompound(i), "data.entity").accept(entityVisitor);
                }
            };
            case "trial_spawner" -> (SimpleTagVisitor) tag -> {
                Utils.getCompound(tag, "spawn_data.entity").accept(entityVisitor);
                for (String s : List.of("normal_config", "ominous_config")) {
                    ListTag potentials = tag.getCompound(s).getList("spawn_potentials", Tag.TAG_COMPOUND);
                    for (int i = 0; i < potentials.size(); ++i) {
                        Utils.getCompound(potentials.getCompound(i), "data.entity").accept(entityVisitor);
                    }
                }
            };
            case "command_block" -> (SimpleTagVisitor) tag -> {
                String command = FunctionHandler.processFunction(List.of(tag.getString("Command")));
                tag.putString("Command", command);
            };
            default -> SimpleTagVisitor.INSTANCE;
        };
    }

}
