package com.example.mely.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "mely.ai")
public class FinanceCoachProperties {

    private String provider = "groq";
    private final Groq groq = new Groq();
    private final Gemini gemini = new Gemini();
    private final OpenAi openai = new OpenAi();

    @Data
    public static class Groq {
        private String baseUrl = "https://api.groq.com/openai/v1";
        private String apiKey = "";
        private String model = "llama-3.1-8b-instant";
    }

    @Data
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-1.5-flash";
    }

    @Data
    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
    }
}
