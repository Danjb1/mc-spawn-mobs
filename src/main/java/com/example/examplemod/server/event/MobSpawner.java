package com.example.examplemod.server.event;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.common.RandomUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Spawns mobs around players.
 *
 * @author Dan Quill
 */
public class MobSpawner {

    private static final Logger LOGGER = LogManager.getLogger(ExampleMod.ID);

    private static final int TICKS_PER_SECOND = 20;

    private static final int SPAWN_HEIGHT = 2;

    /**
     * Set of Player IDs for whom mobs have been spawned since the last spawn
     * was due.
     */
    private final Set<Integer> playersDoneSpawning = new HashSet<>();

    /**
     * Flag set when mobs are allowed to spawn.
     * <p>
     * This is only used to prevent mobs spawning immediately. After the
     * first spawn event, this will always be true.
     */
    private boolean spawnReady;

    /**
     * Whether or not mobs should spawn.
     */
    private boolean active = true;

    /**
     * Mob type to spawn in the next wave.
     */
    private ResourceLocation mobToSpawn = new ResourceLocation("minecraft:creeper");

    /**
     * Minimum number of mobs to spawn in the next wave.
     */
    private int minMobsToSpawn = 10;

    /**
     * Maximum number of mobs to spawn in the next wave.
     */
    private int maxMobsToSpawn = 40;

    /**
     * Minimum number of ticks to wait before the next wave will spawn.
     */
    private int minIntervalTicks = 45 * TICKS_PER_SECOND;

    /**
     * Maximum number of ticks to wait before the next wave will spawn.
     */
    private int maxIntervalTicks = 120 * TICKS_PER_SECOND;

    /**
     * Maximum horizontal distance from the player at which to spawn mobs.
     */
    private int maxSpawnDist = 30;

    /**
     * Ticks remaining until the next wave spawns.
     */
    private int ticksUntilNextWave = minIntervalTicks;

    /**
     * Exception signifying that no ground is available in a particular column.
     */
    private class NoGroundException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (!active) {
            return;
        }

        ticksUntilNextWave--;

        if (ticksUntilNextWave == 0) {
            spawnReady = true;
            playersDoneSpawning.clear();
            scheduleNextWave();
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != Phase.END) {
            // Only run on the server, and only during the "end" phase.
            // This is important as we expect this to be called only ONCE, per
            // player, per tick.
            return;
        }

        int playerId = event.player.getId();

        if (spawnReady && !playersDoneSpawning.contains(playerId)) {
            spawnMobs(event.player);
            playersDoneSpawning.add(playerId);
        }
    }

    private void spawnMobs(Player player) {
        int numMobs = RandomUtils.randBetween(minMobsToSpawn, maxMobsToSpawn);
        for (int i = 0; i < numMobs; i++) {
            spawnMob(player);
        }
    }

    // Based on SummonCommand.execute()
    private void spawnMob(Player player) {
        ServerLevel level = (ServerLevel) player.getLevel();
        double x = player.getX() + RandomUtils.randBetween(
                -maxSpawnDist, maxSpawnDist);
        double z = player.getZ() + RandomUtils.randBetween(
                -maxSpawnDist, maxSpawnDist);
        double y;

        try {
            // Spawn some distance above the ground
            y = getGroundLevel(level, x, z) + SPAWN_HEIGHT;
        } catch (NoGroundException ex) {
            // No suitable spawn location
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);

        if (!Level.isInSpawnableBounds(pos)) {
            return;
        }

        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("id", mobToSpawn.toString());
        Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (e) -> {
           e.moveTo(pos.getX(), pos.getY(), pos.getZ(), e.getYRot(), e.getXRot());
           return e;
        });

        if (entity == null || !(entity instanceof Mob)) {
            LOGGER.warn("Failed to load mob: " + mobToSpawn);
            return;
        }

        ((Mob) entity).finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(entity.blockPosition()),
                MobSpawnType.COMMAND,
                (SpawnGroupData) null,
                (CompoundTag) null);

        if (!level.tryAddFreshEntityWithPassengers(entity)) {
            LOGGER.warn("Failed to spawn mob: " + entity);
        }
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private int getGroundLevel(Level level, double x, double z)
            throws NoGroundException {

        int height = level.getHeight();
        int maxY = height - 1;
        boolean inCeiling = false;

        // Start at the top, and work down until we find a suitable spawn
        // location
        for (int y = maxY; y > 0; y--) {
            BlockPos blockPos = new BlockPos(x, y, z);
            boolean foundSolid = isSolid(level, blockPos);

            if (foundSolid) {
                if (y == maxY) {
                    // In the Nether, we have to skip past the ceiling before
                    // we start looking for a floor
                    inCeiling = true;
                } else if (!inCeiling) {
                    // We found the floor!
                    return y;
                }
            } else {
                inCeiling = false;
            }
        }

        // The world is solid all the way up!
        throw new NoGroundException();
    }

    private boolean isSolid(Level level, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        // We consider fluids to be solid, otherwise Creepers would spawn at
        // the bottom of the ocean!
        return !blockState.isAir() || !blockState.getFluidState().isEmpty();
    }

    private void scheduleNextWave() {
        ticksUntilNextWave =
                RandomUtils.randBetween(minIntervalTicks, maxIntervalTicks);
    }

    public void setMobToSpawn(ResourceLocation mobToSpawn) {
        this.mobToSpawn = mobToSpawn;
    }

    public void setInterval(int minSecs, int maxSecs) {
        minIntervalTicks = minSecs * TICKS_PER_SECOND;
        maxIntervalTicks = maxSecs * TICKS_PER_SECOND;
        scheduleNextWave();
    }

    public void setSpawningActive(boolean active) {
        this.active = active;
    }

    public void setNumMobsToSpawn(int minCount, int maxCount) {
        this.minMobsToSpawn = minCount;
        this.maxMobsToSpawn = maxCount;
    }

    public void setMaxSpawnDist(int maxSpawnDist) {
        this.maxSpawnDist = maxSpawnDist;
    }

}
