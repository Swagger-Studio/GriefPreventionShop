package com.swaggerstudio.griefpreventionshop.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Reflection-based scheduling helper that lets the plugin run identically on
 * traditional BukkitScheduler servers (Paper / Purpur / Spigot) and on Folia's
 * region-threaded model, without bumping the compile-time paper-api version.
 *
 * <p>All Folia detection and method handles are resolved once in the static
 * initializer and cached — nothing is reflected per task call.</p>
 */
public final class SchedulerUtil {

    private static final boolean FOLIA;

    // Folia scheduler accessors (null when not on Folia)
    private static Method getGlobalRegionScheduler;
    private static Method globalExecute;
    private static Method bukkitGetEntityScheduler;
    private static Method entityRun;            // EntityScheduler#run(Plugin, Consumer<ScheduledTask>, Runnable)
    private static Method entityRunAtFixedRate; // EntityScheduler#runAtFixedRate(Plugin, Consumer, Runnable, long, long)
    private static Method scheduledTaskCancel;  // ScheduledTask#cancel()

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        FOLIA = folia;

        if (FOLIA) {
            try {
                getGlobalRegionScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Class<?> globalScheduler = getGlobalRegionScheduler.getReturnType();
                globalExecute = globalScheduler.getMethod("execute", Plugin.class, Runnable.class);

                bukkitGetEntityScheduler = Entity.class.getMethod("getScheduler");
                Class<?> entityScheduler = bukkitGetEntityScheduler.getReturnType();
                entityRun = entityScheduler.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunAtFixedRate = entityScheduler.getMethod(
                        "runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);

                Class<?> scheduledTask = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.ScheduledTask");
                scheduledTaskCancel = scheduledTask.getMethod("cancel");
            } catch (ReflectiveOperationException e) {
                // Should never happen on a real Folia build; fail loud so it is noticed.
                throw new IllegalStateException("Failed to resolve Folia scheduler API", e);
            }
        }
    }

    private SchedulerUtil() {
    }

    /** @return true when running on a Folia (region-threaded) server. */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * Runs a task on the "global" context — the Folia GlobalRegionScheduler, which is the
     * correct serialization point for mutations to global plugin state (e.g. GriefPrevention's
     * datastore). On non-Folia servers this runs inline when already on the primary thread
     * (preserving synchronous ordering of purchases) or on the main scheduler otherwise.
     */
    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) {
            try {
                Object scheduler = getGlobalRegionScheduler.invoke(null);
                globalExecute.invoke(scheduler, plugin, task);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to schedule global Folia task", e);
            }
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Runs a task that touches a specific entity (e.g. messaging/opening a GUI for a player).
     * On Folia this uses that entity's region scheduler; otherwise the main scheduler.
     */
    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            try {
                Object scheduler = bukkitGetEntityScheduler.invoke(entity);
                // EntityScheduler#run(plugin, Consumer<ScheduledTask>, retired) — wrap our Runnable.
                Consumer<Object> consumer = ignored -> task.run();
                entityRun.invoke(scheduler, plugin, consumer, (Runnable) null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to schedule entity Folia task", e);
            }
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Schedules a repeating task tied to an entity. On Folia this uses the entity's region
     * scheduler (with a retired callback invoked if the entity is removed / logs out);
     * otherwise the main BukkitScheduler.
     *
     * @param delay  initial delay in ticks (clamped to >= 1 for Folia compatibility)
     * @param period repeat period in ticks
     * @param retired run if the entity is retired (removed/offline) before the task ends; may be null
     */
    public static Task runAtFixedRate(Plugin plugin, Entity entity, Runnable task,
                                      Runnable retired, long delay, long period) {
        long safeDelay = Math.max(1L, delay);
        if (FOLIA) {
            try {
                Object scheduler = bukkitGetEntityScheduler.invoke(entity);
                Consumer<Object> consumer = ignored -> task.run();
                Object scheduledTask = entityRunAtFixedRate.invoke(
                        scheduler, plugin, consumer, retired, safeDelay, period);
                return new FoliaTask(scheduledTask);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to schedule repeating Folia task", e);
            }
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, safeDelay, period);
        return new BukkitTaskWrapper(bukkitTask);
    }

    /** Scheduler-agnostic task handle so callers never depend on BukkitTask vs Folia ScheduledTask. */
    public interface Task {
        void cancel();
    }

    private static final class BukkitTaskWrapper implements Task {
        private final BukkitTask task;

        private BukkitTaskWrapper(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }

    private static final class FoliaTask implements Task {
        private final Object scheduledTask;

        private FoliaTask(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public void cancel() {
            if (scheduledTask == null) return;
            try {
                scheduledTaskCancel.invoke(scheduledTask);
            } catch (ReflectiveOperationException e) {
                // Best-effort cancel; nothing actionable if this fails.
            }
        }
    }
}
