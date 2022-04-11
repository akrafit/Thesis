package main.controller;

import com.alibaba.fastjson.JSONObject;
import main.model.GlobalSetting;
import main.model.User;
import main.repo.GlobalSettingRepository;
import main.repo.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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



}
