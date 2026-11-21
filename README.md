# MapTemplate

A small, thread-safe Java library for substituting variables in strings — and inside maps and collections of them.

```java
Map<String, Object> variables = new HashMap<>();
variables.put("name", "World");

// Flag the strings once...
MapTemplate template = MapTemplate.mark("Hello {name}!");

// ...then fill them in with any replacer
template.apply(VariableStringReplacer.builder()
        .setVariableMap(variables)
        .build()); // "Hello World!"
```

No dependencies, no reflection, Java 8 and up.

## What it does

- **Flags first, replaces later.** `MapTemplate.mark(object)` walks a structure once and records where its strings
  are. `template.apply(replacer)` then builds the filled-in object without inspecting the structure again, so the same
  template can be reused with different replacers.
- **Looks inside containers.** Give it a map or a collection and every key, value and element gets
  processed the same way a plain string would.
- **Handles nested variables.** `{test_{property}_value}` is resolved from the inside out, in one pass.
- **Lets you write literal braces.** `\{name}` stays `{name}`, and backslashes can be escaped too.
- **Keeps objects as objects.** If a whole string is a single variable, you get the value back as it is,
  not a stringified copy.
- **Is safe to share.** A `MapTemplate` holds no replacer and no mutable state, so one instance can serve many threads.

## Installation

```xml
<dependency>
    <groupId>io.github.projectunified</groupId>
    <artifactId>map-template</artifactId>
    <version>3.0.0</version>
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

Function<String, Object> replacer = VariableStringReplacer.builder()
        .setVariableMap(variables)
        .build();

MapTemplate.mark("Hello {name}!").apply(replacer); // "Hello John!"
MapTemplate.mark("{age}").apply(replacer);         // 25, still an Integer
MapTemplate.mark("{missing}").apply(replacer);     // "{missing}", untouched
```

A variable whose value is another template is resolved before it is used:

```java
variables.put("firstName", "John");
variables.put("lastName", "Doe");
variables.put("fullName", "{firstName} {lastName}");

MapTemplate.mark("{fullName}").apply(replacer); // "John Doe"
```

### Maps and collections

The same template works on a whole structure. Containers keep their shape (a `List` stays a `List`, a
`Set` stays a `Set`, a `ConcurrentMap` stays concurrent), and a value that resolves to a collection is
flattened into the surrounding one.

```java
variables.put("user", "Alice");
variables.put("names", Arrays.asList("John", "Doe"));

MapTemplate template = MapTemplate.mark(Arrays.asList("Hi {user}", "{names}"));

template.apply(replacer); // ["Hi Alice", "John", "Doe"]
```

Map keys are resolved as well, so a variable can stand in for a key — or for a whole map, whose entries
are then merged into the result. When two resolved keys are equal the last one wins, and keys or values
that resolve to `null` are kept by map types that allow them (`HashMap`, `LinkedHashMap`); a
`ConcurrentMap` result is a `ConcurrentHashMap`, so it rejects them like any other concurrent map.

The flagged template holds a copy of the structure it was made from, so changing the original map or
collection afterwards does not affect it. Every `apply` builds a fresh result.

### Nested and escaped variables

```java
variables.put("property", "name");
variables.put("test_name_value", "Result");
variables.put("test_{property}_value", "Escaped");

MapTemplate.mark("{test_{property}_value}").apply(replacer);   // "Result"  - property resolves first
MapTemplate.mark("{test_\\{property}_value}").apply(replacer); // "Escaped" - the braces are literal
MapTemplate.mark("\\{name}").apply(replacer);                  // "{name}"
```

If `{}` gets in the way of the text you process, change the markers:

```java
MapTemplate template = MapTemplate.mark("${test}");

template.apply(VariableStringReplacer.builder()
        .setStartVariable("${")
        .setEndVariable("}")
        .setEscapeChar("!")
        .setVariableFunction(name -> "Value of " + name)
        .build()); // "Value of test"
```

`setVariableFunction` is the general form of `setVariableMap`: any lookup you like works, as long as it
returns `null` for names it does not know (those are left in place).

A marker that is its own end marker gives the `%name%` style: occurrences alternate between opening and
closing, and an escaped marker is just a literal marker.

```java
Function<String, Object> percents = VariableStringReplacer.builder()
        .setStartVariable("%")
        .setEndVariable("%")
        .setVariableMap(variables)
        .build();

MapTemplate.mark("Hello %name%!").apply(percents);        // "Hello John!"
MapTemplate.mark("%firstName% %lastName%").apply(percents); // "John Doe"
MapTemplate.mark("100\\% of %name%").apply(percents);      // "100% of John"
```

### Your own replacer

`MapTemplate` knows nothing about `{...}` — it only needs a `Function<String, Object>` that replaces one
string. Anything that implements it fits, including a lambda:

```java
MapTemplate template = MapTemplate.mark("Hello [name]!");

template.apply(string -> string.replace("[name]", "John")); // "Hello John!"
```

When your function returns something that is not a string — an object or a collection — `MapTemplate`
keeps processing that result, so a replacer can resolve to a list or a map just like a variable can. That
result only exists at apply time, so it is walked then; return a `MapTemplate` instead of a raw container
to keep that path just as fast:

```java
MapTemplate names = MapTemplate.mark(Arrays.asList("John", "Doe"));
MapTemplate template = MapTemplate.mark(Arrays.asList("Hi", "{names}"));

template.apply(name -> name.equals("{names}") ? names : name); // ["Hi", "John", "Doe"]
```

### Combining replacers

`CompositeStringReplacer` runs several replacers through the same string, in the order they were added,
and stops at the first result that is no longer a string. With nothing added, the string passes through
unchanged.

```java
Function<String, Object> replacer = new CompositeStringReplacer()
        .addReplacer(VariableStringReplacer.builder().setVariableMap(variables).build())
        .addReplacer(string -> string.replace("[name]", "John"))
        .build();

MapTemplate template = MapTemplate.mark(structure);
template.apply(replacer);
```

This is also how one pass resolves more than one syntax, e.g. `{}` and `%%` in the same map:

```java
Function<String, Object> replacer = new CompositeStringReplacer()
        .addReplacer(VariableStringReplacer.builder()
                .setVariableMap(variables)
                .build())
        .addReplacer(VariableStringReplacer.builder()
                .setStartVariable("%")
                .setEndVariable("%")
                .setVariableMap(variables)
                .build())
        .build();

Map<String, Object> structure = new LinkedHashMap<>();
structure.put("%player_name%", "Hi {name}, you have %count% items");

MapTemplate.mark(structure).apply(replacer); // {"Alice": "Hi John, you have 3 items"}
```

Both syntaxes are read from the same variable map, and each string is scanned once per replacer, so two
syntaxes cost about twice one. A value that resolves to a whole object or container is returned as it is
and the remaining replacers are skipped for that string.

## Thread safety

Neither `MapTemplate` nor `VariableStringReplacer` keeps mutable state between calls — all parsing
happens on the local stack — so one instance can be shared across threads without synchronization. The
variable map itself is read on every call, so it has to be safe to read concurrently; a `HashMap` that
is never modified after startup is enough.
