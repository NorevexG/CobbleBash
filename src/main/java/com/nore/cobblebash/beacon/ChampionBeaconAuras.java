package com.nore.cobblebash.beacon;

import com.cobblemon.mod.common.PlayerSpawnerAccessor;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.block.ApricornBlock;
import com.cobblemon.mod.common.block.ApricornSaplingBlock;
import com.cobblemon.mod.common.block.BerryBlock;
import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

public final class ChampionBeaconAuras {
    public static final double SHINY_EXTRA_CHANCE = 1.0D / 5461.0D;
    public static final float LURE_EXTRA_TIMER_PROGRESS_PER_TICK = 0.5F;

    private static final long CROP_GROWTH_INTERVAL_TICKS = 20L * 120L;
    private static final long CROP_GROWTH_INTERVAL_TICKS_BASE = 20L * 240L;
    private static final long DAYCARE_INTERVAL_TICKS = 20L * 60L;
    private static final long EV_INTERVAL_TICKS = 20L * 240L;
    private static final long PASTURE_DISCOVERY_INTERVAL_TICKS = 20L * 600L;
    private static final int CROP_DISCOVERY_SCAN_INTERVAL_PULSES = 5;
    private static final int CROP_DISCOVERY_CHUNKS_PER_SCAN = 8;
    private static final int CROP_DISCOVERY_VERTICAL_RANGE = 16;
    private static final int DAYCARE_XP_PER_MINUTE = 60;
    private static final int DAYCARE_XP_PER_MINUTE_UPGRADED = 120;
    private static final int EV_AMOUNT = 1;
    private static final int EV_AMOUNT_UPGRADED = 2;
    private static final SidemodExperienceSource DAYCARE_EXPERIENCE_SOURCE = new SidemodExperienceSource("cobblebash");

    private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKED_BEACONS = new ConcurrentHashMap<>();
    private static final Map<BeaconKey, AuraCache> AURA_CACHES = new ConcurrentHashMap<>();
    private static final Set<UUID> DEBUG_PULSE_PLAYERS = ConcurrentHashMap.newKeySet();

    private ChampionBeaconAuras() {
    }

    public static void track(ChampionBeaconBlockEntity beacon) {
        Level level = beacon.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Set<BlockPos> positions = TRACKED_BEACONS.computeIfAbsent(serverLevel.dimension(), ignored -> ConcurrentHashMap.newKeySet());
        if (isPotentiallyActive(beacon)) {
            positions.add(beacon.getBlockPos().immutable());
            if (hasCacheableAura(beacon)) {
                prepareAuraCache(serverLevel, beacon);
            }
        } else {
            positions.remove(beacon.getBlockPos());
            AURA_CACHES.remove(new BeaconKey(serverLevel.dimension(), beacon.getBlockPos().immutable()));
        }
    }

    public static void untrack(Level level, BlockPos pos) {
        if (level == null || level.isClientSide) {
            return;
        }

        Set<BlockPos> positions = TRACKED_BEACONS.get(level.dimension());
        if (positions != null) {
            positions.remove(pos);
        }
        AURA_CACHES.remove(new BeaconKey(level.dimension(), pos.immutable()));
    }

    public static void tickBeacon(ChampionBeaconBlockEntity beacon) {
        Level level = beacon.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!isPotentiallyActive(beacon)) {
            AURA_CACHES.remove(new BeaconKey(serverLevel.dimension(), beacon.getBlockPos().immutable()));
            return;
        }

        if (hasPastureAura(beacon) && isBeaconPulse(serverLevel, beacon.getBlockPos(), PASTURE_DISCOVERY_INTERVAL_TICKS)) {
            discoverAllLoadedPastures(serverLevel, beacon, prepareAuraCache(serverLevel, beacon));
        }

        if (hasCropAura(beacon) && isBeaconPulse(serverLevel, beacon.getBlockPos(), getCropGrowthInterval(beacon))) {
            AuraCache cache = prepareAuraCache(serverLevel, beacon);
            pulseCrops(serverLevel, beacon, cache);
        }

