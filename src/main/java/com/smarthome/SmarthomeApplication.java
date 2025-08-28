package com.smarthome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
@IntegrationComponentScan
public class SmarthomeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmarthomeApplication.class, args);
    }

}
