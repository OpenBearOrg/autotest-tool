package org.openbear.tool.autotest.dsl.compile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openbear.tool.autotest.core.AutotestVersion;
import org.openbear.tool.autotest.core.config.DatabaseConfig;
import org.openbear.tool.autotest.core.config.EnvironmentConfig;
import org.openbear.tool.autotest.core.config.MessagingConfig;
import org.openbear.tool.autotest.core.config.PollingConfig;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.config.SecretRef;
import org.openbear.tool.autotest.core.config.ServiceConfig;
import org.openbear.tool.autotest.core.domain.CompilationMetadata;
import org.openbear.tool.autotest.core.domain.CompiledEnvironment;
import org.openbear.tool.autotest.core.domain.CompiledScenario;
import org.openbear.tool.autotest.core.domain.CompiledStep;
import org.openbear.tool.autotest.core.domain.DatabaseResource;
import org.openbear.tool.autotest.core.domain.EnvironmentId;
import org.openbear.tool.autotest.core.domain.ExecutionPlan;
import org.openbear.tool.autotest.core.domain.ExecutionSettings;
import org.openbear.tool.autotest.core.domain.MessagingResource;
import org.openbear.tool.autotest.core.domain.PluginRequirement;
import org.openbear.tool.autotest.core.domain.PollingSettings;
import org.openbear.tool.autotest.core.domain.ResourceId;
import org.openbear.tool.autotest.core.domain.ScenarioExecutionPolicy;
import org.openbear.tool.autotest.core.domain.ScenarioId;
import org.openbear.tool.autotest.core.domain.ScenarioSource;
import org.openbear.tool.autotest.core.domain.SecretReference;
import org.openbear.tool.autotest.core.domain.ServiceResource;
import org.openbear.tool.autotest.core.domain.StepId;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.model.StepDefinition;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.util.DurationParser;
import org.openbear.tool.autotest.core.util.Workspace;
import org.openbear.tool.autotest.dsl.ConfigLoader;
import org.openbear.tool.autotest.dsl.ObjectMappers;
import org.openbear.tool.autotest.dsl.ScenarioCompiler;
import org.openbear.tool.autotest.dsl.SchemaValidator;
import org.openbear.tool.autotest.dsl.SuiteResolver;
import org.openbear.tool.autotest.dsl.ValidationException;
import org.openbear.tool.autotest.spi.resource.PluginResource;
import org.openbear.tool.autotest.spi.resource.ResourceCompileContext;
import org.openbear.tool.autotest.spi.resource.ResourceTypeProvider;
import org.openbear.tool.autotest.spi.service.ResourceAccess;
import org.openbear.tool.autotest.spi.step.StepCompileContext;

public final class WorkspaceCompiler {
  private final Clock clock;
  private final GitCommitProvider gitCommitProvider;
  private final PluginRegistry plugins;
  private final ObjectMapper jsonMapper = ObjectMappers.json();

  public WorkspaceCompiler(Clock clock) {
    this(null, clock, WorkspaceCompiler::readGitCommit);
  }

  public WorkspaceCompiler(Clock clock, GitCommitProvider gitCommitProvider) {
    this(null, clock, gitCommitProvider);
  }

  /** Creates a compiler that resolves dynamic step and resource providers from the public SPI. */
  public WorkspaceCompiler(PluginRegistry plugins, Clock clock) {
    this(Objects.requireNonNull(plugins, "plugins"), clock, WorkspaceCompiler::readGitCommit);
  }

  private WorkspaceCompiler(
      PluginRegistry plugins, Clock clock, GitCommitProvider gitCommitProvider) {
    this.plugins = plugins;
    this.clock = Objects.requireNonNull(clock, "clock");
    this.gitCommitProvider = Objects.requireNonNull(gitCommitProvider, "gitCommitProvider");
  }

