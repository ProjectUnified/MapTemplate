package io.github.projectunified.maptemplate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves {@code {name}} markers in a string through a variable function.
 * Use {@link #builder()} or {@link #VariableStringReplacer(Options)} to create one, and pass it to
 * {@link MapTemplate} as the string replacer.
 */
public class VariableStringReplacer implements Function<String, Object> {
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
     * Create a new replacer with the given options.
     *
     * @param options the options
     */
    public VariableStringReplacer(Options options) {
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

    @Override
    public Object apply(String string) {
        if (variableFunction == null) {
            return string;
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
                        Object resolved = value instanceof String ? apply((String) value) : value;

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

    private static class Opener {
        final int index;
        final boolean isEscaped;

        Opener(int index, boolean isEscaped) {
            this.index = index;
            this.isEscaped = isEscaped;
        }
    }

    /**
     * Configuration options for a {@link VariableStringReplacer}.
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
     * A fluent builder for creating {@link VariableStringReplacer} instances.
     */
    public static class Builder {
        private String startVariable = START_VARIABLE;
        private String endVariable = END_VARIABLE;
        private String escapeChar = ESCAPE_CHAR;
        private Function<String, Object> variableFunction = key -> null;

        public Builder() {
        }

        public Builder(Options options) {
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
         * Builds the {@link VariableStringReplacer} with the configured options.
         *
         * @return a new VariableStringReplacer
         */
        public VariableStringReplacer build() {
            return new VariableStringReplacer(new Options(startVariable, endVariable, escapeChar, variableFunction));
        }
    }
}
