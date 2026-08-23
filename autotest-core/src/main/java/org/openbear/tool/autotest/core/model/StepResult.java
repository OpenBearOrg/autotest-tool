package org.openbear.tool.autotest.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StepResult {
  private String id;
  private String name;
  private String description;
  private String type;
  private ResultStatus status;
  private Instant startedAt;
  private Instant endedAt;
  private long durationMs;
  private String error;
  private Map<String, Object> evidence = new LinkedHashMap<>();
  private List<PollObservation> observations = new ArrayList<>();

  public void setEvidence(Map<String, Object> evidence) {
    this.evidence = evidence == null ? new LinkedHashMap<>() : evidence;
  }

  public void setObservations(List<PollObservation> observations) {
    this.observations = observations == null ? new ArrayList<>() : observations;
  }
}
