package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedVariableTest {
    @Test
    public void testRecursiveReplacement() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "John");
        map.put("lastName", "Doe");
        map.put("fullName", "{firstName} {lastName}");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("John Doe", template.apply("{fullName}"));
    }

    @Test
    public void testMultipleLevels() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "b");
        map.put("b", "c");
        map.put("c", "done");
        map.put("start", "{a}");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("done", template.apply("{c}"));
    }

    @Test
    public void testDeepNesting() {
        Map<String, Object> map = new HashMap<>();
        map.put("property", "name");
        map.put("test_name_value", "Result");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("Result", template.apply("{test_{property}_value}"));
    }

    @Test
    public void testIncompleteNesting() {
        Map<String, Object> map = new HashMap<>();
        map.put("test_value", "123");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("{test_123", template.apply("{test_{test_value}"));
    }

    @Test
    public void testFlatteningList() {
        Map<String, Object> map = new HashMap<>();
        map.put("names", Arrays.asList("John", "Doe"));

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        List<?> result = (List<?>) template.apply(Arrays.asList("Start", "{names}", "End"));
        assertEquals(4, result.size());
        assertEquals("Start", result.get(0));
        assertEquals("John", result.get(1));
        assertEquals("Doe", result.get(2));
        assertEquals("End", result.get(3));
    }

    @Test
    public void testFlatteningMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("k1", "v1");
        data.put("k2", "v2");
        map.put("extra", data);

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        Map<String, String> target = new HashMap<>();
        target.put("{extra}", "dummy");

        Map<?, ?> result = (Map<?, ?>) template.apply(target);
        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testTrailingEndMarker() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", "value");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("value}", template.apply("{test}}"));
    }

    @Test
    public void testEscapedNestedVariable() {
        Map<String, Object> map = new HashMap<>();
        map.put("test_{property}_test", "Success");

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("Success", template.apply("{test_\\{property}_test}"));
    }

    @Test
    public void testEscapedNestedVariableNoVariableMap() {
        MapTemplate template = MapTemplate.builder().build();

        assertEquals("{test_{property}_test}", template.apply("{test_\\{property}_test}"));
    }
}
