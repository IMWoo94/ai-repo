package com.imwoo.airepo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AiRepoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRepoApplication.class, args);
    }
}
