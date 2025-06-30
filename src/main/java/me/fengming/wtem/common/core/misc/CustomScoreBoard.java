package me.fengming.wtem.common.core.misc;

import me.fengming.wtem.common.core.TranslationContext;
import me.fengming.wtem.common.core.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;

/**
 * @author FengMing
 */
public class CustomScoreBoard extends Scoreboard {
    public SavedData.Factory<ScoreboardSaveData> dataFactory() {
        return new SavedData.Factory<>(this::createData, this::createData, DataFixTypes.SAVED_DATA_SCOREBOARD);
    }

    private ScoreboardSaveData createData() {
        return new ScoreboardSaveData(this);
    }

    private ScoreboardSaveData createData(CompoundTag tag, HolderLookup.Provider registries) {
        return this.createData().load(tag, registries);
    }

    public void extract() {
        this.getPlayerTeams().forEach(t -> {
            TranslationContext.setKey("score.team." + t.getName());
            t.setDisplayName(Utils.literal2Translatable(t.getDisplayName()));
            t.setPlayerPrefix(Utils.literal2Translatable(t.getPlayerPrefix()));
            t.setPlayerSuffix(Utils.literal2Translatable(t.getPlayerSuffix()));
        });
        this.getObjectives().forEach(o -> {
            TranslationContext.setKey("score.objective." + o.getName());
            o.setDisplayName(Utils.literal2Translatable(o.getDisplayName()));
        });
    }
}
