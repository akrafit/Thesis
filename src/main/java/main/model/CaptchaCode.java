package main.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;

@Entity
@Data
@Table(name = "captcha_codes")
@NoArgsConstructor
public class CaptchaCode {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private String time;

    @Column(nullable = false, columnDefinition = "TINYTEXT")
    private String code;

    @Column(name = "secret_code", nullable = false, columnDefinition = "TINYTEXT")
    private String secretCode;

    public CaptchaCode(String secret, String captchaSecret, String time) {
        this.time = time;
        this.code = secret;
        this.secretCode = captchaSecret;
    }
}
