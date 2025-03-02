package org.dendrocopos.chzzkbot.ollama.config;

public class OllamaResponse {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public static class Message {
        private String content;

        public String getContent() {
            return content;
        }
    }
}
