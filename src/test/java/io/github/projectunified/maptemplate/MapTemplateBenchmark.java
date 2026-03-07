package io.github.projectunified.maptemplate;

import java.util.HashMap;
import java.util.Map;

public class MapTemplateBenchmark {
    public static void main(String[] args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            map.put("key" + i, "value" + i);
        }
        map.put("name", "John");
        map.put("age", 25);
        map.put("test", "test");

        String testStr = "Hello {name}, your age is {age}. {missing} and {test}";

        // Warmup
        for (int i = 0; i < 1000; i++) {
            MapTemplate.apply(testStr, map);
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            MapTemplate.apply(testStr, map);
        }
        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start) + "ms");
    }
}
