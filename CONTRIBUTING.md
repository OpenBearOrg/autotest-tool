# Contributing

## Requirements

- JDK 21
- No local Maven installation is required; use `./mvnw`.

## Build and quality gate

```bash
./mvnw test
./mvnw verify
./mvnw spotless:apply
```

`./mvnw verify` is the required pull-request gate. It runs formatting checks, PMD,
unit tests, integration-test lifecycle hooks, and packaging.

Tests named `*Test` are unit/component tests. Tests named `*IT` belong to the Failsafe
integration lifecycle.

## Architecture

- `org.openbear.tool.autotest.spi` is the public plugin API.
- Core must not import concrete transport plugins.
- Plugins must not depend on the CLI.
- Workspace data, payloads, SQL, and environment profiles stay under `autotest-workspace/`.

Before opening a pull request, run `./mvnw spotless:apply` followed by `./mvnw verify`.
