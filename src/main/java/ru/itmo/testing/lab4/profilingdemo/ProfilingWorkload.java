package ru.itmo.testing.lab4.profilingdemo;

import java.util.ArrayList;
import java.util.List;

public class ProfilingWorkload {

    private static final Object SHARED_LOCK = new Object();
    private static final int INNER_CPU_LOOP = 45_000;
    private static final int ALLOCATION_SIZE = 2_000;
    private static final int RETAINED_OBJECTS_LIMIT = 24;
    private static final int LOCK_SLEEP_MILLIS = 2;
    private static final int FIBONACCI_BASE_CASE = 1;

    private final List<int[]> retainedObjects = new ArrayList<>();

    public long runCpuHotspot(int iterations) {
        long result = 0L;
        for (int i = 0; i < iterations; i++) {
            result += expensiveMath(i);
            result += expensiveBranching(i);
        }
        return result;
    }

    public long runAllocationHotspot(int batches) {
        long checksum = 0L;
        for (int batch = 0; batch < batches; batch++) {
            int[] data = new int[ALLOCATION_SIZE + batch];
            for (int i = 0; i < data.length; i++) {
                data[i] = (i * 31) ^ batch;
                checksum += data[i];
            }
            retainedObjects.add(data);
            if (retainedObjects.size() > RETAINED_OBJECTS_LIMIT) {
                retainedObjects.remove(0);
            }
        }
        return checksum + retainedObjects.size();
    }

    public long runRecursiveHotspot(int depth) {
        return fibonacci(depth);
    }

    public long runLockContentionHotspot(int threadCount, int rounds) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        ContentionCounter counter = new ContentionCounter();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> doLockedWork(rounds, counter), "profiling-demo-" + i);
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        return counter.getValue();
    }

    private long expensiveMath(int seed) {
        long acc = 0L;
        for (int i = 1; i < INNER_CPU_LOOP; i++) {
            acc += (seed + i) * (long) (i % 17);
            acc ^= (acc << 3) ^ (acc >>> 2);
        }
        return acc;
    }

    private long expensiveBranching(int seed) {
        long acc = 0L;
        for (int i = 0; i < INNER_CPU_LOOP; i++) {
            if ((i + seed) % 7 == 0) {
                acc += i * 13L;
            } else if ((i + seed) % 5 == 0) {
                acc -= i * 11L;
            } else {
                acc += i;
            }
        }
        return acc;
    }

    private long fibonacci(int n) {
        if (n <= FIBONACCI_BASE_CASE) {
            return n;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    private void doLockedWork(int rounds, ContentionCounter counter) {
        for (int i = 0; i < rounds; i++) {
            synchronized (SHARED_LOCK) {
                counter.increment();
                busySleep();
            }
        }
    }

    private void busySleep() {
        try {
            Thread.sleep(LOCK_SLEEP_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class ContentionCounter {
        private long value;

        void increment() {
            value++;
        }

        long getValue() {
            return value;
        }
    }
}
