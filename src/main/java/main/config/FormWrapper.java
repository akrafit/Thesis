package main.config;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FormWrapper {
    private MultipartFile photo;
    private String name;
    private String email;
    private String password;
    private Integer removePhoto;
}
