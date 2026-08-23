# Operations

## Environment profiles

Keep URLs, JDBC endpoints and brokers in `environments/*.yaml`. Keep scenario logic and resources unchanged between baseline and candidate environments.

Credentials can be set as either literal values or environment references. Wrap a value in `${...}` to require env lookup; leave it plain to use the literal string. Legacy plain env names still resolve when that environment variable exists:

```yaml
username:
  secret: "${ENV:DB_USERNAME}"
password:
  secret: dbPassword
```

Do not commit secret values.

## Recommended execution workflow

Workspace users run Autotest exclusively through Docker Compose. The workspace is mounted at `/workspace`; no host Java, Maven, JAR, CLI, or launcher is required.

1. `docker compose run --rm autotest-tool validate --workspace /workspace`
2. `docker compose run --rm autotest-tool doctor --workspace /workspace --env docker-local`
3. Execute the baseline with a stable label.
4. Archive the complete report directory.
5. Upgrade the target system.
6. Run the identical test commit against the candidate environment.
7. Compare baseline/candidate `run.json` files.
8. Investigate regressions using the per-step evidence and async transition timeline.

`validate` is static and is the command AI authoring workflows should run automatically after changing workspace artifacts. `doctor` and `run` contact configured systems and must only be run when connectivity or execution is requested.

The source repository Compose file mounts `autotest-workspace/` by default. Set `AUTOTEST_WORKSPACE=/absolute/path/to/workspace` when operating on a copied workspace. A customer workspace can instead keep its own `compose.yaml` beside `autotest-tool.yaml` and mount `./:/workspace`.

## External JDBC drivers

Autotest does not distribute Oracle JDBC. Place a compatible, user-supplied Oracle driver at
`lib/jdbc/ojdbc.jar` and start the source checkout through `bin/autotest-tool`. The launcher adds
all JARs in `lib/jdbc/` to the initial JVM classpath. Docker Compose mounts
`${AUTOTEST_JDBC_LIB_DIR:-./lib/jdbc}` read-only at runtime, so the driver never becomes part of
the published image.

Only put JDBC drivers you trust in that directory. They execute with the same permissions as
Autotest; do not commit driver binaries.

## ActiveMQ topology

Preferred production-test topology:

```text
Batch -> ActiveMQ business destination -> Integration
             |
             +-> copy/tap -> TEST.OBSERVATION.* queue -> autotest-tool
```

The copy/tap is infrastructure-specific and is intentionally outside the test platform. This avoids stealing a business message from Integration.

## Exit codes

- `0`: successful command / all tests passed
- `1`: scenario failure or comparison regression
- `2`: configuration/usage error
- `3`: DSL validation error
- `4`: environment/preflight failure
- `5`: internal platform error

## Logging

Diagnostic logging defaults to `INFO`. Override it for a command with the global `--log-level` option:

```bash
autotest-tool --log-level DEBUG run --workspace <repo> --env <env> --suite <suite>
autotest-tool --log-level ERROR doctor --workspace <repo> --env <env>
```

Supported levels are `INFO`, `DEBUG`, and `ERROR`. Log records include timestamp, level, thread, logger/class, run context where available, and message. Logs are written to stderr; command summaries and machine-readable output remain on stdout. Request bodies, credentials, SQL parameters and message payloads must not be logged.

## Parallel execution

Default parallelism is 1. A scenario must declare `execution.isolation: parallel-safe` before it can be grouped for parallel execution. Use parallel mode only when test data and asynchronous correlations are isolated.

## Report retention

A run directory contains `run.json`, `junit.xml`, HTML summary and scenario pages. Archive `run.json` for upgrade comparison. The JSON is the canonical machine-readable evidence format for 1.0.

## DB write policy

`databasePolicy.allowWrites` defaults to false. Enable writes only for a dedicated test environment and only when setup/cleanup SQL is intentionally required.
