package main.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DefaultController {
    //@Autowired

    @Value("${someParameter.value}")
    private Integer someParameter;

    @RequestMapping("/")
    public String index(String text){
        return "index";
    }



}
