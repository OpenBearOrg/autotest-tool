# External JDBC drivers

Place trusted, user-supplied JDBC driver JARs in this directory. Autotest does not distribute
Oracle JDBC drivers.

For Oracle, use the standard location:

```text
lib/jdbc/ojdbc.jar
```

Start the tool through `bin/autotest-tool`, which adds every JAR under `lib/jdbc/` to the JVM
application classpath before Autotest starts. Docker Compose mounts this directory read-only into
the container at runtime.

Only place drivers you trust here: every JAR in this directory executes with the same permissions
as Autotest. Do not commit driver binaries to this repository.
