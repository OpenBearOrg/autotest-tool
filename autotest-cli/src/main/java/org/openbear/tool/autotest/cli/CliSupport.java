package org.openbear.tool.autotest.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openbear.tool.autotest.core.config.ProjectConfig;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.util.Workspace;

public final class CliSupport {
  private CliSupport() {}

  public static Workspace workspace(Path p) {
    return new Workspace(p);
  }

  public static String environmentName(String cli, ProjectConfig project) {
    String v = cli != null ? cli : project.getDefaults().getEnvironment();
    if (v == null || v.isBlank())
      throw new IllegalArgumentException(
          "Environment is required. Use --env or set defaults.environment in autotest-tool.yaml");
    return v;
  }

  public static Map<String, Object> parseVars(List<String> values) {
    Map<String, Object> out = new LinkedHashMap<>();
    if (values == null) return out;
    ObjectMapper json = new ObjectMapper();
    for (String item : values) {
      int eq = item.indexOf('=');
      if (eq <= 0) throw new IllegalArgumentException("--var must use name=value: " + item);
      String k = item.substring(0, eq), raw = item.substring(eq + 1);
      out.put(k, scalar(json, raw));
    }
    return out;
  }

  private static Object scalar(ObjectMapper json, String raw) {
    String t = raw.trim();
    if (t.equals("true")
        || t.equals("false")
        || t.equals("null")
        || t.matches("-?[0-9]+(\\.[0-9]+)?")
        || t.startsWith("{")
        || t.startsWith("[")) {
      try {
        return json.readValue(t, Object.class);
      } catch (Exception ignored) {
      }
    }
    return raw;
  }

  public static List<org.openbear.tool.autotest.spi.doctor.DoctorCheckResult> doctor(
      org.openbear.tool.autotest.core.plugin.PluginRegistry plugins) {
    return plugins.doctorChecks().stream()
        .map(org.openbear.tool.autotest.spi.doctor.DoctorCheck::run)
        .toList();
  }

  public static boolean printPublicDoctor(
      List<org.openbear.tool.autotest.spi.doctor.DoctorCheckResult> checks) {
    boolean ok = true;
    for (var result : checks) {
      boolean pass = result.status() == org.openbear.tool.autotest.spi.doctor.DoctorStatus.PASS;
      System.out.printf(
          "%-10s %-28s %-5s  %s%n", "-", result.id(), pass ? "PASS" : "FAIL", result.message());
      ok &= pass;
    }
    return ok;
  }

  public static String gitCommit(Path root) {
    try {
      Process p =
          new ProcessBuilder("git", "-C", root.toString(), "rev-parse", "HEAD")
              .redirectErrorStream(true)
              .start();
      String s = new String(p.getInputStream().readAllBytes()).trim();
      return p.waitFor() == 0 ? s : null;
    } catch (Exception e) {
      return null;
    }
  }

  public static Path reportDir(
      Workspace w, ProjectConfig p, String explicit, String label, String runId) {
    String root = explicit != null ? explicit : p.getReporting().getDirectory();
    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String suffix = label == null || label.isBlank() ? runId : label;
    return w.resolve(root).resolve(stamp + "-" + suffix.replaceAll("[^A-Za-z0-9._-]", "_"));
  }

  public static List<ScenarioPlan> dedupe(List<ScenarioPlan> plans) {
    LinkedHashMap<String, ScenarioPlan> m = new LinkedHashMap<>();
    for (ScenarioPlan p : plans) m.put(p.scenario().getId(), p);
    return new ArrayList<>(m.values());
  }
}
