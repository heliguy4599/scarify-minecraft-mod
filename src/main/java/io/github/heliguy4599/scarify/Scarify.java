package io.github.heliguy4599.scarify;

import io.github.heliguy4599.scarify.mixin.MobGoalSelectorAccessor;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Scarify implements ModInitializer {
	public static final String MOD_ID = "scarify";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Path MOD_CONFIG_PATH;
	public static ConfigFile configFile;

	public static final GameRules.Key<GameRules.BooleanRule> ENABLE_SCARIFY = GameRuleRegistry.register(
		"enableScarify",
		GameRules.Category.PLAYER,
		GameRuleFactory.createBooleanRule(true)
	);

	public static boolean getIsPehkuiLoaded() {
		return FabricLoader.getInstance().isModLoaded("pehkui");
	}

	@Override
	public void onInitialize() {
		MOD_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".cfg");
		configFile = ConfigFile.loadFromFile(MOD_CONFIG_PATH, false);

		final var commandinator = new Commandinator(configFile);
		commandinator.registerCommands();

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof MobEntity mob && configFile != null) {
				injectFleePlayerGoal(mob);
			}
		});
	}

	private void injectFleePlayerGoal(MobEntity mob) {
		if (mob instanceof MobGoalSelectorAccessor accessMob && accessMob instanceof PathAwareEntity paMob) {
			var goalSelector = accessMob.getGoalSelector();
			goalSelector.getGoals().removeIf(goal -> goal.getGoal() instanceof FleeFromPlayerGoal);
			var goal = new FleeFromPlayerGoal(paMob, configFile);
			goalSelector.add(0, goal);
		}
	}
}