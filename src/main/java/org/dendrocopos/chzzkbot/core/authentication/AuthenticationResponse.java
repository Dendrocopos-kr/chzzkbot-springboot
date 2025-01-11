package org.dendrocopos.chzzkbot.core.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("content")
    private Content content;

    public static class Content {

        @JsonProperty("refreshToken")
        private String refreshToken;

        @JsonProperty("accessToken")
        private String accessToken;

        @JsonProperty("tokenType")
        private String tokenType;

        @JsonProperty("expiresIn")
        private Integer expiresIn;

        @JsonProperty("scope")
        private String scope;

    }
}
