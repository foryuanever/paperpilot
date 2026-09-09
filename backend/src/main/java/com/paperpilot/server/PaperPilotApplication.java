package com.paperpilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

import com.paperpilot.server.config.RequestTimingFilter;

@SpringBootApplication
@EnableScheduling
public class PaperPilotApplication {

    @Bean
    public RequestTimingFilter requestTimingFilter() {
        return new RequestTimingFilter();
    }

    public static void main(String[] args) {
        SpringApplication.run(PaperPilotApplication.class, args);
    }
}
