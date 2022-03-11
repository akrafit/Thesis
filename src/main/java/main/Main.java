package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Main {
    public static Map<String,Integer> session = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
