/*
 * Brecher's Dimensions - Temporary resettable dimensions for exploration
 * Copyright (C) 2025 Einbrecher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.tinkstav.brecher_dim.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.debug.RegistryFieldDiagnostics;
import net.tinkstav.brecher_dim.util.AdvancementLockChecker;
import net.tinkstav.brecher_dim.util.DimensionCounterUtil;
import net.tinkstav.brecher_dim.generation.ChunkPreGenerator;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.*;

public class BrecherCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Suggestion provider for base dimensions only
    private static final SuggestionProvider<CommandSourceStack> BASE_DIMENSION_SUGGESTIONS = 
        (context, builder) -> {
            List<String> enabledDimensions = BrecherConfig.getEnabledDimensions();
            return SharedSuggestionProvider.suggest(enabledDimensions, builder);
        };
    
    /**
     * Register all commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main exploration command structure
        dispatcher.register(
            Commands.literal("exploration")
                .then(Commands.literal("list")
                    .executes(ctx -> listDimensions(ctx)))
                .then(Commands.literal("tp")
                    .then(Commands.argument("dimension", StringArgumentType.greedyString())
                        .suggests(BASE_DIMENSION_SUGGESTIONS)
                        .executes(ctx -> teleportToDimension(ctx))))
                .then(Commands.literal("info")
                    .executes(ctx -> showInfo(ctx)))
                .then(Commands.literal("return")
                    .executes(ctx -> returnFromExploration(ctx)))
                .then(Commands.literal("help")
                    .executes(ctx -> showHelp(ctx)))
        );
        
        // Admin commands
        dispatcher.register(
            Commands.literal("explorationadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("returnall")
                    .executes(ctx -> returnAllPlayers(ctx)))
                .then(Commands.literal("info")
                    .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> showDimensionInfoArg(ctx))))
                .then(Commands.literal("stats")
                    .executes(ctx -> showStats(ctx)))
                .then(Commands.literal("debug")
                    .then(Commands.literal("registry")
                        .executes(ctx -> debugRegistry(ctx)))
                    .then(Commands.literal("compass")
                        .executes(ctx -> debugCompass(ctx))))
                .then(Commands.literal("counter")
                    .then(Commands.literal("show")
                        .executes(ctx -> showCounters(ctx)))
                    .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                            .executes(ctx -> resetAllCounters(ctx)))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> resetDimensionCounter(ctx)))))
                .then(Commands.literal("pregen")
                    .then(Commands.literal("start")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> startPregen(ctx))
                            .then(Commands.argument("radius", StringArgumentType.string())
                                .executes(ctx -> startPregenWithRadius(ctx)))))
                    .then(Commands.literal("stop")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> stopPregen(ctx))))
                    .then(Commands.literal("pause")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> pausePregen(ctx))))
                    .then(Commands.literal("resume")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> resumePregen(ctx))))
                    .then(Commands.literal("status")
                        .executes(ctx -> pregenStatus(ctx))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> pregenStatusDimension(ctx))))
                    .then(Commands.literal("stopall")
                        .executes(ctx -> stopAllPregen(ctx))))
                // Manual unlock management (progression gating)
                .then(Commands.literal("unlock")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                            .suggests(BASE_DIMENSION_SUGGESTIONS)
                            .executes(ctx -> grantManualUnlock(ctx)))))
                .then(Commands.literal("lock")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                            .suggests(BASE_DIMENSION_SUGGESTIONS)
                            .executes(ctx -> revokeManualUnlock(ctx)))))
                .then(Commands.literal("unlocks")
                    .executes(ctx -> listManualUnlocks(ctx, null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> listManualUnlocksForPlayer(ctx))))
        );
    }
    
    // --- Player Commands ---
    
    private static int teleportToDimension(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String dimensionName = StringArgumentType.getString(ctx, "dimension");
        
        return teleportToExploration(ctx, dimensionName);
    }
    
    private static int teleportToExploration(CommandContext<CommandSourceStack> ctx, String dimensionName) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        
        if (manager == null) {
            player.displayClientMessage(
                Component.literal("Exploration dimensions are not available yet!")
                    .withStyle(ChatFormatting.RED),
                false
            );
            return 0;
        }
        
        // Parse dimension name - use tryParse to handle invalid input gracefully
        ResourceLocation baseDim = ResourceLocation.tryParse(dimensionName);
        if (baseDim == null) {
            player.displayClientMessage(
                Component.literal("Invalid dimension name: " + dimensionName)
                    .withStyle(ChatFormatting.RED),
                false
            );
            return 0;
        }
        
        // Check if dimension is enabled
        List<String> enabledDims = BrecherConfig.getEnabledDimensions();
        if (!enabledDims.contains(baseDim.toString())) {
            player.displayClientMessage(
                Component.literal("This dimension is not enabled for exploration!")
                    .withStyle(ChatFormatting.RED),
                false
            );
            return 0;
        }

        // Check dimension locks (advancement requirements) - admin bypass at permission level 2+
        if (!player.hasPermissions(2) && BrecherConfig.isDimensionLocksEnabled()) {
            AdvancementLockChecker.LockCheckResult lockResult = AdvancementLockChecker.getDimensionLockStatus(player, baseDim);
            if (lockResult.status() == AdvancementLockChecker.LockStatus.LOCKED) {
                // Player is locked - show informative message
                String advName = lockResult.advancementDisplayName()
                    .map(Component::getString)
                    .orElse(lockResult.requiredAdvancementId().orElse("unknown"));

                player.displayClientMessage(
                    Component.literal("You must complete '")
                        .append(Component.literal(advName).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("' to explore this dimension!"))
                        .withStyle(ChatFormatting.RED),
                    false
                );
                player.displayClientMessage(
                    Component.literal("Visit " + formatDimensionName(baseDim) + " in the normal world first.")
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
                LOGGER.debug("Player {} blocked from {} - missing advancement {}",
                    player.getName().getString(), baseDim, lockResult.requiredAdvancementId().orElse("unknown"));
                return 0;
            }
        }

        // Check dimension restriction if enabled
        if (BrecherConfig.isRestrictToCurrentDimension()) {
            ResourceLocation playerCurrentDim = player.level().dimension().location();
            ResourceLocation playerBaseDim = getBaseDimension(playerCurrentDim, manager);
            
            if (!playerBaseDim.equals(baseDim)) {
                player.displayClientMessage(
                    Component.literal("You can only teleport to the exploration version of your current dimension!")
                        .withStyle(ChatFormatting.RED),
                    false
                );
                player.displayClientMessage(
                    Component.literal("Current dimension: " + playerBaseDim.getPath() + " → Allowed: exploration_" + playerBaseDim.getPath())
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
                return 0;
            }
        }
        
        // Get exploration dimension
        LOGGER.debug("Looking for exploration dimension for base: {}", baseDim);
        Optional<ServerLevel> explorationOpt = manager.getExplorationDimension(baseDim);
        if (explorationOpt.isEmpty()) {
            player.displayClientMessage(
                Component.literal("Could not find exploration dimension for " + baseDim)
                    .withStyle(ChatFormatting.RED),
                false
            );
            
            // Debug info
            Map<String, Object> stats = manager.getStatistics();
            player.displayClientMessage(
                Component.literal("Available dimensions: " + stats.get("totalMappings"))
                    .withStyle(ChatFormatting.YELLOW),
                false
            );
            
            LOGGER.debug("Failed to find exploration dimension. Stats: {}", stats);
            return 0;
        }
        
        ServerLevel exploration = explorationOpt.get();
        
        // Check if already in exploration dimension
        ResourceLocation playerDim = player.level().dimension().location();
        ResourceLocation targetDim = exploration.dimension().location();
        
        LOGGER.debug("Teleport check - Player in: {}, target: {}, same: {}", 
            playerDim, targetDim, playerDim.equals(targetDim));
            
        if (manager.isExplorationDimension(playerDim)) {
            // Check if exploration-to-exploration teleportation is allowed
            if (BrecherConfig.isRestrictToCurrentDimension()) {
                player.displayClientMessage(
                    Component.literal("You cannot teleport between exploration dimensions!")
                        .withStyle(ChatFormatting.RED),
                    false
                );
                player.displayClientMessage(
                    Component.literal("Use /exploration return to go back first.")
                        .withStyle(ChatFormatting.YELLOW),
                    false
                );
                return 0;
            }
            // Otherwise, allow exploration-to-exploration teleportation
        }
        
        if (playerDim.equals(targetDim)) {
            player.displayClientMessage(
                Component.literal("You are already in that dimension!")
                    .withStyle(ChatFormatting.YELLOW),
                false
            );
            return 0;
        }
        
        // Teleport player
        TeleportHandler.teleportToExploration(player, exploration);
        return 1;
    }
    
    private static int returnFromExploration(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        
        ResourceLocation currentDim = player.level().dimension().location();
        boolean isInExploration = manager != null && manager.isExplorationDimension(currentDim);
        
        LOGGER.debug("Return command - Player {} in dimension: {}, is exploration: {}", 
            player.getName().getString(), currentDim, isInExploration);
        
        if (!isInExploration) {
            player.displayClientMessage(
                Component.literal("You are not in an exploration dimension!")
                    .withStyle(ChatFormatting.RED),
                false
            );
            return 0;
        }
        
        TeleportHandler.returnFromExploration(player);
        return 1;
    }
    
    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        DimensionRegistrar registrar = BrecherDimensions.getDimensionRegistrar();
        
        source.sendSuccess(() -> Component.literal("=== Brecher's Exploration Dimensions Info ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        source.sendSuccess(() -> Component.literal("Seed Strategy: ")
            .append(Component.literal(BrecherConfig.getSeedStrategy())
                .withStyle(ChatFormatting.AQUA)), false);
        
        long debugSeed = BrecherConfig.getDebugSeed();
        if (debugSeed != -1) {
            source.sendSuccess(() -> Component.literal("Debug Seed: ")
                .append(Component.literal(String.valueOf(debugSeed))
                    .withStyle(ChatFormatting.RED)), false);
        }
        
        source.sendSuccess(() -> Component.literal("\nExploration dimensions will be replaced on server restart!")
            .withStyle(ChatFormatting.YELLOW), false);

        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal("=== Brecher's Exploration Dimensions ===")
            .withStyle(ChatFormatting.GOLD), false);

        source.sendSuccess(() -> Component.literal(""), false);

        // /exploration list
        source.sendSuccess(() -> Component.literal("/exploration list")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/exploration list"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to run"))))
            .append(Component.literal(" - Show available exploration dimensions")
                .withStyle(ChatFormatting.GRAY)), false);

        // /exploration tp
        source.sendSuccess(() -> Component.literal("/exploration tp <dimension>")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/exploration tp "))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to start typing"))))
            .append(Component.literal(" - Teleport to an exploration dimension")
                .withStyle(ChatFormatting.GRAY)), false);

        // /exploration return
        source.sendSuccess(() -> Component.literal("/exploration return")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/exploration return"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to run"))))
            .append(Component.literal(" - Return to your saved position")
                .withStyle(ChatFormatting.GRAY)), false);

        // /exploration info
        source.sendSuccess(() -> Component.literal("/exploration info")
            .withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/exploration info"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Click to run"))))
            .append(Component.literal(" - Show seed and reset information")
                .withStyle(ChatFormatting.GRAY)), false);

        source.sendSuccess(() -> Component.literal(""), false);
        source.sendSuccess(() -> Component.literal("Exploration dimensions reset on server restart.")
            .withStyle(ChatFormatting.YELLOW), false);

        return 1;
    }

    private static int listDimensions(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        DimensionRegistrar registrar = BrecherDimensions.getDimensionRegistrar();

        source.sendSuccess(() -> Component.literal("=== Available Exploration Dimensions ===")
            .withStyle(ChatFormatting.GOLD), false);

        if (registrar == null) {
            source.sendSuccess(() -> Component.literal("Dimensions not yet initialized!")
                .withStyle(ChatFormatting.RED), false);
            return 0;
        }

        List<String> dimensionInfo = registrar.getDimensionInfo();
        if (dimensionInfo.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No exploration dimensions are currently active.")
                .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("They will be created on server restart.")
                .withStyle(ChatFormatting.GRAY), false);
        } else {
            // Check if the source is a player for lock checking
            ServerPlayer player = null;
            boolean isAdmin = false;
            try {
                player = source.getPlayerOrException();
                isAdmin = player.hasPermissions(2);
            } catch (CommandSyntaxException e) {
                // Not a player (console), show all as available
            }

            boolean locksEnabled = BrecherConfig.isDimensionLocksEnabled();
            boolean hasLockedDimensions = false;

            for (String info : dimensionInfo) {
                // Parse the info string to get the base dimension
                String baseDim = info.split(" -> ")[0];
                String friendlyName = getFriendlyDimensionName(baseDim);
                ResourceLocation baseDimLoc = ResourceLocation.tryParse(baseDim);

                // Check lock status if player and locks enabled
                boolean isLocked = false;
                String lockReason = null;

                if (player != null && locksEnabled && !isAdmin && baseDimLoc != null) {
                    AdvancementLockChecker.LockCheckResult lockResult =
                        AdvancementLockChecker.getDimensionLockStatus(player, baseDimLoc);

                    if (lockResult.status() == AdvancementLockChecker.LockStatus.LOCKED) {
                        isLocked = true;
                        hasLockedDimensions = true;
                        lockReason = lockResult.advancementDisplayName()
                            .map(Component::getString)
                            .orElse(lockResult.requiredAdvancementId().orElse("unknown advancement"));
                    }
                }

                Component dimComponent;
                if (isLocked) {
                    // Locked dimension: gray text with [LOCKED] indicator, no click action
                    final String reason = lockReason;
                    dimComponent = Component.literal(" x ")
                        .withStyle(ChatFormatting.RED)
                        .append(Component.literal(friendlyName)
                            .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GRAY)
                                .withStrikethrough(true)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Locked - Requires: ")
                                        .withStyle(ChatFormatting.RED)
                                        .append(Component.literal(reason)
                                            .withStyle(ChatFormatting.GOLD))))));
                } else {
                    // Unlocked dimension: aqua text, clickable
                    dimComponent = Component.literal(" • " + friendlyName)
                        .withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/exploration tp " + baseDim))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to explore " + friendlyName))));
                }

                source.sendSuccess(() -> dimComponent, false);
            }

            if (hasLockedDimensions) {
                source.sendSuccess(() -> Component.literal("\nx = Locked (complete required advancements)")
                    .withStyle(ChatFormatting.GRAY), false);
            }

            source.sendSuccess(() -> Component.literal("\nClick a dimension to explore it!")
                .withStyle(ChatFormatting.GRAY), false);
        }

        return 1;
    }
    
    private static String getFriendlyDimensionName(String dimensionId) {
        // Handle namespaced dimensions
        if (dimensionId.contains(":")) {
            String[] parts = dimensionId.split(":");
            String namespace = parts[0];
            String path = parts[1];
            
            // Handle vanilla dimensions
            if ("minecraft".equals(namespace)) {
                switch (path) {
                    case "overworld":
                        return "Overworld";
                    case "the_nether":
                        return "The Nether";
                    case "the_end":
                        return "The End";
                    default:
                        // For other minecraft dimensions, capitalize the path
                        return capitalizeWords(path.replace("_", " "));
                }
            }
            
            // For modded dimensions, show namespace and capitalize path
            return namespace + ":" + capitalizeWords(path.replace("_", " "));
        }
        
        // If no namespace, just capitalize
        return capitalizeWords(dimensionId.replace("_", " "));
    }
    
    private static String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    // --- Admin Commands ---
    
    private static int returnAllPlayers(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        
        if (manager == null) {
            source.sendFailure(Component.literal("Dimension manager not available!"));
            return 0;
        }
        
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (manager.isExplorationDimension(player.level().dimension().location())) {
                TeleportHandler.returnFromExploration(player);
                count++;
            }
        }
        
        int finalCount = count;
        source.sendSuccess(() -> Component.literal("Returned " + finalCount + " players to their home dimensions")
            .withStyle(ChatFormatting.GREEN), true);
        
        return count;
    }
    
    private static int showDimensionInfoArg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        return showDimensionInfo(ctx, dimension.dimension().location().toString());
    }
    
    private static int showDimensionInfo(CommandContext<CommandSourceStack> ctx, String dimensionName) {
        CommandSourceStack source = ctx.getSource();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        DimensionRegistrar registrar = BrecherDimensions.getDimensionRegistrar();
        
        // Use tryParse to handle invalid input gracefully instead of throwing
        ResourceLocation dimLoc = ResourceLocation.tryParse(dimensionName);
        if (dimLoc == null) {
            source.sendFailure(Component.literal("Invalid dimension name: " + dimensionName));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Dimension Info: " + dimLoc + " ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        // Check if it's an exploration dimension
        if (manager != null && manager.isExplorationDimension(dimLoc)) {
            ServerLevel level = source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));
            if (level != null) {
                source.sendSuccess(() -> Component.literal("Status: ACTIVE")
                    .withStyle(ChatFormatting.GREEN), false);
                source.sendSuccess(() -> Component.literal("Players: " + level.players().size()), false);
                source.sendSuccess(() -> Component.literal("Loaded Chunks: " + level.getChunkSource().getLoadedChunksCount()), false);
                
                // Show seed
                ResourceKey<Level> dimKey = level.dimension();
                registrar.getDimensionSeed(dimKey).ifPresent(seed -> {
                    source.sendSuccess(() -> Component.literal("Seed: " + seed)
                        .withStyle(ChatFormatting.AQUA), false);
                });
                
                // Count entities
                final int count = level.getEntities(
                    EntityTypeTest.forClass(net.minecraft.world.entity.Entity.class),
                    entity -> true
                ).size();
                source.sendSuccess(() -> Component.literal("Entities: " + count), false);
            } else {
                source.sendSuccess(() -> Component.literal("Status: REGISTERED")
                    .withStyle(ChatFormatting.YELLOW), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("Not an exploration dimension")
                .withStyle(ChatFormatting.RED), false);
        }
        
        return 1;
    }
    
    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        BrecherDimensionManager manager = BrecherDimensions.getDimensionManager();
        
        if (manager == null) {
            source.sendFailure(Component.literal("Dimension manager not available!"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Exploration Dimension Statistics ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        Map<String, Object> stats = manager.getStatistics();
        
        source.sendSuccess(() -> Component.literal("Total Mappings: " + stats.get("totalMappings")), false);
        source.sendSuccess(() -> Component.literal("Active Dimensions: " + stats.get("activeDimensions")), false);
        source.sendSuccess(() -> Component.literal("Players in Exploration: " + stats.get("playersInExploration")), false);
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> perDimStats = (Map<String, Integer>) stats.get("perDimensionPlayers");
        if (!perDimStats.isEmpty()) {
            source.sendSuccess(() -> Component.literal("\nPer-Dimension Players:")
                .withStyle(ChatFormatting.YELLOW), false);
            perDimStats.forEach((dim, count) -> {
                source.sendSuccess(() -> Component.literal("  " + dim + ": " + count), false);
            });
        }
        
        return 1;
    }
    
    // --- Debug Commands ---
    
    private static int debugRegistry(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Registry Diagnostics ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        try {
            // Phase 1: Basic registry field analysis
            source.sendSuccess(() -> Component.literal("Phase 1: Analyzing registry structure...")
                .withStyle(ChatFormatting.YELLOW), false);
            
            RegistryFieldDiagnostics.analyzeMappedRegistryFields();
            RegistryFieldDiagnostics.testCurrentShadowFields();
            RegistryFieldDiagnostics.identifyFieldMappingIssues();
            
            // Phase 2: Test MixinRegistryFixed functionality
            source.sendSuccess(() -> Component.literal("Phase 2: Testing MixinRegistryFixed...")
                .withStyle(ChatFormatting.YELLOW), false);
            
            boolean mixinSuccess = testMixinRegistryFixed(source);
            
            // Phase 3: Registry state validation
            source.sendSuccess(() -> Component.literal("Phase 3: Validating registry state...")
                .withStyle(ChatFormatting.YELLOW), false);
            
            boolean stateSuccess = validateRegistryStates(source);
            
            // Summary
            if (mixinSuccess && stateSuccess) {
                source.sendSuccess(() -> Component.literal("✓ All registry diagnostics PASSED")
                    .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() -> Component.literal("⚠ Some registry diagnostics FAILED")
                    .withStyle(ChatFormatting.RED), false);
            }
            
            source.sendSuccess(() -> Component.literal("Check server logs for detailed analysis.")
                .withStyle(ChatFormatting.AQUA), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Registry diagnostics failed: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    private static int debugCompass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        
        source.sendSuccess(() -> Component.literal("=== Compass Debug Information ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        // Show current dimension info
        ResourceKey<Level> currentDim = level.dimension();
        boolean isExploration = BrecherDimensionManager.isExplorationDimension(currentDim);
        
        source.sendSuccess(() -> Component.literal("Current dimension: " + currentDim.location())
            .withStyle(ChatFormatting.YELLOW), false);
        
        source.sendSuccess(() -> Component.literal("Is exploration dimension: " + 
            (isExploration ? "YES" : "NO"))
            .withStyle(isExploration ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        
        if (isExploration) {
            Optional<ResourceKey<Level>> parentDim = BrecherDimensionManager.getParentDimension(currentDim);
            source.sendSuccess(() -> Component.literal("Parent dimension: " + 
                parentDim.map(k -> k.location().toString()).orElse("unknown"))
                .withStyle(ChatFormatting.AQUA), false);
            
            // Show seed comparison
            if (parentDim.isPresent()) {
                ServerLevel parentLevel = source.getServer().getLevel(parentDim.get());
                if (parentLevel != null) {
                    long explorationSeed = level.getSeed();
                    long parentSeed = parentLevel.getSeed();
                    
                    source.sendSuccess(() -> Component.literal("Exploration seed: " + explorationSeed)
                        .withStyle(ChatFormatting.GRAY), false);
                    source.sendSuccess(() -> Component.literal("Parent seed: " + parentSeed)
                        .withStyle(ChatFormatting.GRAY), false);
                    source.sendSuccess(() -> Component.literal("Seeds match: " + 
                        (explorationSeed == parentSeed ? "NO (correct)" : "YES (error)"))
                        .withStyle(explorationSeed == parentSeed ? ChatFormatting.RED : ChatFormatting.GREEN), false);
                }
            }
        } else {
            source.sendSuccess(() -> Component.literal("Normal dimension - compass will work normally")
                .withStyle(ChatFormatting.GREEN), false);
        }
        
        return 1;
    }
    
    private static boolean testMixinRegistryFixed(CommandSourceStack source) {
        try {
            var server = source.getServer();
            
            // Test DimensionType registry
            var dimTypeRegistry = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
            
            if (dimTypeRegistry instanceof net.tinkstav.brecher_dim.accessor.IRegistryAccessor) {
                @SuppressWarnings("unchecked")
                var accessor = (net.tinkstav.brecher_dim.accessor.IRegistryAccessor<net.minecraft.world.level.dimension.DimensionType>) dimTypeRegistry;
                
                source.sendSuccess(() -> Component.literal("  ✓ DimensionType registry cast to IRegistryAccessor")
                    .withStyle(ChatFormatting.GREEN), false);
                
                // Test frozen state
                boolean frozen = accessor.brecher_dim$isFrozen();
                source.sendSuccess(() -> Component.literal("  • Registry frozen state: " + frozen)
                    .withStyle(ChatFormatting.GRAY), false);
                
                // Test validation
                boolean valid = accessor.brecher_dim$validateRegistryState();
                source.sendSuccess(() -> Component.literal("  • Registry state valid: " + valid)
                    .withStyle(valid ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                
                // Test runtime entries
                var runtimeEntries = accessor.brecher_dim$getRuntimeEntries();
                source.sendSuccess(() -> Component.literal("  • Runtime entries: " + runtimeEntries.size())
                    .withStyle(ChatFormatting.GRAY), false);
                
                // Dump diagnostics to log
                accessor.brecher_dim$dumpRegistryDiagnostics();
                
                return valid;
                
            } else {
                source.sendFailure(Component.literal("  ✗ DimensionType registry NOT cast to IRegistryAccessor"));
                return false;
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("  ✗ MixinRegistryFixed test failed: " + e.getMessage()));
            return false;
        }
    }
    
    private static boolean validateRegistryStates(CommandSourceStack source) {
        try {
            var server = source.getServer();
            boolean allValid = true;
            
            // Test multiple registries
            String[] registryNames = {"DimensionType", "LevelStem"};
            var registries = new Object[]{
                server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE),
                server.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
            };
            
            for (int i = 0; i < registryNames.length; i++) {
                String name = registryNames[i];
                Object registry = registries[i];
                
                if (registry instanceof net.tinkstav.brecher_dim.accessor.IRegistryAccessor) {
                    @SuppressWarnings("unchecked")
                    var accessor = (net.tinkstav.brecher_dim.accessor.IRegistryAccessor<Object>) registry;
                    
                    boolean valid = accessor.brecher_dim$validateRegistryState();
                    var runtimeEntries = accessor.brecher_dim$getRuntimeEntries();
                    
                    String status = (valid ? "✓ Valid" : "✗ Invalid") + " (" + runtimeEntries.size() + " runtime)";
                    source.sendSuccess(() -> Component.literal("  " + name + ": " + status)
                        .withStyle(valid ? ChatFormatting.GREEN : ChatFormatting.RED), false);
                    
                    if (!valid) allValid = false;
                } else {
                    source.sendSuccess(() -> Component.literal("  " + name + ": No mixin access")
                        .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            
            return allValid;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("  ✗ Registry state validation failed: " + e.getMessage()));
            return false;
        }
    }
    
    // --- Counter Commands ---
    
    private static int showCounters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Dimension Counters ===")
            .withStyle(ChatFormatting.GOLD), false);
        
        Map<String, Long> counters = DimensionCounterUtil.getAllCounters();
        if (counters.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No counters initialized yet")
                .withStyle(ChatFormatting.YELLOW), false);
        } else {
            counters.forEach((dimType, count) -> {
                source.sendSuccess(() -> Component.literal(String.format("  %s: %d", dimType, count))
                    .withStyle(ChatFormatting.AQUA), false);
            });
        }
        
        source.sendSuccess(() -> Component.literal("\nNext dimension creation will use these counters")
            .withStyle(ChatFormatting.GRAY), false);
        
        return 1;
    }
    
    private static int resetAllCounters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        
        DimensionCounterUtil.resetCounters();
        
        source.sendSuccess(() -> Component.literal("All dimension counters reset to 0")
            .withStyle(ChatFormatting.GREEN), true);
        
        source.sendSuccess(() -> Component.literal("Next server restart will create dimensions starting from _0")
            .withStyle(ChatFormatting.YELLOW), false);
        
        return 1;
    }
    
    private static int resetDimensionCounter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        ResourceLocation dimLoc = dimension.dimension().location();
        
        DimensionCounterUtil.resetCounter(dimLoc);
        
        source.sendSuccess(() -> Component.literal("Counter for " + dimLoc.getPath() + " reset to 0")
            .withStyle(ChatFormatting.GREEN), true);
        
        return 1;
    }
    
    /**
     * Determines the base dimension from a given dimension location
     * Handles both normal dimensions and exploration dimensions.
     * Uses authoritative lookup from BrecherDimensionManager when available,
     * falls back to string parsing for edge cases.
     */
    private static ResourceLocation getBaseDimension(ResourceLocation dimensionLoc, BrecherDimensionManager manager) {
        // First try authoritative lookup from manager (handles modded dimensions correctly)
        if (manager != null && manager.isExplorationDimension(dimensionLoc)) {
            java.util.Optional<ResourceLocation> baseDim = manager.getBaseDimensionForExploration(dimensionLoc);
            if (baseDim.isPresent()) {
                return baseDim.get();
            }

            // Fallback to string parsing if lookup fails (shouldn't happen, but be defensive)
            String path = dimensionLoc.getPath();
            // Format: exploration_<base>_<id>
            if (path.startsWith("exploration_")) {
                String withoutPrefix = path.substring("exploration_".length());
                // Find the last underscore to remove the ID
                int lastUnderscore = withoutPrefix.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String baseName = withoutPrefix.substring(0, lastUnderscore);
                    // Check if this is a vanilla dimension
                    if (baseName.equals("overworld") || baseName.equals("the_nether") || baseName.equals("the_end")) {
                        return ResourceLocation.fromNamespaceAndPath("minecraft", baseName);
                    }
                    // For modded dimensions, try to reconstruct the full resource location
                    return ResourceLocation.fromNamespaceAndPath(dimensionLoc.getNamespace(), baseName);
                }
            }
        }

        // If it's already a normal dimension, return it as-is
        return dimensionLoc;
    }

    /**
     * Format a dimension ResourceLocation for user-friendly display.
     * E.g., "minecraft:the_end" -> "The End"
     */
    private static String formatDimensionName(ResourceLocation dimension) {
        if (dimension == null) return "Unknown";
        String path = dimension.getPath();
        // Replace underscores with spaces and capitalize each word
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        return result.toString();
    }

    // --- Pregen Commands ---
    
    private static int startPregen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        MinecraftServer server = ctx.getSource().getServer();
        
        Component result = ChunkPreGenerator.startGeneration(server, dimension.dimension(), 0);
        ctx.getSource().sendSuccess(() -> result, true);
        
        return 1;
    }
    
    private static int startPregenWithRadius(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        String radiusStr = StringArgumentType.getString(ctx, "radius");
        MinecraftServer server = ctx.getSource().getServer();
        
        int radius;
        try {
            radius = Integer.parseInt(radiusStr);
            if (radius < 1 || radius > 5000) {
                ctx.getSource().sendFailure(Component.literal("Radius must be between 1 and 5000 chunks")
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
        } catch (NumberFormatException e) {
            ctx.getSource().sendFailure(Component.literal("Invalid radius: " + radiusStr)
                .withStyle(ChatFormatting.RED));
            return 0;
        }
        
        Component result = ChunkPreGenerator.startGeneration(server, dimension.dimension(), radius);
        ctx.getSource().sendSuccess(() -> result, true);
        
        return 1;
    }
    
    private static int stopPregen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        MinecraftServer server = ctx.getSource().getServer();
        
        Component result = ChunkPreGenerator.stopGeneration(server, dimension.dimension());
        ctx.getSource().sendSuccess(() -> result, true);
        
        return 1;
    }
    
    private static int pregenStatus(CommandContext<CommandSourceStack> ctx) {
        Component result = ChunkPreGenerator.getStatus(null);
        ctx.getSource().sendSuccess(() -> result, false);
        
        return 1;
    }
    
    private static int pregenStatusDimension(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");

        Component result = ChunkPreGenerator.getStatus(dimension.dimension());
        ctx.getSource().sendSuccess(() -> result, false);

        return 1;
    }

    private static int pausePregen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");

        Component result = ChunkPreGenerator.pauseGeneration(dimension.dimension());
        ctx.getSource().sendSuccess(() -> result, true);

        return 1;
    }

    private static int resumePregen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");

        Component result = ChunkPreGenerator.resumeGeneration(dimension.dimension());
        ctx.getSource().sendSuccess(() -> result, true);

        return 1;
    }

    private static int stopAllPregen(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();

        Component result = ChunkPreGenerator.stopAll(server);
        ctx.getSource().sendSuccess(() -> result, true);

        return 1;
    }

    // --- Manual Unlock Commands (Progression Gating) ---

    private static int grantManualUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
        String dimensionName = StringArgumentType.getString(ctx, "dimension");

        ResourceLocation dimLoc = ResourceLocation.tryParse(dimensionName);
        if (dimLoc == null) {
            source.sendFailure(Component.literal("Invalid dimension name: " + dimensionName));
            return 0;
        }

        // Verify dimension is in enabled list
        List<String> enabledDims = BrecherConfig.getEnabledDimensions();
        if (!enabledDims.contains(dimLoc.toString())) {
            source.sendFailure(Component.literal("Dimension '" + dimensionName + "' is not enabled for exploration"));
            return 0;
        }

        BrecherSavedData data = BrecherSavedData.get(source.getServer());
        String playerName = targetPlayer.getName().getString();

        // Check if already unlocked
        if (data.hasManualUnlock(targetPlayer.getUUID(), dimLoc)) {
            source.sendSuccess(() -> Component.literal(playerName + " already has a manual unlock for " + formatDimensionName(dimLoc))
                .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        data.grantManualUnlock(targetPlayer.getUUID(), dimLoc);

        source.sendSuccess(() -> Component.literal("Granted manual unlock for ")
            .append(Component.literal(formatDimensionName(dimLoc)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" to "))
            .append(Component.literal(playerName).withStyle(ChatFormatting.GREEN)), true);

        // Notify the target player if online and different from source
        if (!source.isPlayer() || !source.getPlayerOrException().getUUID().equals(targetPlayer.getUUID())) {
            targetPlayer.displayClientMessage(
                Component.literal("You have been granted access to explore ")
                    .append(Component.literal(formatDimensionName(dimLoc)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("!"))
                    .withStyle(ChatFormatting.GREEN), false);
        }

        return 1;
    }

    private static int revokeManualUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
        String dimensionName = StringArgumentType.getString(ctx, "dimension");

        ResourceLocation dimLoc = ResourceLocation.tryParse(dimensionName);
        if (dimLoc == null) {
            source.sendFailure(Component.literal("Invalid dimension name: " + dimensionName));
            return 0;
        }

        BrecherSavedData data = BrecherSavedData.get(source.getServer());
        String playerName = targetPlayer.getName().getString();

        if (data.revokeManualUnlock(targetPlayer.getUUID(), dimLoc)) {
            source.sendSuccess(() -> Component.literal("Revoked manual unlock for ")
                .append(Component.literal(formatDimensionName(dimLoc)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" from "))
                .append(Component.literal(playerName).withStyle(ChatFormatting.RED)), true);

            // Notify the target player if online and different from source
            if (!source.isPlayer() || !source.getPlayerOrException().getUUID().equals(targetPlayer.getUUID())) {
                targetPlayer.displayClientMessage(
                    Component.literal("Your access to explore ")
                        .append(Component.literal(formatDimensionName(dimLoc)).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" has been revoked."))
                        .withStyle(ChatFormatting.RED), false);
            }

            return 1;
        } else {
            source.sendSuccess(() -> Component.literal(playerName + " does not have a manual unlock for " + formatDimensionName(dimLoc))
                .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
    }

    private static int listManualUnlocksForPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
        return listManualUnlocks(ctx, targetPlayer);
    }

    private static int listManualUnlocks(CommandContext<CommandSourceStack> ctx, ServerPlayer targetPlayer) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();

        // If no target player specified and source is a player, show their own unlocks
        if (targetPlayer == null) {
            if (source.isPlayer()) {
                targetPlayer = source.getPlayerOrException();
            } else {
                source.sendFailure(Component.literal("Must specify a player when running from console"));
                return 0;
            }
        }

        String playerName = targetPlayer.getName().getString();
        BrecherSavedData data = BrecherSavedData.get(source.getServer());
        Set<ResourceLocation> unlocks = data.getManualUnlocks(targetPlayer.getUUID());

        source.sendSuccess(() -> Component.literal("=== Manual Unlocks for " + playerName + " ===")
            .withStyle(ChatFormatting.GOLD), false);

        if (unlocks.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No manual unlocks")
                .withStyle(ChatFormatting.GRAY), false);
        } else {
            for (ResourceLocation dim : unlocks) {
                String friendlyName = formatDimensionName(dim);
                source.sendSuccess(() -> Component.literal(" • " + friendlyName)
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" (" + dim + ")")
                        .withStyle(ChatFormatting.GRAY)), false);
            }
        }

        // Also show which dimensions the player has unlocked via advancements
        if (BrecherConfig.isDimensionLocksEnabled()) {
            List<String> advancementUnlocked = new ArrayList<>();
            ServerPlayer finalTargetPlayer = targetPlayer;
            for (String dimStr : BrecherConfig.getEnabledDimensions()) {
                ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
                if (dimLoc != null && !unlocks.contains(dimLoc)) {
                    // Check if locked by config
                    Optional<String> lockOpt = BrecherConfig.getDimensionLock(dimStr);
                    if (lockOpt.isPresent()) {
                        // Check if player has the advancement
                        try {
                            ResourceLocation advLoc = ResourceLocation.parse(lockOpt.get());
                            if (AdvancementLockChecker.hasAdvancement(finalTargetPlayer, advLoc)) {
                                advancementUnlocked.add(formatDimensionName(dimLoc));
                            }
                        } catch (Exception e) {
                            // Invalid advancement ID, skip
                        }
                    }
                }
            }

            if (!advancementUnlocked.isEmpty()) {
                source.sendSuccess(() -> Component.literal("\nUnlocked via advancements:")
                    .withStyle(ChatFormatting.YELLOW), false);
                for (String dim : advancementUnlocked) {
                    source.sendSuccess(() -> Component.literal(" • " + dim)
                        .withStyle(ChatFormatting.GREEN), false);
                }
            }
        }

        return 1;
    }
}