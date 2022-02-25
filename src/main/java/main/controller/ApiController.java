package main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.model.GlobalSetting;
import main.model.Post;
import main.model.Tag;
import main.model.enums.ModerationStatus;
import main.repo.GlobalSettingRepository;
import main.repo.PostRepository;
import main.repo.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ApiInit apiInit;
    private List<Tag> tags;

    @GetMapping("/init")
    public Map<String, String> init() {
        HashMap<String, String> map = new HashMap<>();
        map.put("title", apiInit.getTitle());
        map.put("subtitle", apiInit.getSubtitle());
        map.put("phone", apiInit.getPhone());
        map.put("email", apiInit.getEmail());
        map.put("copyright", apiInit.getCopyright());
        map.put("copyrightFrom", apiInit.getCopyrightFrom());
        return map;
    }

    @Autowired
    private GlobalSettingRepository globalSettingRepository;
    @GetMapping("/settings")
    public Map<String, String> apiSettings() {
        HashMap<String, String> map = new HashMap<>();
        Iterable<GlobalSetting> globalSettings = globalSettingRepository.findAll();
        globalSettings.forEach(globalSetting -> {
                        map.put(globalSetting.getCode(), String.valueOf(globalSetting.getValue().equals("YES")));
        });
        return map;
    }

    @Autowired
    private PostRepository postRepository;
    @GetMapping("/post")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPost(Integer offset, Integer limit, String mode) {
        Map<String, Object> map = new HashMap<>();
        long postCount = postRepository.countAllActivePosts();
        Page<Post> page = null;
        if(offset == null | limit == null){
            offset = 0;
            limit = 10;
        }
        switch (mode){
            case("popular"):
                Pageable popular = PageRequest.of(offset, limit);
                page = postRepository.findPostsWithPagination(popular);
                break;
            case("best"):
                Pageable best = PageRequest.of(offset, limit);
                page = postRepository.findPostsWithPaginationBest(best);
                break;
            case("early"):
                Pageable early = PageRequest.of(offset, limit, Sort.Direction.ASC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, early);
                break;
            default:
                //recent
                Pageable pageable = PageRequest.of(offset, limit, Sort.Direction.DESC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, pageable);
                break;
        }
        ArrayList<Map> arrayList = new ArrayList<>();
        page.forEach(post -> {
            try {
                arrayList.add(post.getMapResponse(postCount));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        if (postCount == 0){
            map.put("count", 0);
            map.put("posts", new ArrayList());
        }else{
            map.put("count", postCount);
            map.put("posts", arrayList);
        }
        return map;
    }
    @GetMapping("/post/search")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostQuery(Integer offset, Integer limit, String mode, String query) {
        Map<String, Object> map = new HashMap<>();
        long postCount = postRepository.countAllActivePosts();
        Page<Post> page = null;
        if(offset == null | limit == null){
            offset = 0;
            limit = 10;
        }
        switch (mode){
            case("popular"):
                Pageable popular = PageRequest.of(offset, limit);
                page = postRepository.findPostsWithPagination(popular);
                break;
            case("best"):
                Pageable best = PageRequest.of(offset, limit);
                page = postRepository.findPostsWithPaginationBest(best);
                break;
            case("early"):
                Pageable early = PageRequest.of(offset, limit, Sort.Direction.ASC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, early);
                break;
            default:
                //recent
                Pageable pageable = PageRequest.of(offset, limit, Sort.Direction.DESC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, pageable);
                break;
        }
        ArrayList<Map> arrayList = new ArrayList<>();
        page.forEach(post -> {
            try {
                arrayList.add(post.getMapResponse(postCount));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        if (postCount == 0){
            map.put("count", 0);
            map.put("posts", new ArrayList());
        }else{
            map.put("count", postCount);
            map.put("posts", arrayList);
        }
        return map;
    }



    @GetMapping("/auth/check")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiAuthCheck(){
        Map<String, Object> map = new HashMap<>();
        map.put("result", false);
        return map;
    }

    @Autowired
    private TagRepository tagRepository;
    @GetMapping("/tag")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiTag(){
        double postCount = (double) postRepository.countAllActivePosts();

        List<Map> response = new ArrayList<>();
        tags = tagRepository.findAllTag();
        Tag maxValueTag = tags.stream().max(Comparator.comparing(tag -> tag.getTag2Posts().size())).get();
        double dWeightMax = 1/(maxValueTag.getTag2Posts().size()/postCount);
        tags.forEach(tag -> {
            Map<String,Object> mapForArray = new HashMap<>();
            String name = tag.getName();
            Double weight = tag.getTag2Posts().size()*dWeightMax/postCount;
            mapForArray.put("name",name);
            mapForArray.put("weight",weight);
            response.add(mapForArray);
        });

        Map<String, Object> map = new HashMap<>();
        map.put("tags", response);
        return map;
    }
}
