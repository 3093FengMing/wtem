package me.fengming.wtem.common.core.datapack;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.StringRange;
import me.fengming.wtem.common.core.Utils;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author FengMing
 */
public class FunctionHandler extends NonExtraResourceHandler {
    public static final HandlerFactory FACTORY = FunctionHandler::new;

    public FunctionHandler(Function<ResourceLocation, Path> filePath, Context context) {
        super("function", filePath);
    }

    @Override
    public void handle(ResourceLocation rl, IoSupplier<InputStream> supplier) {
        Utils.writeLines(getFilePath(rl), processFunction(supplier));
    }

    private static String processFunction(IoSupplier<InputStream> supplier) {
        List<String> lines;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(supplier.get(), StandardCharsets.UTF_8))) {
            lines = bufferedReader.lines().toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return processFunction(lines);
    }

    public static String processFunction(List<String> lines) {
        final Function<String, ParseResults<CommandSourceStack>> parser =
                line -> new Commands(
                        Commands.CommandSelection.ALL,
                        Commands.createValidationContext(VanillaRegistries.createLookup())
                ).getDispatcher().parse(line, new CommandSourceStack(
                        CommandSource.NULL, Vec3.ZERO, Vec2.ZERO,
                        null, 3, "WTEM",
                        CommonComponents.EMPTY, null, null)
                );

        List<String> modified = new ArrayList<>();
        for (int i = 0, size = lines.size(); i < size; i++) {
            String line = lines.get(i).trim();
            if (!lineNeedReplace(line)) continue;

            String finalLine = line;
            if (line.endsWith("\\")) {
                StringBuilder sb1 = new StringBuilder(line);
                do {
                    if (++i >= size) throw new IllegalArgumentException("Line continuation at end of file");
                    sb1.deleteCharAt(sb1.length() - 1);
                    sb1.append(lines.get(i).trim());
                } while (sb1.toString().endsWith("\\"));
                finalLine = sb1.toString();
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
}
