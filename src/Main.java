import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {

        // Инициализируем пул
        CustomThreadPoolExecutor pool = new CustomThreadPoolExecutor(2, 4, 5,
                TimeUnit.SECONDS, 3, 1, "DemoPool");

        System.out.println("\nОбычная нагрузка");
        for (int i = 1; i <= 6; i++) {
            int n = i;
            pool.execute(() -> {
                System.out.printf("[Task-%d] start in %s%n", n, Thread.currentThread().getName());
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException ignore) {}
                System.out.printf("[Task-%d] done%n", n);
            });
        }

        Thread.sleep(8_000);

        System.out.println("Перегрузка пула");
        try {
            for (int i = 7; i <= 17; i++) {
                int n = i;
                pool.execute(() -> {
                    System.out.printf("[Task-%d] run%n", n);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignore) {}
                });
            }
        } catch (Exception ex) {
            System.out.println("Overload: " + ex);
        }

        pool.shutdown();
        System.out.println("\n[Main] shutdown()");
        Thread.sleep(6_000);

        System.out.println("\nCрочное завершение");
        List<Runnable> rest = pool.shutdownNow();
        System.out.println("[Main] shutdownNow() returned " + rest.size() + " tasks.");
    }
}
