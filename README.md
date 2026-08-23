# Autotest 1.0.0

Autotest is a standalone Java 21 regression and integration test platform. It runs outside the target system and executes the same versioned scenarios through HTTP, Oracle JDBC, and ActiveMQ/JMS.

## What v1.0.0 includes

- Versioned YAML DSL (`dslVersion: "1.0"`)
- External JSON request bodies with runtime substitution and JSON Patch (`add`, `replace`, `remove`)
- HTTP calls with captured response values and safe retry rules
- Oracle JDBC with named bind parameters and read-only-by-default policy
- Asynchronous SQL polling with observed state-transition history
- ActiveMQ queue browse and selector-correlated dedicated observation
- Runtime variables, `${random:...}` test-data expressions, and response/DB/message captures
- Captures are available to later assertions in the same step, including `expect` blocks on `http`, `sql`, `awaitSql`, and `awaitMessage`
- Singleton JsonPath capture results are normalized to scalars before being stored in the execution context
- Setup / steps / cleanup lifecycle with per-step names and descriptions
- Scenario, suite and tag execution
- Sequential or explicitly `parallel-safe` execution
- Environment profiles and environment-variable secret provider
- DSL/schema/semantic/resource validation before execution
- `doctor` connectivity preflight
- HTML, JSON and JUnit XML reports
- Scenario/resource SHA-256 checksums
- Baseline/candidate comparison
- CI-friendly exit codes
- Executable shaded JAR and Docker runtime definition

## Repository layout

```text
autotest-tool-v1.0.0/
├── autotest-core/       # execution model, variables, polling, assertions
├── autotest-spi/        # stable SPI contracts, registry, discovery and secrets
├── autotest-dsl/        # YAML compiler, schemas, semantic validation
├── autotest-jdbc/       # shared JDBC runtime and SQL plugin
├── autotest-spi-plugins/ # plugin aggregators
│   ├── built-in/        # shipped plugins, one Maven module per plugin
│   └── extensions/      # optional plugin modules
├── autotest-spi-plugins/built-in/reporting-default-plugin/ # default reports and comparison HTML
├── autotest-cli/        # autotest-tool commands
├── autotest-testkit/    # programmatic embedding facade
├── autotest-dist/       # shaded executable JAR
├── autotest-workspace/# ready-to-copy example test workspace
├── ai/
├── AGENTS.md
├── bin/
└── ci/
```

The plugin architecture and HTML reporting behavior are documented in [`ai/architecture.md`](ai/architecture.md) and [`ai/delivery-validation.md`](ai/delivery-validation.md).

## Build

Prerequisites: Java 21. Maven is provided by the project wrapper.

```bash
./mvnw --version
./mvnw clean verify
```

The runnable JAR is produced at:

```text
autotest-dist/target/autotest-tool-1.0.0.jar
```

Run it through the included launcher after building:

```bash
./bin/autotest-tool version
```

Direct `java -jar` execution is suitable only when no external JDBC driver is required.

## External JDBC drivers

Autotest does not distribute Oracle JDBC drivers. To use Oracle, obtain a compatible driver
under Oracle's applicable license terms and place it at:

```text
lib/jdbc/ojdbc.jar
```

The launcher puts every JAR under `lib/jdbc/` on the JVM application classpath before Autotest
starts:

```bash
./bin/autotest-tool doctor --workspace autotest-workspace --env local
./bin/autotest-tool run --workspace autotest-workspace --env local --suite upgrade-regression
```

Only place drivers you trust in this directory. They execute with the same permissions as
Autotest and must not be committed to the repository.

## Docker

The repo includes a runtime image definition plus a compose file and wrapper script for common workflows.

Build the shaded JAR first, then build the image:

```bash
./mvnw clean verify
docker compose build
```

The source-repository Compose file uses the local image tag `autotest-tool:1.0.0` by
default. Set `AUTOTEST_IMAGE` when you want to run a different local or registry image.

Run the tool through Compose against the example workspace:

```bash
docker compose run --rm autotest-tool version
docker compose run --rm autotest-tool validate --workspace /workspace
docker compose run --rm autotest-tool doctor --workspace /workspace --env docker-local
```

Or use the helper script:

```bash
./scripts/autotest-docker.sh version
./scripts/autotest-docker.sh validate --workspace .
```

By default, both helpers mount `autotest-workspace/` into `/workspace`. Override that with `AUTOTEST_WORKSPACE=/absolute/path/to/workspace` if you want to point at a copied workspace.
For Oracle, Compose also mounts `${AUTOTEST_JDBC_LIB_DIR:-./lib/jdbc}` read-only at runtime. Put
your user-supplied `ojdbc.jar` in that directory; it is not copied into the image.

