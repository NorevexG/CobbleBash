package com.nore.cobblebash.instance;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class GymInstanceManager {
    private static final Map<UUID, GymInstance> ACTIVE_BY_PLAYER = new HashMap<>();
    private static final Map<Integer, GymInstance> ACTIVE_BY_SLOT = new HashMap<>();
    private static final Map<String, Queue<Integer>> FREE_SLOTS_BY_GYM = new HashMap<>();

    private static int nextSlotId = 0;

    public static GymInstance createOrGet(
            UUID playerId,
            String gymType,
            boolean repeatClear,
            int[] trainerLevels,
            ResourceKey<Level> returnDimension,
            double returnX,
            double returnY,
            double returnZ,
            float returnYRot,
            float returnXRot,
            GameType returnGameMode
    ) {
        GymInstance existing = ACTIVE_BY_PLAYER.get(playerId);
        if (existing != null) {
            return existing;
        }

        int slotId = getReusableSlot(gymType);

        GymInstance instance = new GymInstance(
                slotId,
                playerId,
                gymType,
                repeatClear,
                trainerLevels,
                returnDimension,
                returnX,
                returnY,
                returnZ,
                returnYRot,
                returnXRot,
                returnGameMode
        );

        ACTIVE_BY_PLAYER.put(playerId, instance);
        ACTIVE_BY_SLOT.put(slotId, instance);

        return instance;
    }

    public static GymInstance getActive(UUID playerId) {
        return ACTIVE_BY_PLAYER.get(playerId);
    }

    public static GymInstance clear(UUID playerId) {
        GymInstance instance = ACTIVE_BY_PLAYER.remove(playerId);

        if (instance != null) {
            ACTIVE_BY_SLOT.remove(instance.getSlotId());
            FREE_SLOTS_BY_GYM
                    .computeIfAbsent(instance.getGymType(), ignored -> new ArrayDeque<>())
                    .add(instance.getSlotId());
        }

        return instance;
    }

    public static int getActiveCount() {
        return ACTIVE_BY_PLAYER.size();
    }

    public static int getFreeSlotCount() {
        int count = 0;
        for (Queue<Integer> slots : FREE_SLOTS_BY_GYM.values()) {
            count += slots.size();
        }

        return count;
    }

    public static int getNextSlotId() {
        return nextSlotId;
    }

    private static int getReusableSlot(String gymType) {
        Queue<Integer> freeSlots = FREE_SLOTS_BY_GYM.get(gymType);
        if (freeSlots != null && !freeSlots.isEmpty()) {
            return freeSlots.poll();
        }

        return nextSlotId++;
    }
}
