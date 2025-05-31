import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.*;
import java.util.function.IntSupplier;

public class Worker implements Runnable {
    private final TaskQueue queue;
    private final long keepAliveMillis;
    private final IntSupplier poolSizeSupplier;
    private final int corePoolSize;
    private final AtomicBoolean shutdownFlag;

    public Worker(TaskQueue q, long keepAliveMillis, IntSupplier poolSizeSupplier, int corePoolSize,
                  AtomicBoolean shutdownFlag) {
        this.queue = q;
        this.keepAliveMillis = keepAliveMillis;
        this.poolSizeSupplier = poolSizeSupplier;
        this.corePoolSize = corePoolSize;
        this.shutdownFlag = shutdownFlag;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        System.out.printf("[Worker] %s started.%n", name);

        try {
            while (true) {
                if (shutdownFlag.get() && queue.size() == 0) break;

                Runnable task;
                try {
                    task = queue.poll(keepAliveMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    System.out.printf("[Worker] %s interrupted, exiting.%n", name);
                    break;
                }

                if (task != null) {
                    if (shutdownFlag.get()) {
                        System.out.printf("[Worker] %s pool shutdown - skipping %s%n", name, task);
                        continue;
                    }
                    System.out.printf("[Worker] %s executes %s%n", name, task);
                    try {
                        task.run();
                    } catch (Throwable t) {
                        System.out.printf("[Worker] %s caught exception: %s%n", name, t);
                    }
                } else {
                    if (poolSizeSupplier.getAsInt() > corePoolSize) {
                        System.out.printf("[Worker] %s idle timeout (%,d ms), stopping.%n", name, keepAliveMillis);
                        break;
                    }
                }
            }
        } finally {
            System.out.printf("[Worker] %s terminated.%n", name);
        }
    }
}
