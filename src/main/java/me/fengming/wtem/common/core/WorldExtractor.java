package me.fengming.wtem.common.core;

import com.mojang.datafixers.DataFixer;
import me.fengming.wtem.common.core.datapack.ResourceHandler;
import me.fengming.wtem.common.Wtem;
import me.fengming.wtem.common.core.datapack.ResourceHandlers;
import me.fengming.wtem.common.core.handler.BlockEntityWHandler;
import me.fengming.wtem.common.core.handler.EntityWHandler;
import me.fengming.wtem.common.core.handler.StructureTemplateWHandler;
import me.fengming.wtem.common.core.misc.CustomScoreBoard;
import me.fengming.wtem.common.mixin.AccessorWorldUpgrader;
import me.fengming.wtem.common.mixin.MixinStructureTemplateManager;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class WorldExtractor extends WorldUpgrader {
    public static final Component STATUS_EXTRACTING = Component.translatable("gui.wtem.main.extraction.working");
    public static final Component STATUS_FINISHED_EXTRACTION = Component.translatable("gui.wtem.main.extraction.finished");

    private final Minecraft mc;
    private final DataFixer dataFixer;
    private final WorldStem worldStem;
    private final LevelStorageSource.LevelStorageAccess levelStorage;
    private final RegistryAccess registry;
    private final StructureTemplateManager structureManager;

    public WorldExtractor(Minecraft mc,
                          DataFixer dataFixer,
                          WorldStem worldStem,
                          LevelStorageSource.LevelStorageAccess levelStorage,
                          RegistryAccess registry) {
        super(levelStorage, dataFixer, registry, false, false);
        this.mc = mc;
        this.dataFixer = dataFixer;
        this.worldStem = worldStem;
        this.levelStorage = levelStorage;
        this.registry = registry;
        HolderGetter<Block> holderGetter = this.registry.lookupOrThrow(Registries.BLOCK).filterFeatures(this.worldStem.worldData().enabledFeatures());
        this.structureManager = new StructureTemplateManager(this.worldStem.resourceManager(), levelStorage, dataFixer, holderGetter);
    }

    public void startThread() {
        ((AccessorWorldUpgrader) this).getThread().start();
    }

    @Override
    public void work() {
        var thiz = (AccessorWorldUpgrader) this;

        new ChunkExtractor().upgrade();
        new EntityExtractor().upgrade();
        extractScoreBoard(thiz.getOverworldDataStorage());
        extractBossBar(this.levelStorage, this.registry, this.worldStem.worldData());
        extractDatapacks(this.worldStem.resourceManager(), this.levelStorage, this.structureManager);
        extractStructures(this.structureManager);
        thiz.setFinished(true);
    }

    public static void extractStructures(StructureTemplateManager manager) {
        var managerMixin = (MixinStructureTemplateManager) manager;
        managerMixin.invokeListGenerated().forEach(rl -> {
            var optional = manager.get(rl);
            if (optional.isEmpty()) return;
            CompoundTag modified = new StructureTemplateWHandler().handle(optional.get());
            Path filePath = FileUtil.createPathToResource(
                    managerMixin.getGeneratedDir().resolve(rl.getNamespace()).resolve("structures"),
                    rl.getPath(), ".nbt"
            );
            Utils.writeNbt(filePath, modified);
        });
    }

    public static void extractDatapacks(ResourceManager resourceManager,
                                        LevelStorageSource.LevelStorageAccess levelStorage,
                                        StructureTemplateManager structureManager) {
        final var datapackDir = levelStorage.getLevelPath(LevelResource.DATAPACK_DIR);

        for (PackResources pack : resourceManager.listPacks().toList()) {
            String packId = pack.packId();
            if ("vanilla".equals(packId)) continue;
            String oPackId = packId.split("/")[1];
            try (var stream = Files.newDirectoryStream(datapackDir.resolve(oPackId + "/data"))) {
                for (Path namespacePath : stream) {
                    String namespace = namespacePath.getFileName().toString();
                    Function<ResourceLocation, Path> filePath = rl -> datapackDir.resolve(oPackId + "_wtem/data/" + rl.getNamespace() + "/" + rl.getPath());
                    ResourceHandler.Context context = ResourceHandler.Context.of(null, structureManager);
                    ResourceHandlers.getStream().forEachOrdered(factory -> {
                        var handler = factory.newHandler(filePath, context);
                        pack.listResources(PackType.SERVER_DATA, namespace, handler.getPath(), handler::handle);
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
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

    class ChunkExtractor extends WorldUpgrader.AbstractUpgrader<ChunkStorage> {
        private final BlockEntityWHandler beHandler = new BlockEntityWHandler();
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

            ListTag blockEntities = compoundTag.getList("block_entities", Tag.TAG_COMPOUND);
            Wtem.LOGGER.info("blockEntities = {}", blockEntities);
            for (int i = 0; i < blockEntities.size(); i++) {
                isUpdated |= beHandler.handle(blockEntities.getCompound(i));
            }
            Wtem.LOGGER.info("updated blockEntities = {}", blockEntities);

            if (!isUpdated) return false;

            if (this.previousWriteFuture != null) {
                this.previousWriteFuture.join();
            }
            this.previousWriteFuture = chunkStorage.write(chunkPos, () -> compoundTag);
            return true;
        }
    }

    class EntityExtractor extends WorldUpgrader.SimpleRegionStorageUpgrader {
        private final EntityWHandler entityHandler = new EntityWHandler();
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

            ListTag entities = compoundTag.getList("Entities", Tag.TAG_COMPOUND);
            Wtem.LOGGER.info("Entities = {}", entities);
            for (int i = 0; i < entities.size(); i++) {
                isUpdated |= entityHandler.handle(entities.getCompound(i));
            }
            Wtem.LOGGER.info("updated Entities = {}", entities);

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
