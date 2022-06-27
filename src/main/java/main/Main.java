package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Main {
    public static Map<Object, Integer> session = new HashMap<>();
    public static Map<String, Boolean> globalSettings = new HashMap<>();

    public static void main(String[] args) {

        SpringApplication.run(Main.class, args);
    }
}
