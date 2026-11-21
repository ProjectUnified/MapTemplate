package io.github.projectunified.maptemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MapTemplateBenchmark {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            variables.put("key" + i, "value" + i);
        }
        variables.put("name", "John");
        variables.put("age", 25);
        variables.put("test", "test");

        String testStr = "Hello {name}, your age is {age}. {missing} and {test}";
        String resultStr = "Hello John, your age is 25. {missing} and test";
        List<Object> expected = Arrays.asList(resultStr, 1, "plain");

        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(variables)
                .build();

        long markStart = System.currentTimeMillis();
        MapTemplate template = MapTemplate.mark(Arrays.asList(testStr, 1, "plain"));
        long markEnd = System.currentTimeMillis();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            check(expected.equals(template.apply(replacer)), "Results don't match");
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            check(expected.equals(template.apply(replacer)), "Results don't match");
        }
        long end = System.currentTimeMillis();

        System.out.println("Marking time: " + (markEnd - markStart) + "ms");
        System.out.println("Apply time: " + (end - start) + "ms");
    }
}
