package io.github.projectunified.maptemplate;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
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
    private final Map<String, Object> variableMap;

    /**
     * Create a new {@link MapTemplate} with the given options.
     *
     * @param options the options
     */
    public MapTemplate(Options options) {
        this.startVariable = options.getStartVariable();
        this.endVariable = options.getEndVariable();
        this.escapeChar = options.getEscapeChar();
        this.variableMap = options.getVariableMap();
    }

    /**
     * Create a new builder for {@link Options}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private Object getVariableValue(String string, Map<String, Object> variableMap) {
        if (string.length() < startVariable.length() + endVariable.length() || !string.startsWith(startVariable) || !string.endsWith(endVariable)) {
            return null;
        }
        String variable = string.substring(startVariable.length(), string.length() - endVariable.length());
        Object variableValue = variableMap.get(variable);
        if (variableValue == null) {
            return null;
        }
        return apply(variableValue, variableMap);
    }

    /**
     * Apply the variables to the collection.
     *
     * @param collection  the collection
     * @param variableMap the variable map
     * @return the result
     */
    public Collection<?> apply(Collection<?> collection, Map<String, Object> variableMap) {
        Stream<Object> stream = collection.stream()
                .flatMap(obj -> {
                    if (!(obj instanceof String)) {
                        return Stream.of(apply(obj, variableMap));
                    }
                    String string = (String) obj;
                    Object variableValue = getVariableValue(string, variableMap);
                    if (variableValue != null) {
                        if (variableValue instanceof Collection) {
                            return ((Collection<?>) variableValue).stream();
                        }
                        return Stream.of(variableValue);
                    }
                    return Stream.of(apply(string, variableMap));
                });
        if (collection instanceof List) {
            return stream.collect(Collectors.toList());
        } else if (collection instanceof Set) {
            return stream.collect(Collectors.toSet());
        } else {
            return stream.collect(Collectors.toCollection(ArrayList::new));
        }
    }

    /**
     * Apply the variables to the map.
     *
     * @param map         the map
     * @param variableMap the variable map
     * @return the result
     */
    public Map<?, ?> apply(Map<?, ?> map, Map<String, Object> variableMap) {
        Stream<Map.Entry<?, ?>> stream = map.entrySet().stream()
                .flatMap(entry -> {
                    Object key = entry.getKey();
                    if (!(key instanceof String)) {
                        return Stream.of(new AbstractMap.SimpleEntry<>(key, apply(entry.getValue(), variableMap)));
                    }
                    String string = (String) key;
                    Object variableValue = getVariableValue(string, variableMap);
                    if (variableValue == null) {
                        return Stream.of(new AbstractMap.SimpleEntry<>(key, apply(entry.getValue(), variableMap)));
                    }
                    if (variableValue instanceof Map) {
                        return ((Map<?, ?>) variableValue).entrySet().stream();
                    }
                    return Stream.empty();
                });
        if (map instanceof ConcurrentMap) {
            return stream.collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
        } else if (map instanceof LinkedHashMap) {
            return stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        } else {
            return stream.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        }
    }

    /**
     * Apply the variables to the string.
     *
     * @param string      the string
     * @param variableMap the variable map
     * @return the result
     */
    public Object apply(String string, Map<String, Object> variableMap) {
        if (!string.contains(startVariable) || !string.contains(endVariable)) {
            return string;
        }

        Object variableValue = getVariableValue(string, variableMap);
        if (variableValue != null) {
            return apply(variableValue, variableMap);
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < string.length()) {
            int start = string.indexOf(startVariable, i);
            if (start == -1) {
                sb.append(string.substring(i));
                break;
            }

            sb.append(string, i, start);

            if (start > 0 && string.startsWith(escapeChar, start - escapeChar.length())) {
                int escapeCount = 0;
                int j = start - 1;
                while (j >= 0 && string.startsWith(escapeChar, j)) {
                    escapeCount++;
                    j--;
                }
                if (escapeCount % 2 != 0) {
                    sb.setLength(sb.length() - escapeCount);
                    for (int k = 0; k < escapeCount / 2; k++) {
                        sb.append(escapeChar);
                    }
                    sb.append(startVariable);
                    i = start + startVariable.length();
                    continue;
                } else {
                    sb.setLength(sb.length() - escapeCount);
                    for (int k = 0; k < escapeCount / 2; k++) {
                        sb.append(escapeChar);
                    }
                }
            }

            int end = string.indexOf(endVariable, start + startVariable.length());
            boolean matched = false;
            while (end != -1) {
                String key = string.substring(start + startVariable.length(), end);
                Object value = variableMap.get(key);
                if (value != null) {
                    sb.append(value);
                    i = end + endVariable.length();
                    matched = true;
                    break;
                }
                end = string.indexOf(endVariable, end + endVariable.length());
            }

            if (!matched) {
                sb.append(startVariable);
                i = start + startVariable.length();
            }
        }
        return sb.toString();
    }

    /**
     * Apply the variables to the object.
     *
     * @param obj         the object
     * @param variableMap the variable map
     * @return the result
     */
    public Object apply(Object obj, Map<String, Object> variableMap) {
        if (obj instanceof Collection) {
            return apply((Collection<?>) obj, variableMap);
        }
        if (obj instanceof Map) {
            return apply((Map<?, ?>) obj, variableMap);
        }
        if (obj instanceof String) {
            return apply((String) obj, variableMap);
        }
        return obj;
    }

    /**
     * Apply the variables to the object using the internal variable map.
     *
     * @param obj the object
     * @return the result
     */
    public Object apply(Object obj) {
        return apply(obj, this.variableMap);
    }

    /**
     * Options for {@link MapTemplate}.
     */
    public static class Options {
        private String startVariable = START_VARIABLE;
        private String endVariable = END_VARIABLE;
        private String escapeChar = ESCAPE_CHAR;
        private Map<String, Object> variableMap = Collections.emptyMap();

        public String getStartVariable() {
            return startVariable;
        }

        public void setStartVariable(String startVariable) {
            this.startVariable = startVariable;
        }

        public String getEndVariable() {
            return endVariable;
        }

        public void setEndVariable(String endVariable) {
            this.endVariable = endVariable;
        }

        public String getEscapeChar() {
            return escapeChar;
        }

        public void setEscapeChar(String escapeChar) {
            this.escapeChar = escapeChar;
        }

        public Map<String, Object> getVariableMap() {
            return variableMap;
        }

        public void setVariableMap(Map<String, Object> variableMap) {
            this.variableMap = variableMap;
        }
    }

    /**
     * Builder for {@link Options}.
     */
    public static class Builder {
        private final Options options = new Options();

        public Builder setStartVariable(String startVariable) {
            options.setStartVariable(startVariable);
            return this;
        }

        public Builder setEndVariable(String endVariable) {
            options.setEndVariable(endVariable);
            return this;
        }

        public Builder setEscapeChar(String escapeChar) {
            options.setEscapeChar(escapeChar);
            return this;
        }

        public Builder setVariableMap(Map<String, Object> variableMap) {
            options.setVariableMap(variableMap);
            return this;
        }

        public MapTemplate build() {
            return new MapTemplate(options);
        }
    }
}
