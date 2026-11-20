package io.github.projectunified.maptemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Gathers string replacers and folds them into a single {@link Function} for {@link MapTemplate}.
 * The replacers run in the order they were added, each one only while the current result is still a string.
 */
public class CompositeStringReplacer {
    private final List<Function<String, Object>> replacers = new ArrayList<>();

    /**
     * Adds a replacer to run after the previously added ones.
     *
     * @param replacer the replacer to add
     * @return this instance
     */
    public CompositeStringReplacer addReplacer(Function<String, Object> replacer) {
        replacers.add(replacer);
        return this;
    }

    /**
     * Builds the combined replacer.
     * With no replacer the input is returned as-is, with one replacer it is used directly, and with
     * several they are folded into one function, stopping as soon as a result is not a string.
     *
     * @return the combined replacer
     */
    public Function<String, Object> build() {
        if (replacers.isEmpty()) {
            return input -> input;
        }
        if (replacers.size() == 1) {
            return replacers.get(0);
        }
        List<Function<String, Object>> snapshot = new ArrayList<>(replacers);
        return input -> {
            Object result = input;
            for (Function<String, Object> replacer : snapshot) {
                if (!(result instanceof String)) {
                    break;
                }
                result = replacer.apply((String) result);
            }
            return result;
        };
    }
}
