package com.github.peacockxt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PeacockXtApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeacockXtApplication.class, args);
        System.out.println("PeacockXtrApplication started");
    }

}