  public ExecutionPlan compile(CompileRequest request) {
    Objects.requireNonNull(request, "request");
    Workspace workspace = new Workspace(request.workspace());
    ConfigLoader loader = new ConfigLoader(workspace);
    ProjectConfig project = loader.loadProject();
    String environmentName = selectEnvironment(request, project);
    EnvironmentConfig environment = loader.loadEnvironment(environmentName);
    ScenarioCompiler scenarios = new ScenarioCompiler(workspace, plugins != null);
    Selection selection = selectScenarios(request, loader, scenarios);
    if (selection.plans().isEmpty())
      throw new IllegalArgumentException(
          "No scenarios selected. Use --scenario, --suite, or --tag");

    List<CompiledScenario> compiledScenarios =
        selection.plans().stream().map(this::compileScenario).toList();
    CompiledEnvironment compiledEnvironment = compileEnvironment(workspace, project, environment);
    ExecutionSettings settings =
        new ExecutionSettings(
            request.parallelismOverride() == null
                ? project.getExecution().getParallelism()
                : request.parallelismOverride(),
            request.failFastOverride() == null
                ? project.getExecution().isFailFast()
                : request.failFastOverride(),
            !request.skipDoctor());
    Set<PluginRequirement> requirements = requirements(compiledScenarios, compiledEnvironment);
    CompilationMetadata metadata =
        new CompilationMetadata(
            AutotestVersion.DSL_VERSION,
            gitCommitProvider.commit(workspace.root()),
            clock.instant());
    return new ExecutionPlan(
        project.getName(),
        compiledEnvironment,
        compiledScenarios,
        selection.commonVariables(),
        settings,
        requirements,
        metadata);
  }

  public ValidationResult validate(ValidationRequest request) {
    Objects.requireNonNull(request, "request");
    Workspace workspace = new Workspace(request.workspace());
    ConfigLoader loader = new ConfigLoader(workspace);
    ProjectConfig project = loader.loadProject();
    List<ValidationMessage> messages = new ArrayList<>();
    ScenarioCompiler scenarios = new ScenarioCompiler(workspace, plugins != null);
    List<CompiledScenario> compiledScenarios = new ArrayList<>();
    try {
      compiledScenarios.addAll(scenarios.discover().stream().map(this::compileScenario).toList());
    } catch (RuntimeException e) {
      add(messages, "scenarios", e);
    }

    int suiteCount = 0;
    Path suites = workspace.resolve("suites");
    if (Files.exists(suites)) {
      try (var stream = Files.walk(suites)) {
        List<Path> files =
            stream.filter(Files::isRegularFile).filter(this::isYaml).sorted().toList();
        for (Path file : files) {
          try {
            loader.loadSuite(workspace.root().relativize(file).toString());
            suiteCount++;
          } catch (RuntimeException e) {
            add(messages, workspace.root().relativize(file).toString(), e);
          }
        }
      } catch (IOException e) {
        add(messages, "suites", e);
      }
    }

    List<CompiledEnvironment> environments = new ArrayList<>();
    Path environmentRoot = workspace.resolve("environments");
    if (Files.exists(environmentRoot)) {
      try (var stream = Files.walk(environmentRoot)) {
        List<Path> files =
            stream.filter(Files::isRegularFile).filter(this::isYaml).sorted().toList();
        for (Path file : files) {
          String location = workspace.root().relativize(file).toString();
          try {
            environments.add(
                compileEnvironment(workspace, project, loader.loadEnvironment(location)));
          } catch (RuntimeException e) {
            add(messages, location, e);
          }
        }
      } catch (IOException e) {
        add(messages, "environments", e);
      }
    }
    return new ValidationResult(project, compiledScenarios, suiteCount, environments, messages);
  }

  private Selection selectScenarios(
      CompileRequest request, ConfigLoader loader, ScenarioCompiler compiler) {
    LinkedHashMap<String, ScenarioPlan> selected = new LinkedHashMap<>();
    LinkedHashMap<String, Object> variables = new LinkedHashMap<>();
    if (request.suite() != null && !request.suite().isBlank()) {
      var suite = loader.loadSuite(request.suite());
      variables.putAll(suite.getVariables());
      for (ScenarioPlan plan : new SuiteResolver(compiler).resolve(suite))
        selected.put(plan.scenario().getId(), plan);
    }
    for (String reference : request.scenarios()) {
      ScenarioPlan plan = compiler.find(reference);
      selected.put(plan.scenario().getId(), plan);
    }
    if (!request.includeTags().isEmpty())
      for (ScenarioPlan plan : compiler.discover())
        if (plan.scenario().getTags().stream().anyMatch(request.includeTags()::contains))
          selected.put(plan.scenario().getId(), plan);
    variables.putAll(request.runtimeVariables());
    return new Selection(List.copyOf(selected.values()), Collections.unmodifiableMap(variables));
  }

