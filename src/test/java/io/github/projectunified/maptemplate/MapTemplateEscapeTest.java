package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapTemplateEscapeTest {
    @Test
    public void testEscape() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 25);

        assertEquals("John", MapTemplate.apply("{name}", map));
        assertEquals("{name}", MapTemplate.apply("\\{name}", map));
        assertEquals("Hello {name}, your age is 25", MapTemplate.apply("Hello \\{name}, your age is {age}", map));
        assertEquals("Double escape: \\John", MapTemplate.apply("Double escape: \\\\{name}", map));
        assertEquals("Triple escape: \\{name}", MapTemplate.apply("Triple escape: \\\\\\{name}", map));
        assertEquals("No variable: {unknown}", MapTemplate.apply("No variable: {unknown}", map));
        assertEquals("Escaped no variable: {unknown}", MapTemplate.apply("Escaped no variable: \\{unknown}", map));
    }
}
