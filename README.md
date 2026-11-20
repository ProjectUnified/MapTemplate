# MapTemplate

A small, thread-safe Java library for substituting variables in strings — and inside maps and collections of them.

```java
Map<String, Object> variables = new HashMap<>();
variables.put("name", "World");

MapTemplate template = new MapTemplate(VariableStringReplacer.builder()
        .setVariableMap(variables)
        .build());

template.apply("Hello {name}!"); // "Hello World!"
```

No dependencies, no reflection, Java 8 and up.

## What it does

- **Looks inside containers.** Give it a map or a collection and every key, value and element gets
  processed the same way a plain string would.
- **Handles nested variables.** `{test_{property}_value}` is resolved from the inside out, in one pass.
- **Lets you write literal braces.** `\{name}` stays `{name}`, and backslashes can be escaped too.
- **Keeps objects as objects.** If a whole string is a single variable, you get the value back as it is,
  not a stringified copy.
- **Is safe to share.** A `MapTemplate` is immutable once built, so one instance can serve many threads.

## Installation

```xml
<dependency>
    <groupId>io.github.projectunified</groupId>
    <artifactId>map-template</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Usage

### Strings

`VariableStringReplacer` is the built-in `{name}` syntax: a `Function<String, Object>` configured with
the variables to look up.

```java
Map<String, Object> variables = new HashMap<>();
variables.put("name", "John");
variables.put("age", 25);

MapTemplate template = new MapTemplate(VariableStringReplacer.builder()
        .setVariableMap(variables)
        .build());

template.apply("Hello {name}!");              // "Hello John!"
template.apply("{age}");                      // 25, still an Integer
template.apply("{missing}");                  // "{missing}", untouched
```

A variable whose value is another template is resolved before it is used:

```java
variables.put("firstName", "John");
variables.put("lastName", "Doe");
variables.put("fullName", "{firstName} {lastName}");

template.apply("{fullName}"); // "John Doe"
```

### Maps and collections

The same template works on a whole structure. Containers keep their shape (a `List` stays a `List`, a
`Set` stays a `Set`, a `ConcurrentMap` stays concurrent), and a value that resolves to a collection is
flattened into the surrounding one.

```java
variables.put("user", "Alice");
variables.put("names", Arrays.asList("John", "Doe"));

template.apply(Arrays.asList("Hi {user}", "{names}")); // ["Hi Alice", "John", "Doe"]
```

Map keys are resolved as well, so a variable can stand in for a key — or for a whole map, whose entries
are then merged into the result.

### Nested and escaped variables

```java
variables.put("property", "name");
variables.put("test_name_value", "Result");
variables.put("test_{property}_value", "Escaped");

template.apply("{test_{property}_value}");   // "Result"  - property resolves first
template.apply("{test_\\{property}_value}"); // "Escaped" - the braces are literal
template.apply("\\{name}");                  // "{name}"
```

If `{}` gets in the way of the text you process, change the markers:

```java
MapTemplate template = new MapTemplate(VariableStringReplacer.builder()
        .setStartVariable("${")
        .setEndVariable("}")
        .setEscapeChar("!")
        .setVariableFunction(name -> "Value of " + name)
        .build());

template.apply("${test}"); // "Value of test"
```

`setVariableFunction` is the general form of `setVariableMap`: any lookup you like works, as long as it
returns `null` for names it does not know (those are left in place).

### Your own replacer

`MapTemplate` knows nothing about `{...}` — it only needs a `Function<String, Object>` that replaces one
string. Anything that implements it fits, including a lambda:

```java
MapTemplate template = new MapTemplate(string -> string.replace("[name]", "John"));

template.apply("Hello [name]!"); // "Hello John!"
```

When your function returns something that is not a string — an object or a collection — `MapTemplate`
keeps processing that result, so a replacer can resolve to a list or a map just like a variable can.

### Combining replacers

`CompositeStringReplacer` runs several replacers through the same string, in the order they were added,
and stops at the first result that is no longer a string. With nothing added, the string passes through
unchanged.

```java
Function<String, Object> replacer = new CompositeStringReplacer()
        .addReplacer(VariableStringReplacer.builder().setVariableMap(variables).build())
        .addReplacer(string -> string.replace("[name]", "John"))
        .build();

MapTemplate template = new MapTemplate(replacer);
```

## Thread safety

Neither `MapTemplate` nor `VariableStringReplacer` keeps mutable state between calls — all parsing
happens on the local stack — so one instance can be shared across threads without synchronization. The
variable map itself is read on every call, so it has to be safe to read concurrently; a `HashMap` that
is never modified after startup is enough.
