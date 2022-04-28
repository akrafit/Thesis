package main.controller;

import com.alibaba.fastjson.JSONObject;
import main.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;

    public ApiAuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/check")
    public Map<String, Object> getApiAuthCheck() {
        return userService.getAuthUser();
    }

    @GetMapping("/captcha")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getCaptcha() throws IOException, ParseException, NoSuchAlgorithmException {
        return userService.getCaptcha();
    }

    @GetMapping("/logout")
    public Map<String, Object> logout() {
        return userService.logout();
    }

    @PostMapping("/login")
    public Map<String, Object> auth(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        Map<String, Object> map = new HashMap<>(jsonObject);
        return userService.auth(map);
    }

    @PostMapping("/register")
    public @ResponseBody
    ResponseEntity<Map> registration(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        Map<String, Object> map = new HashMap<>(jsonObject);
        return userService.registration(map);
    }

    @PostMapping("/restore")
    public Map<String, Object> restore(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        String eMail = jsonObject.get("email").toString();
        return userService.restore(eMail);
    }

    @PostMapping("/password")
    public Map<String, Object> password(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        Map<String, Object> map = new HashMap<>(jsonObject);
        return userService.password(map);
    }
}
