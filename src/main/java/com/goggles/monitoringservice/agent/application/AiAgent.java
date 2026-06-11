package com.goggles.monitoringservice.agent.application;

import java.util.Map;

public interface AiAgent {
  AgentResponse assignTask(Map<String, Object> inputs);
}
