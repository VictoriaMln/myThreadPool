import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskQueue {
    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private final int id;
    private final BlockingQueue<Runnable> queue;

    public TaskQueue(int queueSize) {
        this.id = COUNTER.getAndIncrement();
        this.queue = new ArrayBlockingQueue<>(queueSize);
    }

    public int getId() {
        return id;
    }

    public boolean offer(Runnable task) {
        boolean ok = queue.offer(task);
        System.out.printf(ok ? "[Pool] Task accepted into queue #%d: %s%n" :
                "[Pool] Queue #%d is full, task cannot be enqueued: %s%n", id, task);
        return ok;
    }

    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int size() {
        return queue.size();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    public void drainTo(java.util.Collection<? super Runnable> target) {
        queue.drainTo(target);
    }

    @Override
    public String toString() {
        return "TaskQueue{" + "id=" + id + ", size=" + queue.size() +
                ", remaining=" + queue.remainingCapacity() + '}';
    }
}
