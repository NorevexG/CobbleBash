package com.p1nero.cobblebashscreen.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class TrainingSimulatorOpenEvent extends Event {
    private final ServerPlayer player;
    private final Level level;
    private final BlockPos pos;

    private TrainingSimulatorOpenEvent(ServerPlayer player, Level level, BlockPos pos) {
        this.player = player;
        this.level = level;
        this.pos = pos.immutable();
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public static final class Pre extends TrainingSimulatorOpenEvent implements ICancellableEvent {
        public Pre(ServerPlayer player, Level level, BlockPos pos) {
            super(player, level, pos);
        }
    }
}
