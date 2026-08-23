# Autotest DSL 1.0 Mapping Rules

## Business action to step type

| User intent | DSL step |
|---|---|
| Call an API | `http` |
| Query the database once | `sql` |
| Wait for database state | `awaitSql` |
| Wait for a JMS message | `awaitMessage` |
| Define a context value | `set` |
| Compare captured or context values | `assert` |

Do not translate “wait a few seconds” into a fixed delay. Identify an observable state and use `awaitSql` or `awaitMessage`.

## Variables

Use `requiredVariables` for caller-supplied values, scenario `variables` for safe defaults, suite `variables` for safe shared overrides, and captures for values produced during execution. Built-ins include `${runId}`, `${executionId}`, `${scenarioId}`, and `${environment}`.

Variable precedence is scenario defaults, suite variables, CLI variables passed to the container command, captures, and immutable built-ins.

## HTTP bodies

Use inline `body` for an empty or very small body and `bodyFile` for substantial JSON. Keep static structure in `payloads/` and use JSON Patch operations `add`, `replace`, or `remove` with JSON Pointer paths for runtime substitutions.

## Captures

Create captures only when an assertion or later step consumes the value. Verify every JSONPath against a representative response, query result, or message. Never invent a JSONPath from a field description alone.

HTTP response example:

```yaml
capture:
  resourceId:
    from: response.body
    jsonPath: $.resourceId
```

SQL response example:

```yaml
capture:
  resourceId:
    from: response.body
    jsonPath: $.rows[0].resourceId
```

Message example:

```yaml
capture:
  eventAction:
    from: message.body
    jsonPath: $.action
```

## Assertions

Use direct equality or `equals` for exact values, `notNull` for required values, `isNull` for absence, `contains` for substrings, `matches` for formats, `in` for accepted states, and `greaterThan` or `lessThan` for numeric bounds. Prefer business-significant assertions over checking every response field.

## SQL

Store queries under a domain folder in `sql/`. Use named bind parameters such as `WHERE RESOURCE_ID = :resourceId`; never interpolate `${variable}` into SQL text. Use `sql` for immediate consistency and `awaitSql` for eventual consistency.

## Message observation

Use `dedicated` when a dedicated test or audit destination exists; it requires correlation or selector-compatible matching. Use `browse` only when the user accepts its race and timing limitations.

## Suites

Use an explicit `scenarios` list for curated membership and `includeTags` for membership that should grow automatically. Use `excludeTags` for known exclusions. Confirm the intended behavior before combining explicit and tag selection.

## Final quality checks

Check scenario and step ID uniqueness, resource existence, capture-before-use ordering, variable completeness, suite paths, assertion operators, timeout and polling syntax, dedicated message correlation, and absence of embedded secrets before Compose validation.
