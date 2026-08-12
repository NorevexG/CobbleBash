package com.nore.cobblebash.progress;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public class TrainingSimulatorUnlockData extends SavedData {
    private static final String DATA_NAME = "cobblebash_training_simulator_unlocks";
    private static final String PLAYERS_KEY = "players";
    private static final String CLEAR_COUNTS_KEY = "clearCounts";
    private static final SavedData.Factory<TrainingSimulatorUnlockData> FACTORY = new SavedData.Factory<>(
            TrainingSimulatorUnlockData::new,
            TrainingSimulatorUnlockData::load
    );

    private final CompoundTag players = new CompoundTag();
    private final CompoundTag clearCounts = new CompoundTag();

    public static TrainingSimulatorUnlockData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static TrainingSimulatorUnlockData load(CompoundTag tag, HolderLookup.Provider provider) {
        TrainingSimulatorUnlockData data = new TrainingSimulatorUnlockData();
        if (tag.contains(PLAYERS_KEY, Tag.TAG_COMPOUND)) {
            data.players.merge(tag.getCompound(PLAYERS_KEY));
        }
        if (tag.contains(CLEAR_COUNTS_KEY, Tag.TAG_COMPOUND)) {
            data.clearCounts.merge(tag.getCompound(CLEAR_COUNTS_KEY));
        }
        return data;
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
        int current = getClearCount(playerId, gymType);
        int updated = current == Integer.MAX_VALUE ? current : current + 1;
        playerCounts.putInt(gymType, updated);
        clearCounts.put(playerKey, playerCounts);
        setDirty();
        return updated;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put(PLAYERS_KEY, players.copy());
        tag.put(CLEAR_COUNTS_KEY, clearCounts.copy());
        return tag;
    }
}
