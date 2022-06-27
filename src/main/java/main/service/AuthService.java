package main.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static main.Main.globalSettings;

@Service
public class AuthService {
    @Value("${url.value}")
    String url;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long oneHour = 3600000;
    int secretCodeLength = 4;
    boolean useLettersForSecretCode = true;
    boolean useNumbersForSecretCode = false;


    private final ObjectFactory<HttpSession> httpSessionFactory;
    private final PostRepository postRepository;
    private final CaptchaCodeRepository captchaCodeRepository;
    private final UserRepository userRepository;
    private final MailSender mailSender;

    public AuthService(ObjectFactory<HttpSession> httpSessionFactory, PostRepository postRepository, CaptchaCodeRepository captchaCodeRepository, UserRepository userRepository, MailSender mailSender) {
        this.httpSessionFactory = httpSessionFactory;
        this.postRepository = postRepository;
        this.captchaCodeRepository = captchaCodeRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    public Map<String, Object> getAuthUser() {
        HttpSession session = httpSessionFactory.getObject();
        int moderationCount = postRepository.countAllPostIsModeration();
        Map<String, Object> map = new HashMap<>();
        if (Main.session.containsValue(session.getId())) {
            Integer authorizedUserID = Main.session.get(session);
            User user = userRepository.getOne(Long.valueOf(authorizedUserID));
            map.put("result", true);
            map.put("user", user.getUserForAuth(moderationCount));
        } else {
            map.put("result", false);
        }
        return map;
    }

    public Map<String, Object> getCaptcha() throws IOException, ParseException, NoSuchAlgorithmException {
        List<CaptchaCode> captchaCode = captchaCodeRepository.findAllCaptchaCode();
        long createdTime;
        if (!captchaCode.isEmpty()) {
            createdTime = sdf.parse(captchaCode.get(0).getTime()).getTime();
        } else {
            createdTime = 0;
        }
        long difference = System.currentTimeMillis() - createdTime;
        if (difference > oneHour) {
            captchaCodeRepository.deleteAll();
            for (int i = 0; i < 10; i++) {
                String secret = generateSecretCode();
                String captchaSecret = getHashCode(secret);
                String time = sdf.format(new Date());
                captchaCodeRepository.save(new CaptchaCode(secret, captchaSecret, time));
            }
        }
        int random = (int) (Math.random() * 10);
        CaptchaCode captchaCodeRandom = captchaCodeRepository.findAllCaptchaCode().get(random);
        Map<String, Object> map = new HashMap<>();
        Path path = Paths.get("/../upload/");
        Files.createDirectories(path);
        OutputStream os = new FileOutputStream("/../upload/image.png", false);
        byte[] fileContent;
        String text = captchaCodeRandom.getCode();
        Cage cage = new Cage();
        try {
            cage.draw(text, os);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            os.close();
        }
        fileContent = FileUtils.readFileToByteArray(new File("/../upload/image.png"));
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        map.put("secret", captchaCodeRandom.getSecretCode());
        map.put("image", "data:image/png;base64, " + encodedString);
        return map;
    }

    private String generateSecretCode() {
        return RandomStringUtils.random(secretCodeLength, useLettersForSecretCode, useNumbersForSecretCode);
    }

    public static String getHashCode(String password) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = md5.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (Byte b : bytes) {
            stringBuilder.append(String.format("%02X", b));
        }
        return String.valueOf(stringBuilder);
    }

    private User getAuthorizedUser() {
        Long id = Long.valueOf(Main.session.getOrDefault(httpSessionFactory.getObject().getId(), 0));
        if (id > 0) {
            return userRepository.getOne(id);
        }
        return null;
    }

    public static boolean checkText(String content) {
        return content.matches("[а-яА-Яa-zA-Z0-9]+");
    }

    public Map<String, Object> logout() {
        User user = getAuthorizedUser();
        Map<String, Object> map = new HashMap<>();
        if (user != null) {
            String session = httpSessionFactory.getObject().getId();
            Integer userId = Main.session.get(session);
            if (userId > 0) {
                Main.session.remove(session);
            }
        }
        map.put("result", true);
        return map;
    }

