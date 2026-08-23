package org.openbear.tool.autotest.jdbc.plugin;

import java.util.Collection;
import java.util.List;
import org.openbear.tool.autotest.jdbc.runtime.JdbcRuntime;
import org.openbear.tool.autotest.jdbc.spi.JdbcDriverProvider;
import org.openbear.tool.autotest.jdbc.step.AwaitSqlStepHandler;
import org.openbear.tool.autotest.jdbc.step.AwaitSqlStepTypeProvider;
import org.openbear.tool.autotest.jdbc.step.SqlStepHandler;
import org.openbear.tool.autotest.jdbc.step.SqlStepTypeProvider;
import org.openbear.tool.autotest.spi.SpiVersion;
import org.openbear.tool.autotest.spi.doctor.DoctorCheck;
import org.openbear.tool.autotest.spi.doctor.DoctorCheckResult;
import org.openbear.tool.autotest.spi.doctor.DoctorStatus;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;
import org.openbear.tool.autotest.spi.plugin.PluginDescriptor;
import org.openbear.tool.autotest.spi.plugin.PluginRuntime;
import org.openbear.tool.autotest.spi.plugin.PluginRuntimeContext;
import org.openbear.tool.autotest.spi.resource.DatabaseResource;
import org.openbear.tool.autotest.spi.step.StepHandler;
import org.openbear.tool.autotest.spi.step.StepTypeProvider;

public final class JdbcPlugin implements AutotestPlugin {
  @Override
  public PluginDescriptor descriptor() {
    return new PluginDescriptor("jdbc", "JDBC Foundation", "1.0.0", SpiVersion.CURRENT);
  }

  @Override
  public Collection<? extends StepTypeProvider<?, ?>> stepTypes() {
    return List.of(new SqlStepTypeProvider(), new AwaitSqlStepTypeProvider());
  }

  @Override
  public PluginRuntime open(PluginRuntimeContext context) {
    List<JdbcDriverProvider> drivers = context.capabilities().all(JdbcDriverProvider.class);
    JdbcRuntime runtime = new JdbcRuntime(context.environment(), context.secrets(), drivers);
    return new PluginRuntime() {
      @Override
      public Collection<? extends StepHandler<?>> stepHandlers() {
        return List.of(new SqlStepHandler(runtime), new AwaitSqlStepHandler(runtime));
      }

      @Override
      public Collection<? extends DoctorCheck> doctorChecks() {
        return context.environment().databases().stream()
            .map(resource -> new JdbcDoctorCheck(runtime, resource))
            .toList();
      }

      @Override
      public void close() {
        runtime.close();
      }
    };
  }

  private record JdbcDoctorCheck(JdbcRuntime runtime, DatabaseResource resource)
      implements DoctorCheck {
    @Override
    public String id() {
      return "jdbc:" + resource.name();
    }

    @Override
    public DoctorCheckResult run() {
      try {
        runtime.ping(resource.name());
        return new DoctorCheckResult(
            id(), DoctorStatus.PASS, "Connected to " + resource.driver(), null);
      } catch (RuntimeException failure) {
        return new DoctorCheckResult(id(), DoctorStatus.FAIL, failure.getMessage(), null);
      }
    }
  }
}
