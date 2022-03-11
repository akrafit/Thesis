package main.controller;

import main.model.GlobalSetting;
import main.model.Post;
import main.model.Tag;
import main.model.enums.ModerationStatus;
import main.repo.GlobalSettingRepository;
import main.repo.PostRepository;
import main.repo.TagRepository;
import main.specification.PostSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        }else{
            offset = offset/limit;
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
    @GetMapping("/post/{id}")
    public  @ResponseBody
    ResponseEntity <Map> getPost(@PathVariable(value="id") Long id) {
        Post post = postRepository.getOne(id);
        if(id != null || post != null){
            try {
                return new ResponseEntity<>(Post.getSinglePost(post),HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/post/search")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostQuery(Integer offset, Integer limit, String query) {
        Map<String, Object> map = new HashMap<>();
        System.out.println(query.length());

            long postCount = postRepository.countAllActivePosts();
            Page<Post> page = null;
            if (offset == null | limit == null) {
                offset = 0;
                limit = 10;
            } else {
                offset /= limit;
            }
            if (query.length() > 2) {         //recent
                List<String> strings = List.of(query.trim().split(" "));
                List<String> searchList = new ArrayList<>();
                strings.forEach(c -> {
                    String value = c.trim().replaceAll("[^A-Za-zА-Яа-я0-9]", "");
                    if (value.length() > 2) searchList.add(value);
                 });

                ArrayList<Map> arrayList = new ArrayList<>();
                if(searchList.size() == 1) {
                    Pageable pageable = PageRequest.of(offset, limit);
                    page = postRepository.findByText(searchList.get(0), pageable);
                    page.forEach(post -> {
                        try {
                            arrayList.add(post.getMapResponse(postCount));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });

                }else{
                    page = getPostsWhereTextContainsAnyWord(searchList,offset,limit);
                    page.forEach(post -> {
                        try {
                            arrayList.add(post.getMapResponse(postCount));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    });

                }
                map.put("count", postCount);
                map.put("posts", arrayList);

            } else {
                map.put("count", 0);
                map.put("posts", new ArrayList());
            }
        return map;
    }
    public Page<Post> getPostsWhereTextContainsAnyWord(List<String> words, int offset, int limit) {
        Specification<Post> specification = null;
        for(String word : words) {
            Specification<Post> wordSpecification = PostSpecification.textContains(word);
            if(specification == null) {
                specification = wordSpecification;
            } else {
                specification = specification.or(wordSpecification);
            }
        }
        return postRepository.findAll(specification, PageRequest.of(offset, limit));
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
