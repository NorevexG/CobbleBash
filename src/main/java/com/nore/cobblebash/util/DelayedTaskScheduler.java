package com.nore.cobblebash.util;

import com.nore.cobblebash.CobbleBash;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DelayedTaskScheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new ScheduledTask(Math.max(0, delayTicks), action));
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        List<ScheduledTask> dueTasks = new ArrayList<>();
        Iterator<ScheduledTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            task.ticksRemaining--;

            if (task.ticksRemaining <= 0) {
                iterator.remove();
                dueTasks.add(task);
            }
        }

        for (ScheduledTask task : dueTasks) {
            try {
                task.action.run();
            } catch (Exception exception) {
                CobbleBash.LOGGER.error("CobbleBash delayed task failed.", exception);
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
