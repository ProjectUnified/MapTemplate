package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadSafetyTest {
    @Test
    public void testThreadSafety() throws InterruptedException, ExecutionException {
        Map<String, Object> map = Collections.singletonMap("key", "value");
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    assertEquals("value", template.apply("{key}"));
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testConcurrentModificationWithNestedVariables() throws InterruptedException, ExecutionException {
        // This test ensures that the stack-based parsing in applyString
        // doesn't use any shared mutable state.
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("a", "1");
        map.put("b", "{a}");
        map.put("c", "{b}");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        int threadCount = 10;
        int iterationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    assertEquals("1", template.apply("{c}"));
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
