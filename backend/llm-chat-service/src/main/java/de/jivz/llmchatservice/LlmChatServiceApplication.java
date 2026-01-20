package de.jivz.llmchatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LlmChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmChatServiceApplication.class, args);
    }

}
