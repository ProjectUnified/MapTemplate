package io.github.projectunified.maptemplate;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapTemplate {
    /**
     * The default start variable string.
     */
    public static final String START_VARIABLE = "{";
    /**
     * The default end variable string.
     */
    public static final String END_VARIABLE = "}";
    /**
     * The default escape character string.
     */
    public static final String ESCAPE_CHAR = "\\";

    private final String startVariable;
    private final String endVariable;
    private final String escapeChar;
    private final Function<String, Object> variableFunction;

    /**
     * Create a new {@link MapTemplate} with the given options.
     *
     * @param options the options
     */
    public MapTemplate(Options options) {
        this.startVariable = options.getStartVariable();
        this.endVariable = options.getEndVariable();
        this.escapeChar = options.getEscapeChar();
        this.variableFunction = options.getVariableFunction();
    }

    /**
     * Create a new builder for {@link Options}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Attempts to resolve the given text as a variable name.
     * A variable name is expected to be wrapped in the configured start and end markers (e.g., "{variable}").
     *
     * @param text the text that might be a variable name
     * @return the resolved object if found, otherwise null
     */
    private Object getVariableValue(String text) {
        // A valid variable must start and end with the markers and have at least one character in between
        int minimumLength = startVariable.length() + endVariable.length() + 1;
        if (text.length() < minimumLength || !text.startsWith(startVariable) || !text.endsWith(endVariable)) {
            return null;
        }

        // Extract the name from inside the markers
        String variableName = text.substring(startVariable.length(), text.length() - endVariable.length());

        // Look up the value associated with this name
        Object value = variableFunction.apply(variableName);
        if (value == null) {
            return null;
        }

        // Recursively apply the template to the value in case it contains other variables
        return apply(value);
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

                    // If the string is exactly a variable (e.g., "{my_list}"), try to resolve and flatten it
                    Object variableValue = getVariableValue(text);
                    if (variableValue != null) {
                        if (variableValue instanceof Collection) {
                            // Flattening: if the variable points to a list, we add all its items to our stream
                            return ((Collection<?>) variableValue).stream();
                        }
                        return Stream.of(variableValue);
                    }

                    // Otherwise, apply template replacement for any partial variables inside the string
                    return Stream.of(apply(text));
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

                    // If the key is exactly a variable (e.g., "{extra_data}"), try to merge that map's data
                    Object variableValue = getVariableValue(keyText);
                    if (variableValue instanceof Map) {
                        // Merging: if the variable points to a map, we add all its entries to our result
                        return ((Map<?, ?>) variableValue).entrySet().stream();
                    }

                    // Otherwise, keep the key and process the value recursively
                    return Stream.of(new AbstractMap.SimpleEntry<>(key, apply(value)));
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
     * Apply the variables to the string.
     *
     * @param string the string
     * @return the result
     */
    private Object applyString(String string) {
        if (variableFunction == null) {
            return string;
        }

        // Optimization: check if the entire string is exactly a variable name
        Object directValue = getVariableValue(string);
        if (directValue != null) {
            return apply(directValue);
        }

        StringBuilder content = new StringBuilder(string);
        Deque<Opener> stack = new ArrayDeque<>();
        int i = 0;

        while (i < content.length()) {
            int start = content.indexOf(startVariable, i);
            int end = content.indexOf(endVariable, i);

            // No more variables can be resolved if there's no end marker
            if (end == -1) {
                break;
            }

            // Determine if we are processing a start marker or an end marker
            boolean isStart = (start != -1 && start < end);
            int markerPos = isStart ? start : end;
            String marker = isStart ? startVariable : endVariable;

            // 1. Unescape the marker and its prefix (e.g., \\{ -> \{ or \\\\{ -> \\{)
            int escapeCount = countPrefix(content, markerPos, escapeChar);
            int escapeStart = markerPos - (escapeCount * escapeChar.length());
            String reducedEscapes = repeat(escapeChar, escapeCount / 2);
            boolean escaped = (escapeCount % 2 != 0);

            content.replace(escapeStart, markerPos + marker.length(), reducedEscapes + marker);
            int actualPos = escapeStart + reducedEscapes.length();

            // 2. Handle the marker logic based on its type and escape state
            if (isStart) {
                // If it's a start marker, push to stack (tracking if it was escaped)
                stack.push(new Opener(actualPos, escaped));
                i = actualPos + startVariable.length();
            } else if (escaped || stack.isEmpty()) {
                // Escaped end marker or unmatched end marker: just skip it
                i = actualPos + endVariable.length();
            } else {
                // Balanced unescaped end marker found: perform variable resolution
                Opener opener = stack.pop();
                if (opener.isEscaped) {
                    // This end balances an escaped start (balanced literal block): skip it
                    i = actualPos + endVariable.length();
                } else {
                    // We found a variable name between the unescaped start and end markers
                    String variableName = content.substring(opener.index + startVariable.length(), actualPos);
                    Object value = variableFunction.apply(variableName);

                    if (value != null) {
                        Object resolved = apply(value);

                        // If the entire original string was precisely this variable, return the object directly
                        if (opener.index == 0 && actualPos + endVariable.length() == content.length()) {
                            return resolved;
                        }

                        // Otherwise, replace the `{variable}` text with its string representation
                        String replacement = Objects.toString(resolved);
                        content.replace(opener.index, actualPos + endVariable.length(), replacement);
                        i = opener.index + replacement.length();
                    } else {
                        // Variable not found in map: leave it as-is and continue
                        i = actualPos + endVariable.length();
                    }
                }
            }
        }
        return content.toString();
    }

    private int countPrefix(StringBuilder sb, int end, String prefix) {
        int count = 0;
        int length = prefix.length();
        for (int i = end - length; i >= 0 && sb.substring(i, i + length).equals(prefix); i -= length) {
            count++;
        }
        return count;
    }

    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
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
            return applyString((String) obj);
        }
        return obj;
    }

    private static class Opener {
        final int index;
        final boolean isEscaped;

        Opener(int index, boolean isEscaped) {
            this.index = index;
            this.isEscaped = isEscaped;
        }
    }

    /**
     * Configuration options for {@link MapTemplate}.
     * This class is immutable; use {@link Builder} to create instances.
     */
    public static class Options {
        private final String startVariable;
        private final String endVariable;
        private final String escapeChar;
        private final Function<String, Object> variableFunction;

        /**
         * Create a new instance of {@link Options} with the specified configuration.
         *
         * @param startVariable    the string that indicates the start of a variable
         * @param endVariable      the string that indicates the end of a variable
         * @param escapeChar       the character used to escape markers
         * @param variableFunction the function used to resolve variable values by name
         */
        public Options(String startVariable, String endVariable, String escapeChar, Function<String, Object> variableFunction) {
            this.startVariable = startVariable;
            this.endVariable = endVariable;
            this.escapeChar = escapeChar;
            this.variableFunction = variableFunction;
        }

        /**
         * The string that indicates the start of a variable (e.g., "{").
         *
         * @return start marker
         */
        public String getStartVariable() {
            return startVariable;
        }

        /**
         * The string that indicates the end of a variable (e.g., "}").
         *
         * @return end marker
         */
        public String getEndVariable() {
            return endVariable;
        }

        /**
         * The character used to escape markers (e.g., "\").
         *
         * @return escape character
         */
        public String getEscapeChar() {
            return escapeChar;
        }

        /**
         * The function used to resolve variable values by name.
         *
         * @return variable resolution function
         */
        public Function<String, Object> getVariableFunction() {
            return variableFunction;
        }

        /**
         * Create a new {@link Builder} initialized with these options.
         *
         * @return a new builder
         */
        public Builder toBuilder() {
            return new Builder(this);
        }
    }

    /**
     * A fluent builder for creating {@link Options} and {@link MapTemplate} instances.
     */
    public static class Builder {
        private String startVariable = START_VARIABLE;
        private String endVariable = END_VARIABLE;
        private String escapeChar = ESCAPE_CHAR;
        private Function<String, Object> variableFunction = key -> null;

        private Builder() {
        }

        private Builder(Options options) {
            this.startVariable = options.getStartVariable();
            this.endVariable = options.getEndVariable();
            this.escapeChar = options.getEscapeChar();
            this.variableFunction = options.getVariableFunction();
        }

        /**
         * Sets the start marker for variables.
         *
         * @param startVariable the start marker
         * @return this builder
         */
        public Builder setStartVariable(String startVariable) {
            this.startVariable = startVariable;
            return this;
        }

        /**
         * Sets the end marker for variables.
         *
         * @param endVariable the end marker
         * @return this builder
         */
        public Builder setEndVariable(String endVariable) {
            this.endVariable = endVariable;
            return this;
        }

        /**
         * Sets the escape character for markers.
         *
         * @param escapeChar the escape character
         * @return this builder
         */
        public Builder setEscapeChar(String escapeChar) {
            this.escapeChar = escapeChar;
            return this;
        }

        /**
         * Sets the function used to resolve variable values.
         *
         * @param variableFunction the variable resolution function
         * @return this builder
         */
        public Builder setVariableFunction(Function<String, Object> variableFunction) {
            this.variableFunction = variableFunction;
            return this;
        }

        /**
         * Sets the map of variables to be used for resolution.
         * This is a convenience method that delegates to {@link #setVariableFunction(Function)}.
         *
         * @param variableMap the map of variable names to their values
         * @return this builder
         */
        public Builder setVariableMap(Map<String, Object> variableMap) {
            this.variableFunction = variableMap::get;
            return this;
        }

        /**
         * Builds the {@link MapTemplate} instance with the configured options.
         *
         * @return a new MapTemplate
         */
        public MapTemplate build() {
            return new MapTemplate(new Options(startVariable, endVariable, escapeChar, variableFunction));
        }
    }
}
