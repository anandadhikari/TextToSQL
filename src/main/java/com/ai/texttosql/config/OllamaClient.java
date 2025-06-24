package com.ai.texttosql.config;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient;
    private final String model;

    public OllamaClient(@Value("${ollama.model.name:llama3.2}") String model,
                        @Value("${ollama.model.baseurl:http://localhost:11434}") String baseUrl) {
        this.model = model;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String ask(String promptText) {
        log.debug("Sending prompt to Ollama:\n{}", promptText);

        long start = System.currentTimeMillis();

        String jsonResponse = webClient.post()
                .uri("/api/generate")
                .header("Content-Type", "application/json")
                .bodyValue(Map.of(
                        "model", model,
                        "prompt", promptText,
                        "stream", false
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.boundedElastic())
                .block();

        long duration = System.currentTimeMillis() - start;
        log.info("Ollama call took {} ms", duration);

        JSONObject responseJson = new JSONObject(jsonResponse);
        String response = responseJson.optString("response", "").trim();

        log.debug("Ollama response:\n{}", response);
        return response;
    }
}
