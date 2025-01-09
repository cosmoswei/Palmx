package me.xuqu.palmx.command;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchExecutorQueue<T> {

    static final int DEFAULT_QUEUE_SIZE = 128;
    private final Queue<T> queue;
    private final AtomicBoolean scheduled;
    private final int chunkSize;

    public BatchExecutorQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    public BatchExecutorQueue(int chunkSize) {
        this.queue = new ConcurrentLinkedQueue<>();
        this.scheduled = new AtomicBoolean(false);
        this.chunkSize = chunkSize;
    }

    public void enqueue(T message, Executor executor) {
        queue.add(message);
        scheduleFlush(executor);
    }

    protected void scheduleFlush(Executor executor) {
        if (scheduled.compareAndSet(false, true)) {
            executor.execute(() -> this.run(executor));
        }
    }

    private void run(Executor executor) {
        try {
            Queue<T> snapshot = new LinkedList<>();
            T item;
            while ((item = queue.poll()) != null) {
                snapshot.add(item);
            }
            int i = 0;
            boolean flushedOnce = false;
            while ((item = snapshot.poll()) != null) {
                if (snapshot.size() == 0) {
                    flushedOnce = false;
                    break;
                }
                if (i == chunkSize) {
                    i = 0;
                    flush(item);
                    flushedOnce = true;
                } else {
                    prepare(item);
                    i++;
                }
            }
            if (!flushedOnce && item != null) {
                flush(item);
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush(executor);
            }
        }
    }

    protected void prepare(T item) {}

    protected void flush(T item) {}
}
