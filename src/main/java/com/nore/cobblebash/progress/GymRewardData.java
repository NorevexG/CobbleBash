package com.nore.cobblebash.progress;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;
import java.util.UUID;

public class GymRewardData extends SavedData {
    private static final String DATA_NAME = "cobblebash_gym_rewards";
    private static final String TRAINER_RIBBON_GYMS_KEY = "trainerRibbonGyms";
    private static final String ELITE_FOUR_DISK_AWARDED_KEY = "eliteFourDiskAwarded";
    private static final SavedData.Factory<GymRewardData> FACTORY = new SavedData.Factory<>(
            GymRewardData::new,
            GymRewardData::load
    );

    private final CompoundTag trainerRibbonGyms = new CompoundTag();
    private final CompoundTag eliteFourDiskAwarded = new CompoundTag();

    public static GymRewardData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static GymRewardData load(CompoundTag tag, HolderLookup.Provider provider) {
        GymRewardData data = new GymRewardData();
        if (tag.contains(TRAINER_RIBBON_GYMS_KEY, Tag.TAG_COMPOUND)) {
            data.trainerRibbonGyms.merge(tag.getCompound(TRAINER_RIBBON_GYMS_KEY));
        }
        if (tag.contains(ELITE_FOUR_DISK_AWARDED_KEY, Tag.TAG_COMPOUND)) {
            data.eliteFourDiskAwarded.merge(tag.getCompound(ELITE_FOUR_DISK_AWARDED_KEY));
        }
        return data;
    }

    public Optional<String> getTrainerRibbonGym(UUID playerId) {
        String key = playerId.toString();
        if (!trainerRibbonGyms.contains(key, Tag.TAG_STRING)) {
            return Optional.empty();
        }

        return Optional.of(trainerRibbonGyms.getString(key));
    }

    public String getOrSetTrainerRibbonGym(UUID playerId, String gymType) {
        Optional<String> existing = getTrainerRibbonGym(playerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        trainerRibbonGyms.putString(playerId.toString(), gymType);
        setDirty();
        return gymType;
    }

    public boolean hasEliteFourDiskAwarded(UUID playerId) {
        return eliteFourDiskAwarded.getBoolean(playerId.toString());
    }

    public boolean markEliteFourDiskAwarded(UUID playerId) {
        if (hasEliteFourDiskAwarded(playerId)) {
            return false;
        }

        eliteFourDiskAwarded.putBoolean(playerId.toString(), true);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put(TRAINER_RIBBON_GYMS_KEY, trainerRibbonGyms.copy());
        tag.put(ELITE_FOUR_DISK_AWARDED_KEY, eliteFourDiskAwarded.copy());
        return tag;
    }
}
