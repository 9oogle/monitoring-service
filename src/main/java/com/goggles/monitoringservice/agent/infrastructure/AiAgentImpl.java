package com.goggles.monitoringservice.agent.infrastructure;

import com.goggles.monitoringservice.agent.application.AgentResponse;
import com.goggles.monitoringservice.agent.application.AiAgent;
import com.goggles.monitoringservice.agent.infrastructure.slack.SlackClient;
import com.goggles.monitoringservice.agent.infrastructure.tool.AgentTools;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiAgentImpl implements AiAgent {

  private final ChatClient chatClient;
  private final PromptProperties promptProperties;
  private final SlackClient slackClient;

  @Value("classpath:prompts/system-prompt.st")
  private Resource systemPromptResource;

  @Value("classpath:prompts/content.st")
  private Resource contentResource;

  public AiAgentImpl(
      ChatClient.Builder chatClientBuilder,
      PromptProperties promptProperties,
      AgentTools agentTools,
      SlackClient slackClient) {
    this.promptProperties = promptProperties;
    this.slackClient = slackClient;
    this.chatClient =
        chatClientBuilder
            .defaultTools(agentTools)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
  }

  @Override
  public AgentResponse assignTask(Map<String, Object> inputs) {
    String systemPrompt = new PromptTemplate(systemPromptResource).render();
    String content = new PromptTemplate(contentResource).render(inputs);

    if (content.length() > 3000) {
      content = content.substring(0, 3000) + "...(이하 생략)";
    }

    log.info("렌더링된 content: {}", content);

    return executeTask(systemPrompt, content, inputs);
  }

  private AgentResponse executeTask(
      String systemPrompt, String content, Map<String, Object> inputs) {
    long startTime = System.currentTimeMillis();

    log.info(
        "에이전트 가동: 모델={}, 버전={}", promptProperties.getModelName(), promptProperties.getVersion());

    ChatResponse response =
        chatClient
            .prompt()
            .system(systemPrompt)
            .user(content)
            .options(
                ChatOptions.builder()
                    .model(promptProperties.getModelName())
                    .temperature(promptProperties.getTemperature())
                    .build())
            .call()
            .chatResponse();

    int totalTokens = 0;
    if (response.getMetadata().getUsage() != null) {
      totalTokens = response.getMetadata().getUsage().getTotalTokens();
      log.info("토큰 사용량: {}", totalTokens);
    }

    String result =
        (response.getResult() != null) ? response.getResult().getOutput().getText() : "";

    long elapsed = System.currentTimeMillis() - startTime;
    log.info("분석 완료: 소요시간={}ms", elapsed);
    log.info("Gemini 응답 결과: {}", result);

    slackClient.sendDetailedSlackReport(
        (String) inputs.get("severity"),
        (String) inputs.get("appName"),
        result,
        "장애 대응 매뉴얼을 확인하고 상단 분석 결과에 따라 조치해 주세요.");

    return new AgentResponse(
        result,
        promptProperties.getModelName(),
        promptProperties.getVersion(),
        totalTokens,
        elapsed,
        LocalDateTime.now());
  }
}
