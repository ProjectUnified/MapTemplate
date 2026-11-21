package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomMarkerTest {
    @Test
    public void testCustomStartEnd() {
        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setStartVariable("${")
                .setEndVariable("}")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("John", MapTemplate.mark("${name}").apply(replacer));
        assertEquals("{name}", MapTemplate.mark("{name}").apply(replacer)); // Default markers should not work
    }

    @Test
    public void testDifferentMarkers() {
        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setStartVariable("<<")
                .setEndVariable(">>")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("John", MapTemplate.mark("<<name>>").apply(replacer));
        assertEquals("Hello John!", MapTemplate.mark("Hello <<name>>!").apply(replacer));
    }

    @Test
    public void testCustomEscapeChar() {
        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setEscapeChar("#")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("{name}", MapTemplate.mark("#{name}").apply(replacer));
        assertEquals("\\John", MapTemplate.mark("\\{name}").apply(replacer)); // Default escape should not work
    }

    @Test
    public void testSameStartAndEndMarker() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("a", "1");
        map.put("b", "2");
        map.put("num", 42);

        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setStartVariable("%")
                .setEndVariable("%")
                .setVariableMap(map)
                .build();

        assertEquals("John", MapTemplate.mark("%name%").apply(replacer));
        assertEquals(42, MapTemplate.mark("%num%").apply(replacer));
        assertEquals("Hello John!", MapTemplate.mark("Hello %name%!").apply(replacer));
        assertEquals("1 and 2", MapTemplate.mark("%a% and %b%").apply(replacer));
        assertEquals("%missing%", MapTemplate.mark("%missing%").apply(replacer));
        assertEquals("Hello {name}, 1", MapTemplate.mark("Hello {name}, %a%").apply(replacer)); // only % markers
    }

    @Test
    public void testSameMarkerEscapes() {
        VariableStringReplacer replacer = VariableStringReplacer.builder()
                .setStartVariable("%")
                .setEndVariable("%")
                .setVariableMap(Collections.singletonMap("name", "John"))
                .build();

        assertEquals("%name%", MapTemplate.mark("\\%name%").apply(replacer));
        assertEquals("\\John", MapTemplate.mark("\\\\%name%").apply(replacer));
        // A literal percentage sign must not hide the variable behind it
        assertEquals("100% of John", MapTemplate.mark("100\\% of %name%").apply(replacer));
    }
}
