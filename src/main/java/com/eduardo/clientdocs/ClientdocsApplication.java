package com.eduardo.clientdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClientdocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientdocsApplication.class, args);
    }
}