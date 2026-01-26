package de.jivz.ai_challenge.openrouterservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenrouterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenrouterServiceApplication.class, args);
    }

}
