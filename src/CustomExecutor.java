public interface CustomExecutor extends java.util.concurrent.Executor {
    <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> callable);
    void shutdown();
    java.util.List<java.lang.Runnable> shutdownNow();
}
