package ru.itmo.testing.lab4.profilingdemo;

public class ProfilingDemoMain {

    private static final int CPU_ITERATIONS = 180;
    private static final int ALLOCATION_BATCHES = 120;
    private static final int RECURSION_DEPTH = 28;
    private static final int THREAD_COUNT = 4;
    private static final int LOCK_ROUNDS = 250;

    public static void main(String[] args) throws InterruptedException {
        ProfilingWorkload workload = new ProfilingWorkload();

        long startedAt = System.currentTimeMillis();
        System.out.println("Profiling demo started");

        long cpuResult = workload.runCpuHotspot(CPU_ITERATIONS);
        long allocationResult = workload.runAllocationHotspot(ALLOCATION_BATCHES);
        long recursionResult = workload.runRecursiveHotspot(RECURSION_DEPTH);
        long lockResult = workload.runLockContentionHotspot(THREAD_COUNT, LOCK_ROUNDS);

        long finishedAt = System.currentTimeMillis();
        long totalDuration = finishedAt - startedAt;

        System.out.println("cpuResult = " + cpuResult);
        System.out.println("allocationResult = " + allocationResult);
        System.out.println("recursionResult = " + recursionResult);
        System.out.println("lockResult = " + lockResult);
        System.out.println("totalDurationMs = " + totalDuration);
        System.out.println("Profiling demo finished");
    }
}
