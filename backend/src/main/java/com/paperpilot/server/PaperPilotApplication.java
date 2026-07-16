package com.paperpilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaperPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperPilotApplication.class, args);
    }
}