## First real Autotest run

Use the example workspace as a starting point:

```bash
cd autotest-workspace
```

Adapt the environment files, the API path, queue name, and SQL to your installation. Set secrets:

```bash
export AUTH_TOKEN='...'
export DB_USERNAME='...'
export DB_PASSWORD='...'
export MQ_USERNAME='...'
export MQ_PASSWORD='...'
export USER_SCOPES='...'
```

The Compose configuration forwards these exported values into the container without storing their values in the workspace.

The copied workspace includes its own `docker-compose.yaml`, so workspace users do not need Java, Maven, the JAR, a launcher, or a host-installed CLI. Validate without touching the target system:

```bash
docker compose run --rm autotest-tool validate --workspace /workspace
```

The copied workspace defaults to the published image
`ghcr.io/openbearorg/autotest-tool:stable`. For a locally built image from this source
repository, run the command from the workspace with
`AUTOTEST_IMAGE=autotest-tool:1.0.0`.

Check dependencies:

```bash
docker compose run --rm autotest-tool doctor --workspace /workspace --env docker-local
```

Run the baseline:

```bash
docker compose run --rm autotest-tool run \
  --workspace /workspace \
  --env docker-local \
  --suite upgrade-regression \
  --label before-upgrade
```

Run the same test pack after upgrade:

```bash
docker compose run --rm autotest-tool run \
  --workspace /workspace \
  --env docker-local \
  --suite upgrade-regression \
  --label after-upgrade
```

Compare the resulting run files:

```bash
docker compose run --rm autotest-tool compare \
  --baseline /workspace/reports/<baseline>/run.json \
  --candidate /workspace/reports/<candidate>/run.json \
  --out /workspace/reports/upgrade-comparison
```

## CLI

```text
autotest-tool init       Create an empty workspace
autotest-tool validate   Validate DSL, resources and configuration
autotest-tool doctor     Check configured HTTP / Oracle / ActiveMQ dependencies
autotest-tool list       List scenarios and suites
autotest-tool list plugins
                         List discovered plugins and contributions
autotest-tool list step-types
                         List discovered DSL step types
autotest-tool run        Run scenarios, suites or tags
autotest-tool compare    Compare baseline and candidate run.json files
autotest-tool version    Print platform / DSL versions
```

Run `docker compose run --rm autotest-tool <command> --help` from a copied workspace for command options.

Diagnostic logging defaults to `INFO` and can be changed for a run with the global option:

```bash
autotest-tool --log-level DEBUG run --workspace . --env docker-local --suite upgrade-regression
autotest-tool --log-level ERROR doctor --workspace . --env docker-local
```

Logs include timestamp, level, thread, logger/class and message, and are written to stderr. Normal command results remain on stdout.

## Programmatic execution

Embedding code can use the compiler and engine directly without Picocli:

```java
ExecutionPlan plan = new WorkspaceCompiler(Clock.systemUTC())
    .compile(new CompileRequest(workspace, environment, scenarios, suite, tags, variables,
        parallelism, failFast, false));
try (AutotestEngine engine = new DefaultAutotestEngine(runtimeFactory, Clock.systemUTC())) {
  RunResult result = engine.run(plan, RunRequest.create(new RunId("embedded-run"), "embedded"));
  if (result.getStatus().isFailure()) throw new AssertionError("Autotest run failed");
}
```

Normal reports contain `index.html`, `result.json`, `run.json` (the 1.0 compatibility alias),
and `junit.xml`. Automation should consume the versioned `result.json` or JUnit XML contract.

## Safety defaults

- Database writes are denied unless `databasePolicy.allowWrites: true` is explicitly set.
- SQL parameters use JDBC bind variables (`:resourceId`), not string interpolation.
- GET/HEAD/OPTIONS can use safe transport retries; POST/PUT/DELETE are not retried unless explicitly configured.
- Dedicated message observation requires a JMS selector or selector-convertible correlation match.
- `ObjectMessage` payloads are not deserialized.
- Sensitive fields are redacted from generated reports.
- Scenario resources are restricted to the configured workspace root.

## ActiveMQ requirement

For asynchronous integration validation, the preferred topology is a dedicated test/audit destination that receives a copy of business events. Point `awaitMessage` at that destination with `observationMode: dedicated` and correlate using `JMSCorrelationID` or message properties. `browse` is non-destructive but can miss fast-consumed messages on a shared queue.

See `ai/RTK.md`, `ai/architecture.md`, `ai/operations.md`, and `ai/delivery-validation.md`.
