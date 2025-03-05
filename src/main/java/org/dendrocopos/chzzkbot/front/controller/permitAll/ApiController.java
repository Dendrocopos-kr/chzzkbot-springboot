package org.dendrocopos.chzzkbot.front.controller.permitAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dendrocopos.chzzkbot.core.authentication.AuthenticationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Objects;

@Controller
@RequestMapping("/api")
public class ApiController {

    @Value("${chzzk.api.clientId}")
    String clientId;
    @Value("${chzzk.api.clientSecret}")
    String clientSecret;


    @GetMapping("/path")
    public String path(@RequestParam String code, @RequestParam String state) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HashMap<String, Object> body = new HashMap<>();
        body.put("grantType", "authorization_code");
        body.put("clientId", clientId);
        body.put("clientSecret", clientSecret);
        body.put("code", code);
        body.put("state", state);

        HttpEntity<HashMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange("https://openapi.chzzk.naver.com/auth/v1/token", HttpMethod.POST,entity, String.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            AuthenticationResponse authenticationResponse = objectMapper.readValue(response.getBody(), AuthenticationResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return "/html/permitAll/api/path";
    }
}
