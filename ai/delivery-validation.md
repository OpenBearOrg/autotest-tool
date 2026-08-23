# Delivery validation

Validation performed for the 1.0.0 source delivery:

- All Maven POM files parse as XML.
- All embedded JSON Schema files parse as JSON.
- The example project, environment, suite and scenario YAML files validate against the embedded Draft 2020-12 schemas.
- The example JSON payload parses successfully and all referenced payload/SQL resources exist.
- All main Java 21 sources compile together against API-compatible dependency stubs, catching cross-module type and signature errors in the project code.
- All included unit-test sources compile against dependency and JUnit API stubs.
- Dependency-free behavioral smoke tests pass for duration/size parsing, workspace path isolation, Oracle named parameters, SQL read-only policy, and scenario failure-flow semantics.
- Source scan contains no TODO/FIXME/placeholder implementation markers.

## Environment limitation of this delivery sandbox

The sandbox used to generate this delivery provides Java 21 but does not provide Maven and cannot resolve Maven Central from the container. Therefore `mvn clean verify` and creation of the shaded executable JAR could not be performed here with the real third-party artifacts.

Run this in a normal build environment before promotion:

```bash
mvn clean verify
docker compose build
docker compose run --rm autotest-tool version
docker compose run --rm autotest-tool validate --workspace /workspace
```

The Dockerfile expects the shaded JAR to have been built first.
