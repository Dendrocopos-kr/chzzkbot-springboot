package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class OllamaResponse {
    @JsonProperty("model")
    private String model;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("done_reason")
    private String doneReason;

    @JsonProperty("done")
    private boolean isDone;

    @JsonProperty("total_duration")
    private long totalDuration;

    @JsonProperty("load_duration")
    private long loadDuration;

    @JsonProperty("prompt_eval_count")
    private int promptEvalCount;

    @JsonProperty("prompt_eval_duration")
    private long promptEvalDuration;

    @JsonProperty("eval_count")
    private int evalCount;

    @JsonProperty("eval_duration")
    private long evalDuration;

    // getters and setters
    // ...

    @Data
    public static class Message {

        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        // getters and setters
        // ...
    }
}
