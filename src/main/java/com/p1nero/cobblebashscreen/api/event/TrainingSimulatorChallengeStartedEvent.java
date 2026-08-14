package com.p1nero.cobblebashscreen.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public abstract class TrainingSimulatorChallengeStartedEvent extends Event {
    private final ServerPlayer player;
    private final String gymId;
    private final boolean eliteFour;

    private TrainingSimulatorChallengeStartedEvent(ServerPlayer player, String gymId, boolean eliteFour) {
        this.player = player;
        this.gymId = gymId;
        this.eliteFour = eliteFour;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public String getGymId() {
        return gymId;
    }

    public boolean isEliteFour() {
        return eliteFour;
    }

    public static final class Post extends TrainingSimulatorChallengeStartedEvent {
        public Post(ServerPlayer player, String gymId, boolean eliteFour) {
            super(player, gymId, eliteFour);
        }
    }
}
