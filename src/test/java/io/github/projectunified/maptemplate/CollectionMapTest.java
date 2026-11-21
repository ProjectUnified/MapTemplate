package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionMapTest {
    private static final Function<String, Object> REPLACER = VariableStringReplacer.builder()
            .setVariableMap(Collections.singletonMap("name", "John"))
            .build();

    @Test
    public void testList() {
        List<String> list = Arrays.asList("Hello {name}", "{name}", "pure string");
        List<?> result = (List<?>) MapTemplate.mark(list).apply(REPLACER);

        assertEquals(3, result.size());
        assertEquals("Hello John", result.get(0));
        assertEquals("John", result.get(1));
        assertEquals("pure string", result.get(2));
    }

    @Test
    public void testSet() {
        Set<String> set = new HashSet<>(Collections.singletonList("{name}"));
        Set<?> result = (Set<?>) MapTemplate.mark(set).apply(REPLACER);

        assertEquals(1, result.size());
        assertTrue(result.contains("John"));
    }

    @Test
    public void testMap() {
        Map<String, String> targetMap = new HashMap<>();
        targetMap.put("greeting", "Hello {name}");
        targetMap.put("signature", "{name}");

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(targetMap).apply(REPLACER);

        assertEquals("Hello John", result.get("greeting"));
        assertEquals("John", result.get("signature"));
    }

    @Test
    public void testNestedStructures() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("list", Arrays.asList("{name}", "static"));

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(nested).apply(REPLACER);
        List<?> resultList = (List<?>) result.get("list");

        assertEquals("John", resultList.get(0));
    }

    @Test
    public void testShapeIsKept() {        assertInstanceOf(ArrayList.class, MapTemplate.mark(Arrays.asList("{name}", 1)).apply(REPLACER));
        assertInstanceOf(HashSet.class, MapTemplate.mark(new HashSet<>(Collections.singletonList("{name}"))).apply(REPLACER));
        assertInstanceOf(LinkedHashMap.class, MapTemplate.mark(new LinkedHashMap<>(Collections.singletonMap("{name}", 1))).apply(REPLACER));
        assertInstanceOf(ConcurrentHashMap.class, MapTemplate.mark(new ConcurrentHashMap<>(Collections.singletonMap("{name}", 1))).apply(REPLACER));
    }

    @Test
    public void testDuplicateResolvedKeyLastWins() {
        Map<String, String> target = new LinkedHashMap<>();
        target.put("{a}", "first");
        target.put("{b}", "second");

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(target).apply(string -> string.startsWith("{") ? "same" : string);

        assertEquals(Collections.singletonMap("same", "second"), result);
    }

    @Test
    public void testConcurrentMapDuplicateResolvedKeyLastWins() {
        Map<String, String> target = new ConcurrentHashMap<>();
        target.put("{a}", "value");
        target.put("{b}", "value");

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(target).apply(string -> string.startsWith("{") ? "same" : string);

        assertEquals(Collections.singletonMap("same", "value"), result);
    }

    @Test
    public void testNullValueIsKept() {
        Map<String, Object> target = new HashMap<>();
        target.put("key", null);

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(target).apply(string -> string);

        assertEquals(1, result.size());
        assertNull(result.get("key"));
    }
}
