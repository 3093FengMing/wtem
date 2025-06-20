package me.fengming.wtem.mc1214.core;

import com.mojang.datafixers.DataFixer;
import me.fengming.wtem.mc1214.Wtem;
import me.fengming.wtem.mc1214.core.handler.BlockEntityWHandler;
import me.fengming.wtem.mc1214.core.handler.EntityWHandler;
import me.fengming.wtem.mc1214.mixin.MixinWorldUpgrader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;

/**
 * @author FengMing
 */
public class WorldExtractor extends WorldUpgrader {
    public static final Component STATUS_EXTRACTING = Component.translatable("gui.wtem.main.extraction.working");
    public static final Component STATUS_FINISHED_EXTRACTION = Component.translatable("gui.wtem.main.extraction.finished");

    private final Minecraft mc;
    private final DataFixer dataFixer;
    private final WorldData worldData;
    private final LevelStorageSource.LevelStorageAccess levelStorage;
    private final RegistryAccess registry;

    public WorldExtractor(Minecraft mc,
                          DataFixer dataFixer,
                          WorldData worldData,
                          LevelStorageSource.LevelStorageAccess levelStorage,
                          RegistryAccess registry) {
        super(levelStorage, dataFixer, registry, false, false);
        this.mc = mc;
        this.dataFixer = dataFixer;
        this.worldData = worldData;
        this.levelStorage = levelStorage;
        this.registry = registry;
    }

    @Override
    public void work() {
        new ChunkExtractor().upgrade();
        new EntityExtractor().upgrade();
        extractBossBar(this.levelStorage, this.registry, this.worldData);
        extractScoreBoard(((MixinWorldUpgrader) this).getOverworldDataStorage());
        ((MixinWorldUpgrader) this).setFinished(true);
    }

    public static void extractBossBar(LevelStorageSource.LevelStorageAccess levelStorage, RegistryAccess registry, WorldData worldData) {
        CompoundTag bossBarTag = worldData.getCustomBossEvents();
        if (bossBarTag == null) return;
        bossBarTag.getAllKeys().forEach(key -> Utils.handleString(bossBarTag.getCompound(key), "Name"));
        worldData.setCustomBossEvents(bossBarTag);
        levelStorage.saveDataTag(registry, worldData, null);
    }

    public static void extractScoreBoard(DimensionDataStorage dataStorage) {
        CustomScoreBoard sb = new CustomScoreBoard();
        dataStorage.get(sb.dataFactory(), "scoreboard");
        sb.extract();
    }

    public static final class CustomScoreBoard extends Scoreboard {
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
                t.setDisplayName(Utils.literal2Translatable(t.getDisplayName()));
                t.setPlayerPrefix(Utils.literal2Translatable(t.getPlayerPrefix()));
                t.setPlayerSuffix(Utils.literal2Translatable(t.getPlayerSuffix()));
            });
            this.getObjectives().forEach(o ->
                o.setDisplayName(Utils.literal2Translatable(o.getDisplayName()))
            );
        }
    }

    class ChunkExtractor extends WorldUpgrader.AbstractUpgrader<ChunkStorage> {
        ChunkExtractor() {
            super(DataFixTypes.CHUNK, "chunk", "region", STATUS_EXTRACTING, STATUS_FINISHED_EXTRACTION);
        }

        @Override
        protected @NotNull ChunkStorage createStorage(RegionStorageInfo regionStorageInfo, Path path) {
            return new ChunkStorage(regionStorageInfo, path, WorldExtractor.this.dataFixer, true);
        }

        @Override
        protected boolean tryProcessOnePosition(ChunkStorage chunkStorage, ChunkPos chunkPos, ResourceKey<Level> dimension) {
            CompoundTag compoundTag = chunkStorage.read(chunkPos).join().orElse(null);
            if (compoundTag == null || !ChunkStatus.FULL.getName().equals(compoundTag.getString("Status"))) return false;
            boolean isUpdated = false;

            var beHandler = new BlockEntityWHandler();
            ListTag blockEntities = compoundTag.getList("block_entities", Tag.TAG_COMPOUND);
            Wtem.LOGGER.debug("blockEntities = {}", blockEntities);
            for (int i = 0; i < blockEntities.size(); i++) {
                isUpdated |= beHandler.handle(blockEntities.getCompound(i));
            }
            Wtem.LOGGER.debug("updated blockEntities = {}", blockEntities);

            if (!isUpdated) return false;

            if (this.previousWriteFuture != null) {
                this.previousWriteFuture.join();
            }
            this.previousWriteFuture = chunkStorage.write(chunkPos, () -> compoundTag);
            return true;
        }
    }

    class EntityExtractor extends WorldUpgrader.SimpleRegionStorageUpgrader {
        EntityExtractor() {
            super(DataFixTypes.ENTITY_CHUNK, "entities", STATUS_EXTRACTING, STATUS_FINISHED_EXTRACTION);
        }

        @Override
        protected @NotNull SimpleRegionStorage createStorage(RegionStorageInfo regionStorageInfo, Path path) {
            return new SimpleRegionStorage(regionStorageInfo, path, dataFixer, true, this.dataFixType);
        }

        @Override
        protected boolean tryProcessOnePosition(SimpleRegionStorage simpleRegionStorage, ChunkPos chunkPos, ResourceKey<Level> resourceKey) {
            CompoundTag compoundTag = simpleRegionStorage.read(chunkPos).join().orElse(null);
            if (compoundTag == null) return false;
            boolean isUpdated = false;

            var entityHandler = new EntityWHandler();
            ListTag entities = compoundTag.getList("Entities", Tag.TAG_COMPOUND);
            Wtem.LOGGER.debug("Entities = {}", entities);
            for (int i = 0; i < entities.size(); i++) {
                isUpdated |= entityHandler.handle(entities.getCompound(i));
            }
            Wtem.LOGGER.debug("updated Entities = {}", entities);

            if (!isUpdated) return false;

            if (this.previousWriteFuture != null) {
                this.previousWriteFuture.join();
            }

            this.previousWriteFuture = simpleRegionStorage.write(chunkPos, compoundTag);
            return true;
        }

        @Override
        protected @NotNull CompoundTag upgradeTag(SimpleRegionStorage regionStorage, CompoundTag chunkTag) {
            return chunkTag;
        }
    }

}
