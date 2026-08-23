# SPI compatibility policy

The public plugin API is the package tree under `org.openbear.tool.autotest.spi`.
Core, DSL, CLI, and implementation packages are not binary compatibility promises.

For `autotest-spi`:

- Patch releases contain documentation and implementation fixes only.
- Minor releases may add types, default methods, or new optional contributions.
- Major releases may remove or change incompatible public contracts.
- Existing plugin interfaces must not gain new abstract methods in a minor release.
- `PluginDescriptor.spiVersion` is a runtime loading gate and changes only when loading compatibility changes.

The public-SPI baseline is `org.openbear.tool.autotest:autotest-spi:1.0.0`. This is the first
release containing the redesigned public SPI; it is intentionally not compared against the
pre-SPI compatibility bridge.

The `api-compat` Maven profile runs JApiCmp against an explicitly supplied released baseline:

```bash
SPI_BASELINE_VERSION="1.0.0"
./mvnw -Papi-compat -Dspi.baseline.version="$SPI_BASELINE_VERSION" verify
```

Release CI must publish `autotest-spi:1.0.0` before enabling that comparison for a later version.
Normal `./mvnw verify` does not require a previously published artifact.
