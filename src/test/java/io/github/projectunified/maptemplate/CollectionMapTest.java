package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollectionMapTest {
    @Test
    public void testList() {
        Map<String, Object> map = Collections.singletonMap("name", "John");
        MapTemplate template = MapTemplate.builder().setVariableMap(map).build();

        List<String> list = Arrays.asList("Hello {name}", "{name}", "pure string");
        List<?> result = (List<?>) template.apply(list);

        assertEquals(3, result.size());
        assertEquals("Hello John", result.get(0));
        assertEquals("John", result.get(1));
        assertEquals("pure string", result.get(2));
    }

    @Test
    public void testSet() {
        Map<String, Object> map = Collections.singletonMap("name", "John");
        MapTemplate template = MapTemplate.builder().setVariableMap(map).build();

        Set<String> set = new HashSet<>(Collections.singletonList("{name}"));
        Set<?> result = (Set<?>) template.apply(set);

        assertEquals(1, result.size());
        assertTrue(result.contains("John"));
    }

    @Test
    public void testMap() {
        Map<String, Object> varMap = Collections.singletonMap("name", "John");
        MapTemplate template = MapTemplate.builder().setVariableMap(varMap).build();

        Map<String, String> targetMap = new HashMap<>();
        targetMap.put("greeting", "Hello {name}");
        targetMap.put("signature", "{name}");

        Map<?, ?> result = (Map<?, ?>) template.apply(targetMap);

        assertEquals("Hello John", result.get("greeting"));
        assertEquals("John", result.get("signature"));
    }

    @Test
    public void testNestedStructures() {
        Map<String, Object> varMap = Collections.singletonMap("name", "John");
        MapTemplate template = MapTemplate.builder().setVariableMap(varMap).build();

        Map<String, Object> nested = new HashMap<>();
        nested.put("list", Arrays.asList("{name}", "static"));

        Map<?, ?> result = (Map<?, ?>) template.apply(nested);
        List<?> resultList = (List<?>) result.get("list");

        assertEquals("John", resultList.get(0));
    }
}
