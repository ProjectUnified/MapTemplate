# MapTemplate

A lightweight, thread-safe Java library for applying variable substitution recursively to strings, maps, and collections.

## Features

- **Recursive Replacement**: Automatically deeply traverses `Map` and `Collection` structures to replace placeholders.
- **Nested Variables**: Supports nested placeholders like `{test_{property}_value}`.
- **Balanced Escapes**: Allows literal braces within variable names using escaped markers (e.g., `{test_\{property}_test}`).
- **Immutable Configuration**: Uses an immutable `Options` class with a convenient `Builder` pattern.
- **Thread-Safe**: Designed for concurrent use; the `MapTemplate` instance is immutable after creation.
- **Zero Dependencies**: Pure Java implementation (Java 8+).

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.projectunified</groupId>
    <artifactId>map-template</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Basic Example

```java
Map<String, Object> variables = new HashMap<>();
variables.put("name", "World");

MapTemplate template = MapTemplate.builder()
        .setVariableMap(variables)
        .build();

String result = template.apply("Hello {name}!"); // Returns "Hello World!"
```

### Recursive Map/Collection Support

```java
Map<String, Object> variables = new HashMap<>();
variables.put("user", "Alice");

List<String> list = Arrays.asList("Greeting: {user}", "Role: Admin");
Object result = template.apply(list); // Returns ["Greeting: Alice", "Role: Admin"]
```

### Nested and Escaped Variables

```java
Map<String, Object> variables = new HashMap<>();
variables.put("property", "name");
variables.put("test_name_test", "Success");

// Resolves {property} first to "name", then resolves {test_name_test}
String nested = template.apply("{test_{property}_test}"); // Returns "Success"

// Using literal braces in variable names with escapes
String escaped = template.apply("{test_\\{property}_test}"); // Returns "Success" if key is "test_{property}_test"
```

### Advanced Configuration (Builder)

```java
MapTemplate template = MapTemplate.builder()
        .setStartVariable("${")
        .setEndVariable("}")
        .setEscapeChar("!")
        .setVariableFunction(name -> "Value of " + name)
        .build();

String result = template.apply("${test}"); // Returns "Value of test"
```
