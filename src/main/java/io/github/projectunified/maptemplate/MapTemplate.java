package io.github.projectunified.maptemplate;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapTemplate {
    private final Function<String, Object> stringReplacer;

    /**
     * Create a new {@link MapTemplate}.
     *
     * @param stringReplacer the function replacing variables in strings, e.g. a {@link VariableStringReplacer}
     */
    public MapTemplate(Function<String, Object> stringReplacer) {
        this.stringReplacer = stringReplacer;
    }

    /**
     * Applies variables to each element of a collection.
     * If an element resolves to a another collection, it is flattened into the result.
     *
     * @param inputCollection the collection to process
     * @return a new collection with resolved values
     */
    private Collection<?> applyCollection(Collection<?> inputCollection) {
        Stream<Object> resolvedStream = inputCollection.stream()
                .flatMap(element -> {
                    // Non-string elements are processed recursively (for nested collections/maps)
                    if (!(element instanceof String)) {
                        return Stream.of(apply(element));
                    }

                    String text = (String) element;

                    // Apply template replacement
                    Object resolved = apply(text);

                    // If the result is a collection, flatten it (e.g., "{my_list}" -> [item1, item2])
                    if (resolved instanceof Collection) {
                        return ((Collection<?>) resolved).stream();
                    }

                    return Stream.of(resolved);
                });

        // Collect into a new collection of the same general type
        if (inputCollection instanceof List) {
            return resolvedStream.collect(Collectors.toList());
        } else if (inputCollection instanceof Set) {
            return resolvedStream.collect(Collectors.toSet());
        } else {
            return resolvedStream.collect(Collectors.toCollection(ArrayList::new));
        }
    }

    /**
     * Applies variables to each entry of a map.
     * If a key resolves to another map, that map's entries are merged into the result.
     *
     * @param inputMap the map to process
     * @return a new map with resolved values
     */
    private Map<?, ?> applyMap(Map<?, ?> inputMap) {
        Stream<Map.Entry<?, ?>> entryStream = inputMap.entrySet().stream()
                .flatMap(entry -> {
                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    // Non-string keys are kept as-is, with their values processed recursively
                    if (!(key instanceof String)) {
                        return Stream.of(new AbstractMap.SimpleEntry<>(key, apply(value)));
                    }

                    String keyText = (String) key;

                    Object resolvedKey = apply(keyText);

                    // If the resolved key is a map, merge its entries (e.g., "{extra_data}" -> {k1: v1, k2: v2})
                    if (resolvedKey instanceof Map) {
                        return ((Map<?, ?>) resolvedKey).entrySet().stream();
                    }

                    return Stream.of(new AbstractMap.SimpleEntry<>(resolvedKey, apply(value)));
                });

        // Collect into a new map, maintaining order where appropriate
        if (inputMap instanceof ConcurrentMap) {
            return entryStream.collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
        } else if (inputMap instanceof LinkedHashMap) {
            return entryStream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> replacement, LinkedHashMap::new));
        } else {
            return entryStream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (existing, replacement) -> replacement));
        }
    }

    /**
     * Applies the variables to an object depending on its type.
     * Processes Collections, Maps, and Strings recursively.
     *
     * @param obj the object to process
     * @return the resolved object
     */
    public Object apply(Object obj) {
        if (obj instanceof Collection) {
            return applyCollection((Collection<?>) obj);
        } else if (obj instanceof Map) {
            return applyMap((Map<?, ?>) obj);
        } else if (obj instanceof String) {
            Object resolved = stringReplacer.apply((String) obj);
            // A whole-string variable resolves to an object or a collection: keep processing those
            return resolved instanceof String ? resolved : apply(resolved);
        }
        return obj;
    }
}
