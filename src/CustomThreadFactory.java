import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final String poolName;
    private final boolean daemon;
    private final AtomicInteger counter = new AtomicInteger(1);

    public CustomThreadFactory(String poolName, boolean daemon) {
        this.poolName = poolName;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-worker-" + counter.getAndIncrement();
        Thread t = new Thread(r, threadName);
        t.setDaemon(daemon);
        System.out.printf("[ThreadFactory] Creating new thread: %s%n", threadName);

        t.setUncaughtExceptionHandler((th, ex) ->
                System.out.printf("[ThreadFactory] %s terminated with exception: %s%n", th.getName(), ex));
        return t;
    }
}
