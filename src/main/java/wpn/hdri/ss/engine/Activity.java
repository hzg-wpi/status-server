package wpn.hdri.ss.engine;

import org.apache.log4j.Logger;
import wpn.hdri.ss.client.Client;
import wpn.hdri.ss.client.ClientException;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 29.11.12
 */
public enum Activity {
    LIGHT_POLLING {
        /**
         * Schedules polling tasks at fixed rate if settings is not null. Each scheduled task deletes all previous records before putting a new one.
         *
         * @param scheduler
         * @param ctx
         * @param settings
         * @param logger
         */
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx, ActivitySettings settings, final Logger logger) {
            logger.info("Start light polling.");

            Collection<ScheduledFuture<?>> runningTasks = ctx.getRunningTasks();

            for (final PollingReadAttributeTask task : ctx.getPollTasks()) {
                final Client client = task.getDevClient();

                logger.info("Scheduling light polling task for " + task.getAttribute().getFullName());
                runningTasks.add(
                        scheduler.scheduleAtFixedRate(
                                new PollingReadAttributeTask(task.getAttribute(), client, 1000L, false,logger),
                                rnd.nextInt((int)task.getDelay()),settings == null ? task.getDelay() : settings.getDelay(),TimeUnit.MILLISECONDS));

            }

            for (final EventReadAttributeTask task : ctx.getEventTasks()) {
                try {
                    logger.info("Subscribing for changes from " + task.getAttribute().getFullName());
                    task.getDevClient().subscribeEvent(task.getAttribute().getName().getName(), new EventReadAttributeTask(task.getAttribute(),task.getDevClient(),false,logger));
                    ctx.addSubscribedTask(task);
                } catch (ClientException e) {
                    logger.warn("Event subscription failed.", e);
                }
            }
        }
    },
    HEAVY_DUTY {
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx, ActivitySettings settings, Logger logger) {
            schedulePollTasks(ctx.getPollTasks(), ctx.getRunningTasks(), scheduler, logger);
            subscribeEventTasks(ctx.getEventTasks(), ctx.getSubscribedTasks(), logger);
        }

        private void schedulePollTasks(Collection<PollingReadAttributeTask> tasks, Collection<ScheduledFuture<?>> runningTasks, ScheduledExecutorService scheduler, Logger logger) {
            for (final PollingReadAttributeTask task : tasks) {
                logger.info("Scheduling read task for " + task.getAttribute().getFullName());
                runningTasks.add(scheduler.scheduleWithFixedDelay(task, rnd.nextInt((int) task.getDelay()), task.getDelay(), TimeUnit.MILLISECONDS));
            }
        }

        private void subscribeEventTasks(Collection<EventReadAttributeTask> tasks, Collection<EventReadAttributeTask> subscribedTasks, Logger logger) {
            for (EventReadAttributeTask task : tasks) {
                try {
                    logger.info("Subscribing for changes from " + task.getAttribute().getFullName());

                    task.getDevClient().subscribeEvent(task.getAttribute().getName().getName(), task);
                    subscribedTasks.add(task);
                } catch (ClientException e) {
                    logger.warn("Event subscription failed.", e);
                }
            }
        }
    },
    IDLE {
        @Override
        public void start(ScheduledExecutorService scheduler, ActivityContext ctx, ActivitySettings settings, Logger logger) {
            cancelScheduledTasks(ctx.getRunningTasks(), logger);
            unsubscribeEventTasks(ctx.getSubscribedTasks(), logger);
        }

        private void cancelScheduledTasks(Collection<ScheduledFuture<?>> tasks, Logger logger) {
            for (ScheduledFuture<?> task : tasks) {
                logger.info("Canceling scheduled task...");
                task.cancel(false);
            }
            tasks.clear();
        }

        private void unsubscribeEventTasks(Collection<EventReadAttributeTask> tasks, Logger logger) {
            for (EventReadAttributeTask task : tasks) {
                try {
                    logger.info("Unsubscribing from " + task.getAttribute().getFullName());
                    task.getDevClient().unsubscribeEvent(task.getAttribute().getName().getName());
                } catch (ClientException e) {
                    logger.warn("Event unsubscription failed.", e);
                }
            }
            tasks.clear();
        }
    };

    protected final Random rnd = new Random();

    public abstract void start(ScheduledExecutorService scheduler, ActivityContext ctx, ActivitySettings settings, Logger logger);
}
