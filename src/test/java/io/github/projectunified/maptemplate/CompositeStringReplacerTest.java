package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class CompositeStringReplacerTest {
    @Test
    public void testEmpty() {
        Function<String, Object> replacer = new CompositeStringReplacer().build();

        assertEquals("{name}", MapTemplate.mark("{name}").apply(replacer));
    }

    @Test
    public void testSingle() {
        Function<String, Object> replacer = new CompositeStringReplacer()
                .addReplacer(VariableStringReplacer.builder()
                        .setVariableMap(Collections.singletonMap("name", "John"))
                        .build())
                .build();

        assertEquals("Hello John", MapTemplate.mark("Hello {name}").apply(replacer));
    }

    @Test
    public void testMultipleInOrder() {
        Function<String, Object> replacer = new CompositeStringReplacer()
                .addReplacer(string -> string.replace("{a}", "{b}"))
                .addReplacer(VariableStringReplacer.builder()
                        .setVariableMap(Collections.singletonMap("b", "done"))
                        .build())
                .build();

        assertEquals("done", MapTemplate.mark("{a}").apply(replacer));
    }

    @Test
    public void testStopsWhenNotString() {
        Function<String, Object> replacer = new CompositeStringReplacer()
                .addReplacer(string -> 42)
                .addReplacer(string -> fail("Should not run after a non-string result"))
                .build();

        assertEquals(42, MapTemplate.mark("anything").apply(replacer));
    }

    @Test
    public void testBothMarkerSyntaxesOnAMap() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        variables.put("player_name", "Alice");
        variables.put("count", 3);
        variables.put("names", Arrays.asList("x", "y"));

        Function<String, Object> replacer = new CompositeStringReplacer()
                .addReplacer(VariableStringReplacer.builder().setVariableMap(variables).build())
                .addReplacer(VariableStringReplacer.builder()
                        .setStartVariable("%")
                        .setEndVariable("%")
                        .setVariableMap(variables)
                        .build())
                .build();

        Map<String, Object> structure = new LinkedHashMap<>();
        structure.put("%player_name%", "Hi {name}, you have %count% items");
        structure.put("list", Arrays.asList("{name}", "%player_name%", 7));

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(structure).apply(replacer);

        assertEquals("Hi John, you have 3 items", result.get("Alice"));
        assertEquals(Arrays.asList("John", "Alice", 7), result.get("list"));

        // A whole-string variable still resolves to the object itself, through either syntax
        assertEquals(Arrays.asList("start", "x", "y"),
                MapTemplate.mark(Arrays.asList("start", "%names%")).apply(replacer));
        assertEquals("%names%", MapTemplate.mark("\\%names%").apply(replacer));
    }
}