  private CompiledScenario compileScenario(ScenarioPlan plan) {
    if (plugins != null) return compileDynamicScenario(plan);
    var scenario = plan.scenario();
    return new CompiledScenario(
        new ScenarioId(scenario.getId()),
        scenario.getName(),
        new LinkedHashSet<>(scenario.getTags()),
        new LinkedHashSet<>(scenario.getRequiredVariables()),
        scenario.getVariables(),
        compileSteps(scenario.getSetup()),
        compileSteps(scenario.getSteps()),
        compileSteps(scenario.getCleanup()),
        ScenarioExecutionPolicy.fromDsl(scenario.getExecution().getIsolation()),
        new ScenarioSource(plan.sourcePath(), plan.checksum(), plan.resourceChecksums()));
  }

  private CompiledScenario compileDynamicScenario(ScenarioPlan plan) {
    try {
      JsonNode root = ObjectMappers.yaml().readTree(Files.readString(plan.sourcePath()));
      String scenarioId = root.path("id").asText();
      ResourceAccess resources = new WorkspaceResourceAccess(workspaceFor(plan.sourcePath()));
      DynamicStepCompiler compiler = new DynamicStepCompiler(plugins.stepTypes());
      List<CompiledStep> setup =
          dynamicSteps(root.path("setup"), scenarioId, plan, compiler, resources);
      List<CompiledStep> steps =
          dynamicSteps(root.path("steps"), scenarioId, plan, compiler, resources);
      List<CompiledStep> cleanup =
          dynamicSteps(root.path("cleanup"), scenarioId, plan, compiler, resources);
      validateDynamicScenario(scenarioId, setup, steps, cleanup);
      return new CompiledScenario(
          new ScenarioId(scenarioId),
          root.path("name").asText(),
          jsonMapper.convertValue(root.path("tags"), new TypeReference<LinkedHashSet<String>>() {}),
          jsonMapper.convertValue(
              root.path("requiredVariables"), new TypeReference<LinkedHashSet<String>>() {}),
          jsonMapper.convertValue(
              root.path("variables"), new TypeReference<Map<String, Object>>() {}),
          setup,
          steps,
          cleanup,
          ScenarioExecutionPolicy.fromDsl(
              root.path("execution").path("isolation").asText("sequential")),
          new ScenarioSource(plan.sourcePath(), plan.checksum(), plan.resourceChecksums()));
    } catch (IOException e) {
      throw new ValidationException(
          "Unable to compile scenario " + plan.sourcePath(), List.of(e.getMessage()));
    }
  }

  private Workspace workspaceFor(Path scenarioPath) {
    Path current = scenarioPath;
    while (current != null && !"scenarios".equals(current.getFileName().toString()))
      current = current.getParent();
    if (current == null)
      throw new IllegalArgumentException("Scenario is outside a workspace: " + scenarioPath);
    return new Workspace(current.getParent());
  }

  private List<CompiledStep> dynamicSteps(
      JsonNode steps,
      String scenarioId,
      ScenarioPlan plan,
      DynamicStepCompiler compiler,
      ResourceAccess resources) {
    if (steps.isMissingNode() || steps.isNull()) return List.of();
    List<CompiledStep> compiled = new ArrayList<>();
    for (JsonNode step : steps)
      compiled.add(
          compiler.compile(
              step,
              new StepCompileContext(
                  scenarioId,
                  workspaceFor(plan.sourcePath()).root().relativize(plan.sourcePath()).toString(),
                  resources)));
    return List.copyOf(compiled);
  }

  private static void validateDynamicScenario(
      String scenarioId,
      List<CompiledStep> setup,
      List<CompiledStep> steps,
      List<CompiledStep> cleanup) {
    if (steps.isEmpty())
      throw new ValidationException("Scenario is invalid", List.of("steps are required"));
    Set<String> ids = new LinkedHashSet<>();
    for (CompiledStep step : concat(setup, steps, cleanup))
      if (!ids.add(step.id().value()))
        throw new ValidationException(
            "Scenario " + scenarioId + " is invalid",
            List.of("duplicate step id " + step.id().value()));
  }

  private static List<CompiledStep> concat(
      List<CompiledStep> setup, List<CompiledStep> steps, List<CompiledStep> cleanup) {
    List<CompiledStep> all = new ArrayList<>(setup);
    all.addAll(steps);
    all.addAll(cleanup);
    return all;
  }

