package me.fengming.wtem.mc1214.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import com.mojang.datafixers.DataFixer;
import me.fengming.wtem.mc1214.Wtem;
import me.fengming.wtem.mc1214.core.handler.BlockEntityWHandler;
import me.fengming.wtem.mc1214.core.handler.EntityWHandler;
import me.fengming.wtem.mc1214.core.handler.StructureTemplateWHandler;
import me.fengming.wtem.mc1214.core.misc.CustomScoreBoard;
import me.fengming.wtem.mc1214.mixin.MixinServerFunctionLibrary;
import me.fengming.wtem.mc1214.mixin.MixinStructureTemplateManager;
import me.fengming.wtem.mc1214.mixin.MixinWorldUpgrader;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class WorldExtractor extends WorldUpgrader {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final FileToIdConverter STRUCTURE_CONVERTOR = new FileToIdConverter("structure", ".nbt");
    public static final CommandSourceStack EMPTY_SOURCE = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 3, "", CommonComponents.EMPTY, null, null);

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

    @Override
    public void work() {
        var thiz = (MixinWorldUpgrader) this;
        var datapack = this.worldStem.dataPackResources();

        new ChunkExtractor().upgrade();
        new EntityExtractor().upgrade();
        extractScoreBoard(thiz.getOverworldDataStorage());
        extractBossBar(this.levelStorage, this.registry, this.worldStem.worldData());
        extractDatapacks(datapack, this.worldStem.resourceManager(), this.levelStorage, this.structureManager);
        extractStructures(this.structureManager);
        thiz.setFinished(true);
    }

    public static void extractStructures(StructureTemplateManager manager) {
        var managerMixin = (MixinStructureTemplateManager) manager;
        managerMixin.invokeListGenerated().forEach(rl -> {
            var optional = manager.get(rl);
            if (optional.isEmpty()) return;
            CompoundTag modified = new StructureTemplateWHandler().handle(optional.get());
            Path filePath = FileUtil.createPathToResource(managerMixin.getGeneratedDir().resolve(rl.getNamespace()).resolve("structures"), rl.getPath(), ".nbt");
            Utils.writeNbt(filePath, modified);
        });
    }

    public static void extractDatapacks(ReloadableServerResources datapacks,
                                        ResourceManager resourceManager,
                                        LevelStorageSource.LevelStorageAccess levelStorage,
                                        StructureTemplateManager structureManager) {
        final Function<String, ParseResults<CommandSourceStack>> parser =
                line -> ((MixinServerFunctionLibrary) datapacks.getFunctionLibrary()).getDispatcher().parse(line, EMPTY_SOURCE);
        final var datapackDir = levelStorage.getLevelPath(LevelResource.DATAPACK_DIR);

        for (PackResources pack : resourceManager.listPacks().toList()) {
            String packId = pack.packId();
            if ("vanilla".equals(packId)) continue;
            String oPackId = packId.split("/")[1];
            try (var stream = Files.newDirectoryStream(datapackDir.resolve(oPackId + "/data"))) {
                for (Path namespacePath : stream) {
                    String namespace = namespacePath.getFileName().toString();
                    Function<ResourceLocation, Path> filePath = rl -> datapackDir.resolve(oPackId + "_wtem/data/" + rl.getNamespace() + "/" + rl.getPath());
                    pack.listResources(PackType.SERVER_DATA, namespace, "structure", (rl, supplier) -> {
                        StructureTemplate structure = structureManager.get(STRUCTURE_CONVERTOR.fileToId(rl)).orElse(null);
                        CompoundTag modified = new StructureTemplateWHandler().handle(structure);
                        Utils.writeNbt(filePath.apply(rl), modified);
                    });
                    pack.listResources(PackType.SERVER_DATA, namespace, "function", (rl, supplier) -> {
                        String modified = processFunction(supplier, parser);
                        Utils.writeLines(filePath.apply(rl), modified);
                    });
                    pack.listResources(PackType.SERVER_DATA, namespace, "advancement", (rl, supplier) -> {
                        String modified = processJsonFile(supplier, List.of("display.title", "display.description"));
                        Utils.writeLines(filePath.apply(rl), modified);
                    });
                    pack.listResources(PackType.SERVER_DATA, namespace, "enchantment", (rl, supplier) -> {
                        String modified = processJsonFile(supplier, List.of("description"));
                        Utils.writeLines(filePath.apply(rl), modified);
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static String processJsonFile(IoSupplier<InputStream> supplier, List<String> list) {
        String jsonString = "";
        try (var br = new InputStreamReader(supplier.get(), StandardCharsets.UTF_8)) {
            var jsonObj = JsonParser.parseReader(br).getAsJsonObject();
            for (String s : list) {
                Utils.handleJsonElement(jsonObj, s);
            }
            jsonString = GSON.toJson(jsonObj);
        } catch (Exception e) {
            Wtem.LOGGER.error("Failed to parse JSON", e);
        }
        return jsonString;
    }

    public static String processFunction(IoSupplier<InputStream> supplier, Function<String, ParseResults<CommandSourceStack>> parser) {
        List<String> lines;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8))) {
            lines = bufferedReader.lines().toList();
        } catch (IOException e) {
            return "";
        }

        List<String> modified = new ArrayList<>();
        for (int i = 0, size = lines.size(); i < size; i++) {
            String line = lines.get(i).trim();
            if (!lineNeedReplace(line)) continue;

            String finalLine;
            if (line.endsWith("\\")) {
                StringBuilder sb1 = new StringBuilder(line);
                do {
                    if (++i >= size) throw new IllegalArgumentException("Line continuation at end of file");
                    sb1.deleteCharAt(sb1.length() - 1);
                    sb1.append(lines.get(i).trim());
                } while (sb1.toString().endsWith("\\"));
                finalLine = sb1.toString();
            } else {
                finalLine = line;
            }

            var results = parser.apply(finalLine);
            StringBuilder sb2 = new StringBuilder();
            Optional<ParsedArgument<CommandSourceStack, ?>> optional;
            while ((optional = getComponentArg(results)).isPresent()) {
                var arg = optional.get();
                StringRange range = arg.getRange();
                sb2.append(line, 0, range.getStart())
                        .append(Utils.translatable2String((Component) arg.getResult()))
                        .append(line.substring(range.getEnd()));
                line = sb2.toString();
                results = parser.apply(line);
            }
            modified.add(line);
        }
        return String.join("", modified);
    }

    private static Optional<ParsedArgument<CommandSourceStack, ?>> getComponentArg(ParseResults<CommandSourceStack> results) {
        return results.getContext().getArguments().values().stream()
                .filter(arg -> arg.getResult() instanceof Component c && c.getContents().type() == PlainTextContents.TYPE)
                .findFirst();
    }

    private static boolean lineNeedReplace(String line) {
        return line.startsWith("bossbar") || line.startsWith("scoreboard") ||
                line.startsWith("team") || line.startsWith("tellraw") ||
                line.startsWith("title");
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
