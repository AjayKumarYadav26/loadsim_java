package com.ktlo.simulator.service;

import com.ktlo.simulator.model.ThreadPoolStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class ThreadPoolMonitorService {

    private final ThreadPoolTaskExecutor taskExecutor;

    public ThreadPoolMonitorService(@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public ThreadPoolStatus getCurrentStatus() {
        ThreadPoolStatus status = new ThreadPoolStatus();
        status.setActiveThreads(taskExecutor.getActiveCount());
        status.setPoolSize(taskExecutor.getPoolSize());
        status.setCorePoolSize(taskExecutor.getCorePoolSize());
        status.setMaxPoolSize(taskExecutor.getMaxPoolSize());
        status.setQueueSize(taskExecutor.getThreadPoolExecutor().getQueue().size());
        status.setCompletedTasks(taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
        status.setIsExhausted(taskExecutor.getActiveCount() >= taskExecutor.getMaxPoolSize());
        return status;
    }

    public int getActiveThreads() {
        return taskExecutor.getActiveCount();
    }

    public int getQueuedTasks() {
        return taskExecutor.getThreadPoolExecutor().getQueue().size();
    }

    public long getCompletedTasks() {
        return taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
    }
}
