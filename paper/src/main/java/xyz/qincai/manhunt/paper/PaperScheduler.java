package xyz.qincai.manhunt.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import xyz.qincai.manhunt.platform.Scheduler;

public class PaperScheduler implements Scheduler {
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
    }

    @Override
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
}
