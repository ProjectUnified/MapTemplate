package io.github.projectunified.maptemplate;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomFunctionTest {
    @Test
    public void testFunction() {
        Function<String, Object> replacer = string -> string.replace("[name]", "John");

        assertEquals("Hello John", MapTemplate.mark("Hello [name]").apply(replacer));
        assertEquals("{name}", MapTemplate.mark("{name}").apply(replacer));
    }

    @Test
    public void testFunctionResultIsProcessed() {
        Map<String, Object> map = new HashMap<>();
        map.put("a", Arrays.asList("@b"));
        map.put("b", "done");

        // A function returning a collection gets its elements processed by the template
        Function<String, Object> replacer = string -> string.startsWith("@")
                ? map.get(string.substring(1))
                : string;

        assertEquals(Arrays.asList("done"), MapTemplate.mark("@a").apply(replacer));
        assertEquals("done", MapTemplate.mark("@b").apply(replacer));

        List<?> result = (List<?>) MapTemplate.mark(Arrays.asList("@b", "plain")).apply(replacer);
        assertEquals(Arrays.asList("done", "plain"), result);
    }
}
