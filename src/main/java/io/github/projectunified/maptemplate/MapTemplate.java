package io.github.projectunified.maptemplate;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapTemplate {
  public static final String START_VARIABLE = "{";
  public static final String END_VARIABLE = "}";
  public static final String ESCAPE_CHAR = "\\";

  private static Object getVariableValue(String string, Map<String, Object> variableMap) {
    if (string.length() < START_VARIABLE.length() + END_VARIABLE.length() || !string.startsWith(START_VARIABLE) || !string.endsWith(END_VARIABLE)) {
      return null;
    }
    String variable = string.substring(START_VARIABLE.length(), string.length() - END_VARIABLE.length());
    Object variableValue = variableMap.get(variable);
    if (variableValue == null) {
      return null;
    }
    return apply(variableValue, variableMap);
  }

  public static Collection<?> apply(Collection<?> collection, Map<String, Object> variableMap) {
    Stream<Object> stream = collection.stream()
      .flatMap(obj -> {
        if (!(obj instanceof String)) {
          return Stream.of(obj);
        }
        String string = (String) obj;
        Object variableValue = getVariableValue(string, variableMap);
        if (variableValue == null) {
          return Stream.of(obj);
        }
        if (variableValue instanceof Collection) {
          return ((Collection<?>) variableValue).stream();
        }
        return Stream.of(variableValue);
      });
    if (collection instanceof List) {
      return stream.collect(Collectors.toList());
    } else if (collection instanceof Set) {
      return stream.collect(Collectors.toSet());
    } else {
      return stream.collect(Collectors.toCollection(ArrayList::new));
    }
  }

  public static Map<?, ?> apply(Map<?, ?> map, Map<String, Object> variableMap) {
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

  public static Object apply(Object obj, Map<String, Object> variableMap) {
    if (variableMap == null || variableMap.isEmpty()) {
      return obj;
    }
    if (obj instanceof Collection) {
      return apply((Collection<?>) obj, variableMap);
    }
    if (obj instanceof Map) {
      return apply((Map<?, ?>) obj, variableMap);
    }
    if (obj instanceof String) {
      String result = (String) obj;

      if (!result.contains(START_VARIABLE) || !result.contains(END_VARIABLE)) {
        return result;
      }

      Object variableValue = getVariableValue(result, variableMap);
      if (variableValue != null) {
        return apply(variableValue, variableMap);
      }

      StringBuilder sb = new StringBuilder();
      int i = 0;
      while (i < result.length()) {
        int start = result.indexOf(START_VARIABLE, i);
        if (start == -1) {
          sb.append(result.substring(i));
          break;
        }

        sb.append(result, i, start);

        if (start > 0 && result.startsWith(ESCAPE_CHAR, start - ESCAPE_CHAR.length())) {
          int escapeCount = 0;
          int j = start - 1;
          while (j >= 0 && result.startsWith(ESCAPE_CHAR, j)) {
            escapeCount++;
            j--;
          }
          if (escapeCount % 2 != 0) {
            sb.setLength(sb.length() - escapeCount);
            for (int k = 0; k < escapeCount / 2; k++) {
              sb.append(ESCAPE_CHAR);
            }
            sb.append(START_VARIABLE);
            i = start + START_VARIABLE.length();
            continue;
          } else {
            sb.setLength(sb.length() - escapeCount);
            for (int k = 0; k < escapeCount / 2; k++) {
              sb.append(ESCAPE_CHAR);
            }
          }
        }

        int end = result.indexOf(END_VARIABLE, start + START_VARIABLE.length());
        boolean matched = false;
        while (end != -1) {
          String key = result.substring(start + START_VARIABLE.length(), end);
          Object value = variableMap.get(key);
          if (value != null) {
            sb.append(value);
            i = end + END_VARIABLE.length();
            matched = true;
            break;
          }
          end = result.indexOf(END_VARIABLE, end + END_VARIABLE.length());
        }

        if (!matched) {
          sb.append(START_VARIABLE);
          i = start + START_VARIABLE.length();
        }
      }
      return sb.toString();
    }
    return obj;
  }
}
