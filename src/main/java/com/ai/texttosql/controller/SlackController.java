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
    private final HttpClient httpClient;

    public SlackController(TextToSqlService textToSqlService, 
                         QueryExecutionService queryExecutionService, 
                         QueryHistoryService queryHistoryService) {
        this.textToSqlService = textToSqlService;
        this.queryExecutionService = queryExecutionService;
        this.queryHistoryService = queryHistoryService;
        this.httpClient = HttpClient.newHttpClient();
    }

    @PostConstruct
    public void init() {
        this.executor = Executors.newFixedThreadPool(4);
    }

    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> handleSlashCommand(@RequestParam Map<String, String> payload) {
        String userInput = payload.get("text");
        String responseUrl = payload.get("response_url");
        String userId = payload.get("user_id");

        if (userInput == null || userInput.isBlank()) {
            return ResponseEntity.ok(Map.of(
                "response_type", "ephemeral",
                "text", "❗ Please provide a valid natural language query."
            ));
        }

        // Acknowledge immediately
        Map<String, Object> ackResponse = Map.of(
            "response_type", "ephemeral",
            "text", "🔍 Processing your query. This might take a moment..."
        );

        // Process in background
        executor.submit(() -> processQueryAsync(userInput, responseUrl, userId));
        
        return ResponseEntity.ok(ackResponse);
    }

    private void processQueryAsync(String userInput, String responseUrl, String userId) {
        try {
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
                    "replace_original", true,
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

            sendSlackResponse(responseUrl, slackResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "response_type", "ephemeral",
                "replace_original", true,
                "text", "❌ Error processing your query: " + e.getMessage()
            );
            sendSlackResponse(responseUrl, errorResponse);
        }
    }

    @PostMapping("/interact")
    public ResponseEntity<String> handleSlackInteraction(@RequestParam("payload") String rawPayload) {
        try {
            JSONObject payload = new JSONObject(rawPayload);
            JSONObject actionPayload = new JSONObject(
                    payload.getJSONArray("actions").getJSONObject(0).getString("value")
            );
            String sql = actionPayload.getString("sql");
            String nlq = actionPayload.getString("nlq");
            String userId = payload.getJSONObject("user").getString("id");
            String responseUrl = payload.getString("response_url");

            // Acknowledge immediately
            Map<String, Object> ackResponse = Map.of(
                    "response_type", "ephemeral",
                    "replace_original", false,
                    "text", "⏳ Executing your query. Please wait..."
            );
            sendSlackResponse(responseUrl, ackResponse);

            // Process in background
            executor.submit(() -> runAndPostResult(sql, nlq, userId, responseUrl));
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing interaction");
        }
    }

    private void runAndPostResult(String sql, String nlq, String userId, String responseUrl) {
        try {
            QueryResponse executionResponse = queryExecutionService.executeQuery(sql);
            executionResponse.setNaturalLanguageQuery(nlq);
            executionResponse.setExplanation(executionResponse.getExplanation());

            queryHistoryService.saveQuery(executionResponse, userId);

            String resultMarkdown = formatResultsAsMarkdown(executionResponse.getResults());

            String execSummary = String.format("*Status:* %s  |  *Rows:* %d  |  *Time:* %d ms",
                    executionResponse.getExecutionMetrics().getStatus(),
                    executionResponse.getExecutionMetrics().getResultCount(),
                    executionResponse.getExecutionMetrics().getExecutionTimeMs()
            );

            Map<String, Object> response = Map.of(
                    "response_type", "ephemeral",
                    "replace_original", true,
                    "blocks", List.of(
                            Map.of("type", "section", "text",
                                    Map.of("type", "mrkdwn", "text", "*▶️ Query Executed by <@" + userId + ">*" +
                                            "\n*📝 SQL:*\n```sql\n" + sql + "\n```")),
                            Map.of("type", "context", "elements", List.of(
                                    Map.of("type", "mrkdwn", "text", execSummary)
                            )),
                            Map.of("type", "section", "text",
                                    Map.of("type", "mrkdwn", "text", "*🧪 Results:*\n" + resultMarkdown))
                    )
            );

            sendSlackResponse(responseUrl, response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = Map.of(
                "response_type", "ephemeral",
                "replace_original", true,
                "text", "❌ Error executing query: " + e.getMessage()
            );
            sendSlackResponse(responseUrl, errorResponse);
        }
    }

    private String formatResultsAsMarkdown(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return "```\nNo results found.\n```";
        }

        StringBuilder sb = new StringBuilder("```\n");
        
        // Get column names from first row
        if (!results.isEmpty()) {
            List<String> columns = new ArrayList<>(results.get(0).keySet());
            // Add header
            sb.append(String.join(" | ", columns)).append("\n");
            sb.append("-".repeat(columns.stream().mapToInt(String::length).sum() + (columns.size() - 1) * 3)).append("\n");
            
            // Add rows
            for (Map<String, Object> row : results) {
                List<String> values = new ArrayList<>();
                for (String col : columns) {
                    Object value = row.get(col);
                    values.add(value != null ? value.toString() : "NULL");
                }
                sb.append(String.join(" | ", values)).append("\n");
            }
        }
        
        sb.append("```");
        return sb.toString();
    }

    private void sendSlackResponse(String responseUrl, Map<String, Object> response) {
        try {
            String jsonBody = new JSONObject(response).toString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(responseUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
