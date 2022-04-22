package main.config;

import main.controller.ApiInit;
import main.service.MailParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {
    @Autowired
    private MailParam mailParam;

    @Bean
    public JavaMailSender getMailSender(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailParam.getHost());
        mailSender.setPort(mailParam.getPort());
        mailSender.setUsername(mailParam.getUsername());
        mailSender.setPassword(mailParam.getPassword());

        Properties properties = mailSender.getJavaMailProperties();
        properties.setProperty("mail.transport.protocol",mailParam.getProtocol());
        //properties.setProperty("mail.debug", mailParam.getDebug());
        return mailSender;

    }

}
