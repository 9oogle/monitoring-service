package com.goggles.monitoringservice.agent.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertManagerPayload(
    String status,
    Map<String, String> commonLabels,
    Map<String, String> commonAnnotations,
    List<AlertDetail> alerts) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AlertDetail(
      String status, Map<String, String> labels, Map<String, String> annotations) {}
}
