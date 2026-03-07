package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EscapeSequenceTest {
    @Test
    public void testSingleEscape() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("{name}", template.apply("\\{name}"));
    }

    @Test
    public void testDoubleEscape() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("\\John", template.apply("\\\\{name}"));
    }

    @Test
    public void testTripleEscape() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("\\{name}", template.apply("\\\\\\{name}"));
    }

    @Test
    public void testQuadrupleEscape() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("\\\\John", template.apply("\\\\\\\\{name}"));
    }

    @Test
    public void testEscapeInMiddle() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("Hello {name}!", template.apply("Hello \\{name}!"));
    }

    @Test
    public void testEscapedNoVariable() {
        MapTemplate template = MapTemplate.builder()
                .setVariableMap(Collections.emptyMap())
                .build();

        assertEquals("{unknown}", template.apply("\\{unknown}"));
    }
}
