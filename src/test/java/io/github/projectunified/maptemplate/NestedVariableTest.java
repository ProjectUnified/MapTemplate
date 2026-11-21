package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedVariableTest {
    private static Function<String, Object> replacer(Map<String, Object> variables) {
        return VariableStringReplacer.builder().setVariableMap(variables).build();
    }

    @Test
    public void testRecursiveReplacement() {
        Map<String, Object> map = new HashMap<>();
        map.put("firstName", "John");
        map.put("lastName", "Doe");
        map.put("fullName", "{firstName} {lastName}");

        assertEquals("John Doe", MapTemplate.mark("{fullName}").apply(replacer(map)));
    }

    @Test
    public void testMultipleLevels() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "b");
        map.put("b", "c");
        map.put("c", "done");
        map.put("start", "{a}");

        assertEquals("done", MapTemplate.mark("{c}").apply(replacer(map)));
    }

    @Test
    public void testDeepNesting() {
        Map<String, Object> map = new HashMap<>();
        map.put("property", "name");
        map.put("test_name_value", "Result");

        assertEquals("Result", MapTemplate.mark("{test_{property}_value}").apply(replacer(map)));
    }

    @Test
    public void testIncompleteNesting() {
        Map<String, Object> map = new HashMap<>();
        map.put("test_value", "123");

        assertEquals("{test_123", MapTemplate.mark("{test_{test_value}").apply(replacer(map)));
    }

    @Test
    public void testFlatteningList() {
        Map<String, Object> map = new HashMap<>();
        map.put("names", Arrays.asList("John", "Doe"));

        List<?> result = (List<?>) MapTemplate.mark(Arrays.asList("Start", "{names}", "End")).apply(replacer(map));
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

        Map<String, String> target = new HashMap<>();
        target.put("{extra}", "dummy");

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(target).apply(replacer(map));
        assertEquals(2, result.size());
        assertEquals("v1", result.get("k1"));
        assertEquals("v2", result.get("k2"));
    }

    @Test
    public void testKeyReplacement() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");

        Map<String, String> target = new HashMap<>();
        target.put("{name}", "value");

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(target).apply(replacer(map));
        assertEquals(1, result.size());
        assertEquals("value", result.get("John"));
    }

    @Test
    public void testTrailingEndMarker() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", "value");

        assertEquals("value}", MapTemplate.mark("{test}}").apply(replacer(map)));
    }

    @Test
    public void testEscapedNestedVariable() {
        Map<String, Object> map = new HashMap<>();
        map.put("test_{property}_test", "Success");

        assertEquals("Success", MapTemplate.mark("{test_\\{property}_test}").apply(replacer(map)));
    }

    @Test
    public void testMultipleStartMarkers() {
        Map<String, Object> map = new HashMap<>();
        map.put("test", "Success");

        // One { is remains, one { is matched with }
        assertEquals("{Success", MapTemplate.mark("{{test}").apply(replacer(map)));
    }

    @Test
    public void testEscapedNestedVariableNoVariableMap() {
        Function<String, Object> replacer = VariableStringReplacer.builder().build();

        assertEquals("{test_{property}_test}", MapTemplate.mark("{test_\\{property}_test}").apply(replacer));
    }

    @Test
    public void testAdjacentVariables() {
        Map<String, Object> map = new HashMap<>();
        map.put("test1", "Val1");
        map.put("test2", "Val2");
        map.put("test1}{test2", "Wrong");

        assertEquals("Val1Val2", MapTemplate.mark("{test1}{test2}").apply(replacer(map)));
    }

    @Test
    public void testEmptyValueBeforeAnotherVariable() {
        Map<String, Object> map = new HashMap<>();
        map.put("empty", "");
        map.put("name", "John");

        assertEquals(" John", MapTemplate.mark("{empty} {name}").apply(replacer(map)));
        assertEquals("John", MapTemplate.mark("{{empty}name}").apply(replacer(map)));
    }

    @Test
    public void testNestedVariableAfterAnotherVariable() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "1");
        map.put("property", "name");
        map.put("test_name_value", "Result");

        assertEquals("1Result", MapTemplate.mark("{a}{test_{property}_value}").apply(replacer(map)));
        assertEquals("1 Result", MapTemplate.mark("{a} {test_{property}_value}").apply(replacer(map)));
    }
}
