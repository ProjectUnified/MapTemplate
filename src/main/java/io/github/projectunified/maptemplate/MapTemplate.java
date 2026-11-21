package io.github.projectunified.maptemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * A pre-flagged copy of an object that knows where its strings are.
 * <p>
 * {@link #mark(Object)} walks the object once and records the position of every string; {@link #apply(Function)}
 * then builds the final object by replacing those strings, without inspecting the structure again. A template holds
 * no replacer and no mutable state, so it can be marked once and applied many times, with different replacers, on any
 * thread.
 * <p>
 * Containers keep their shape (a {@link List} stays a list, a {@link Set} stays a set, a {@link ConcurrentMap} stays
 * concurrent), a whole-string variable resolves to the value itself, and a value resolved from a string is flattened
 * into the surrounding collection or merged into the surrounding map. When two resolved keys are equal the last one
 * wins, and null keys or values are only rejected by map types that cannot hold them, like {@link ConcurrentHashMap}.
 */
public final class MapTemplate {
    private final Node root;

    private MapTemplate(Node root) {
        this.root = root;
    }

    /**
     * Walks an object and flags its strings.
     *
     * @param obj the object to flag, e.g. a string, a collection, a map, or another template to apply as it is
     * @return the template, ready to be applied with a string replacer
     */
    public static MapTemplate mark(Object obj) {
        return new MapTemplate(node(obj));
    }

    /**
     * Replaces every flagged string through the given replacer.
     *
     * @param stringReplacer the function replacing one string, e.g. a {@link VariableStringReplacer}
     * @return a new object shaped like the flagged one, with the strings replaced
     */
    public Object apply(Function<String, Object> stringReplacer) {
        Objects.requireNonNull(stringReplacer, "stringReplacer");
        return root.apply(stringReplacer);
    }

    /**
     * Flags a single object depending on its type.
     *
     * @param obj the object to flag
     * @return the flagged node for that object
     */
    private static Node node(Object obj) {
        if (obj instanceof MapTemplate) {
            // An already flagged template is applied as it is, instead of being walked again
            MapTemplate template = (MapTemplate) obj;
            return template::apply;
        } else if (obj instanceof Collection) {
            return collectionNode((Collection<?>) obj);
        } else if (obj instanceof Map) {
            return mapNode((Map<?, ?>) obj);
        } else if (obj instanceof String) {
            String text = (String) obj;
            return stringReplacer -> resolve(text, stringReplacer);
        }
        return stringReplacer -> obj;
    }

    /**
     * Flags each element of a collection.
     *
     * @param input the collection to flag
     * @return the flagged node for that collection
     */
    private static Node collectionNode(Collection<?> input) {
        List<Element> flagged = new ArrayList<>(input.size());
        for (Object element : input) {
            // Only a string can resolve to a collection, and only those are flattened into the result
            flagged.add(new Element(node(element), element instanceof String));
        }
        Element[] elements = flagged.toArray(new Element[0]);

        if (input instanceof Set) {
            return stringReplacer -> {
                Set<Object> result = new HashSet<>(elements.length);
                addElements(result, elements, stringReplacer);
                return result;
            };
        }
        return stringReplacer -> {
            List<Object> result = new ArrayList<>(elements.length);
            addElements(result, elements, stringReplacer);
            return result;
        };
    }

    /**
     * Resolves every flagged element and adds it to the result.
     *
     * @param result         the collection being built
     * @param elements       the flagged elements
     * @param stringReplacer the function replacing one string
     */
    private static void addElements(Collection<Object> result, Element[] elements, Function<String, Object> stringReplacer) {
        for (Element element : elements) {
            Object value = element.node.apply(stringReplacer);
            if (element.flatten && value instanceof Collection) {
                result.addAll((Collection<?>) value);
            } else {
                result.add(value);
            }
        }
    }

    /**
     * Flags each entry of a map.
     *
     * @param input the map to flag
     * @return the flagged node for that map
     */
    private static Node mapNode(Map<?, ?> input) {
        List<Entry> flagged = new ArrayList<>(input.size());
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            // Only a string key can resolve to a map, and only those are merged into the result
            flagged.add(new Entry(node(entry.getKey()), entry.getKey() instanceof String, node(entry.getValue())));
        }
        Entry[] entries = flagged.toArray(new Entry[0]);

        boolean concurrent = input instanceof ConcurrentMap;
        boolean linked = input instanceof LinkedHashMap;
        return stringReplacer -> {
            Map<Object, Object> result;
            if (concurrent) {
                result = new ConcurrentHashMap<>(entries.length);
            } else if (linked) {
                result = new LinkedHashMap<>(entries.length);
            } else {
                result = new HashMap<>(entries.length);
            }

            for (Entry entry : entries) {
                Object key = entry.key.apply(stringReplacer);
                if (entry.merge && key instanceof Map) {
                    // A key variable can stand in for a whole map: its entries are merged into the result
                    result.putAll((Map<?, ?>) key);
                } else {
                    result.put(key, entry.value.apply(stringReplacer));
                }
            }
            return result;
        };
    }

    /**
     * Replaces a flagged string, keeping the processing of a value that is no longer a string.
     *
     * @param text           the flagged string
     * @param stringReplacer the function replacing one string
     * @return the resolved value
     */
    private static Object resolve(String text, Function<String, Object> stringReplacer) {
        Object resolved = stringReplacer.apply(text);
        // A string is never replaced twice, but a value that is an object or a container still has strings in it
        return resolved instanceof String ? resolved : node(resolved).apply(stringReplacer);
    }

    /**
     * A flagged part of an object: it knows how to build its replacement.
     */
    @FunctionalInterface
    private interface Node {
        Object apply(Function<String, Object> stringReplacer);
    }

    /**
     * A flagged collection element.
     */
    private static final class Element {
        private final Node node;
        private final boolean flatten;

        private Element(Node node, boolean flatten) {
            this.node = node;
            this.flatten = flatten;
        }
    }

    /**
     * A flagged map entry.
     */
    private static final class Entry {
        private final Node key;
        private final boolean merge;
        private final Node value;

        private Entry(Node key, boolean merge, Node value) {
            this.key = key;
            this.merge = merge;
            this.value = value;
        }
    }
}
