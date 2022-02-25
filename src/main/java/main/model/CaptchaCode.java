package main.model;

import javax.persistence.*;

@Entity
@Table(name = "captcha_codes")
public class CaptchaCode {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private String time;

    @Column(nullable = false, columnDefinition = "TINYTEXT")
    private String code;

    @Column(nullable = false, columnDefinition = "TINYTEXT")
    private String secret_code;

}
