package io.github.heliguy4599.scarify;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class FleeFromPlayerGoal extends Goal {
    protected final PathAwareEntity mob;
    private final double slowSpeed;
    private final double fastSpeed;
    @Nullable
    protected PlayerEntity targetPlayer;
    protected double fastFleeDistanceLimit;
    @Nullable
    protected Path fleePath;
    protected final EntityNavigation fleeingEntityNavigation;
    private final ConfigFile configFile;

    public FleeFromPlayerGoal(PathAwareEntity mob, ConfigFile _configFile) {
        this.mob = mob;
        this.configFile = _configFile;

        this.fleeingEntityNavigation = mob.getNavigation();
        this.setControls(EnumSet.of(Control.MOVE));

        double mobSpeed = this.mob.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        this.slowSpeed = mobSpeed + 0.8;
        this.fastSpeed = mobSpeed + 1.3;
    }

    @Override
    public boolean canStart() {
        final var world = this.mob.getWorld();
        if (!world.getGameRules().getBoolean(Scarify.ENABLE_SCARIFY)) {
            return false;
        }
        this.targetPlayer = getClosestPlayerInRange();
        if (this.targetPlayer == null) {
            return false;
        }
        Vec3d vec3d = NoPenaltyTargeting.findFrom(this.mob, 16, 7, this.targetPlayer.getPos());
        if (vec3d == null) {
            return false;
        }
        if (this.targetPlayer.squaredDistanceTo(vec3d.x, vec3d.y, vec3d.z) < this.targetPlayer.squaredDistanceTo(this.mob)) {
            return false;
        }
        this.fleePath = this.fleeingEntityNavigation.findPathTo(vec3d.x, vec3d.y, vec3d.z, 0);
        Scarify.LOGGER.info("VIS SCALE OF PLAYER: {}", getVisibilityScale(this.targetPlayer));
        Scarify.LOGGER.info("FLEE LIMIT: [{}]", this.fastFleeDistanceLimit);
        return this.fleePath != null;
    }

    public PlayerEntity getClosestPlayerInRange() {
        final var mobsInRange = new HashMap<PlayerEntity, Double>();

        for (var player : this.mob.getWorld().getPlayers()) {
            String playerName = player.getName().getString();
            boolean playerIsScary = configFile.getSectionNames().contains(playerName);
            if (
                !playerIsScary
                || player.isCreative()
                || player.isSpectator()
                || player.isInvisible()
                || !mob.canSee(player)
            ) {
                continue;
            }
            var maxDistance = 0.0;
            if (configFile.getSectionData(playerName).getOrDefault("distanceOverride", null) instanceof Double configDistance) {
                maxDistance = configDistance;
            } else {
                maxDistance = this.mob.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE) * getVisibilityScale(player);
            }
            maxDistance *= maxDistance;
            final var mobDistance = this.mob.squaredDistanceTo(player);
            Scarify.LOGGER.info("Distance: [{}]", maxDistance);
            if (mobDistance < maxDistance) {
                mobsInRange.put(player, mobDistance);
            }
        }

        // Returns the closest player of the valid players, or null
        return (
            mobsInRange
            .entrySet()
            .stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null)
        );
    }

    // If Pehkui is installed, use get the visibility scale of a given player
    public static float getVisibilityScale(PlayerEntity player) {
        if (!Scarify.getIsPehkuiLoaded()) return 1.0F;

        try {
            // Access the ScaleUtils class dynamically
            Class<?> scaleUtilsClass = Class.forName("virtuoel.pehkui.util.ScaleUtils");

            // Access the getVisibilityScale method that takes an Entity and tickDelta
            Method getVisibilityScaleMethod = scaleUtilsClass.getDeclaredMethod("getVisibilityScale", Entity.class);

            // Make the method accessible in case it's private or protected
            getVisibilityScaleMethod.setAccessible(true);

            // Invoke the method on ScaleUtils class with the provided entity and tickDelta
            return (float) getVisibilityScaleMethod.invoke(null, player);
        } catch (Exception e) {
            Scarify.LOGGER.error("Pehkui was loaded, but we could not get the visibility scale. See error below:");
            Scarify.LOGGER.error(e.toString());
        }

        // Return a default value or throw an exception if reflection fails
        return 1.0F; // Default scaling factor (no scaling)
    }

    @Override
    public boolean shouldContinue() {
        return !this.fleeingEntityNavigation.isIdle();
    }

    @Override
    public void start() {
        this.fleeingEntityNavigation.startMovingAlong(this.fleePath, this.slowSpeed);
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
    }

    @Override
    public void tick() {
//        final var shouldBeFast = this.mob.squaredDistanceTo(this.targetPlayer) < this.fastFleeDistanceLimit;
//        this.mob.getNavigation().setSpeed(shouldBeFast ? this.fastSpeed : this.slowSpeed);
        this.mob.getNavigation().setSpeed(this.fastSpeed);
    }
}
