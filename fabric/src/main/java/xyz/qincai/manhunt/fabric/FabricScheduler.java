package xyz.qincai.manhunt.fabric;

import net.minecraft.server.MinecraftServer;
import xyz.qincai.manhunt.platform.Scheduler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FabricScheduler implements Scheduler {
    private final MinecraftServer server;
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, ScheduledTask> tasks = new ConcurrentHashMap<>();

    public FabricScheduler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public int runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        int taskId = taskIdCounter.incrementAndGet();
        ScheduledTask scheduled = new ScheduledTask(task, periodTicks);
        tasks.put(taskId, scheduled);
        if (delayTicks > 0) {
            server.execute(() -> {
                if (!scheduled.cancelled) {
                    scheduled.run();
                }
            });
        } else {
            scheduled.run();
        }
        return taskId;
    }

    @Override
    public void cancelTask(int taskId) {
        ScheduledTask task = tasks.remove(taskId);
        if (task != null) task.cancelled = true;
    }

    private class ScheduledTask implements Runnable {
        private final Runnable task;
        private final long periodTicks;
        volatile boolean cancelled = false;

        ScheduledTask(Runnable task, long periodTicks) {
            this.task = task;
            this.periodTicks = periodTicks;
        }

        @Override
        public void run() {
            if (cancelled) return;
            task.run();
            if (periodTicks > 0 && !cancelled) {
                server.execute(this);
            }
        }
    }
}
