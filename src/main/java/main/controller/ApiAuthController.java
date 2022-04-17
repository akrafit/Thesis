package main.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.cage.Cage;
import main.Main;
import main.model.CaptchaCode;
import main.model.User;
import main.repo.CaptchaCodeRepository;
import main.repo.PostRepository;
import main.repo.UserRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    @Autowired
    ObjectFactory<HttpSession> httpSessionFactory;
    @Autowired
    private PostRepository postRepository;    ;
    @GetMapping("/check")
    public Map<String, Object> getApiAuthCheck(){
        HttpSession session = httpSessionFactory.getObject();
        int moderationCount = postRepository.countAllPostIsModeration();
        Map<String, Object> map = new HashMap<>();
        if(Main.session.containsValue(session.getId())){
            Integer authorizedUserID = Main.session.get(session);
            User user = userRepository.getOne(Long.valueOf(authorizedUserID));
            map.put("result", true);
            map.put("user", user.getUserForAuth(moderationCount));
        }else{
            map.put("result", false);
        }
        return map;
    }

    @Autowired
    private CaptchaCodeRepository captchaCodeRepository;
    @GetMapping("/captcha")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getCaptcha() throws IOException, ParseException, NoSuchAlgorithmException {
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        CaptchaCode captchaCode = captchaCodeRepository.findAllCaptchaCode().get(0);
        long createdTime = sdf.parse(captchaCode.getTime()).getTime();
        long difference = System.currentTimeMillis()-createdTime;

        if(difference > 3600000){
            captchaCodeRepository.deleteAll();
            for (int i = 0; i < 10; i++){
                String secret = generateSecretCode();
                String captchaSecret = getHashCode(secret);
                String time = sdf.format(new Date());
                captchaCodeRepository.save(new CaptchaCode(secret,captchaSecret,time));
            }
        }
        int random = (int) (Math.random() * 10);
        CaptchaCode captchaCodeRandom = captchaCodeRepository.findAllCaptchaCode().get(random);
        Map<String, Object> map = new HashMap<>();
        OutputStream os = new FileOutputStream("image.png", false);
        byte[] fileContent;
        String text = captchaCodeRandom.getCode();
        Cage cage = new Cage();
        try {
            cage.draw(text,os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            os.close();
        }
        fileContent = FileUtils.readFileToByteArray(new File("image.png"));
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        map.put("secret", captchaCodeRandom.getSecret_code());
        map.put("image", "data:image/png;base64, " + encodedString);
        return map;
    }

    private String generateSecretCode() {
        int length = 4;
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

    @Autowired
    private UserRepository userRepository;
    @PostMapping("/login")
    public Map<String,Object> auth(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        String newSession = httpSessionFactory.getObject().getId();
        String eMail = jsonObject.get("e_mail").toString();
        String password = jsonObject.get("password").toString();
        Map<String,Object> map = new HashMap<>();
        int moderationCount = postRepository.countAllPostIsModeration();
        if(!eMail.isEmpty() || !password.isEmpty()) {
            User user = userRepository.findByEmail(eMail.trim());
            String pass = getHashCode(password);
            //System.out.println(pass);
            if (Objects.equals(user.getPassword(), pass)) {
                map.put("result", true);
                map.put("user", user.getUserForAuth(moderationCount));
                Main.session.put(newSession, Math.toIntExact(user.getId()));
                //System.out.println(Main.session.get(newSession));
            } else {
                map.put("result", false);
            }
        }
        return map;
    }

    @PostMapping("/register")
    public Map<String,Object> registration(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        String eMail = jsonObject.get("e_mail").toString();
        String password = jsonObject.get("password").toString();
        String name = jsonObject.get("name").toString();
        String captcha = jsonObject.get("captcha").toString();
        String captcha_secret = jsonObject.get("captcha_secret").toString();
        CaptchaCode captchaCode = captchaCodeRepository.findCaptchaCode(captcha);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String regTime = sdf.format(new Date());
        User user = userRepository.findByEmail(eMail);
        Map<String,Object> errors = new HashMap<>();
        if(user != null) errors.put("email","Этот e-mail уже зарегистрирован");
        if(password.trim().length() < 6) errors.put("password", "Пароль короче 6-ти символов");
        if (!checkText(name)) errors.put("name", "Имя указано неверно");
        if(captchaCode != null){
            if(!captchaCode.getSecret_code().equals(captcha_secret)) errors.put("captcha", "Код с картинки введён неверно");
        }else{
            errors.put("captcha", "Код с картинки введён неверно");
        }
        if(errors.isEmpty()){
            errors.put("result", true);
            userRepository.save(new User(eMail,getHashCode(password),name,regTime));
        }
        return errors;
    }


    public static String getHashCode(String password) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = md5.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (Byte b:bytes){
            stringBuilder.append(String.format("%02X",b));
        }
        return String.valueOf(stringBuilder);
    }
    public static boolean checkText(String content) {
        return content.matches("[а-яА-Яa-zA-Z0-9]+");
    }
}
