package com.ai.texttosql.controller;

import com.ai.texttosql.model.QueryRequest;
import com.ai.texttosql.model.QueryResponse;
import com.ai.texttosql.service.QueryExecutionService;
import com.ai.texttosql.service.QueryHistoryService;
import com.ai.texttosql.service.TextToSqlService;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/slack")
public class SlackController {

    private final TextToSqlService textToSqlService;
    private final QueryExecutionService queryExecutionService;
    private final QueryHistoryService queryHistoryService;
    private ExecutorService executor;

    public SlackController(TextToSqlService textToSqlService, QueryExecutionService queryExecutionService, QueryHistoryService queryHistoryService) {
        this.textToSqlService = textToSqlService;
        this.queryExecutionService = queryExecutionService;
        this.queryHistoryService = queryHistoryService;
    }

    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(4);
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> handleSlashCommand(@RequestParam Map<String, String> payload) {
        String userInput = payload.get("text");

        if (userInput == null || userInput.isBlank()) {
            return ResponseEntity.ok(Map.of("text", "❗ Please provide a valid natural language query."));
        }

        QueryRequest request = new QueryRequest();
        request.setNaturalLanguageQuery(userInput);
        request.setExplainQuery(false);
        request.setIncludeSchemaContext(true);

        QueryResponse response = textToSqlService.generateSqlQuery(request);

        JSONObject buttonPayload = new JSONObject(Map.of(
                "sql", response.getGeneratedSql(),
                "nlq", response.getNaturalLanguageQuery()
        ));

        Map<String, Object> slackResponse = Map.of(
                "response_type", "in_channel",
                "blocks", List.of(
                        Map.of("type", "section", "text",
                                Map.of("type", "mrkdwn", "text",
                                        "*🧠 Natural Language Query:*\n" + response.getNaturalLanguageQuery())),
                        Map.of("type", "section", "text",
                                Map.of("type", "mrkdwn", "text",
                                        "*📝 Generated SQL:*\n```sql\n" + response.getGeneratedSql() + "\n```")),
                        Map.of("type", "actions", "elements", List.of(
                                Map.of("type", "button",
                                        "text", Map.of("type", "plain_text", "text", "▶️ Run Query"),
                                        "value", buttonPayload.toString(),
                                        "action_id", "run_query")
                        ))
                )
        );

        return ResponseEntity.ok(slackResponse);
    }

    @PostMapping("/interact")
    public ResponseEntity<String> handleSlackInteraction(@RequestParam("payload") String rawPayload) {
        JSONObject payload = new JSONObject(rawPayload);

        JSONObject actionPayload = new JSONObject(
                payload.getJSONArray("actions").getJSONObject(0).getString("value")
        );
        String sql = actionPayload.getString("sql");
        String nlq = actionPayload.getString("nlq");

        String userId = payload.getJSONObject("user").getString("id");
        String responseUrl = payload.getString("response_url");

        executor.submit(() -> runAndPostResult(sql, nlq, userId, responseUrl));
        return (ResponseEntity<String>) ResponseEntity.ok();
    }

    private void runAndPostResult(String sql, String nlq, String userId, String responseUrl) {
        try {
            QueryResponse executionResponse = queryExecutionService.executeQuery(sql);

            executionResponse.setNaturalLanguageQuery(nlq); // Store the correct NLQ for history
            executionResponse.setExplanation(executionResponse.getExplanation());

            queryHistoryService.saveQuery(executionResponse, userId);

            String resultMarkdown = formatResultsAsMarkdown(executionResponse.getResults());

            String execSummary = String.format("*Status:* %s  |  *Rows:* %d  |  *Time:* %d ms",
                    executionResponse.getExecutionMetrics().getStatus(),
                    executionResponse.getExecutionMetrics().getResultCount(),
                    executionResponse.getExecutionMetrics().getExecutionTimeMillis()
            );

            JSONObject body = new JSONObject();
            body.put("response_type", "ephemeral"); // only visible to user
            body.put("blocks", List.of(
                    Map.of("type", "section", "text",
                            Map.of("type", "mrkdwn", "text", "*▶️ Query Executed by <@" + userId + ">*")),
                    Map.of("type", "section", "text",
                            Map.of("type", "mrkdwn", "text", "*📝 SQL:*\n```sql\n" + sql + "\n```")),
                    Map.of("type", "context", "elements", List.of(
                            Map.of("type", "mrkdwn", "text", execSummary)
                    )),
                    Map.of("type", "section", "text",
                            Map.of("type", "mrkdwn", "text", "*🧪 Results:*\n" + resultMarkdown))
            ));

            HttpRequest slackRequest = HttpRequest.newBuilder()
                    .uri(URI.create(responseUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpClient.newHttpClient().send(slackRequest, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatResultsAsMarkdown(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "```\nNo results found.\n```";
        }

        StringBuilder sb = new StringBuilder("```\n");

        // Header
        Set<String> headers = results.get(0).keySet();
        for (String header : headers) {
            sb.append(header).append(" | ");
        }
        sb.setLength(sb.length() - 3); // trim last pipe
        sb.append("\n");

        // Separator
        for (int i = 0; i < headers.size(); i++) {
            sb.append("--- | ");
        }
        sb.setLength(sb.length() - 3);
        sb.append("\n");

        // Rows
        for (Map<String, Object> row : results) {
            for (String header : headers) {
                Object value = row.get(header);
                sb.append(value != null ? value.toString() : "null").append(" | ");
            }
            sb.setLength(sb.length() - 3);
            sb.append("\n");
        }

        sb.append("```");
        return sb.toString();
    }
}
