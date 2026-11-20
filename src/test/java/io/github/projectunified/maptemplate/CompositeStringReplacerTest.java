package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CompositeStringReplacerTest {
    @Test
    public void testEmpty() {
        MapTemplate template = new MapTemplate(new CompositeStringReplacer().build());

        assertEquals("{name}", template.apply("{name}"));
    }

    @Test
    public void testSingle() {
        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();
        MapTemplate template = new MapTemplate(new CompositeStringReplacer()
                .addReplacer(replacer)
                .build());

        assertEquals("Hello John", template.apply("Hello {name}"));
    }

    @Test
    public void testMultipleInOrder() {
        Function<String, Object> first = string -> string.replace("{a}", "{b}");
        Function<String, Object> second = VariableStringReplacer.builder()
                .setVariableMap(Collections.singletonMap("b", "done"))
                .build();
        MapTemplate template = new MapTemplate(new CompositeStringReplacer()
                .addReplacer(first)
                .addReplacer(second)
                .build());

        assertEquals("done", template.apply("{a}"));
    }

    @Test
    public void testStopsWhenNotString() {
        MapTemplate template = new MapTemplate(new CompositeStringReplacer()
                .addReplacer(string -> 42)
                .addReplacer(string -> fail("Should not run after a non-string result"))
                .build());

        assertEquals(42, template.apply("anything"));
    }
}
