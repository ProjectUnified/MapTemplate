package io.github.projectunified.maptemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves {@code {name}} markers in a string through a variable function.
 * Use {@link #builder()} or {@link #VariableStringReplacer(String, String, String, Function)} to create one, and pass
 * it to {@link MapTemplate} as the string replacer.
 * <p>
 * The markers can be changed, and when the start marker is also the end marker the occurrences alternate between
 * opening and closing, which gives the {@code %name%} style.
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
     * Create a new replacer with the given markers and variable function.
     *
     * @param startVariable    the string that indicates the start of a variable (e.g. "{")
     * @param endVariable      the string that indicates the end of a variable (e.g. "}")
     * @param escapeChar       the string used to escape markers (e.g. "\\")
     * @param variableFunction the function resolving a variable name, returning {@code null} for unknown names
     */
    public VariableStringReplacer(String startVariable, String endVariable, String escapeChar, Function<String, Object> variableFunction) {
        this.startVariable = startVariable;
        this.endVariable = endVariable;
        this.escapeChar = escapeChar;
        this.variableFunction = variableFunction;
    }

    /**
     * Create a new builder, starting from the default markers and no variables.
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

        int length = string.length();

        // A marker is only closed by an end marker: without one there is nothing to parse
        if (!string.contains(endVariable)) {
            return string;
        }

        // The result is built next to the input instead of inside a copy of it, and only once something actually
        // changes. "out" holds the output so far, "flushed" how much of the input it covers, so the text between
        // them is still the untouched input: a string with nothing to replace is never copied.
        // With the same string as start and end marker, occurrences alternate between opening and closing.
        boolean pairedMarker = startVariable.equals(endVariable);
        StringBuilder out = null;
        int flushed = 0;
        int changes = 0;
        Opener[] stack = new Opener[4];
        int depth = 0;
        int i = 0;

        while (i < length) {
            boolean isStart;
            int markerPos;
            if (pairedMarker) {
                markerPos = string.indexOf(startVariable, i);

                // No more variables can be resolved if there's no marker left
                if (markerPos < 0) {
                    break;
                }
                isStart = (depth == 0);
            } else {
                int start = string.indexOf(startVariable, i);
                int end = string.indexOf(endVariable, i);

                // No more variables can be resolved if there's no end marker
                if (end < 0) {
                    break;
                }

                // Determine if we are processing a start marker or an end marker
                isStart = (start >= 0 && start < end);
                markerPos = isStart ? start : end;
            }
            String marker = isStart ? startVariable : endVariable;

            // 1. Count the escapes before the marker (e.g. \\{ -> \{) and remember if the marker is escaped
            int escapeCount = countPrefix(string, markerPos, escapeChar);
            boolean escaped = (escapeCount % 2 != 0);
            int escapeStart = markerPos - (escapeCount * escapeChar.length());

            // 2. A start marker opens a variable, and remembers where its name starts
            if (isStart) {
                if (escapeCount > 0) {
                    out = writeEscapes(out, length, string, flushed, escapeStart, escapeCount, marker);
                    changes++;
                    flushed = markerPos + marker.length();
                    // With the same marker for start and end, an escaped marker is only a literal one: it does not
                    // open a literal block, so a stray escape cannot hide a variable behind it
                    if (!pairedMarker || !escaped) {
                        if (depth == stack.length) {
                            stack = Arrays.copyOf(stack, depth * 2);
                        }
                        stack[depth++] = new Opener(out.length(), flushed, changes, escaped);
                    }
                } else {
                    // A plain start marker writes nothing: its position is known from the input and the output
                    int outStart = (out == null ? 0 : out.length()) + (markerPos - flushed) + startVariable.length();
                    if (depth == stack.length) {
                        stack = Arrays.copyOf(stack, depth * 2);
                    }
                    stack[depth++] = new Opener(outStart, markerPos + startVariable.length(), changes, escaped);
                }
                i = markerPos + marker.length();
                continue;
            }

            // 3. An unescaped end marker balancing an unescaped start marker resolves a variable
            Opener opener = (!escaped && depth > 0) ? stack[--depth] : null;

            if (opener == null || opener.isEscaped) {
                // Escaped end marker, unmatched end marker, or the end of a literal block: keep it, resolve nothing
                if (escapeCount > 0) {
                    out = writeEscapes(out, length, string, flushed, escapeStart, escapeCount, marker);
                    changes++;
                    flushed = markerPos + marker.length();
                }
                i = markerPos + marker.length();
                continue;
            }

            // 4. Read the variable name between the unescaped start and end markers. While nothing inside the
            // variable has changed, its name is still part of the input and needs no copy at all.
            boolean modified = (escapeCount > 0 || changes != opener.changesAtPush);
            String variableName;
            if (modified) {
                if (out == null) {
                    out = new StringBuilder(length);
                }
                out.append(string, flushed, escapeStart);
                if (escapeCount > 0) {
                    appendEscapes(out, escapeCount / 2);
                    changes++;
                }
                flushed = markerPos;
                variableName = out.substring(opener.outStart);
            } else {
                variableName = string.substring(opener.inStart, markerPos);
            }

            Object value = variableFunction.apply(variableName);
            int markerEnd = markerPos + marker.length();

            if (value == null) {
                // Variable not found: leave it as it is and continue
                if (modified) {
                    out.append(marker);
                    flushed = markerEnd;
                }
                i = markerEnd;
                continue;
            }

            Object resolved = value instanceof String ? apply((String) value) : value;

            // If the entire original string was precisely this variable, return the object directly
            if (out == null && opener.inStart == startVariable.length() && markerEnd == length) {
                return resolved;
            }

            // Otherwise, replace the marker and the name in it with the string representation of the value
            if (out == null) {
                out = new StringBuilder(length);
            }
            int markerStart = opener.inStart - startVariable.length();
            if (flushed < markerStart) {
                out.append(string, flushed, markerStart);
                flushed = markerStart;
            }
            // Drop the variable from the output and put the value in its place
            out.setLength(opener.outStart - startVariable.length());
            out.append(resolved);
            changes++;
            flushed = markerEnd;
            i = markerEnd;
        }

        if (out == null) {
            return string;
        }
        out.append(string, flushed, length);
        return out.toString();
    }

    /**
     * Appends the input up to the escapes, then the marker with half of its escapes dropped.
     *
     * @param out         the output, created when the input is first copied into it
     * @param length      the length of the input
     * @param input       the string being replaced
     * @param flushed     how much of the input the output already covers
     * @param escapeStart the start of the escapes before the marker
     * @param escapeCount the number of escapes before the marker
     * @param marker      the marker that follows the escapes
     * @return the output
     */
    private StringBuilder writeEscapes(StringBuilder out, int length, String input, int flushed, int escapeStart, int escapeCount, String marker) {
        if (out == null) {
            out = new StringBuilder(length);
        }
        out.append(input, flushed, escapeStart);
        appendEscapes(out, escapeCount / 2);
        out.append(marker);
        return out;
    }

    private int countPrefix(String string, int end, String prefix) {
        int length = prefix.length();
        if (length == 0) {
            return 0;
        }
        int count = 0;
        outer:
        for (int i = end - length; i >= 0; i -= length) {
            for (int j = 0; j < length; j++) {
                if (string.charAt(i + j) != prefix.charAt(j)) {
                    break outer;
                }
            }
            count++;
        }
        return count;
    }

    private void appendEscapes(StringBuilder out, int count) {
        for (int i = 0; i < count; i++) {
            out.append(escapeChar);
        }
    }

    private static class Opener {
        final int outStart;
        final int inStart;
        final int changesAtPush;
        final boolean isEscaped;

        Opener(int outStart, int inStart, int changesAtPush, boolean isEscaped) {
            this.outStart = outStart;
            this.inStart = inStart;
            this.changesAtPush = changesAtPush;
            this.isEscaped = isEscaped;
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

        /**
         * Create a builder starting from the default markers and no variables.
         */
        public Builder() {
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
         * Builds the {@link VariableStringReplacer} with the configured markers and variable function.
         *
         * @return a new VariableStringReplacer
         */
        public VariableStringReplacer build() {
            return new VariableStringReplacer(startVariable, endVariable, escapeChar, variableFunction);
        }
    }
}
