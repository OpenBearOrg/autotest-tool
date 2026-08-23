# Architecture

## Boundaries

`autotest-tool` is a black-box test platform. It never loads application libraries from the target runtime. The platform communicates only through external contracts: HTTP, Oracle JDBC, and ActiveMQ/JMS.

```text
Autotest workspace
  YAML + JSON + SQL + environment profiles
                |
                v
          DSL compiler
  schema -> semantics -> resources -> checksums
                |
                v
   execution engine
   context / captures / assertions / polling
       /           |              \
    HTTP         Oracle         ActiveMQ
       \           |              /
                target runtime
                |
                v
   HTML / JSON / JUnit / compare
```

Execution is compiled into an immutable `ExecutionPlan`. The engine publishes run,
scenario, and step lifecycle events through `ExecutionEventDispatcher`, while
`VirtualThreadScenarioScheduler` owns bounded concurrency, fail-fast scheduling, and
selected-order result assembly. Console output is an optional execution listener.

All scenarios run as `CompiledStep`/`ExecutableStep` values through the public plugin registry.
Built-in HTTP, JDBC, ActiveMQ, and reporting providers use the same registry and lifecycle as
providers loaded with `--plugin-dir`; there is no alternate executable-handler bridge. The command
opens the runtime, executes it through the normal scheduler, writes the standard report artifacts,
and closes the runtime and loader at the end of the command.

Reports are independent contracts: HTML evidence, versioned `result.json` (with the
backward-compatible `run.json` alias), and JUnit XML are written by separate reporters.

## Module responsibilities

- `autotest-core`: dependency-light execution engine, compiled-domain model, and public plugin registry.
- `autotest-spi`: stable plugin contracts, registry primitives, discovery helpers and shared secret resolution.
- `autotest-dsl`: stable DSL 1.0 parser/compiler, YAML DTO ownership, and embedded JSON schemas.
- `autotest-jdbc`: shared JDBC pooling, query execution, SQL step providers and vendor driver capabilities.
- `autotest-spi-plugins`: plugin aggregators.
- `autotest-spi-plugins/built-in`: shipped HTTP, Oracle, MySQL and ActiveMQ plugins, one Maven module per plugin.
- `autotest-spi-plugins/extensions`: optional plugin modules loaded through the external plugin mechanism.
- `autotest-core`: comparison calculation and canonical comparison model.
- `autotest-spi-plugins/built-in/reporting-default-plugin`: sanitized evidence, scenario timelines and HTML/JSON/JUnit report files.
- `autotest-cli`: composition root and user-facing commands.
- `autotest-testkit`: Java API for embedding the compiler in tooling/tests.
- `autotest-dist`: executable fat JAR.

## Execution context

Each scenario gets an isolated public step context containing run ID, execution ID, environment,
scenario variables, suite/CLI variables, and runtime captures. Precedence is:

1. scenario defaults
2. suite variables
3. CLI `--var` variables
4. runtime captures
5. built-in immutable runtime identifiers (`runId`, `executionId`, `scenarioId`, `environment`)

Captured values can be reused by subsequent HTTP, SQL, message and assertion steps, including later `expect` checks in the same step.
Singleton JsonPath capture results are normalized to scalars before storage, which keeps filtered one-item captures usable as plain variables.
Variable values are resolved recursively when they are read, so aliases like `scope: ${ENV:USER_SCOPES}` can be reused as `${scope}` in later steps.

## Async model

`awaitSql` and `awaitMessage` use one polling engine. Polling records only state changes, reducing report noise while retaining the sequence needed to diagnose a stalled distributed workflow.

Fixed sleeps are intentionally not a DSL feature.

## Failure model

A normal step failure skips later normal steps unless the failed step has `continueOnFailure: true`. Cleanup always executes. One failed scenario does not abort the suite unless `--fail-fast` is enabled.

## Upgrade comparison

Every scenario run stores the scenario SHA-256 and checksums for referenced JSON/SQL resources. `compare` matches scenarios by stable scenario ID and reports regressions, fixes, new/missing scenarios and checksum drift between baseline and candidate.

The compatibility policy for the implemented SPI is documented in
[`ai/spi-compatibility.md`](spi-compatibility.md).
