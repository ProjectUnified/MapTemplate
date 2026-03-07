package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicTemplateTest {
    @Test
    public void testBasicReplacement() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 25);

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("John", template.apply("{name}"));
        assertEquals(25, template.apply("{age}"));
        assertEquals("Hello John, age 25", template.apply("Hello {name}, age {age}"));
    }

    @Test
    public void testMissingVariable() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("{missing}", template.apply("{missing}"));
        assertEquals("Hello John, {missing}", template.apply("Hello {name}, {missing}"));
    }

    @Test
    public void testEmptyMap() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.emptyMap())
                .build();

        assertEquals("{name}", template.apply("{name}"));
    }

    @Test
    public void testNullVariableValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", null);

        MapTemplate template = MapTemplate.builder()
                .setVariableMap(map)
                .build();

        assertEquals("{name}", template.apply("{name}"));
    }
}
