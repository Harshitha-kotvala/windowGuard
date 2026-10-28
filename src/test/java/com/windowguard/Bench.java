package com.windowguard;

public class Bench {

    static long sink;   // consume results so the JIT can't delete the work

    static double nsPerOp(RateLimiter l, int history, int ops) {
        long window = history;              // one event per time unit, so ~history live events
        int limit = Integer.MAX_VALUE;      // never rejects: we measure lookup cost, not rejection
        long now = 0;
        for (int i = 0; i < history; i++) l.tryAcquire("k", limit, window, 1, now++);

        long start = System.nanoTime();
        for (int i = 0; i < ops; i++) {
            if (l.tryAcquire("k", limit, window, 1, now++).allowed()) sink++;
        }
        return (System.nanoTime() - start) / (double) ops;
    }

    static double avgReference(int history, int ops, int repeats) {
        double sum = 0;
        for (int r = 0; r < repeats; r++) sum += nsPerOp(new ReferenceLimiter(), history, ops);
        return sum / repeats;
    }

    static long usedMemory() {
        for (int i = 0; i < 5; i++) {
            System.gc();
            try { Thread.sleep(50); } catch (InterruptedException e) { }
        }
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    public static void main(String[] args) {
        int[] histories = {100, 1_000, 10_000};

        // warm-up: let the JIT compile both implementations, results discarded
        for (int round = 0; round < 3; round++) {
            for (int h : histories) {
                nsPerOp(new FastLimiter(h), h, 200_000);
                avgReference(h, 200, 3);
            }
        }

        System.out.println("history | reference ns/op | fast ns/op");
        for (int h : histories) {
            double ref = avgReference(h, 500, 20);
            double fast = nsPerOp(new FastLimiter(h), h, 1_000_000);
            System.out.printf("%7d | %15.0f | %10.0f%n", h, ref, fast);
        }

        // memory per key: 100k keys, 5 events each (key strings included in the figure)
        int keys = 100_000;
        long before = usedMemory();
        FastLimiter l = new FastLimiter(1000);
        for (int k = 0; k < keys; k++) {
            String key = "client-" + k;
            for (int e = 0; e < 5; e++) l.tryAcquire(key, 5, 1000, 1, e);
        }
        long after = usedMemory();
        sink += l.keyCount();   // keep the limiter alive until after the measurement
        System.out.println("approx bytes per key: " + (after - before) / keys);

        System.out.println("sink=" + sink);
        System.out.println("JDK " + System.getProperty("java.version")
                + ", " + Runtime.getRuntime().availableProcessors() + " cores");
    }
}