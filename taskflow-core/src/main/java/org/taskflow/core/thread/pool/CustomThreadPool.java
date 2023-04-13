package org.taskflow.core.thread.pool;

import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自定义线程池
 * Created by ytyht226 on 2023/3/3.
 */
public class CustomThreadPool {

    /**
     * 固定线程，推荐使用
     */
    public static ExecutorService newFixedThreadPoolWrapper(int nThreads) {
        return TtlExecutors.getTtlExecutorService(newFixedThreadPool(nThreads));
    }

    /**
     * 固定线程
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                CustomThreadFactory.newInstance());
    }

}