        if (hasPower(beacon, ChampionBeaconPower.DAYCARE) && isBeaconPulse(serverLevel, beacon.getBlockPos(), DAYCARE_INTERVAL_TICKS)) {
            pulseDaycare(serverLevel, beacon);
        }

        if (hasPower(beacon, ChampionBeaconPower.EV) && isBeaconPulse(serverLevel, beacon.getBlockPos(), EV_INTERVAL_TICKS)) {
            pulseEvAura(serverLevel, beacon);
        }
    }

    public static void handleBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!isCacheRelevantBlock(oldState) && !isCacheRelevantBlock(newState)) {
            return;
        }

        Set<BlockPos> positions = TRACKED_BEACONS.get(level.dimension());
        if (positions == null || positions.isEmpty()) {
            return;
        }

        for (BlockPos beaconPos : positions) {
            ChampionBeaconBlockEntity beacon = getActiveBeacon(level, beaconPos);
            if (beacon == null || !isInHorizontalRange(beaconPos, pos, beacon.getRadius()) || !hasCacheableAura(beacon)) {
                continue;
            }

            updateCacheForBlock(prepareAuraCache(level, beacon), pos, oldState, newState);
        }
    }

    public static boolean shouldRepel(ServerLevel level, BlockPos pos) {
        return hasAura(level, pos, ChampionBeaconPower.REPEL);
    }

    public static boolean tryApplyShinyAura(PokemonEntity pokemon, ServerLevel level, BlockPos pos) {
        if (pokemon.getPokemon().getShiny()) {
            return false;
        }

        if (!hasAura(level, pos, ChampionBeaconPower.SHINY)) {
            return false;
        }

        if (level.random.nextDouble() >= SHINY_EXTRA_CHANCE) {
            return false;
        }

        pokemon.getPokemon().setShiny(true);
        pokemon.getPokemon().updateAspects();
        return true;
    }

    public static void tickPlayer(ServerPlayer player) {
        if (!hasAura(player.serverLevel(), player.blockPosition(), ChampionBeaconPower.LURE)) {
            return;
        }

        if (!(player instanceof PlayerSpawnerAccessor accessor)) {
            return;
        }

        PlayerSpawner spawner = accessor.getPlayerSpawner();
        if (spawner == null || !spawner.getActive()) {
            return;
        }

        float currentTicks = spawner.getTicksUntilNextSpawn();
        if (currentTicks > 1.0F) {
            spawner.setTicksUntilNextSpawn(Math.max(1.0F, currentTicks - LURE_EXTRA_TIMER_PROGRESS_PER_TICK));
        }
    }

    public static DebugInfo debugAt(ServerLevel level, BlockPos pos) {
        EnumSet<ChampionBeaconPower> powers = EnumSet.noneOf(ChampionBeaconPower.class);
        int activeBeacons = 0;

        Set<BlockPos> positions = TRACKED_BEACONS.get(level.dimension());
        if (positions == null) {
            return new DebugInfo(0, powers);
        }

        for (BlockPos beaconPos : positions) {
            ChampionBeaconBlockEntity beacon = getActiveBeacon(level, beaconPos);
            if (beacon == null || !isInHorizontalRange(beaconPos, pos, beacon.getRadius())) {
                continue;
            }

            activeBeacons++;
            addPower(powers, beacon.getPrimaryPower());
            addPower(powers, beacon.getSecondaryPower());
        }

        return new DebugInfo(activeBeacons, powers);
    }

    public static PlayerDebugInfo debugForPlayer(ServerPlayer player) {
        DebugInfo auraInfo = debugAt(player.serverLevel(), player.blockPosition());
        boolean lureActive = auraInfo.powers().contains(ChampionBeaconPower.LURE);
        PlayerSpawner spawner = player instanceof PlayerSpawnerAccessor accessor ? accessor.getPlayerSpawner() : null;

        if (spawner == null) {
            return new PlayerDebugInfo(
                    auraInfo,
                    false,
                    lureActive,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    getCobblemonShinyRate(),
                    auraInfo.powers().contains(ChampionBeaconPower.SHINY) ? SHINY_EXTRA_CHANCE : 0.0D
            );
        }

        float baseProgressPerTick = spawner.getTickTimerMultiplier();
        float lureProgressPerTick = lureActive ? LURE_EXTRA_TIMER_PROGRESS_PER_TICK : 0.0F;
        float effectiveProgressPerTick = baseProgressPerTick + lureProgressPerTick;
        float ticksBetweenSpawns = spawner.getTicksBetweenSpawns();
        float effectiveTicksBetweenSpawnAttempts = effectiveProgressPerTick <= 0.0F
                ? Float.POSITIVE_INFINITY
                : ticksBetweenSpawns / effectiveProgressPerTick;

        return new PlayerDebugInfo(
                auraInfo,
                spawner.getActive(),
                lureActive,
                spawner.getTicksUntilNextSpawn(),
                ticksBetweenSpawns,
                baseProgressPerTick,
                lureProgressPerTick,
                effectiveTicksBetweenSpawnAttempts,
                getCobblemonShinyRate(),
                auraInfo.powers().contains(ChampionBeaconPower.SHINY) ? SHINY_EXTRA_CHANCE : 0.0D
        );
    }

    public static boolean togglePulseDebug(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (DEBUG_PULSE_PLAYERS.remove(playerId)) {
            return false;
        }

        DEBUG_PULSE_PLAYERS.add(playerId);
        return true;
    }

    public static VisualizationInfo visualizeForPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Set<BlockPos> positions = TRACKED_BEACONS.get(level.dimension());
        if (positions == null || positions.isEmpty()) {
            return new VisualizationInfo(0, 0, 0, 0);
        }

        int activeBeacons = 0;
        int apricorns = 0;
        int berries = 0;
        int pastures = 0;

        for (BlockPos beaconPos : positions) {
            ChampionBeaconBlockEntity beacon = getActiveBeacon(level, beaconPos);
            if (beacon == null || !isInHorizontalRange(beaconPos, player.blockPosition(), beacon.getRadius())) {
                continue;
            }

            activeBeacons++;
            if (hasCropAura(beacon)) {
                AuraCache cache = prepareAuraCache(level, beacon);
                discoverAllLoadedCrops(level, beacon, cache);
                apricorns += visualizeCachedPositions(level, cache.apricornPositions, ChampionBeaconAuras::isApricornCropBlock);
                berries += visualizeCachedPositions(level, cache.berryPositions, ChampionBeaconAuras::isBerryCropBlock);
            }

            PastureVisualizationCounter counter = new PastureVisualizationCounter();
            forEachLoadedBlockEntityInHorizontalRange(level, beacon.getBlockPos(), beacon.getRadius(), blockEntity -> {
                if (blockEntity instanceof PokemonPastureBlockEntity) {
                    counter.count++;
                    emitDebugParticle(level, blockEntity.getBlockPos());
                }
            });
            pastures += counter.count;
        }

        return new VisualizationInfo(activeBeacons, apricorns, berries, pastures);
    }

    private static boolean hasAura(ServerLevel level, BlockPos pos, ChampionBeaconPower power) {
        Set<BlockPos> positions = TRACKED_BEACONS.get(level.dimension());
        if (positions == null || positions.isEmpty()) {
            return false;
        }

        for (BlockPos beaconPos : positions) {
            ChampionBeaconBlockEntity beacon = getActiveBeacon(level, beaconPos);
            if (beacon == null) {
                positions.remove(beaconPos);
                continue;
            }

            if (hasPower(beacon, power) && isInHorizontalRange(beaconPos, pos, beacon.getRadius())) {
                return true;
            }
        }

        return false;
    }

    private static AuraCache prepareAuraCache(ServerLevel level, ChampionBeaconBlockEntity beacon) {
        BeaconKey key = new BeaconKey(level.dimension(), beacon.getBlockPos().immutable());
        AuraCache cache = AURA_CACHES.computeIfAbsent(key, ignored -> new AuraCache());
        if (cache.needsRefresh(beacon)) {
            cache.clear();
            cache.capture(beacon);
            if (hasCropAura(beacon)) {
                discoverAllLoadedCrops(level, beacon, cache);
            }
            if (hasPastureAura(beacon)) {
                discoverAllLoadedPastures(level, beacon, cache);
            }
        }
        return cache;
    }

    private static void pulseCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache) {
        int attempts = 1;
        CropPulseStats apricornStats = CropPulseStats.empty();
        CropPulseStats berryStats = CropPulseStats.empty();

        if (hasPower(beacon, ChampionBeaconPower.APRICORN)) {
            apricornStats = growCachedCrops(level, cache.apricornPositions, ChampionBeaconAuras::isApricornCropBlock, attempts);
        }

        if (hasPower(beacon, ChampionBeaconPower.BERRY)) {
            berryStats = growCachedCrops(level, cache.berryPositions, ChampionBeaconAuras::isBerryCropBlock, attempts);
        }

        cache.cropPulseCount++;
        if (cache.cropPulseCount % CROP_DISCOVERY_SCAN_INTERVAL_PULSES == 0) {
            discoverNextLoadedCropChunks(level, beacon, cache);
        }

        sendPulseDebug(level, beacon, Component.literal(
                "Champion Beacon crop pulse at "
                        + formatPos(beacon.getBlockPos())
                        + ": attempts/crop="
                        + attempts
                        + ", apricorns "
                        + apricornStats.describe()
                        + ", berries "
                        + berryStats.describe()
        ));
    }

    private static CropPulseStats growCachedCrops(ServerLevel level, Set<BlockPos> positions, CropMatcher cropMatcher, int attempts) {
        CropPulseStats stats = new CropPulseStats(positions.size());
        Iterator<BlockPos> iterator = positions.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (shouldRemoveCachedCrop(level, pos, cropMatcher, attempts, stats)) {
                iterator.remove();
            }
        }
        return stats;
    }

    private static boolean shouldRemoveCachedCrop(ServerLevel level, BlockPos pos, CropMatcher cropMatcher, int attempts, CropPulseStats stats) {
        BlockState state = level.getBlockState(pos);
        if (!cropMatcher.matches(state) || !(state.getBlock() instanceof BonemealableBlock crop)) {
            stats.removedInvalid++;
            return true;
        }

        for (int i = 0; i < attempts; i++) {
            if (!crop.isValidBonemealTarget(level, pos, state)) {
                stats.matureOrSkipped++;
                return false;
            }
            if (crop.isBonemealSuccess(level, level.random, pos, state)) {
                crop.performBonemeal(level, level.random, pos, state);
                stats.growthAttemptsPerformed++;
            }

            state = level.getBlockState(pos);
            if (!cropMatcher.matches(state) || !(state.getBlock() instanceof BonemealableBlock refreshedCrop)) {
                stats.removedInvalid++;
                return true;
            }
            crop = refreshedCrop;
        }

        return false;
    }

    private static void discoverAllLoadedCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache) {
        BlockPos center = beacon.getBlockPos();
        int radius = beacon.getRadius();
        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                scanLoadedChunkForCrops(level, beacon, cache, chunkX, chunkZ);
            }
        }
    }

    private static void discoverNextLoadedCropChunks(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache) {
        BlockPos center = beacon.getBlockPos();
        int radiusChunks = Math.max(1, Math.ceilDiv(beacon.getRadius(), 16));
        int diameter = radiusChunks * 2 + 1;
        int totalChunks = diameter * diameter;
        int centerChunkX = Math.floorDiv(center.getX(), 16);
        int centerChunkZ = Math.floorDiv(center.getZ(), 16);

        for (int i = 0; i < CROP_DISCOVERY_CHUNKS_PER_SCAN; i++) {
            int cursor = Math.floorMod(cache.discoveryCursor++, totalChunks);
            int offsetX = cursor % diameter - radiusChunks;
            int offsetZ = cursor / diameter - radiusChunks;
            scanLoadedChunkForCrops(level, beacon, cache, centerChunkX + offsetX, centerChunkZ + offsetZ);
        }
    }

    private static void scanLoadedChunkForCrops(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache, int chunkX, int chunkZ) {
        ServerChunkCache chunkSource = level.getChunkSource();
        LevelChunk chunk = chunkSource.getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }

        BlockPos center = beacon.getBlockPos();
        int radius = beacon.getRadius();
        int minX = Math.max(chunkX << 4, center.getX() - radius);
        int maxX = Math.min((chunkX << 4) + 15, center.getX() + radius);
        int minZ = Math.max(chunkZ << 4, center.getZ() - radius);
        int maxZ = Math.min((chunkZ << 4) + 15, center.getZ() + radius);
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - CROP_DISCOVERY_VERTICAL_RANGE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + CROP_DISCOVERY_VERTICAL_RANGE);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                mutablePos.set(x, center.getY(), z);
                if (!isInHorizontalRange(center, mutablePos, radius)) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    mutablePos.set(x, y, z);
                    cacheCropPosition(cache, level.getBlockState(mutablePos), mutablePos);
                }
            }
        }
    }

    private static void cacheCropPosition(AuraCache cache, BlockState state, BlockPos pos) {
        if (isApricornCropBlock(state)) {
            cache.apricornPositions.add(pos.immutable());
        } else if (isBerryCropBlock(state)) {
            cache.berryPositions.add(pos.immutable());
        }
    }

    private static void updateCacheForBlock(AuraCache cache, BlockPos pos, BlockState oldState, BlockState newState) {
        if (isApricornCropBlock(oldState)) {
            cache.apricornPositions.remove(pos);
        }
        if (isBerryCropBlock(oldState)) {
            cache.berryPositions.remove(pos);
        }
        if (isPastureBlock(oldState)) {
            cache.pasturePositions.remove(pos);
        }

        if (isApricornCropBlock(newState)) {
            cache.apricornPositions.add(pos.immutable());
        }
        if (isBerryCropBlock(newState)) {
            cache.berryPositions.add(pos.immutable());
        }
        if (isPastureBlock(newState)) {
            cache.pasturePositions.add(pos.immutable());
        }
    }

    private static boolean isCacheRelevantBlock(BlockState state) {
        return isApricornCropBlock(state) || isBerryCropBlock(state) || isPastureBlock(state);
    }

    private static boolean isApricornCropBlock(BlockState state) {
        return state.getBlock() instanceof ApricornBlock || state.getBlock() instanceof ApricornSaplingBlock;
    }

    private static boolean isBerryCropBlock(BlockState state) {
        return state.getBlock() instanceof BerryBlock;
    }

    private static boolean isPastureBlock(BlockState state) {
        return state.getBlock() instanceof PastureBlock;
    }

    private static int visualizeCachedPositions(ServerLevel level, Set<BlockPos> positions, CropMatcher cropMatcher) {
        int count = 0;
        for (BlockPos pos : positions) {
            if (cropMatcher.matches(level.getBlockState(pos))) {
                count++;
                emitDebugParticle(level, pos);
            }
        }
        return count;
    }

    private static void emitDebugParticle(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.5D,
                8,
                0.25D,
                0.25D,
                0.25D,
                0.0D
        );
    }

    private static void pulseDaycare(ServerLevel level, ChampionBeaconBlockEntity beacon) {
        int pool = isPrimaryUpgraded(beacon, ChampionBeaconPower.DAYCARE) ? DAYCARE_XP_PER_MINUTE_UPGRADED : DAYCARE_XP_PER_MINUTE;
        LinkedHashMap<UUID, Pokemon> eligiblePokemon = collectPasturePokemon(level, beacon, prepareAuraCache(level, beacon));
        eligiblePokemon.entrySet().removeIf(entry -> !entry.getValue().canLevelUpFurther());

        if (eligiblePokemon.isEmpty()) {
            sendPulseDebug(level, beacon, Component.literal(
                    "Champion Beacon daycare pulse at "
                            + formatPos(beacon.getBlockPos())
                            + ": no eligible pasture Pokemon found."
            ));
            return;
        }

        int baseAmount = pool / eligiblePokemon.size();
        int remainder = pool % eligiblePokemon.size();
        int index = 0;
        List<String> awards = new ArrayList<>();
        for (Pokemon pokemon : eligiblePokemon.values()) {
            int amount = baseAmount + (index < remainder ? 1 : 0);
            index++;
            if (amount > 0) {
                int beforeLevel = pokemon.getLevel();
                int beforeExperience = pokemon.getExperience();
                pokemon.addExperience(DAYCARE_EXPERIENCE_SOURCE, amount);
                awards.add(
                        pokemon.getDisplayName(false).getString()
                                + " "
                                + pokemon.getUuid()
                                + " +"
                                + amount
                                + " XP "
                                + beforeExperience
                                + "->"
                                + pokemon.getExperience()
                                + ", Lv "
                                + beforeLevel
                                + "->"
                                + pokemon.getLevel()
                );
            }
        }

        sendPulseDebug(level, beacon, Component.literal(
                "Champion Beacon daycare pulse at "
                        + formatPos(beacon.getBlockPos())
                        + ": pool="
                        + pool
                        + ", pokemon="
                        + eligiblePokemon.size()
                        + ", "
                        + String.join("; ", awards)
        ));
    }

    private static void pulseEvAura(ServerLevel level, ChampionBeaconBlockEntity beacon) {
        Stat stat = getEvStat(beacon.getPaymentItem());
        int amount = isPrimaryUpgraded(beacon, ChampionBeaconPower.EV) ? EV_AMOUNT_UPGRADED : EV_AMOUNT;
        LinkedHashMap<UUID, Pokemon> eligiblePokemon = collectPasturePokemon(level, beacon, prepareAuraCache(level, beacon));

        if (eligiblePokemon.isEmpty()) {
            sendPulseDebug(level, beacon, Component.literal(
                    "Champion Beacon EV pulse at "
                            + formatPos(beacon.getBlockPos())
                            + ": no eligible pasture Pokemon found. Stat="
                            + stat.getDisplayName().getString()
            ));
            return;
        }

        List<Pokemon> candidates = new ArrayList<>(eligiblePokemon.values());
        Pokemon pokemon = candidates.get(level.random.nextInt(candidates.size()));
        List<String> awards = new ArrayList<>();
        int before = getEvValue(pokemon, stat);
        int beforeTotal = getEvTotal(pokemon);
        addEv(pokemon, stat, amount);
        int after = getEvValue(pokemon, stat);
        int afterTotal = getEvTotal(pokemon);
        int added = Math.max(0, after - before);
        awards.add(
                pokemon.getDisplayName(false).getString()
                        + " "
                        + pokemon.getUuid()
                        + " +"
                        + added
                        + " "
                        + stat.getDisplayName().getString()
                        + " EV "
                        + before
                        + "->"
                        + after
                        + ", total "
                        + beforeTotal
                        + "->"
                        + afterTotal
        );

        sendPulseDebug(level, beacon, Component.literal(
                "Champion Beacon EV pulse at "
                        + formatPos(beacon.getBlockPos())
                        + ": stat="
                        + stat.getDisplayName().getString()
                        + ", amount="
                        + amount
                        + ", candidates="
                        + eligiblePokemon.size()
                        + ", selected=1"
                        + ", "
                        + String.join("; ", awards)
        ));
    }

    private static LinkedHashMap<UUID, Pokemon> collectPasturePokemon(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache) {
        LinkedHashMap<UUID, Pokemon> eligiblePokemon = new LinkedHashMap<>();
        Iterator<BlockPos> iterator = cache.pasturePositions.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!isInHorizontalRange(beacon.getBlockPos(), pos, beacon.getRadius())) {
                iterator.remove();
                continue;
            }

            if (!(level.getBlockEntity(pos) instanceof PokemonPastureBlockEntity pasture)) {
                if (!isPastureBlock(level.getBlockState(pos))) {
                    iterator.remove();
                }
                continue;
            }
            for (PokemonPastureBlockEntity.Tethering tethering : pasture.getTetheredPokemon()) {
                Pokemon pokemon = tethering.getPokemon();
                if (pokemon != null) {
                    eligiblePokemon.putIfAbsent(pokemon.getUuid(), pokemon);
                }
            }
        }

        return eligiblePokemon;
    }

    private static void discoverAllLoadedPastures(ServerLevel level, ChampionBeaconBlockEntity beacon, AuraCache cache) {
        forEachLoadedBlockEntityInHorizontalRange(level, beacon.getBlockPos(), beacon.getRadius(), blockEntity -> {
            if (blockEntity instanceof PokemonPastureBlockEntity) {
                cache.pasturePositions.add(blockEntity.getBlockPos().immutable());
            }
        });
    }

    private static int getEvValue(Pokemon pokemon, Stat stat) {
        try {
            Object evs = getEvs(pokemon);
            Method method = evs.getClass().getMethod("getOrDefault", Stat.class);
            return (Integer) method.invoke(evs, stat);
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private static int getEvTotal(Pokemon pokemon) {
        try {
            Object evs = getEvs(pokemon);
            Method method = evs.getClass().getMethod("total");
            return (Integer) method.invoke(evs);
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private static int addEv(Pokemon pokemon, Stat stat, int amount) {
        try {
            Object evs = getEvs(pokemon);
            Class<?> evSourceClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.EvSource");
            Method method = evs.getClass().getMethod("add", Stat.class, int.class, evSourceClass);
            return (Integer) method.invoke(evs, stat, amount, new SidemodEvSource("cobblebash", pokemon));
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private static Object getEvs(Pokemon pokemon) throws ReflectiveOperationException {
        Method method = Pokemon.class.getMethod("getEvs");
        return method.invoke(pokemon);
    }

    private static void forEachLoadedBlockEntityInHorizontalRange(ServerLevel level, BlockPos center, int radius, Consumer<BlockEntity> consumer) {
        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);
        ServerChunkCache chunkSource = level.getChunkSource();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = chunkSource.getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (isInHorizontalRange(center, blockEntity.getBlockPos(), radius)) {
                        consumer.accept(blockEntity);
                    }
                }
            }
        }
    }

    private static boolean isBeaconPulse(ServerLevel level, BlockPos pos, long interval) {
        return Math.floorMod(level.getGameTime() + pos.asLong(), interval) == 0L;
    }

    private static long getCropGrowthInterval(ChampionBeaconBlockEntity beacon) {
        return beacon.isUpgraded() && beacon.getPrimaryPower().isUpgradeable()
                ? CROP_GROWTH_INTERVAL_TICKS
                : CROP_GROWTH_INTERVAL_TICKS_BASE;
    }

    private static Stat getEvStat(ResourceLocation paymentItem) {
        String path = paymentItem == null ? "" : paymentItem.getPath();
        return switch (path) {
            case "fire_stone", "shiny_stone" -> Stats.ATTACK;
            case "water_stone", "leaf_stone" -> Stats.HP;
            case "thunder_stone" -> Stats.SPEED;
            case "dawn_stone", "sun_stone" -> Stats.SPECIAL_ATTACK;
            case "ice_stone", "dusk_stone" -> Stats.DEFENCE;
            case "moon_stone" -> Stats.SPECIAL_DEFENCE;
            default -> Stats.HP;
        };
    }

    private static void sendPulseDebug(ServerLevel level, ChampionBeaconBlockEntity beacon, Component message) {
        if (DEBUG_PULSE_PLAYERS.isEmpty()) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (DEBUG_PULSE_PLAYERS.contains(player.getUUID())
                    && isInHorizontalRange(beacon.getBlockPos(), player.blockPosition(), beacon.getRadius())) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static ChampionBeaconBlockEntity getActiveBeacon(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ChampionBeaconBlockEntity beacon && isPotentiallyActive(beacon)) {
            return beacon;
        }

        return null;
    }

    private static boolean isPotentiallyActive(ChampionBeaconBlockEntity beacon) {
        return beacon.hasBeam() && beacon.getPrimaryPower() != ChampionBeaconPower.NONE;
    }

    private static boolean hasPower(ChampionBeaconBlockEntity beacon, ChampionBeaconPower power) {
        return beacon.getPrimaryPower() == power || beacon.getSecondaryPower() == power;
    }

    private static boolean hasCropAura(ChampionBeaconBlockEntity beacon) {
        return hasPower(beacon, ChampionBeaconPower.APRICORN) || hasPower(beacon, ChampionBeaconPower.BERRY);
    }

    private static boolean hasPastureAura(ChampionBeaconBlockEntity beacon) {
        return hasPower(beacon, ChampionBeaconPower.DAYCARE) || hasPower(beacon, ChampionBeaconPower.EV);
    }

    private static boolean hasCacheableAura(ChampionBeaconBlockEntity beacon) {
        return hasCropAura(beacon) || hasPastureAura(beacon);
    }

    private static boolean isPrimaryUpgraded(ChampionBeaconBlockEntity beacon, ChampionBeaconPower power) {
        return beacon.getPrimaryPower() == power && beacon.isUpgraded();
    }

    private static void addPower(EnumSet<ChampionBeaconPower> powers, ChampionBeaconPower power) {
        if (power != ChampionBeaconPower.NONE) {
            powers.add(power);
        }
    }

    private static boolean isInHorizontalRange(BlockPos beaconPos, BlockPos targetPos, int radius) {
        double x = targetPos.getX() - beaconPos.getX();
        double z = targetPos.getZ() - beaconPos.getZ();
        return x * x + z * z <= radius * radius;
    }

    private static float getCobblemonShinyRate() {
        return Cobblemon.INSTANCE.getConfig().getShinyRate();
    }

    private static String formatPos(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    public record DebugInfo(int activeBeacons, EnumSet<ChampionBeaconPower> powers) {
    }

    public record VisualizationInfo(int activeBeacons, int apricorns, int berries, int pastures) {
    }

    public record PlayerDebugInfo(
            DebugInfo auraInfo,
            boolean spawnerActive,
            boolean lureActive,
            float ticksUntilNextSpawn,
            float ticksBetweenSpawnAttempts,
            float baseProgressPerTick,
            float lureProgressPerTick,
            float effectiveTicksBetweenSpawnAttempts,
            float cobblemonShinyRate,
            double activeExtraShinyChance
    ) {
    }

    private record BeaconKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private interface CropMatcher {
        boolean matches(BlockState state);
    }

    private static final class PastureVisualizationCounter {
        private int count;
    }

    private static final class CropPulseStats {
        private final int cachedBefore;
        private int growthAttemptsPerformed;
        private int matureOrSkipped;
        private int removedInvalid;

        private CropPulseStats(int cachedBefore) {
            this.cachedBefore = cachedBefore;
        }

        private static CropPulseStats empty() {
            return new CropPulseStats(0);
        }

        private String describe() {
            return "cached="
                    + cachedBefore
                    + ", growthAttempts="
                    + growthAttemptsPerformed
                    + ", mature/skipped="
                    + matureOrSkipped
                    + ", removed="
                    + removedInvalid;
        }
    }

    private static final class AuraCache {
        private final Set<BlockPos> apricornPositions = new HashSet<>();
        private final Set<BlockPos> berryPositions = new HashSet<>();
        private final Set<BlockPos> pasturePositions = new HashSet<>();
        private int radius = -1;
        private int cropPulseCount;
        private int discoveryCursor;
        private ChampionBeaconPower primaryPower = ChampionBeaconPower.NONE;
        private ChampionBeaconPower secondaryPower = ChampionBeaconPower.NONE;
        private boolean upgraded;

        private boolean needsRefresh(ChampionBeaconBlockEntity beacon) {
            return radius != beacon.getRadius()
                    || primaryPower != beacon.getPrimaryPower()
                    || secondaryPower != beacon.getSecondaryPower()
                    || upgraded != beacon.isUpgraded();
        }

        private void capture(ChampionBeaconBlockEntity beacon) {
            radius = beacon.getRadius();
            primaryPower = beacon.getPrimaryPower();
            secondaryPower = beacon.getSecondaryPower();
            upgraded = beacon.isUpgraded();
        }

        private void clear() {
            apricornPositions.clear();
            berryPositions.clear();
            pasturePositions.clear();
            cropPulseCount = 0;
            discoveryCursor = 0;
        }
    }
}
