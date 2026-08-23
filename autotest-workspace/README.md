# Autotest workspace

This directory is intentionally separate from the Java platform source. Copy the entire directory, including the hidden `.agents/` directory, to its own repository and adapt the environment URLs, queue name, API path, payload, and SQL to your target schema. Workspace users only need Docker Compose; Java, Maven, the Autotest JAR, and a host CLI are not required.

The included `docker-compose.yaml` mounts the current directory at `/workspace` and runs
the published `ghcr.io/openbearorg/autotest-tool:stable` image. To use an image built
from the source repository instead, set `AUTOTEST_IMAGE=autotest-tool:1.0.0`.

Required runtime variables/secrets for the included example:

```bash
export DB_USERNAME='...'
export DB_PASSWORD='...'
```

The Compose configuration forwards these exported values into the container without storing their values in the workspace.

Validate first:

```bash
docker compose run --rm autotest-tool validate --workspace /workspace
```

Check connectivity:

```bash
docker compose run --rm autotest-tool doctor --workspace /workspace --env docker-local
```

Run the same scenario before and after the upgrade:

```bash
docker compose run --rm autotest-tool run --workspace /workspace --env docker-local --suite upgrade-regression --label before-upgrade
docker compose run --rm autotest-tool run --workspace /workspace --env docker-local --suite upgrade-regression --label after-upgrade
```

Then compare the two generated `run.json` files:

```bash
docker compose run --rm autotest-tool compare --baseline /workspace/reports/<baseline>/run.json --candidate /workspace/reports/<candidate>/run.json --out /workspace/reports/upgrade-comparison
```

## AI-assisted scenario authoring

The workspace includes `AGENTS.md` and the repository-scoped `autotest-author` skill. Launch Codex from this directory so it discovers both:

```bash
codex
```

Example request:

```text
Use $autotest-author to create a regression scenario. I have a cURL request and response example. Interview me for the remaining HTTP, database, message, and suite information, then generate the files and validate them through Docker Compose.
```

The authoring workflow asks for missing contract details instead of inventing executable values, keeps secrets out of artifacts, and validates with the containerized tool.
