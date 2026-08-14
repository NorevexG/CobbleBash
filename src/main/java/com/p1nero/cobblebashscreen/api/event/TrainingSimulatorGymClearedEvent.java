package com.p1nero.cobblebashscreen.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public abstract class TrainingSimulatorGymClearedEvent extends Event {
    private final ServerPlayer player;
    private final String gymId;
    private final int clearCount;
    private final boolean firstClear;

    private TrainingSimulatorGymClearedEvent(
            ServerPlayer player,
            String gymId,
            int clearCount,
            boolean firstClear
    ) {
        this.player = player;
        this.gymId = gymId;
        this.clearCount = clearCount;
        this.firstClear = firstClear;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getGymId() {
        return gymId;
    }

    public int getClearCount() {
        return clearCount;
    }

    public boolean isFirstClear() {
        return firstClear;
    }

    public static final class Post extends TrainingSimulatorGymClearedEvent {
        public Post(ServerPlayer player, String gymId, int clearCount, boolean firstClear) {
            super(player, gymId, clearCount, firstClear);
        }
    }
}
