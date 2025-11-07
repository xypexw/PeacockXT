package com.example.peacockxt;

import com.example.peacockxt.Service.SystemModule.SnowflakeIdGeneratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdRunner implements CommandLineRunner {

    private final SnowflakeIdGeneratorService generator;

    public SnowflakeIdRunner(SnowflakeIdGeneratorService generator) {
        this.generator = generator;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Snowflake ID test:");
        for (int i = 0; i < 10; i++) {
            System.out.println(generator.nextId());
        }
    }
}
