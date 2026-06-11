package com.goggles.monitoringservice.agent.application;

import java.time.LocalDateTime;

public record AgentResponse(
    String content,
    String model,
    String version,
    Integer usageTokens,
    long durationMs,
    LocalDateTime executedAt) {}
