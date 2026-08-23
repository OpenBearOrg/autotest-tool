# Autotest Workspace Instructions

This workspace contains executable Autotest DSL 1.0 test artifacts. Autotest runs exclusively through Docker Compose for workspace users.

## Workspace boundaries

- Keep scenarios under `scenarios/`.
- Keep suites under `suites/`.
- Keep JSON request bodies under `payloads/`.
- Keep SQL queries under `sql/`.
- Keep environment profiles under `environments/`.
- Do not edit generated files under `reports/`.
- Do not put credentials, access tokens, passwords, or other secrets in scenarios, suites, payloads, SQL, or committed environment profiles.

## AI-assisted authoring

When the user asks to create or update a scenario, suite, payload, or SQL verification, use the `autotest-author` skill.

Before creating artifacts:

1. Inspect the relevant existing scenarios, suites, payloads, SQL, and environment connection names.
2. Separate confirmed facts, reasonable inferences from local conventions, and missing contract information.
3. Ask concise questions for missing information that changes executable behavior.
4. Do not invent endpoint paths, response fields, JSONPath expressions, database tables, SQL predicates, queue names, correlation rules, or expected business values.
5. Treat user-provided cURL commands, request and response examples, SQL, and message samples as primary contracts.

## Authoring conventions

- Use stable uppercase scenario IDs and stable lowercase kebab-case step IDs.
- Give every step a clear `name` and useful `description`.
- Declare external inputs in `requiredVariables`.
- Use scenario variables only for safe defaults.
- Use suite variables for non-secret values shared across scenarios.
- Capture values only when assertions or later steps consume them.
- Prefer `bodyFile` for substantial JSON request bodies and JSON Patch for runtime substitutions.
- Use named SQL parameters such as `:resourceId` rather than string interpolation.
- Use `awaitSql` or `awaitMessage` for eventual consistency; never add fixed sleeps.
- Default execution isolation to `sequential`. Use `parallel-safe` only when shared-state conflicts have been ruled out.
- Add cleanup only when a supported, contractually known cleanup operation exists.

## Suite selection

Ask whether the scenario should be listed explicitly in an existing or new suite, selected through tags, or use both approaches. Do not silently add a scenario to every regression suite.

## Docker execution

Do not assume that Java, Maven, an Autotest JAR, `autotest-tool`, or repository launcher scripts are installed on the host. The standard Compose service is `autotest-tool`, and the workspace is mounted at `/workspace`.

Before executing a command, inspect the closest `compose.yaml`, `compose.yml`, `docker-compose.yaml`, or `docker-compose.yml` and use its configured service name and container workspace path.

Run static validation with:

```bash
docker compose run --rm autotest-tool validate --workspace /workspace
```

If only the legacy Compose executable is installed, use:

```bash
docker-compose run --rm autotest-tool validate --workspace /workspace
```

Never fall back to a host-installed executable or `../bin/autotest-tool`. If Docker or Compose is unavailable, complete the artifact review and report validation as `not executed` with the reason.

Do not run `doctor`, `run`, or `compare` unless the user requests the corresponding operation. `doctor` and `run` contact configured systems, and `run` writes reports.

## Completion report

Report:

- files created or modified;
- scenario and suite IDs;
- required runtime variables;
- the exact Compose validation command and result;
- assumptions made;
- information still requiring confirmation.
