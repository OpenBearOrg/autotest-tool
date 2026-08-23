package org.openbear.tool.autotest.dsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.openbear.tool.autotest.core.model.ScenarioPlan;
import org.openbear.tool.autotest.core.model.SuiteDefinition;

public class SuiteResolver {
  private final ScenarioCompiler compiler;

  public SuiteResolver(ScenarioCompiler compiler) {
    this.compiler = compiler;
  }

  public List<ScenarioPlan> resolve(SuiteDefinition suite) {
    LinkedHashMap<String, ScenarioPlan> selected = new LinkedHashMap<>();
    for (String ref : suite.getScenarios()) {
      ScenarioPlan p = compiler.find(ref);
      selected.put(p.scenario().getId(), p);
    }
    if (!suite.getIncludeTags().isEmpty())
      for (ScenarioPlan p : compiler.discover())
        if (p.scenario().getTags().stream().anyMatch(suite.getIncludeTags()::contains))
          selected.putIfAbsent(p.scenario().getId(), p);
    if (!suite.getExcludeTags().isEmpty())
      selected
          .values()
          .removeIf(
              p -> p.scenario().getTags().stream().anyMatch(suite.getExcludeTags()::contains));
    return new ArrayList<>(selected.values());
  }
}
