# Autotest RTK 1.0

The RTK describes orchestration. Large JSON payloads and complex SQL stay in external files.
The embedded JSON schemas in `autotest-dsl/src/main/resources/schema/` carry stable `$id` values under `https://openbear.org/schemas/autotest-tool/`.

## Scenario envelope

```yaml
dslVersion: "1.0"
id: RESOURCE-CREATE-001
name: Create a resource
tags: [resource, upgrade]
requiredVariables: [resourceId]
variables:
  resourceType: SAMPLE_RESOURCE
execution:
  isolation: sequential
setup: []
steps: []
cleanup: []
```

Scenario and step IDs must be stable and unique.
Steps may also include an optional `name` and `description` for human-readable reporting.

## Variables

Use `${name}` anywhere in supported scalar/list/map values. An exact token preserves its runtime type; an embedded token is converted to text.

Built-ins:

- `${runId}`
- `${executionId}`
- `${scenarioId}`
- `${environment}`
- `${ENV:VARIABLE_NAME}` for process environment values where runtime string resolution is supported

Variable values themselves are also resolved at runtime, so you can define aliases such as `scope: ${ENV:USER_SCOPES}` and then reuse `${scope}` in later paths, headers, SQL parameters or captures.

Required variables are declared with `requiredVariables` and supplied by suite variables or `--var name=value`.

### Runtime random expressions

The reserved `${random:...}` namespace generates runtime test data and cannot conflict with user
variables such as `${customerEnv}`. Each occurrence generates a new value. Bind an expression to a
scenario variable when its value must be reused across steps.

```yaml
variables:
  correlationId: "${random:uuid}"
  requestReference: "test-${random:nanoId}"
```

Supported expressions are `${random:uuid}`, `${random:timestamp}` (Unix seconds),
`${random:isoTimestamp}` (UTC ISO-8601), `${random:nanoId}`, `${random:alphaNumeric}`,
`${random:boolean}`, `${random:int}` (0–1000), `${random:color}`, `${random:hexColor}`,
`${random:abbreviation}`, `${random:word}`, and `${random:words}` (three words).

An exact expression preserves its type: `${random:boolean}`, `${random:int}`, and
`${random:timestamp}` produce Boolean or numeric values; an expression embedded in text is rendered
as text.

## `http`

```yaml
- http:
    id: create-resource
    description: Create a resource and capture its identifier.
    service: api
    request:
      method: POST
      path: /api/resources
      bodyFile: payloads/resource.json
      patch:
        - op: replace
          path: /resource/id
          value: "${resourceId}"
      timeout: 60s
    expect:
      status: 201
      values:
        "$.resourceId": {notNull: true}
    capture:
      resourceId:
        from: response.body
        jsonPath: $.resourceId
```

`body` and `bodyFile` are mutually exclusive. JSON Patch operations in 1.0 are `add`, `replace`, and `remove` using JSON Pointer paths.
Captures are materialized before the same step's `expect` block is evaluated, so a later assertion in that step can reference a captured value.
When a capture JSONPath resolves to a singleton collection, the stored value is flattened to the scalar element.

Business writes are not retried by default. An explicit retry is possible:

```yaml
retry:
  enabled: true
  maxAttempts: 2
  delay: 500ms
```

Only use it for requests that are known to be idempotent.

## `sql`

```yaml
- sql:
    id: verify-created
    connection: db
    queryFile: sql/resource/find.sql
    parameters:
      resourceId: "${resourceId}"
    expect:
      rowCount: 1
      values:
        "$.rows[0].STATUS": NEW
```

Named parameters such as `:resourceId` are converted to JDBC bind parameters.
If a step defines `capture`, those values are materialized before the same step's `expect` block is evaluated, so later assertions in that step can reference captured values.
Singleton capture results are flattened to scalars before they are stored.

## `awaitSql`

```yaml
- awaitSql:
    id: wait-batch
    connection: db
    queryFile: sql/resource/find.sql
    parameters:
      resourceId: "${resourceId}"
    polling:
      timeout: 5m
      interval: 5s
    expect:
      values:
        "$.rows[0].STATUS": PROCESSED
```

The last result and state transitions are preserved as evidence.
Captures on `awaitSql` follow the same step rule as `http`: they are available to that step's `expect` block.
Singleton capture results are flattened to scalars before they are stored.

## `awaitMessage`

```yaml
- awaitMessage:
    id: integration-event
    connection: test-mq
    destination: TEST.OBSERVATION.INTEGRATION
    observationMode: dedicated
    polling:
      timeout: 2m
      interval: 1s
    match:
      correlationId: "${correlationId}"
    expect:
        values:
          "$.body.resourceId": "${resourceId}"
```

Captures on `awaitMessage` follow the same step rule as `http`: they are available to that step's `expect` block.
Singleton capture results are flattened to scalars before they are stored.

Modes:

- `dedicated`: consumes from a dedicated test/audit queue. A selector/correlation criterion is mandatory.
- `browse`: non-destructive queue browsing. Suitable only where timing/race behavior is acceptable.

Message model exposed to assertions:

```json
{
  "headers": {"messageId": "...", "correlationId": "..."},
  "properties": {},
  "body": {}
}
```

## `set`

```yaml
- set:
    id: set-expected
    values:
      expectedStatus: SUCCESS
```

## `assert`

```yaml
- assert:
    id: verify-capture
    values:
      "${capturedStatus}": SUCCESS
```

## Assertion operators

Path-based expectations support direct equality and these operator maps:

```yaml
"$.id": {notNull: true}
"$.error": {isNull: true}
"$.status": {equals: SUCCESS}
"$.text": {contains: completed}
"$.code": {matches: "ORD-[0-9]+"}
"$.status": {in: [NEW, PROCESSING, DONE]}
"$.count": {greaterThan: 0}
"$.duration": {lessThan: 1000}
```

## Suite

```yaml
suiteVersion: "1.0"
id: UPGRADE-REGRESSION
name: Upgrade Regression
variables:
  resourceType: SAMPLE_RESOURCE
scenarios:
  - scenarios/resource/create-resource.yaml
```

Suites can also select by `includeTags` and exclude using `excludeTags`.
