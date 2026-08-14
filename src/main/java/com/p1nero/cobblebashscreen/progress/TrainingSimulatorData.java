package com.p1nero.cobblebashscreen.progress;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public final class TrainingSimulatorData extends SavedData {
    private static final String DATA_NAME = "cobblebash_screen_training_simulator";
    private static final String PLAYERS_KEY = "players";
    private static final String CLEAR_COUNTS_KEY = "clearCounts";
    private static final String CHALLENGES_STARTED_KEY = "challengesStarted";
    private static final SavedData.Factory<TrainingSimulatorData> FACTORY = new SavedData.Factory<>(
            TrainingSimulatorData::new,
            TrainingSimulatorData::load
    );

    private final CompoundTag players = new CompoundTag();
    private final CompoundTag clearCounts = new CompoundTag();
    private final CompoundTag challengesStarted = new CompoundTag();

    public static TrainingSimulatorData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static TrainingSimulatorData load(CompoundTag tag, HolderLookup.Provider provider) {
        TrainingSimulatorData data = new TrainingSimulatorData();
        mergeCompound(tag, PLAYERS_KEY, data.players);
        mergeCompound(tag, CLEAR_COUNTS_KEY, data.clearCounts);
        mergeCompound(tag, CHALLENGES_STARTED_KEY, data.challengesStarted);
        return data;
    }

    private static void mergeCompound(CompoundTag source, String key, CompoundTag target) {
        if (source.contains(key, Tag.TAG_COMPOUND)) {
            target.merge(source.getCompound(key));
        }
    }

    public int getUnlockMask(UUID playerId) {
        return players.getInt(playerId.toString());
    }

    public boolean isUnlocked(UUID playerId, int challengeIndex) {
        return challengeIndex >= 0 && challengeIndex < Integer.SIZE
                && (getUnlockMask(playerId) & (1 << challengeIndex)) != 0;
    }

    public boolean unlock(UUID playerId, int challengeIndex) {
        if (challengeIndex < 0 || challengeIndex >= Integer.SIZE || isUnlocked(playerId, challengeIndex)) {
            return false;
        }
        players.putInt(playerId.toString(), getUnlockMask(playerId) | (1 << challengeIndex));
        setDirty();
        return true;
    }

    public int getClearCount(UUID playerId, String gymType) {
        String playerKey = playerId.toString();
        if (!clearCounts.contains(playerKey, Tag.TAG_COMPOUND)) {
            return 0;
        }
        return Math.max(0, clearCounts.getCompound(playerKey).getInt(gymType));
    }

    public int incrementClearCount(UUID playerId, String gymType) {
        String playerKey = playerId.toString();
        CompoundTag playerCounts = clearCounts.contains(playerKey, Tag.TAG_COMPOUND)
                ? clearCounts.getCompound(playerKey)
                : new CompoundTag();
        int updated = incrementSaturated(getClearCount(playerId, gymType));
        playerCounts.putInt(gymType, updated);
        clearCounts.put(playerKey, playerCounts);
        setDirty();
        return updated;
    }

    public int getChallengesStarted(UUID playerId) {
        return Math.max(0, challengesStarted.getInt(playerId.toString()));
    }

    public int incrementChallengesStarted(UUID playerId) {
        int updated = incrementSaturated(getChallengesStarted(playerId));
        challengesStarted.putInt(playerId.toString(), updated);
        setDirty();
        return updated;
    }

    private static int incrementSaturated(int value) {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put(PLAYERS_KEY, players.copy());
        tag.put(CLEAR_COUNTS_KEY, clearCounts.copy());
        tag.put(CHALLENGES_STARTED_KEY, challengesStarted.copy());
        return tag;
    }
}
