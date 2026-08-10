package com.nore.cobblebash.progress;

import com.nore.cobblebash.structure.EliteFourStructure;
import com.nore.cobblebash.structure.GymPlatformBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class GymCacheMigrationData extends SavedData {
    private static final String DATA_NAME = "cobblebash_gym_cache_migrations";
    private static final String MIGRATION_ID = "cached_slot_layout_0_1_3_full_slot";
    private static final String MIGRATED_SLOTS_KEY = "migratedSlots";
    private static final SavedData.Factory<GymCacheMigrationData> FACTORY = new SavedData.Factory<>(
            GymCacheMigrationData::new,
            GymCacheMigrationData::load
    );

    private final Set<String> migratedSlots = new HashSet<>();

    public static GymCacheMigrationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static GymCacheMigrationData load(CompoundTag tag, HolderLookup.Provider provider) {
        GymCacheMigrationData data = new GymCacheMigrationData();
        if (tag.contains(MIGRATED_SLOTS_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag migrated = tag.getCompound(MIGRATED_SLOTS_KEY);
            for (String key : migrated.getAllKeys()) {
                if (migrated.getBoolean(key)) {
                    data.migratedSlots.add(key);
                }
            }
        }
        return data;
    }

    private void migrateSlotIfNeeded(String gymType, int slotId, Runnable migration) {
        String key = MIGRATION_ID + ":" + gymType + ":" + slotId;
        if (migratedSlots.contains(key)) {
            return;
        }

        migration.run();
        migratedSlots.add(key);
        setDirty();
    }

    public void migrateGymSlotIfNeeded(ServerLevel level, String gymType, int slotId, BlockPos origin) {
        migrateSlotIfNeeded(gymType, slotId, () -> {
            if (EliteFourStructure.GYM_TYPE.equals(gymType)) {
                EliteFourStructure.clearCachedBlocks(level, origin);
            } else {
                GymPlatformBuilder.clearCachedGymBlocks(level, origin, gymType);
            }
        });
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag migrated = new CompoundTag();
        for (String key : migratedSlots) {
            migrated.putBoolean(key, true);
        }
        tag.put(MIGRATED_SLOTS_KEY, migrated);
        return tag;
    }
}
