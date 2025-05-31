import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.IntSupplier;
import java.util.concurrent.*;

public class CustomThreadPoolExecutor implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveMillis;
    private final int minSpareThreads;
    private final int queueSize;

    private final List<TaskQueue> queues = new ArrayList<>();
    private final CustomThreadFactory threadFactory;
    private final Set<Thread> workers = new HashSet<>();
    private final AtomicInteger nextIdx = new AtomicInteger(0);
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);

    public CustomThreadPoolExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit,
                                    int queueSize, int minSpareThreads, String poolName) {
        if (corePoolSize < 0 || maxPoolSize < corePoolSize)
            throw new IllegalArgumentException("Pool size parameters are illegal");

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveMillis = unit.toMillis(keepAliveTime);
        this.minSpareThreads = minSpareThreads;
        this.queueSize = queueSize;
        this.threadFactory = new CustomThreadFactory(poolName, false);

        for (int i = 0; i < corePoolSize; i++) addWorker();
    }

    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task);

        synchronized (this) {
            if (shutdownFlag.get()) {
                reject(task);
                return;
            }

            TaskQueue q = queues.get(nextIdx.getAndIncrement() % queues.size());

            if (q.offer(task)) {
                System.out.printf("[Pool] Task accepted into queue #%d (size=%d, remaining=%d): %s%n",
                        q.getId(), q.size(), q.remainingCapacity(), task);
                ensureSpare();
                return;
            }

            if (workers.size() < maxPoolSize) {
                System.out.printf("[Pool] Queue is full - spawning extra worker.%n");
                addWorker();
                q = queues.get(nextIdx.getAndIncrement() % queues.size());
                if (q.offer(task)){
                    System.out.printf("[Pool] Task accepted into queue #%d (size=%d, remaining=%d): %s%n",
                            q.getId(), q.size(), q.remainingCapacity(), task);
                    return;
                }
            }
            reject(task);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> c) {
        FutureTask<T> ft = new FutureTask<>(c);
        execute(ft);
        return ft;
    }

    @Override
    public void shutdown() {
        shutdownFlag.set(true);
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdownFlag.set(true);
        List<Runnable> remaining = new ArrayList<>();
        synchronized (this) {
            for (TaskQueue q : queues) q.drainTo(remaining);
            for (Thread t : workers) t.interrupt();
        }
        return remaining;
    }

    private void addWorker() {
        TaskQueue q = new TaskQueue(queueSize);
        queues.add(q);

        IntSupplier sizeSup = () -> workers.size();
        Worker w = new Worker(q, keepAliveMillis, sizeSup, corePoolSize, shutdownFlag);
        Thread t = threadFactory.newThread(w);
        workers.add(t);
        t.start();
    }

    private void ensureSpare() {
        long idle = workers.stream().filter(th -> th.getState() == Thread.State.WAITING
                || th.getState() == Thread.State.TIMED_WAITING).count();
        if (idle < minSpareThreads && workers.size() < maxPoolSize) {
            System.out.printf("[Pool] Idle=(%d) < minSpareThreads=(%d) - spawning extra worker.%n", idle, minSpareThreads);
            addWorker();
        }
    }

    private void reject(Runnable task) {
        System.out.printf("[Rejected] Task %s was rejected due to overload.%n", task);
        throw new RejectedExecutionException("Task rejected: " + task);
    }
}
