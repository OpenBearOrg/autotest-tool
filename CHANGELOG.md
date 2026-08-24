# Changelog

## Unreleased

- Fixed public-SPI assertion mismatches escaping as `AssertionError`; they now produce ordinary
  test `FAIL` results with complete step, scenario and run lifecycle events.
- Emit terminal `RunFinished(ERROR)` events before rethrowing unexpected runtime or serious JVM
  execution failures.
- Pinned GitHub Actions to immutable commit revisions and documented the supported direct Maven
  range. The Maven Wrapper remains pinned to Maven 3.9.9.

## 1.0.0 - 2026-08-19

- Initial stable DSL 1.0.
- HTTP, Oracle and ActiveMQ adapters.
- Async SQL/message polling with transition evidence.
- External JSON payloads plus JSON Patch.
- Suites, tags, setup/cleanup and controlled parallelism.
- Validation, doctor, reports, checksums and baseline comparison.
- Executable JAR, Docker runtime, and example workspace.
