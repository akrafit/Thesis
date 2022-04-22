package main.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.cage.Cage;
import main.Main;
import main.model.CaptchaCode;
import main.model.User;
import main.repo.CaptchaCodeRepository;
import main.repo.PostRepository;
import main.repo.UserRepository;
import main.service.MailSender;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<CaptchaCode> captchaCode = captchaCodeRepository.findAllCaptchaCode();
        long createdTime;
        if(!captchaCode.isEmpty()) {
            createdTime = sdf.parse(captchaCode.get(0).getTime()).getTime();
        }else{
            createdTime = 0;
        }
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
        map.put("secret", captchaCodeRandom.getSecretCode());
        map.put("image", "data:image/png;base64, " + encodedString);
        return map;
    }

    @GetMapping("/logout")
    public Map<String,Object> logout(){
        User user = getAuthorizedUser();
        Map<String, Object> map = new HashMap<>();
        if (user != null) {
            String session = httpSessionFactory.getObject().getId();
            Integer userId = Main.session.get(session);
            if (userId > 0) {
                Main.session.remove(session);
            }
        }
        map.put("result",true);
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
    public @ResponseBody
    ResponseEntity<Map> registration(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        String multiUserMode = Main.globalSettings.get("MULTIUSER_MODE");
        System.out.println(multiUserMode);
        if(multiUserMode.equals("YES")) {
            String eMail = jsonObject.get("e_mail").toString();
            String password = jsonObject.get("password").toString();
            String name = jsonObject.get("name").toString();
            String captcha = jsonObject.get("captcha").toString();
            String captcha_secret = jsonObject.get("captcha_secret").toString();
            CaptchaCode captchaCode = captchaCodeRepository.findCaptchaCode(captcha);
            SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String regTime = sdf.format(new Date());
            User user = userRepository.findByEmail(eMail);
            Map<String, Object> errors = new HashMap<>();
            if (user != null) errors.put("email", "Этот e-mail уже зарегистрирован");
            if (password.trim().length() < 6) errors.put("password", "Пароль короче 6-ти символов");
            if (!checkText(name)) errors.put("name", "Имя указано неверно");
            if (captchaCode != null) {
                if (!captchaCode.getSecretCode().equals(captcha_secret))
                    errors.put("captcha", "Код с картинки введён неверно");
            } else {
                errors.put("captcha", "Код с картинки введён неверно");
            }
            if (errors.isEmpty()) {
                errors.put("result", true);
                userRepository.save(new User(eMail, getHashCode(password), name, regTime));
            }
            return new ResponseEntity<>(errors, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @Autowired
    private MailSender mailSender;
    @PostMapping("/restore")
    public Map<String,Object> restore(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        String eMail = jsonObject.get("email").toString();
        User user = userRepository.findByEmail(eMail);
        Map<String,Object> map = new HashMap<>();
        if(user != null){
            user.setCode(getHashCode(generateSecretCode()));
            userRepository.save(user);
            String message = String.format(
                    "Приветствую, %s! \n" +
                            "перейдите по ссылке для восстановления доступа http://localhost:8080/login/change-password/" + user.getCode(),user.getName(), user.getCode()
            );
            mailSender.send(user.getEmail(), "Activation code", message);
            map.put("result", true);
        }else{
            map.put("result", false);
        }
        return map;
    }

    @PostMapping("/password")
    public Map<String,Object> password(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> errors = new HashMap<>();
        String code = jsonObject.get("code").toString();
        String password = jsonObject.get("password").toString();
        String captcha = jsonObject.get("captcha").toString();
        String captchaSecret = jsonObject.get("captcha_secret").toString();
        CaptchaCode captchaCode = captchaCodeRepository.findCaptchaCode(captcha);
        User user = userRepository.findByCode(code);

        if(user == null) errors.put("code","Ссылка для восстановления пароля устарела.<a href=\"/auth/restore\">Запросить ссылку снова</a>");
        if(password.trim().length() < 6) errors.put("password", "Пароль короче 6-ти символов");
        if(captchaCode != null){
            if(!captchaCode.getSecretCode().equals(captchaSecret)) errors.put("captcha", "Код с картинки введён неверно");
        }else{
            errors.put("captcha", "Код с картинки введён неверно");
        }
        if(errors.isEmpty()){
            map.put("result", true);
            assert user != null;
            user.setPassword(getHashCode(password));
            userRepository.save(user);
        }else{
            map.put("result", false);
            map.put("errors", errors);
        }
        return map;
    }

    private User getAuthorizedUser() {
        Long id = Long.valueOf(Main.session.getOrDefault(httpSessionFactory.getObject().getId(),0));
        if(id > 0) {
            return userRepository.getOne(id);
        }
        return null;
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
