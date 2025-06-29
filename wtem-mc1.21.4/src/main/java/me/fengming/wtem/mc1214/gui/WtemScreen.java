package me.fengming.wtem.mc1214.gui;

import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import me.fengming.wtem.mc1214.Wtem;
import me.fengming.wtem.mc1214.core.WorldExtractor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

/**
 * @author FengMing
 */
@Environment(EnvType.CLIENT)
public class WtemScreen extends Screen {
    public static final Component WTEM_SCREEN_TITLE = Component.translatable("gui.wtem.main.title");
    public static final Component WTEM_EXTRACT = Component.translatable("gui.wtem.extract");

    private Button extractButton;
    private final BooleanConsumer callback;
    private final WorldExtractor worldExtractor;

    public static WtemScreen create(Minecraft mc, BooleanConsumer callback, DataFixer dataFixer, LevelStorageSource.LevelStorageAccess levelStorage) {
        try {
            WorldOpenFlows worldOpenFlows = mc.createWorldOpenFlows();
            PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorage);

            WtemScreen wtemScreen;
            try (WorldStem worldStem = worldOpenFlows.loadWorldStem(levelStorage.getDataTag(), false, packRepository)) {
                RegistryAccess.Frozen frozen = worldStem.registries().compositeAccess();
                levelStorage.saveDataTag(frozen, worldStem.worldData());
                wtemScreen = new WtemScreen(mc, callback, dataFixer, worldStem, levelStorage, frozen);
            }
            return wtemScreen;
        } catch (Exception e) {
            Wtem.LOGGER.warn("Failed to load world, can't extract world", e);
        }
        return null;
    }

    private WtemScreen(Minecraft mc,
                       BooleanConsumer callback,
                       DataFixer dataFixer,
                       WorldStem worldStem,
                       LevelStorageSource.LevelStorageAccess levelStorage,
                       RegistryAccess registryAccess) {
        super(WTEM_SCREEN_TITLE);
        this.callback = callback;
        this.worldExtractor = new WorldExtractor(mc, dataFixer, worldStem, levelStorage, registryAccess);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.worldExtractor.cancel();
            this.callback.accept(false);
        }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());

        extractButton = this.addRenderableWidget(Button.builder(WTEM_EXTRACT, button -> {
            this.worldExtractor.startThread();
        }).bounds(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build());
    }

    @Override
    public void tick() {
        if (this.worldExtractor.isFinished()) {
            this.callback.accept(true);
        }
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @Override
    public void removed() {
        this.worldExtractor.cancel();
        this.worldExtractor.close();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        int left = this.width / 2 - 150;
        int right = this.width / 2 + 150;
        int bottom = this.height / 4 + 100;
        int top = bottom + 10;
        if (this.worldExtractor.getTotalChunks() > 0) {
            extractButton.visible = false;

            guiGraphics.fill(left - 1, bottom - 1, right + 1, top + 1, -16777216);
            guiGraphics.drawString(this.font, Component.translatable("gui.wtem.main.info.extracted", this.worldExtractor.getConverted()), left, 40, 10526880);
            guiGraphics.drawString(this.font, Component.translatable("gui.wtem.main.info.total", this.worldExtractor.getTotalChunks()), left, 40 + (9 + 3) * 2, 10526880);
            int process = 0;

            for (ResourceKey<Level> resourceKey : this.worldExtractor.levels()) {
                int n = Mth.floor(this.worldExtractor.dimensionProgress(resourceKey) * (right - left));
                guiGraphics.fill(left + process, bottom, left + process + n, top, -2236963 * resourceKey.hashCode());
                process += n;
            }

            int o = this.worldExtractor.getConverted() + this.worldExtractor.getSkipped();
            Component component = Component.translatable("gui.wtem.main.progress.counter", o, this.worldExtractor.getTotalChunks());
            Component component2 = Component.translatable("gui.wtem.main.progress.percentage", Mth.floor(this.worldExtractor.getProgress() * 100.0F));
            guiGraphics.drawCenteredString(this.font, component, this.width / 2, bottom + 2 * 9 + 2, 10526880);
            guiGraphics.drawCenteredString(this.font, component2, this.width / 2, bottom + (top - bottom) / 2 - 9 / 2, 10526880);
        }
    }
}
