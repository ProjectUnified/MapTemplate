package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomMarkerTest {
    @Test
    public void testCustomStartEnd() {
        MapTemplate template = MapTemplate.builder()
                .setStartVariable("${")
                .setEndVariable("}")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("John", template.apply("${name}"));
        assertEquals("{name}", template.apply("{name}")); // Default markers should not work
    }

    @Test
    public void testDifferentMarkers() {
        MapTemplate template = MapTemplate.builder()
                .setStartVariable("<<")
                .setEndVariable(">>")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("John", template.apply("<<name>>"));
        assertEquals("Hello John!", template.apply("Hello <<name>>!"));
    }

    @Test
    public void testCustomEscapeChar() {
        MapTemplate template = MapTemplate.builder()
                .setEscapeChar("#")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("{name}", template.apply("#{name}"));
        assertEquals("\\John", template.apply("\\{name}")); // Default escape should not work
    }
}
