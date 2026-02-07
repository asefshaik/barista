package com.barista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BaristaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaristaApplication.class, args);
    }
}
