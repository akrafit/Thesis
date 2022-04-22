package main.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.mail")
public class MailParam {
    private String host;
    private String username;
    private String password;
    private int port;
    private String protocol;
    private String debug;
}