    public Map<String, Object> auth(Map<String, Object> objectMap) throws NoSuchAlgorithmException {
        String newSession = httpSessionFactory.getObject().getId();
        String eMail = objectMap.get("e_mail").toString();
        String password = objectMap.get("password").toString();
        Map<String, Object> map = new HashMap<>();
        int moderationCount = postRepository.countAllPostIsModeration();
        if (!eMail.isEmpty() || !password.isEmpty()) {
            User user = userRepository.findByEmail(eMail.trim());
            String pass = getHashCode(password);
            if (user != null && Objects.equals(user.getPassword(), pass)) {
                map.put("result", true);
                map.put("user", user.getUserForAuth(moderationCount));
                Main.session.put(newSession, Math.toIntExact(user.getId()));
            } else {
                map.put("result", false);
            }
        }
        return map;
    }

    public ResponseEntity<Map> registration(Map<String, Object> objectMap) throws NoSuchAlgorithmException {
        //System.out.println(globalSettings);
        Boolean multiUserMode = globalSettings.get("MULTIUSER_MODE");
        //System.out.println(multiUserMode);
        if (multiUserMode != null) {
            String eMail = objectMap.get("e_mail").toString();
            String password = objectMap.get("password").toString();
            String name = objectMap.get("name").toString();
            String captcha = objectMap.get("captcha").toString();
            String captcha_secret = objectMap.get("captcha_secret").toString();
            CaptchaCode captchaCode = captchaCodeRepository.findCaptchaCode(captcha);
            String regTime = sdf.format(new Date());
            User user = userRepository.findByEmail(eMail);
            Map<String, Object> errors = new HashMap<>();
            if (user != null) {
                errors.put("email", "Этот e-mail уже зарегистрирован");
            }
            if (password.trim().length() < 6) {
                errors.put("password", "Пароль короче 6-ти символов");
            }
            if (!checkText(name)){
                errors.put("name", "Имя указано неверно");
            }
            if (captchaCode != null) {
                if (!captchaCode.getSecretCode().equals(captcha_secret))
                    errors.put("captcha", "Код с картинки введён неверно");
            } else {
                errors.put("captcha", "Код с картинки введён неверно");
            }
            if (errors.isEmpty()) {
                errors.put("result", true);
                userRepository.save(new User(eMail, getHashCode(password), name, regTime));
                return new ResponseEntity<>(errors, HttpStatus.OK);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("result", false);
            map.put("errors", errors);

            return new ResponseEntity<>(map, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    public Map<String, Object> restore(String eMail) throws NoSuchAlgorithmException {
        User user = userRepository.findByEmail(eMail);
        Map<String, Object> map = new HashMap<>();
        if (user != null) {
            user.setCode(getHashCode(generateSecretCode()));
            userRepository.save(user);
            String message = String.format(
                    "Приветствую, %s! \n" +
                            "перейдите по ссылке для восстановления доступа "+ url +"/login/change-password/" + user.getCode(), user.getName(), user.getCode()
            );
            mailSender.send(user.getEmail(), "Activation code", message);
            map.put("result", true);
        } else {
            map.put("result", false);
        }
        return map;
    }

    public Map<String, Object> password(Map<String, Object> objectMap) throws NoSuchAlgorithmException {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        String code = objectMap.get("code").toString();
        String password = objectMap.get("password").toString();
        String captcha = objectMap.get("captcha").toString();
        String captchaSecret = objectMap.get("captcha_secret").toString();
        CaptchaCode captchaCode = captchaCodeRepository.findCaptchaCode(captcha);
        User user = userRepository.findByCode(code);

        if (user == null){
            errors.put("code", "Ссылка для восстановления пароля устарела.<a href=\"/auth/restore\">Запросить ссылку снова</a>");
        }
        if (password.trim().length() < 6){
            errors.put("password", "Пароль короче 6-ти символов");
        }
        if (captchaCode != null) {
            if (!captchaCode.getSecretCode().equals(captchaSecret))
                errors.put("captcha", "Код с картинки введён неверно");
        } else {
            errors.put("captcha", "Код с картинки введён неверно");
        }
        if (errors.isEmpty()) {
            map.put("result", true);
            assert user != null;
            user.setPassword(getHashCode(password));
            userRepository.save(user);
        } else {
            map.put("result", false);
            map.put("errors", errors);
        }
        return map;
    }
}
