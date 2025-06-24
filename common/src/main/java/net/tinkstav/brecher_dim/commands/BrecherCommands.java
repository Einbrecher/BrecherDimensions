package net.tinkstav.brecher_dim.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.tinkstav.brecher_dim.BrecherDimensions;
import net.tinkstav.brecher_dim.config.BrecherConfig;
import net.tinkstav.brecher_dim.data.BrecherSavedData;
import net.tinkstav.brecher_dim.dimension.BrecherDimensionManager;
import net.tinkstav.brecher_dim.dimension.DimensionRegistrar;
import net.tinkstav.brecher_dim.teleport.TeleportHandler;
import net.tinkstav.brecher_dim.debug.RegistryFieldDiagnostics;
import net.tinkstav.brecher_dim.util.DimensionCounterUtil;
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
        // Main brecher command structure
        dispatcher.register(
            Commands.literal("brecher")
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
        );
        
        // Admin commands
        dispatcher.register(
            Commands.literal("brecheradmin")
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
                        .executes(ctx -> debugRegistry(ctx))))
                .then(Commands.literal("counter")
                    .then(Commands.literal("show")
                        .executes(ctx -> showCounters(ctx)))
                    .then(Commands.literal("reset")
                        .then(Commands.literal("all")
                            .executes(ctx -> resetAllCounters(ctx)))
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                            .executes(ctx -> resetDimensionCounter(ctx)))))
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
        
        // Parse dimension name
        ResourceLocation baseDim = ResourceLocation.parse(dimensionName);
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
                    Component.literal("Use /brecher return to go back first.")
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
        
        source.sendSuccess(() -> Component.literal("=== Brecher's Dimensions Info ===")
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
        
        source.sendSuccess(() -> Component.literal("\nExploration dimensions reset with new seeds on server restart!")
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
            for (String info : dimensionInfo) {
                // Parse the info string to get the base dimension
                String baseDim = info.split(" -> ")[0];
                
                Component dimComponent = Component.literal(" • " + info)
                    .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/brecher tp " + baseDim))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Component.literal("Click to explore " + baseDim))));
                
                source.sendSuccess(() -> dimComponent, false);
            }
            
            source.sendSuccess(() -> Component.literal("\nClick a dimension to explore it!")
                .withStyle(ChatFormatting.GRAY), false);
        }
        
        return 1;
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
        
        ResourceLocation dimLoc = ResourceLocation.parse(dimensionName);
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
                int entityCount = 0;
                for (var entity : level.getEntities().getAll()) {
                    entityCount++;
                }
                final int count = entityCount;
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
     * Handles both normal dimensions and exploration dimensions
     */
    private static ResourceLocation getBaseDimension(ResourceLocation dimensionLoc, BrecherDimensionManager manager) {
        // If it's an exploration dimension, extract the base dimension
        if (manager.isExplorationDimension(dimensionLoc)) {
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
                    // This is a simplified approach - in practice, modded dimensions might need special handling
                    return ResourceLocation.fromNamespaceAndPath(dimensionLoc.getNamespace(), baseName);
                }
            }
        }
        
        // If it's already a normal dimension, return it as-is
        return dimensionLoc;
    }
}