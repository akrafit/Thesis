package main.controller;

import com.alibaba.fastjson.JSONObject;
import main.config.FormWrapper;
import main.service.GeneralService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiGeneralController {

    private final GeneralService generalService;

    public ApiGeneralController(GeneralService generalService) {
        this.generalService = generalService;
    }

    @GetMapping("/init")
    public Map<String, String> init() {
        return generalService.apiInit();
    }

    @GetMapping("/settings")
    public Map<String, Boolean> apiSettings() {
        return generalService.apiSettings();
    }

    @PutMapping("/settings")
    public void updateSettings(@Valid @RequestBody JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>(jsonObject);
        generalService.updateSettings(map);
    }

    @GetMapping("/calendar")
    public Map<String, Object> getCalendar() throws ParseException {
        return generalService.getCalendar();
    }

    @GetMapping("/tag")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiTag() {
        return generalService.getApiTag();
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadImage(@RequestParam("image") MultipartFile image) throws IOException, NoSuchAlgorithmException {
        return generalService.uploadImage(image);
    }

    @PostMapping("/comment")
    public Map<String, Object> addComment(@Valid @RequestBody JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>(jsonObject);
        return generalService.addComment(map);
    }

    @PostMapping("/moderation")
    public Map<String, Object> postModeration(@Valid @RequestBody JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>(jsonObject);
        return generalService.postModeration(map);
    }

    @PostMapping(value = "/profile/my")
    public Map<String, Object> userProfileUpdate(@Valid @RequestBody JSONObject jsonObject) throws IOException, NoSuchAlgorithmException {
        String name = jsonObject.getString("name");
        String email = jsonObject.getString("email");
        String password = jsonObject.getString("password");
        String removePhoto = jsonObject.getString("removePhoto");
        return generalService.changeProfile(name, email, password, removePhoto, null);
    }

    @PostMapping(value = "/profile/my", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> userProfileUpdateWith(@ModelAttribute FormWrapper model) throws IOException, NoSuchAlgorithmException {
        return generalService.changeProfile(model.getName(), model.getEmail(), model.getPassword(), "0", model.getPhoto());
    }

    @GetMapping("/statistics/my")
    public Map<String, Object> userStatistics() throws ParseException {
        return generalService.userStatistics();
    }

    @GetMapping("/statistics/all")
    public ResponseEntity<Map> allStatistics() throws ParseException {
        return generalService.allStatistics();
    }
}

