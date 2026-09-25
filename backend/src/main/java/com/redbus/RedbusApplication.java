package com.redbus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RedbusApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedbusApplication.class, args);
    }
}
