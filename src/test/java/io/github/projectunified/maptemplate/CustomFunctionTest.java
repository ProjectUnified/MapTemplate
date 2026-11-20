package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomFunctionTest {
    @Test
    public void testFunction() {
        MapTemplate template = new MapTemplate(string -> string.replace("[name]", "John"));

        assertEquals("Hello John", template.apply("Hello [name]"));
        assertEquals("{name}", template.apply("{name}"));
    }

    @Test
    public void testFunctionResultIsProcessed() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", Arrays.asList("@b"));
        map.put("b", "done");

        // A function returning a collection gets its elements processed by the template
        MapTemplate template = new MapTemplate(string -> string.startsWith("@")
                ? map.get(string.substring(1))
                : string);

        assertEquals(Arrays.asList("done"), template.apply("@a"));
        assertEquals("done", template.apply("@b"));

        List<?> result = (List<?>) template.apply(Arrays.asList("@b", "plain"));
        assertEquals(Arrays.asList("done", "plain"), result);
    }
}
