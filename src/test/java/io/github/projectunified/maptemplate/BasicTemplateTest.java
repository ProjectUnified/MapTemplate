package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class BasicTemplateTest {
    @Test
    public void testBasicReplacement() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 25);

        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(map)
                .build();

        assertEquals("John", MapTemplate.mark("{name}").apply(replacer));
        assertEquals(25, MapTemplate.mark("{age}").apply(replacer));
        assertEquals("Hello John, age 25", MapTemplate.mark("Hello {name}, age {age}").apply(replacer));
    }

    @Test
    public void testMissingVariable() {
        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("{missing}", MapTemplate.mark("{missing}").apply(replacer));
        assertEquals("Hello John, {missing}", MapTemplate.mark("Hello {name}, {missing}").apply(replacer));
    }

    @Test
    public void testEmptyMap() {
        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(Collections.emptyMap())
                .build();

        assertEquals("{name}", MapTemplate.mark("{name}").apply(replacer));
    }

    @Test
    public void testUnchangedStringsAreNotCopied() {
        String plain = "no markers here";
        String unknown = "{name}";
        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setVariableMap(Collections.emptyMap())
                .build();

        assertSame(plain, replacer.apply(plain));
        assertSame(unknown, replacer.apply(unknown));
    }

    @Test
    public void testDirectConstructor() {
        VariableStringReplacer replacer = new VariableStringReplacer("{", "}", "\\",
                Collections.singletonMap("name", "John")::get);

        assertEquals("John", MapTemplate.mark("{name}").apply(replacer));
    }

    @Test
    public void testNullVariableValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", null);

        Function<String, Object> replacer = VariableStringReplacer.builder()
                .setVariableMap(map)
                .build();

        assertEquals("{name}", MapTemplate.mark("{name}").apply(replacer));
    }
}
