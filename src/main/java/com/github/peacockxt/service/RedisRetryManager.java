package com.github.peacockxt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Slf4j
public class RedisRetryManager {

    private static class RetryTask {
        final Runnable command;
        int retryCount = 0;

        RetryTask(Runnable command) {
            this.command = command;
        }
    }

    private final Queue<RetryTask> retryQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_RETRIES = 3;

    public void execute(Runnable command) {
        try {
            command.run();
        } catch (Exception e) {
            if (retryQueue.size() < 5000) {
                retryQueue.offer(new RetryTask(command));
            } else {
                log.error("Retry queue is full! Dropping task to save RAM.");
            }
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void process() {
        if (retryQueue.isEmpty()) return;

        int batchSize = Math.min(retryQueue.size(), 100);

        for (int i = 0; i < batchSize; i++) {
            RetryTask task = retryQueue.poll();
            if (task == null) break;
            try {
                task.command.run();
                log.info("Retry successful!");
            } catch (Exception e) {
                task.retryCount++;
                if (task.retryCount < MAX_RETRIES) {
                    retryQueue.offer(task);
                } else {
                    log.error("Task failed after {} retries. Dropping task (Dead Letter).", MAX_RETRIES);
                }
            }
        }
    }
}