package com.nore.cobblebash.util;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DelayedTaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new ScheduledTask(delayTicks, action));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        Iterator<ScheduledTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            task.ticksRemaining--;

            if (task.ticksRemaining <= 0) {
                task.action.run();
                iterator.remove();
            }
        }
    }

    private static class ScheduledTask {
        private int ticksRemaining;
        private final Runnable action;

        private ScheduledTask(int ticksRemaining, Runnable action) {
            this.ticksRemaining = ticksRemaining;
            this.action = action;
        }
    }
}