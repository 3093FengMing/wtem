package me.fengming.wtem.mc1214.core;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.datafixers.DataFixer;
import me.fengming.wtem.mc1214.Wtem;
import me.fengming.wtem.mc1214.core.handler.BlockEntityWHandler;
import me.fengming.wtem.mc1214.core.handler.EntityWHandler;
import me.fengming.wtem.mc1214.mixin.MixinServerFunctionLibrary;
import me.fengming.wtem.mc1214.mixin.MixinWorldUpgrader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
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
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    }

    @Override
    public void work() {
        var thiz = (MixinWorldUpgrader) this;
        var datapack = this.worldStem.dataPackResources();

        new ChunkExtractor().upgrade();
        new EntityExtractor().upgrade();
        extractScoreBoard(thiz.getOverworldDataStorage());
        extractBossBar(this.levelStorage, this.registry, this.worldStem.worldData());
        extractFunctions(datapack.getFunctionLibrary(), this.worldStem.resourceManager(), this.levelStorage);
        extractAdvancements(datapack.getAdvancements());
        thiz.setFinished(true);
    }

    public static void extractFunctions(ServerFunctionLibrary functions, ResourceManager manager, LevelStorageSource.LevelStorageAccess levelStorage) {
        var dispatcher = ((MixinServerFunctionLibrary) functions).getDispatcher();
        var css = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 3, "", CommonComponents.EMPTY, null, null);
        Path datapackDir = levelStorage.getLevelPath(LevelResource.DATAPACK_DIR);

        for (PackResources pack : manager.listPacks().toList()) {
            String packId = pack.packId();
            if ("vanilla".equals(packId)) continue;
            String oPackId = packId.split("/")[1];
            try (var stream = Files.newDirectoryStream(datapackDir.resolve(oPackId + "/data"))) {
                for (Path namespacePath : stream) {
                    pack.listResources(PackType.SERVER_DATA, namespacePath.getFileName().toString(), "function", (rl, supplier) -> {
                        List<String> modified = replaceComponents(Utils.readLines(supplier, rl), line -> dispatcher.parse(line, css));
                        Path filePath = datapackDir.resolve(oPackId + "_wtem/data/" + rl.getNamespace() + "/" + rl.getPath());
                        Utils.writeLines(filePath, String.join("\n", modified));
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static List<String> replaceComponents(List<String> lines, Function<String, ParseResults<CommandSourceStack>> parser) {
        List<String> modified = new ArrayList<>();
        for (String line : lines) {
            if (!lineNeedReplace(line)) continue;

            var results = parser.apply(line);
            StringBuilder sb = new StringBuilder();
            Optional<ParsedArgument<CommandSourceStack, ?>> optional;
            while ((optional = getComponentArg(results)).isPresent()) {
                var arg = optional.get();
                var translatable = Utils.literal2Translatable((Component) arg.getResult());
                StringRange range = arg.getRange();
                sb.append(line, 0, range.getStart())
                        .append(Utils.component2String(translatable))
                        .append(line.substring(range.getEnd()));
                line = sb.toString();
                results = parser.apply(line);
            }
            modified.add(line);
        }
        return modified;
    }

    private static Optional<ParsedArgument<CommandSourceStack, ?>> getComponentArg(ParseResults<CommandSourceStack> results) {
        return results.getContext().getArguments().values().stream()
                .filter(WorldExtractor::argNeedReplace)
                .findFirst();
    }

    private static boolean argNeedReplace(ParsedArgument<?, ?> arg) {
        return arg.getResult() instanceof Component c && c.getContents().type() == PlainTextContents.TYPE;
    }

    private static boolean lineNeedReplace(String line) {
        return line.startsWith("bossbar") || line.startsWith("scoreboard") ||
                line.startsWith("team") || line.startsWith("tellraw") ||
                line.startsWith("title");
    }

    public static void extractAdvancements(ServerAdvancementManager manager) {
        for (AdvancementHolder advancement : manager.getAllAdvancements()) {
            DisplayInfo display = advancement.value().display().orElse(null);
            if (display == null) continue;
            Utils.literal2Translatable(display.getDescription());
            Utils.literal2Translatable(display.getTitle());
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
