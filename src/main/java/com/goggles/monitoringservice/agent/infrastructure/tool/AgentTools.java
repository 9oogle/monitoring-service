package com.goggles.monitoringservice.agent.infrastructure.tool;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Slf4j
@Component
public class AgentTools {
  private final WebClient lokiWebClient;

  public AgentTools(@Value("${LOKI_HOST}") String lokiHost) {

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

    this.lokiWebClient =
        WebClient.builder()
            .baseUrl("http://" + lokiHost + ":3100")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
            .clientConnector(connector)
            .build();
  }

  @Tool(description = "Loki에서 특정 애플리케이션의 'ERROR' 레벨 로그를 추출합니다.", name = "errorLogs")
  public String fetchErrorLogs(
      @ToolParam(description = "대상 애플리케이션 명 (예: 'order-service')") String appName,
      @ToolParam(description = "로그 최대 개수 (기본 3)", required = false) Integer limit) {

    int finalLimit = (limit != null) ? limit : 3;
    String logQl = String.format("{app=\"%s\"} | level=\"ERROR\"", appName);

    log.info("Loki 에러 로그 추출 시도: appName={}, limit={}", appName, finalLimit);

    try {
      return lokiWebClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/loki/api/v1/query_range")
                      .queryParam("query", "{query}")
                      .queryParam("limit", finalLimit)
                      .build(logQl))
          .retrieve()
          .bodyToMono(String.class)
          .timeout(Duration.ofSeconds(10))
          .onErrorResume(
              e -> {
                log.error("Loki 조회 실패: {}", e.getMessage());
                return Mono.just("현재 Loki 시스템에서 로그를 가져올 수 없습니다. 사유: " + e.getMessage());
              })
          .block(); // [핵심] LLM에게 결과값을 동기적으로 전달
    } catch (Exception e) {
      log.error("Loki 호출 중 예외 발생", e);
      return "Loki 통신 장애 발생";
    }
  }
}
