package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class VideoAsyncConfig {

    @Bean("videoTaskExecutor")
    public Executor videoAsyncConfig() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        executor.setCorePoolSize(5);
        // 设置最大线程数
        executor.setMaxPoolSize(10);
        // 设置队列大小
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("videoTaskExecutor-");
        // 拒绝策略：当队列和线程池都满了
        // CallerRunsPolicy 表示由调用者线程（即 Tomcat 线程）直接执行，起到一种降级压力的作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
