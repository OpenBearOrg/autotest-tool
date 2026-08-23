package org.openbear.tool.autotest.jdbc.runtime;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.resource.EnvironmentView;
import org.openbear.tool.autotest.spi.service.Secrets;

public final class JdbcPoolManager implements AutoCloseable {
  private final EnvironmentView environment;
  private final Secrets secrets;
  private final List<JdbcDriverProvider> drivers;
  private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
  private final AtomicBoolean closed = new AtomicBoolean();

  public JdbcPoolManager(
      EnvironmentView environment, Secrets secrets, List<? extends JdbcDriverProvider> drivers) {
    this.environment = Objects.requireNonNull(environment, "environment");
    this.secrets = Objects.requireNonNull(secrets, "secrets");
    this.drivers = List.copyOf(drivers);
  }

  public HikariDataSource pool(String connectionName) {
    if (closed.get()) throw new IllegalStateException("JDBC runtime is closed");
    return pools.computeIfAbsent(connectionName, this::createPool);
  }

  public JdbcDriverProvider driver(String connectionName) {
    DatabaseResource resource = resource(connectionName);
    return drivers.stream()
        .filter(provider -> provider.supports(resource))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No JDBC driver provider for: " + resource.driver()));
  }

  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    pools.values().forEach(HikariDataSource::close);
    pools.clear();
  }

  private HikariDataSource createPool(String name) {
    DatabaseResource resource = resource(name);
    JdbcDriverProvider driver = driver(name);
    HikariConfig config = new HikariConfig();
    config.setPoolName("autotest-" + name);
    config.setMaximumPoolSize(resource.maximumPoolSize());
    config.setConnectionTimeout(resource.connectionTimeout().toMillis());
    config.setValidationTimeout(resource.validationTimeout().toMillis());
    config.setAutoCommit(true);
    driver.configure(config, resource, secrets);
    return new HikariDataSource(config);
  }

  private DatabaseResource resource(String name) {
    return environment
        .database(name)
        .orElseThrow(
            () -> new IllegalArgumentException("Database connection is not configured: " + name));
  }
}
