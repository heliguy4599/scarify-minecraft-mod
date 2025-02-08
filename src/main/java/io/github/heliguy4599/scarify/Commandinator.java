package io.github.heliguy4599.scarify;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.HashMap;

public class Commandinator {
    public final ConfigFile configFile;

    public Commandinator(ConfigFile _configFile) {
        configFile = _configFile;
    }

    // Register our commands
    public void registerCommands() {
        // Command: /scarify add <player>
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("add")
                .then(
                    CommandManager.argument("player_name", StringArgumentType.string())
                    .suggests(new PlayerSuggestion().searchInWorld().excludeConfigEntries(configFile))
                    .executes(this::addPlayer)
                )
            )
        ));

        // Command: /scarify remove <player>
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("remove")
                .then(
                    CommandManager.argument("player_name", StringArgumentType.string())
                    .suggests(new PlayerSuggestion().searchInConfigFile(configFile))
                    .executes(this::removePlayer)
                )
            )
        ));

        // Command: /scarify distanceOverride set <player> <absolute block distance>
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("distanceOverride")
                .then(
                    CommandManager.literal("set")
                    .then(
                        CommandManager.argument("player_name", StringArgumentType.string())
                        .suggests(new PlayerSuggestion().searchInWorld().searchInConfigFile(configFile))
                        .then(
                            CommandManager.argument("block_distance", DoubleArgumentType.doubleArg())
                            .executes(this::overrideDistance)
                        )
                    )
                )
            )
        ));

        // Command: /scarify distanceOverride reset <player>
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("distanceOverride")
                .then(
                    CommandManager.literal("reset")
                    .then(
                        CommandManager.argument("player_name", StringArgumentType.string())
                        .suggests(new PlayerSuggestion().searchInConfigFile(configFile).checkForKey("distanceOverride"))
                        .executes(this::resetOverrideDistance)
                    )
                )
            )
        ));

        // Command: /scarify listAddedPlayers
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("listAddedPlayers")
                .executes(this::listPlayers)
            )
        ));

        // Command: /scarify view <player name>
        CommandRegistrationCallback.EVENT.register((
            commandDispatcher,
            commandRegistryAccess,
            registrationEnvironment
        ) -> commandDispatcher.register(
            CommandManager.literal("scarify")
            .requires(source -> source.hasPermissionLevel(1))
            .then(
                CommandManager.literal("view")
                .then(
                    CommandManager.argument("player_name", StringArgumentType.string())
                    .suggests(new PlayerSuggestion().searchInConfigFile(configFile))
                    .executes(this::view)
                )
            )
        ));
    }

    // Command: /scarify add <player>
    public int addPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String playerName = StringArgumentType.getString(context, "player_name");

        // Find the player in the config file. Only needed for user feedback if player is already added
        if (configFile.getSectionNames().contains(playerName)) {
            throw new SimpleCommandExceptionType(Text.literal(playerName + " has already been added to Scarify")).create();
        }

        configFile.setSectionData(playerName, new HashMap<>());
        configFile.saveToFile(Scarify.MOD_CONFIG_PATH);
        context.getSource().sendFeedback(() -> Text.literal("[Scarify]: Made " + playerName + " scary!"), true);
        return 1;
    }

    // Command: /scarify remove <player>
    public int removePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String playerName = StringArgumentType.getString(context, "player_name");

        // Find the player in the config file. Only needed for user feedback if player is not added
        if (!configFile.getSectionNames().contains(playerName)) {
            throw new SimpleCommandExceptionType(Text.literal(playerName + " has not been added to Scarify")).create();
        }

        configFile.deleteSection(playerName);
        configFile.saveToFile(Scarify.MOD_CONFIG_PATH);
        context.getSource().sendFeedback(() -> Text.literal("[Scarify]: " + playerName + " is no longer scary"), true);
        return 1;
    }

    // Command: /scarify distanceOverride set <player> <absolute block distance>
    public int overrideDistance(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String playerName = StringArgumentType.getString(context, "player_name");

        final double distance = DoubleArgumentType.getDouble(context, "block_distance");
        if (distance <= 0) {
            throw new SimpleCommandExceptionType(Text.literal("Invalid distance amount. Distance must be greater than 0")).create();
        }

        HashMap<String, Object> data = new HashMap<>();
        data.put("distanceOverride", distance);
        configFile.setSectionData(playerName, data);
        configFile.saveToFile(Scarify.MOD_CONFIG_PATH);
        context.getSource().sendFeedback(() -> Text.literal("[Scarify]: " + playerName + " now has a distance override of " + distance), true);
        return 1;
    }

    // Command: /scarify distanceOverride reset <player>
    public int resetOverrideDistance(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String playerName = StringArgumentType.getString(context, "player_name");

        // Find the player in the config file. Only needed for user feedback if player is not added
        if (!configFile.getSectionNames().contains(playerName)) {
            throw new SimpleCommandExceptionType(Text.literal(playerName + " has not been added to Scarify")).create();
        }

        // Get the distanceOverride for the player. Only needed for user feedback if player has no override
        if (!configFile.getSectionData(playerName).containsKey("distanceOverride")) {
            throw new SimpleCommandExceptionType(Text.literal(playerName + " does not have a distance override set")).create();
        }

        var sectionData = configFile.getSectionData(playerName);
        sectionData.remove("distanceOverride");
        configFile.saveToFile(Scarify.MOD_CONFIG_PATH);
        context.getSource().sendFeedback(() -> Text.literal("[Scarify]: " + playerName + " no longer has a distance override"), true);
        return 1;
    }

    // Command: /scarify listAddedPlayers
    public int listPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var playerNames = configFile.getSectionNames();
        if (playerNames.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("[Scarify]: No players have been added"), false);
        }
        playerNames.removeIf(String::isEmpty);
        context.getSource().sendFeedback(() -> Text.literal("[Scarify]: Added Players: " + playerNames), false);
        return 1;
    }

    // Command: /scarify view <player name>
    public int view(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String playerName = StringArgumentType.getString(context, "player_name");

        // Find the player in the config file. Only needed for user feedback if player is not added
        if (!configFile.getSectionNames().contains(playerName)) {
            throw new SimpleCommandExceptionType(Text.literal(playerName + " has not been added to Scarify")).create();
        }

        var data = configFile.getSectionData(playerName);
        context.getSource().sendFeedback(() -> Text.literal("[" + playerName + "]: " + data), false);
        return 1;
    }
}
