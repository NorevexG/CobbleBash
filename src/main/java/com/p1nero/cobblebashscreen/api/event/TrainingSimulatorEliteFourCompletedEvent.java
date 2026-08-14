package com.p1nero.cobblebashscreen.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

public abstract class TrainingSimulatorEliteFourCompletedEvent extends Event {
    private final ServerPlayer player;

    private TrainingSimulatorEliteFourCompletedEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public static final class Post extends TrainingSimulatorEliteFourCompletedEvent {
        public Post(ServerPlayer player) {
            super(player);
        }
    }
}
