package me.fengming.wtem.mc1214.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.ServerFunctionLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerFunctionLibrary.class)
public interface MixinServerFunctionLibrary {
    @Accessor("dispatcher")
    CommandDispatcher<CommandSourceStack> getDispatcher();
}
