package com.goggles.monitoringservice.agent.infrastructure;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "prompt")
public class PromptProperties {
  private final String systemPrompt;
  private final String content;
  private final String modelName;
  private final Double temperature;
  private final Integer maxTokens;
  private final String version;

  public PromptProperties(
      String systemPrompt,
      String content,
      String modelName,
      Double temperature,
      Integer maxTokens,
      String version) {
    this.systemPrompt = systemPrompt;
    this.content = content;
    this.modelName = modelName;
    this.temperature = temperature;
    this.maxTokens = maxTokens;
    this.version = version;
  }
}
