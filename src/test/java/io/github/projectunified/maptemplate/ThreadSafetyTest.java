package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadSafetyTest {
    private static void runConcurrently(int threadCount, int iterationsPerThread, Runnable task) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    task.run();
                }
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testThreadSafety() throws InterruptedException, ExecutionException {
        Map<String, Object> map = Collections.singletonMap("key", "value");
        MapTemplate template = MapTemplate.mark("{key}");
        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(map)
                .build();

        runConcurrently(10, 1000, () -> assertEquals("value", template.apply(replacer)));
    }

    @Test
    public void testConcurrentModificationWithNestedVariables() throws InterruptedException, ExecutionException {
        // This test ensures that the stack-based parsing in applyString
        // doesn't use any shared mutable state.
        Map<String, Object> map = new ConcurrentHashMap<>();
        map.put("a", "1");
        map.put("b", "{a}");
        map.put("c", "{b}");

        MapTemplate template = MapTemplate.mark("{c}");
        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(map)
                .build();

        runConcurrently(10, 500, () -> assertEquals("1", template.apply(replacer)));
    }

    @Test
    public void testSameTemplateWithDifferentReplacers() throws InterruptedException, ExecutionException {
        MapTemplate template = MapTemplate.mark(Collections.singletonList("Hello {name}"));

        runConcurrently(10, 500, () -> {
            assertEquals(Collections.singletonList("Hello John"), template.apply(string -> string.replace("{name}", "John")));
            assertEquals(Collections.singletonList("Hello Jane"), template.apply(string -> string.replace("{name}", "Jane")));
        });
    }
}
