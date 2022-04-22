package main.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
public class DefaultController {
    //@Autowired

    @Value("${someParameter.value}")
    private Integer someParameter;

    @RequestMapping("/")
    public String index(String text){
        return "index";
    }

    @GetMapping("/login/change-password/{code}")
    public String activate(@PathVariable String code){
        //System.out.println(code);

        return "index";
    }



}
