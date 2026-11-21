package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
