# Repository Guide

This repository is Autotest, a multi-module Java 21/Maven project for black-box upgrade validation against the target system through HTTP, Oracle JDBC, and ActiveMQ/JMS.

## What to read first

- `README.md` for the user-facing overview and build/run commands.
- `ai/RTK.md` for the canonical DSL reference.
- `ai/architecture.md` for module boundaries and execution flow.
- `ai/operations.md` for environment, workflow and runtime guidance.

## Working rules

- Treat `ai/` as the source of truth for repository documentation. Do not recreate the old docs layout.
- Use `apply_patch` for file edits.
- Prefer `rg` for search and `rg --files` for file discovery.
- Do not hand-edit generated outputs under `autotest-workspace/reports/` unless the task explicitly asks for it.
- Keep scenario data, payloads, SQL and environment profiles inside `autotest-workspace/`.
- When changing the DSL, update the schemas, README and `ai/` docs together.

## Repository shape

- `autotest-core` holds shared execution, model, config and utility code.
- `autotest-dsl` owns parsing, semantic validation and embedded schemas.
- `autotest-spi-plugins/built-in` contains one Maven module per shipped transport/adaptation plugin.
- `autotest-spi-plugins/extensions` contains optional extension plugin modules; new extensions belong under this parent.
- `autotest-spi-plugins/built-in/reporting-default-plugin` generates sanitized run evidence and comparison output.
- `autotest-cli` wires the executable commands.
- `autotest-workspace` is an example workspace, not product code.

## Validation

- Use `mvn clean verify` when you need a full build and the environment can resolve dependencies.
- Use `autotest-tool validate --workspace .` and `autotest-tool doctor --workspace . --env <env>` when checking an example workspace.
