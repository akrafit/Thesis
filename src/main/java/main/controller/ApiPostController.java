package main.controller;

import com.alibaba.fastjson.JSONObject;
import main.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class ApiPostController {
    private final PostService postService;

    public ApiPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiPost(Integer offset, Integer limit, String mode) {
        return postService.getApiPost(offset, limit, mode);
    }

    @GetMapping("/{id}")
    public @ResponseBody
    ResponseEntity<Map> getPost(@PathVariable(value = "id") Long id) {
        return postService.getPost(id);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiPostQuery(Integer offset, Integer limit, String query) {
        return postService.getApiPostQuery(offset, limit, query);
    }

    @GetMapping("/byDate")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiPostByDate(Integer offset, Integer limit, String date) throws ParseException {
        return postService.getApiPostByDate(offset, limit, date);
    }

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getPostModeration(Integer offset, Integer limit, String status) throws ParseException {
        return postService.getPostModeration(offset, limit, status);
    }

    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getPostMy(Integer offset, Integer limit, String status) {
        return postService.getPostMy(offset, limit, status);
    }

    @GetMapping("/byTag")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiPostByTag(Integer offset, Integer limit, String tag) throws ParseException {
        return postService.getApiPostByTag(offset, limit, tag);
    }

    @PostMapping
    public Map<String, Object> addPost(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        return postService.addPost(jsonObject);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePost(@Valid @RequestBody JSONObject jsonObject, @PathVariable(value = "id") Long id) throws NoSuchAlgorithmException {
        return postService.updatePost(jsonObject, id);
    }

    @PostMapping("/like")
    public Map<String, Object> like(@Valid @RequestBody JSONObject jsonObject) {
        return postService.like(jsonObject);
    }

    @PostMapping("/dislike")
    public Map<String, Object> dislike(@Valid @RequestBody JSONObject jsonObject) {
        return postService.dislike(jsonObject);
    }

}
