package com.manerfan.booster.bootstrap.loader.loaderutils;

import lombok.extern.slf4j.Slf4j;

/**
 * IsolatedThreadGroup
 *
 * <pre>
 *     线程池工具
 * </pre>
 *
 * @author Maner.Fan
 * @date 2022/8/6
 */
@Slf4j
public class IsolatedThreadGroup extends ThreadGroup {
    private final Object monitor = new Object();

    private Throwable exception;

    public IsolatedThreadGroup(String name) {
        super(name);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!(ex instanceof ThreadDeath)) {
            synchronized (this.monitor) {
                this.exception = (this.exception == null ? ex : this.exception);
            }

            log.error("[ManerFan Booster Bootstrap] Uncaught Exception", ex);
        }
    }

    public synchronized void rethrowUncaughtException() throws RuntimeException {
        synchronized (this.monitor) {
            if (this.exception != null) {
                throw new RuntimeException(
                    "An exception occurred while running. " + this.exception.getMessage(), this.exception);
            }
        }
    }
}