  private List<CompiledStep> compileSteps(List<StepDefinition> steps) {
    List<CompiledStep> compiled = new ArrayList<>();
    for (StepDefinition step : steps) {
      JsonNode node = jsonMapper.valueToTree(step);
      JsonNode body = unwrap(node);
      Map<String, Object> configuration =
          jsonMapper.convertValue(body, new TypeReference<Map<String, Object>>() {});
      compiled.add(
          new CompiledStep(
              new StepId(step.getId()),
              step.getName(),
              step.getDescription(),
              step.isContinueOnFailure(),
              step.type(),
              configuration));
    }
    return List.copyOf(compiled);
  }

  private CompiledEnvironment compileEnvironment(
      Workspace workspace, ProjectConfig project, EnvironmentConfig environment) {
    Map<String, ServiceResource> services = new LinkedHashMap<>();
    for (var entry : environment.getServices().entrySet()) {
      ServiceConfig config = entry.getValue();
      services.put(
          entry.getKey(),
          new ServiceResource(
              new ResourceId(entry.getKey()),
              URI.create(config.getBaseUrl()),
              config.getDefaultHeaders(),
              duration(config.getConnectTimeout()),
              duration(config.getRequestTimeout()),
              config.getHealthPath(),
              config.getSafeRetryAttempts()));
    }
    Map<String, DatabaseResource> databases = new LinkedHashMap<>();
    boolean allowWrites = environment.getDatabasePolicy().isAllowWrites();
    for (var entry : environment.getDatabases().entrySet()) {
      DatabaseConfig config = entry.getValue();
      databases.put(
          entry.getKey(),
          new DatabaseResource(
              new ResourceId(entry.getKey()),
              config.getDriver(),
              config.getJdbcUrl(),
              secret(config.getUsername()),
              secret(config.getPassword()),
              config.getMaximumPoolSize(),
              duration(config.getConnectionTimeout()),
              duration(config.getValidationTimeout()),
              allowWrites,
              Map.of()));
    }
    Map<String, MessagingResource> messaging = new LinkedHashMap<>();
    for (var entry : environment.getMessaging().entrySet()) {
      MessagingConfig config = entry.getValue();
      messaging.put(
          entry.getKey(),
          new MessagingResource(
              new ResourceId(entry.getKey()),
              config.getType(),
              URI.create(config.getBrokerUrl()),
              secret(config.getUsername()),
              secret(config.getPassword()),
              duration(config.getConnectTimeout()),
              Map.of()));
    }
    return new CompiledEnvironment(
        new EnvironmentId(environment.getName()),
        services,
        databases,
        messaging,
        allowWrites,
        polling(project, environment),
        compilePluginResources(workspace, environment));
  }

