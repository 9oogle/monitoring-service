package com.goggles.monitoringservice.agent.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/operation")
public class OperationController {
  private final ObjectMapper objectMapper;
  @Value("${LOKI_HOST}") String lokiHost;
  @Value("${GRAFANA_HOST}") String grafanaHost;

  @PostMapping(value = "/interactive", consumes = "application/x-www-form-urlencoded")
  public Mono<Void> handleInteractive(@RequestParam("payload") String payloadString) {

    return Mono.fromCallable(() -> objectMapper.readTree(payloadString))
        .flatMap(
            payload -> {
              String actionId = payload.at("/actions/0/action_id").asText();
              String serviceName = payload.at("/actions/0/value").asText();
              String query = "{app=\"" + serviceName + "\"} | level=\"ERROR\"";
              String responseUrl = payload.path("response_url").asText();

              log.info("슬랙 액션 수신: actionId={}, service={}", actionId, serviceName);

              if ("view_logs".equals(actionId)) {
                WebClient.create()
                    .get()
                    .uri(
                        "http://" + lokiHost + "/loki/api/v1/query_range"
                            + "?query="
                            + URLEncoder.encode(query, StandardCharsets.UTF_8)
                            + "&limit=20")
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(
                        lokiResponse -> {
                          return WebClient.create()
                              .post()
                              .uri(responseUrl)
                              .bodyValue(
                                  Map.of(
                                      "text",
                                      "📋 최근 에러 로그:\n```" + lokiResponse + "```",
                                      "replace_original",
                                      true))
                              .retrieve()
                              .toBodilessEntity();
                        })
                    .subscribe();

              } else if ("grafana".equals(actionId)) {
                WebClient.create()
                    .post()
                    .uri(responseUrl)
                    .bodyValue(
                        Map.of(
                            "text",
                            "📋 로그 확인: http://" + grafanaHost + ":3000/explore",
                            "replace_original",
                            true))
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe();
              }

              return Mono.empty();
            });
  }
}
