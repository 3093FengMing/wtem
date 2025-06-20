package me.fengming.wtem.common.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.apache.commons.compress.utils.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class WorldExtractor {
    private Minecraft mc;
    private LevelStorageSource levelSource;

    public WorldExtractor(Minecraft mc, LevelStorageSource levelSource) {
        this.mc = mc;
        this.levelSource = levelSource;
    }

    public void openWorld(String worldName) {
        try (LevelStorageSource.LevelStorageAccess levelStorageAccess = this.levelSource.validateAndCreateAccess(worldName)) {
            // Will skip all the checks
            Path levelPath = levelStorageAccess.getLevelDirectory().path();
            readWorld(levelPath);
        } catch (IOException | ContentValidationException e) {
            SystemToast.onWorldAccessFailure(this.mc, worldName);
            this.mc.setScreen(null);
        }
    }

    private void readWorld(Path worldPath) {
        List<Path> dimensions = readDimensions(worldPath);
        dimensions.forEach(System.out::println);
    }

    private static List<Path> readDimensions(Path worldPath) {
        List<Path> dimensions = new ArrayList<>();
        dimensions.add(worldPath); // overworld
        dimensions.add(worldPath.resolve("DIM1")); // the_end
        dimensions.add(worldPath.resolve("DIM-1")); // nether

        Path customs = worldPath.resolve("dimensions");
        if (Files.notExists(customs)) return dimensions;

        try (var paths = Files.walk(customs, 2)) {
            paths.forEach(e -> {
                if (Files.isDirectory(e)) dimensions.add(e);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return dimensions;
    }

}
