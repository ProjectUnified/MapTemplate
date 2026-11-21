package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class PreFlagTest {
    @Test
    public void testMarkedOnceAppliedManyTimes() {
        MapTemplate template = MapTemplate.mark(Arrays.asList("Hello {name}", 3));

        assertEquals(Arrays.asList("Hello John", 3), template.apply(string -> string.replace("{name}", "John")));
        assertEquals(Arrays.asList("Hello Jane", 3), template.apply(string -> string.replace("{name}", "Jane")));
        assertEquals(Arrays.asList("Hello John", 3), template.apply(string -> string.replace("{name}", "John")));
    }

    @Test
    public void testEveryApplyBuildsANewCopy() {
        MapTemplate template = MapTemplate.mark(new ArrayList<>(Collections.singletonList("plain")));

        Object first = template.apply(string -> string);
        Object second = template.apply(string -> string);

        assertNotSame(first, second);
        assertEquals(first, second);
    }

    @Test
    public void testInputIsNotReadAfterMarking() {
        List<Object> input = new ArrayList<>(Collections.singletonList("{name}"));
        MapTemplate template = MapTemplate.mark(input);

        input.set(0, "changed");

        assertEquals(Collections.singletonList("John"), template.apply(string -> "John"));
    }

    @Test
    public void testOneReplacerCallPerString() {
        List<String> seen = new ArrayList<>();
        Function<String, Object> replacer = string -> {
            seen.add(string);
            return string;
        };

        MapTemplate.mark(Arrays.asList("a", "b", 1)).apply(replacer);

        assertEquals(Arrays.asList("a", "b"), seen);
    }

    @Test
    public void testFixedContentNeedsNoReplacement() {
        Function<String, Object> failing = string -> fail("should not be replaced: " + string);

        List<?> list = (List<?>) MapTemplate.mark(Arrays.asList(1, true, 2.5)).apply(failing);
        assertEquals(Arrays.asList(1, true, 2.5), list);
        assertInstanceOf(ArrayList.class, list);
        assertInstanceOf(HashSet.class, MapTemplate.mark(new HashSet<>(Arrays.asList(1, 2))).apply(failing));
        assertInstanceOf(LinkedHashMap.class, MapTemplate.mark(new LinkedHashMap<>(Collections.singletonMap(1, 2))).apply(failing));
        assertInstanceOf(ConcurrentHashMap.class, MapTemplate.mark(new ConcurrentHashMap<>(Collections.singletonMap(1, 2))).apply(failing));
    }

    @Test
    public void testFixedContentIsCopiedOnEveryApply() {
        MapTemplate template = MapTemplate.mark(new ArrayList<>(Arrays.asList(1, 2, 3)));

        List<?> first = (List<?>) template.apply(string -> string);
        List<?> second = (List<?>) template.apply(string -> string);

        assertNotSame(first, second);
        assertEquals(first, second);

        @SuppressWarnings("unchecked")
        List<Object> mutable = (List<Object>) first;
        mutable.set(0, 9);
        assertEquals(Arrays.asList(1, 2, 3), template.apply(string -> string));
    }

    @Test
    public void testFixedContentInsideAStructure() {
        List<String> replaced = new ArrayList<>();
        Function<String, Object> replacer = string -> {
            replaced.add(string);
            return string.replace("{name}", "John");
        };

        Map<String, Object> structure = new LinkedHashMap<>();
        structure.put("{name}", Arrays.asList(1, 2, 3));

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(structure).apply(replacer);

        // Only the key had to be replaced, the list holds nothing that can change
        assertEquals(Collections.singletonList("{name}"), replaced);
        assertEquals(Arrays.asList(1, 2, 3), result.get("John"));
    }

    @Test
    public void testMissingReplacer() {
        MapTemplate template = MapTemplate.mark("value");
        assertThrows(NullPointerException.class, () -> template.apply(null));
    }

    @Test
    public void testReplacerReturningAMarkedCollectionIsFlattened() {
        MapTemplate extra = MapTemplate.mark(Arrays.asList("{name}", "plain"));

        List<?> result = (List<?>) MapTemplate.mark(Arrays.asList("start", "{extra}"))
                .apply(string -> string.equals("{extra}") ? extra : string.replace("{name}", "John"));

        assertEquals(Arrays.asList("start", "John", "plain"), result);
    }

    @Test
    public void testReplacerReturningAMarkedMapIsMerged() {
        MapTemplate extra = MapTemplate.mark(Collections.singletonMap("{name}", 1));

        Map<?, ?> result = (Map<?, ?>) MapTemplate.mark(new LinkedHashMap<>(Collections.singletonMap("{extra}", "dropped")))
                .apply(string -> string.equals("{extra}") ? extra : string.replace("{name}", "John"));

        assertEquals(Collections.singletonMap("John", 1), result);
    }

    @Test
    public void testMarkedTemplateInsideAStructureIsApplied() {
        MapTemplate extra = MapTemplate.mark("{name}!");

        List<?> result = (List<?>) MapTemplate.mark(Arrays.asList(extra, "x"))
                .apply(string -> string.replace("{name}", "John"));

        assertEquals(Arrays.asList("John!", "x"), result);
    }
}
