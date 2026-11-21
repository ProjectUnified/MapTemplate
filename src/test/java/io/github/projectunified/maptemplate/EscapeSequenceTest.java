package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EscapeSequenceTest {
    private static final Function<String, Object> REPLACER = VariableStringReplacer.builder()
            .setVariableMap(Collections.singletonMap("name", "John"))
            .build();

    @Test
    public void testSingleEscape() {
        assertEquals("{name}", MapTemplate.mark("\\{name}").apply(REPLACER));
    }

    @Test
    public void testDoubleEscape() {
        assertEquals("\\John", MapTemplate.mark("\\\\{name}").apply(REPLACER));
    }

    @Test
    public void testTripleEscape() {
        assertEquals("\\{name}", MapTemplate.mark("\\\\\\{name}").apply(REPLACER));
    }

    @Test
    public void testQuadrupleEscape() {
        assertEquals("\\\\John", MapTemplate.mark("\\\\\\\\{name}").apply(REPLACER));
    }

    @Test
    public void testEscapeInMiddle() {
        assertEquals("Hello {name}!", MapTemplate.mark("Hello \\{name}!").apply(REPLACER));
    }

    @Test
    public void testEscapedNoVariable() {
        Function<String, Object> emptyReplacer = VariableStringReplacer.builder()
                .setVariableMap(Collections.emptyMap())
                .build();

        assertEquals("{unknown}", MapTemplate.mark("\\{unknown}").apply(emptyReplacer));
    }

    @Test
    public void testEscapeBeforeAnotherVariable() {
        assertEquals("{name} John", MapTemplate.mark("\\{name} {name}").apply(REPLACER));
        assertEquals("\\John John", MapTemplate.mark("\\\\{name} {name}").apply(REPLACER));
    }
}
