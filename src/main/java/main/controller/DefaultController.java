package main.controller;

import main.model.GlobalSetting;
import main.repo.GlobalSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DefaultController {
    //@Autowired

    @Value("${someParameter.value}")
    private Integer someParameter;

    @RequestMapping("/")
    public String index(String text){
        text = "hello world";

        return "index";
    }

}
