package com.goggles.monitoringservice.agent.infrastructure.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Component
public class SlackClient {
  private final WebClient slackWebClient;
  private final ObjectMapper objectMapper;

  public SlackClient(@Value("${monitoring.slack.url}") String slackUrl, ObjectMapper objectMapper) {

    ConnectionProvider provider =
        ConnectionProvider.builder("monitoring-pool")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();

    HttpClient httpClient =
        HttpClient.create(provider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .doOnConnected(
                conn ->
                    conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)))
            .keepAlive(true);

    ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

    this.slackWebClient = WebClient.builder().baseUrl(slackUrl).clientConnector(connector).build();

    this.objectMapper = objectMapper;
  }

  public String sendDetailedSlackReport(
      @ToolParam(description = "위험도 (CRITICAL, WARNING)") String severity,
      @ToolParam(description = "장애 서비스 명") String service,
      @ToolParam(description = "분석 내용 (마크다운)") String analysis,
      @ToolParam(description = "추천 조치 사항") String recommendedAction) {

    log.info("슬랙 리포트 발송 프로세스 시작: service={}", service);

    try {
      List<Object> blocks = new ArrayList<>();
      String emoji = "critical".equalsIgnoreCase(severity) ? "🚨" : "⚠️";

      blocks.add(
          Map.of(
              "type",
              "header",
              "text",
              Map.of(
                  "type",
                  "plain_text",
                  "text",
                  String.format("%s [%s] %s 서비스 이상 감지", emoji, severity.toUpperCase(), service),
                  "emoji",
                  true)));
      blocks.add(
          Map.of(
              "type",
              "section",
              "text",
              Map.of("type", "mrkdwn", "text", String.format("*분석 결과:*\n%s", analysis))));
      blocks.add(
          Map.of(
              "type",
              "section",
              "text",
              Map.of("type", "mrkdwn", "text", String.format("*추천 조치:*\n%s", recommendedAction))));
      blocks.add(Map.of("type", "divider"));
      blocks.add(createActionButtons(service));

      String payload = objectMapper.writeValueAsString(Map.of("blocks", blocks));
      log.info("슬랙 전달 payload: {}", payload);

      String result =
          slackWebClient
              .post()
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(payload)
              .retrieve()
              .bodyToMono(String.class)
              .timeout(Duration.ofSeconds(5))
              .block();

      log.info("슬랙 전송 성공 응답: {}", result);
      return "슬랙 보고 완료: " + result;

    } catch (Exception e) {
      log.error("슬랙 메시지 발송 실패", e);
      return "슬랙 메시지 전송 중 오류 발생: " + e.getMessage();
    }
  }

  private Map<String, Object> createActionButtons(String service) {
    return Map.of(
        "type",
        "actions",
        "elements",
        List.of(
            Map.of(
                "type",
                "button",
                "text",
                Map.of("type", "plain_text", "text", "로그 상세 보기"),
                "style",
                "primary",
                "action_id",
                "view_logs",
                "value",
                service),
            Map.of(
                "type",
                "button",
                "text",
                Map.of("type", "plain_text", "text", "그라파나 확인하기"),
                "style",
                "danger",
                "action_id",
                "grafana",
                "value",
                service)));
  }
}
