package xyz.qincai.manhunt.platform;

public interface Scheduler {
    int runTaskTimer(Runnable task, long delayTicks, long periodTicks);
    void cancelTask(int taskId);
}
