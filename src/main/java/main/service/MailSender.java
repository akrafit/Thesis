package main.service;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailSender {
    public MailSender(JavaMailSender mailSender, MailParam mailParam) {
        this.mailSender = mailSender;
        this.mailParam = mailParam;
    }

    private JavaMailSender mailSender;
    private MailParam mailParam;

    public void send(String emailTo, String subject, String message){
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailParam.getUsername());
        mailMessage.setTo(emailTo);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);
        mailSender.send(mailMessage);
    }
}
