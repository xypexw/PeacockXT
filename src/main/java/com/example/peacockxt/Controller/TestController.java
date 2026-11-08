package com.example.peacockxt.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/test-json")
    public Map<String, Object> test() {
        return Map.of(
                "message", "hello",
                "number", 123
        );
    }
}
