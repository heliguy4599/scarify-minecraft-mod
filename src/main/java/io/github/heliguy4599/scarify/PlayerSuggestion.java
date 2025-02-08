package io.github.heliguy4599.scarify;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public class PlayerSuggestion implements SuggestionProvider<ServerCommandSource> {
    private ConfigFile configFile = null;
    private boolean shouldSearchInWorld = false;
    private boolean excludeConfigFileEntries = false;
    private String keyToCheck = "";

    public PlayerSuggestion searchInWorld() {
        shouldSearchInWorld = true;
        return this;
    }

    public PlayerSuggestion excludeConfigEntries(ConfigFile _configFile) {
        configFile = _configFile;
        excludeConfigFileEntries = true;
        return this;
    }

    public PlayerSuggestion searchInConfigFile(ConfigFile _configFile) {
        configFile = _configFile;
        return this;
    }

    public PlayerSuggestion checkForKey(String key) {
        keyToCheck = key;
        return this;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        Set<String> toAdd = new TreeSet<>();
        ServerCommandSource source = context.getSource();

        if (shouldSearchInWorld) {
            toAdd.addAll(source.getPlayerNames());
        }

        if (configFile != null) {
            Set<String> configNames = configFile.getSectionNames();
            if (excludeConfigFileEntries) {
                toAdd.removeAll(configNames);
            } else {
                toAdd.addAll(configNames);
            }
        }

        if (configFile != null && !keyToCheck.isEmpty()) {
            for (var p : toAdd) {
                Scarify.LOGGER.info("ENTRY HAS KEY: {}", playerEntryHasKey(p));
            }
            toAdd.removeIf(name -> !playerEntryHasKey(name));
        }

        toAdd.forEach(builder::suggest);
        return builder.buildFuture();
    }

    private boolean playerEntryHasKey(String playerName) {
        return configFile == null || configFile.getSectionData(playerName).containsKey(keyToCheck);
    }
}
