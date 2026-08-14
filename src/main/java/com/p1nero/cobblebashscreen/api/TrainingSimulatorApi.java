package com.p1nero.cobblebashscreen.api;

import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.progress.GymProgressManager;
import com.p1nero.cobblebashscreen.progress.TrainingSimulatorData;
import net.minecraft.server.level.ServerPlayer;

public final class TrainingSimulatorApi {
    private TrainingSimulatorApi() {
    }

    public static boolean isGymUnlocked(ServerPlayer player, GymType gymType) {
        return data(player).isUnlocked(player.getUUID(), gymType.ordinal());
    }

    public static boolean unlockGym(ServerPlayer player, GymType gymType) {
        return data(player).unlock(player.getUUID(), gymType.ordinal());
    }

    public static int unlockAllGyms(ServerPlayer player) {
        int unlocked = 0;
        for (GymType gymType : GymType.values()) {
            if (unlockGym(player, gymType)) {
                unlocked++;
            }
        }
        return unlocked;
    }

    public static boolean isEliteFourAvailable(ServerPlayer player) {
        return GymProgressManager.get(player.getUUID()).getCompletedGymCount() >= GymType.values().length;
    }

    public static boolean isEliteFourUnlocked(ServerPlayer player) {
        return data(player).isUnlocked(player.getUUID(), GymType.values().length);
    }

    public static boolean unlockEliteFour(ServerPlayer player) {
        return isEliteFourAvailable(player)
                && data(player).unlock(player.getUUID(), GymType.values().length);
    }

    public static int getGymClearCount(ServerPlayer player, GymType gym) {
        return data(player).getClearCount(player.getUUID(), gym.getId());
    }

    public static boolean hasClearedGym(ServerPlayer player, GymType gym) {
        return getGymClearCount(player, gym) > 0;
    }

    public static int getClearedGymCount(ServerPlayer player) {
        int cleared = 0;
        for (GymType gym : GymType.values()) {
            if (hasClearedGym(player, gym)) {
                cleared++;
            }
        }
        return cleared;
    }

    public static int getChallengesStarted(ServerPlayer player) {
        return data(player).getChallengesStarted(player.getUUID());
    }

    private static TrainingSimulatorData data(ServerPlayer player) {
        return TrainingSimulatorData.get(player.server);
    }
}