  private Map<String, List<PluginResource>> compilePluginResources(
      Workspace workspace, EnvironmentConfig environment) {
    if (environment.getResources().isEmpty()) return Map.of();
    if (plugins == null)
      throw new ValidationException(
          "Plugin resource compilation requires a plugin registry", List.of("resources"));
    Map<String, List<PluginResource>> compiled = new LinkedHashMap<>();
    ResourceAccess resources = new WorkspaceResourceAccess(workspace);
    SchemaValidator schemas = new SchemaValidator(ObjectMappers.yaml());
    for (var typeEntry : environment.getResources().entrySet()) {
      ResourceTypeProvider<?, ?> provider = plugins.resourceTypes().require(typeEntry.getKey());
      List<PluginResource> values = new ArrayList<>();
      for (var resourceEntry : typeEntry.getValue().entrySet()) {
        JsonNode body = jsonMapper.valueToTree(resourceEntry.getValue());
        if (provider.schemaResource() != null && !provider.schemaResource().isBlank())
          schemas.validate(body, provider.schemaResource());
        values.add(
            compileResource(
                provider,
                resourceEntry.getKey(),
                body,
                new ResourceCompileContext(environment.getName(), resources)));
      }
      compiled.put(typeEntry.getKey(), List.copyOf(values));
    }
    return Map.copyOf(compiled);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private PluginResource compileResource(
      ResourceTypeProvider provider, String name, JsonNode body, ResourceCompileContext context) {
    Object configuration = ObjectMappers.json().convertValue(body, provider.configurationType());
    return (PluginResource) provider.compile(name, configuration, context);
  }

  private PollingSettings polling(ProjectConfig project, EnvironmentConfig environment) {
    PollingConfig projectPolling =
        project.getDefaults() == null ? null : project.getDefaults().getPolling();
    PollingConfig environmentPolling =
        environment.getDefaults() == null ? null : environment.getDefaults().getPolling();
    String timeout =
        environmentPolling != null && environmentPolling.getTimeout() != null
            ? environmentPolling.getTimeout()
            : projectPolling == null ? null : projectPolling.getTimeout();
    String interval =
        environmentPolling != null && environmentPolling.getInterval() != null
            ? environmentPolling.getInterval()
            : projectPolling == null ? null : projectPolling.getInterval();
    return new PollingSettings(duration(timeout), duration(interval));
  }

  private Set<PluginRequirement> requirements(
      List<CompiledScenario> scenarios, CompiledEnvironment environment) {
    LinkedHashSet<PluginRequirement> requirements = new LinkedHashSet<>();
    for (CompiledScenario scenario : scenarios)
      for (CompiledStep step : allSteps(scenario)) {
        switch (step.type()) {
          case "http" -> requirements.add(new PluginRequirement("http"));
          case "sql", "awaitSql" -> requirements.add(new PluginRequirement("database"));
          case "awaitMessage" -> requirements.add(new PluginRequirement("messaging"));
          default -> {}
        }
      }
    if (!environment.services().isEmpty()) requirements.add(new PluginRequirement("http"));
    if (!environment.databases().isEmpty()) requirements.add(new PluginRequirement("database"));
    if (!environment.messaging().isEmpty()) requirements.add(new PluginRequirement("messaging"));
    return requirements;
  }

  private List<CompiledStep> allSteps(CompiledScenario scenario) {
    List<CompiledStep> steps = new ArrayList<>();
    steps.addAll(scenario.setup());
    steps.addAll(scenario.steps());
    steps.addAll(scenario.cleanup());
    return steps;
  }

  private static String selectEnvironment(CompileRequest request, ProjectConfig project) {
    String selected =
        request.environment() != null && !request.environment().isBlank()
            ? request.environment()
            : project.getDefaults().getEnvironment();
    if (selected == null || selected.isBlank())
      throw new IllegalArgumentException(
          "Environment is required. Use --env or set defaults.environment in autotest-tool.yaml");
    return selected;
  }

  private static JsonNode unwrap(JsonNode node) {
    if (node != null && node.isObject() && node.size() == 1) return node.elements().next();
    return node;
  }

  private static SecretReference secret(SecretRef value) {
    return value == null || value.getSecret() == null
        ? null
        : new SecretReference(value.getSecret());
  }

  private static Duration duration(String value) {
    return DurationParser.parse(value);
  }

  private boolean isYaml(Path path) {
    String name = path.toString();
    return name.endsWith(".yaml") || name.endsWith(".yml");
  }

  private static void add(List<ValidationMessage> messages, String location, Throwable failure) {
    if (failure instanceof ValidationException validation && !validation.errors().isEmpty())
      for (String error : validation.errors())
        messages.add(new ValidationMessage(ValidationSeverity.ERROR, location, error));
    else
      messages.add(
          new ValidationMessage(
              ValidationSeverity.ERROR, location, String.valueOf(failure.getMessage())));
  }

  private static String readGitCommit(Path workspace) {
    try {
      Process process =
          new ProcessBuilder("git", "-C", workspace.toString(), "rev-parse", "HEAD")
              .redirectErrorStream(true)
              .start();
      String value = new String(process.getInputStream().readAllBytes()).trim();
      return process.waitFor() == 0 && !value.isBlank() ? value : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  private record Selection(List<ScenarioPlan> plans, Map<String, Object> commonVariables) {}

  private record WorkspaceResourceAccess(Workspace workspace) implements ResourceAccess {
    @Override
    public String readText(String workspaceRelativePath) {
      try {
        return workspace.readText(workspaceRelativePath);
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to read resource: " + workspaceRelativePath, e);
      }
    }

    @Override
    public byte[] readBytes(String workspaceRelativePath) {
      try {
        return Files.readAllBytes(workspace.resolve(workspaceRelativePath));
      } catch (IOException e) {
        throw new IllegalArgumentException("Unable to read resource: " + workspaceRelativePath, e);
      }
    }
  }
}
