---
name: autotest-author
description: Interview users and create or update Autotest DSL 1.0 scenarios, suites, JSON payloads, and SQL resources from business flows, cURL examples, API contracts, database checks, or message expectations. Use for Autotest artifact authoring, but not for changing the Java DSL implementation or merely running existing tests.
---

# Autotest Author

Transform a user's test intent into validated Autotest workspace artifacts.

## Locate the workspace

Work in the nearest directory containing `autotest-tool.yaml` plus `scenarios/` and `suites/`.

Inspect existing artifacts relevant to the user's domain before interviewing the user. Reuse established service names, connection names, folder organization, headers, tags, and naming patterns only when they match the requested behavior.

## Choose the workflow

- For an incomplete business requirement, read [references/interview-guide.md](references/interview-guide.md).
- Before generating or modifying executable artifacts, read [references/dsl-mapping.md](references/dsl-mapping.md).
- If the user provides a complete executable contract, skip questions already answered and ask only about material gaps.

## Maintain an internal fact model

Separate collected information into confirmed facts, inferred local conventions, and missing facts required for executable behavior. Tell the user about material inferences. Never present an inference as a confirmed contract.

## Interview behavior

Ask small, related groups of questions rather than the entire questionnaire at once. Prioritize blockers in this order:

1. business outcome and scenario boundary;
2. ordered operations and external contracts;
3. expected results and captures;
4. asynchronous database or message verification;
5. variables, cleanup, isolation, and suite membership.

Accept `unknown` as an answer. When essential information remains unknown, generate only the portions that can be correct and identify the blocked artifacts. Do not fabricate placeholders that appear executable.

## Propose the test shape

When the request is materially ambiguous, summarize the proposed scenario identity, ordered setup/normal/cleanup steps, variables and captures, supporting resources, and suite membership before writing. Ask for confirmation only when different interpretations would change business behavior.

## Generate artifacts

Create or update only files required by the request. Preserve unrelated user changes. Reuse an existing payload or SQL resource only when its behavior exactly matches the request.

Ensure that:

- file references are relative to the workspace;
- IDs are stable and unique;
- every variable is defined, captured, built in, or declared required;
- every capture is produced before it is consumed;
- polling steps have explicit timeout and interval;
- dedicated message observation has a correlation or selector criterion;
- suite scenario paths resolve to existing files;
- secrets are referenced through environment configuration rather than embedded.

## Validate through Docker Compose

Autotest is containerized. Never require or use a host-installed Autotest CLI, Java runtime, Maven build, JAR, or repository launcher.

Locate and inspect the closest Compose file. Confirm the Autotest service name, host workspace mount, and container workspace path. The standard configuration uses service `autotest-tool` and path `/workspace`.

Run static validation:

```bash
docker compose run --rm autotest-tool validate --workspace /workspace
```

If only the legacy executable is installed:

```bash
docker-compose run --rm autotest-tool validate --workspace /workspace
```

Fix authoring errors within the user's requested scope and rerun validation. Do not build the Java project, invoke `autotest-tool` directly, use `../bin/autotest-tool`, or run `doctor` as a substitute for validation.

If Compose cannot run because Docker is unavailable, the image cannot be pulled, or container execution is not permitted, complete the static artifact review and report validation as `not executed`, including the reason. Never claim validation passed based only on inspection.

Do not run `doctor`, `run`, or `compare` unless the user requests that operation.

## Finish

Report created and modified artifacts, required runtime inputs, suite membership, the exact validation command and result, assumptions, and unresolved contract gaps.
