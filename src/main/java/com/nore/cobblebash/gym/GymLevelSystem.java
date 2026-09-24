package com.nore.cobblebash.gym;

import com.nore.cobblebash.Config;

import java.util.List;

public class GymLevelSystem {

    private static final int TOTAL_GYMS = 18;

    public static int[] getTrainerLevels(int completedGyms) {
        int gymIndex = Math.max(0, Math.min(completedGyms, TOTAL_GYMS - 1));
        List<? extends Integer> configuredLevels = Config.GYM_BASE_LEVELS.get();
        List<? extends Integer> levels = configuredLevels.isEmpty()
                ? Config.DEFAULT_GYM_BASE_LEVELS
                : configuredLevels;
        int baseLevel = levels.get(Math.min(gymIndex, levels.size() - 1));

        return new int[]{
                clampLevel(baseLevel + Config.TRAINER_ONE_LEVEL_OFFSET.get()),
                clampLevel(baseLevel + Config.TRAINER_TWO_LEVEL_OFFSET.get()),
                clampLevel(baseLevel + Config.GYM_LEADER_LEVEL_OFFSET.get())
        };
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(100, level));
    }
}
