package org.taskflow.core.thread.pool;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义线程工厂
 * Created by ytyht226 on 2023/3/3.
 */
public class CustomThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger mThreadNum = new AtomicInteger(1);

    private final String mPrefix;

    private final boolean mDaemon;

    private ThreadGroup threadGroup;

    public CustomThreadFactory() {
        this("taskFlow-" + POOL_SEQ.getAndIncrement(), true);
    }

    public CustomThreadFactory(String prefix) {
        this(prefix, true);
    }

    public CustomThreadFactory(String prefix, boolean daemon) {
        mPrefix = prefix + "-thread-";
        mDaemon = daemon;
        SecurityManager s = System.getSecurityManager();
        threadGroup = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    public static CustomThreadFactory newInstance() {
        return new CustomThreadFactory();
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public void setThreadGroup(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = mPrefix + mThreadNum.getAndIncrement();
        Thread ret = new Thread(threadGroup, runnable, name, 0);
        ret.setDaemon(mDaemon);
        return ret;
    }
}