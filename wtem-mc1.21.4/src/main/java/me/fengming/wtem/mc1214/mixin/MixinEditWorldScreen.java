package me.fengming.wtem.mc1214.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.fengming.wtem.mc1214.gui.WtemScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SelectWorldScreen.class)
public class MixinSelectWorldScreen extends Screen {
    @Shadow private WorldSelectionList list;

    @Unique private Button translateButton;

    protected MixinSelectWorldScreen(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/SelectWorldScreen;updateButtonStatus(Lnet/minecraft/world/level/storage/LevelSummary;)V"))
    private void onInit(CallbackInfo ci) {
        this.translateButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.wtem.translate"), button -> this.list.getSelectedOpt().ifPresent(this::wtem$openWtemScreen))
                .bounds(this.width / 2 - 234, this.height - 28, 72, 20)
                .build());
    }

    @Inject(method = "updateButtonStatus", at = @At("TAIL"))
    private void onUpdateButtonStatus(CallbackInfo ci, @Local(argsOnly = true) LevelSummary levelSummary) {
        if (levelSummary == null) {
            this.translateButton.active = false;
        } else {
            this.translateButton.active = levelSummary.canEdit();
        }
    }

    @Unique
    private void wtem$openWtemScreen(WorldSelectionList.WorldListEntry worldListEntry) {
        String levelId = ((LevelSummaryAccessor) ((WorldListEntryAccessor) (Object) worldListEntry).getSummary()) .getLevelId();
        this.minecraft.setScreen(
                new ConfirmScreen(b -> {
                    if (b) {
                        this.minecraft.setScreen(new WtemScreen(levelId, Component.translatable("gui.wtem.main.title")));
                    } else {
                        this.minecraft.setScreen(this);
                    }
                }, Component.translatable("gui.wtem.main.warn"),
                        Component.translatable("gui.wtem.main.warn"),
                        CommonComponents.GUI_CONTINUE,
                        CommonComponents.GUI_CANCEL)
        );
    }
}
