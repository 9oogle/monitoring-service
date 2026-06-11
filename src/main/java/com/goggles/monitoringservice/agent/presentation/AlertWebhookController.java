package com.goggles.monitoringservice.agent.presentation;

import com.goggles.monitoringservice.agent.application.AiAgent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertWebhookController {

  private final AiAgent aiAgent;

  @PostMapping
  public ResponseEntity<String> handleWebhook(@RequestBody AlertManagerPayload payload) {
    log.info("Alertmanager Webhook 수신: 상태={}", payload.status());

    if (!"firing".equalsIgnoreCase(payload.status())) {
      return ResponseEntity.ok("Resolved는 분석하지 않음");
    }
    log.info("checkpoint1: {}", payload);

    try {
      for (AlertManagerPayload.AlertDetail alert : payload.alerts()) {
        Map<String, Object> inputs = createAgentInputs(alert);
        String appName = (String) inputs.get("appName");

        log.info("에이전트 분석 시작: [Service: {}] - {}", appName, inputs.get("alertName"));

        aiAgent.assignTask(inputs);
      }
      return ResponseEntity.accepted().body("Analysis task submitted.");

    } catch (Exception e) {
      log.error("Webhook 처리 중 오류: {}", e.getMessage());
      return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
    }
  }

  private Map<String, Object> createAgentInputs(AlertManagerPayload.AlertDetail alert) {
    Map<String, Object> inputs = new HashMap<>();

    inputs.put("alertName", alert.labels().getOrDefault("alertname", "UnknownAlert"));
    inputs.put(
        "appName",
        alert
            .labels()
            .getOrDefault("service", alert.labels().getOrDefault("job", "unknown-service")));
    inputs.put(
        "app",
        alert
            .labels()
            .getOrDefault("service", alert.labels().getOrDefault("job", "unknown-service")));
    inputs.put("severity", alert.labels().getOrDefault("severity", "warning"));
    inputs.put("summary", alert.annotations().getOrDefault("summary", "N/A"));
    inputs.put("description", alert.annotations().getOrDefault("description", "N/A"));

    return inputs;
  }
}
