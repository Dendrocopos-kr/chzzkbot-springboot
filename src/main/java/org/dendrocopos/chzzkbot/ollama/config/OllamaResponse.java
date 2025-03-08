package org.dendrocopos.chzzkbot.ollama.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class OllamaResponse {
    @JsonProperty("model")
    private String model;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("message")
    private OllamaMessage message;

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

}
