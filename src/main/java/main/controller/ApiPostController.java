package main.controller;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import main.Main;
import main.model.*;
import main.model.enums.ModerationStatus;
import main.repo.*;
import main.specification.PostSpecification;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/post")
public class ApiPostController {
    private int offsetInt;
    private int limitInt;

    @Autowired
    private PostRepository postRepository;
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiPost(Integer offset, Integer limit, String mode) {
        Page<Post> page;
        checkOffsetAndLimit(offset,limit);
        switch (mode){
            case("popular"):
                Pageable popular = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findPostsWithPagination(popular);
                break;
            case("best"):
                Pageable best = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findPostsWithPaginationBest(best);
                break;
            case("early"):
                Pageable early = PageRequest.of(offsetInt, limitInt, Sort.Direction.ASC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, early);
                break;
            default:
                //recent
                Pageable pageable = PageRequest.of(offsetInt, limitInt, Sort.Direction.DESC, "time");
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, pageable);

                break;
        }
        return getMapResponseForm(page);
    }
    @GetMapping("/{id}")
    public  @ResponseBody
    ResponseEntity<Map> getPost(@PathVariable(value="id") Long id) {
        Post post = postRepository.getOne(id);
        if(id != null || post != null){
            try {
                User user = getAuthorizedUser();
                if(user != null) {
                    if (!user.getId().equals(post.getUser().getId()) & user.getIsModerator() == 0) {
                        postRepository.plusOneToVisit(post.getId());
                    }
                }else{
                    postRepository.plusOneToVisit(post.getId());
                }
                return new ResponseEntity<>(Post.getSinglePost(post),HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostQuery(Integer offset, Integer limit, String query) {
        Map<String, Object> map = new HashMap<>();
        Page<Post> page = null;
        long postCount;
        checkOffsetAndLimit(offset,limit);
        if (query.length() > 2) {         //recent
            List<String> strings = List.of(query.trim().split(" "));
            List<String> searchList = new ArrayList<>();
            strings.forEach(c -> {
                String value = c.trim().replaceAll("[^A-Za-zА-Яа-я0-9]", "");
                if (value.length() > 2) searchList.add(value);
            });
            ArrayList<Map> arrayList = new ArrayList<>();
            if(searchList.size() == 1) {
                Pageable pageable = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findByText(searchList.get(0), pageable);
                postCount = page.getTotalElements();
                page.forEach(post -> {
                    try {
                        arrayList.add(post.getMapResponse());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
            }else{
                page = getPostsWhereTextContainsAnyWord(searchList,offsetInt, limitInt);
                postCount = page.getTotalElements();
                page.forEach(post -> {
                    try {
                        arrayList.add(post.getMapResponse());
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

    @GetMapping("/byDate")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostByDate(Integer offset, Integer limit, String date) throws ParseException {
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        checkOffsetAndLimit(offset,limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(sdf.parse(date));
        endDate.add(Calendar.DAY_OF_MONTH, 1);
        String end = sdf.format(endDate.getTime());
        Page<Post> page = postRepository.findByDate(date,end, pageable);
        return getMapResponseForm(page);
    }

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getPostModeration(Integer offset, Integer limit, String status) throws ParseException {
        checkOffsetAndLimit(offset,limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Page<Post> page;
        switch (status){
            case("declined"):
                page = postRepository.findPostsByModeration(1,String.valueOf(ModerationStatus.DECLINED),pageable);
                break;
            case("accepted"):
                page = postRepository.findPostsByModeration(1,String.valueOf(ModerationStatus.ACCEPTED),pageable);
                break;
            default:
                page = postRepository.findPostsByModeration(1,String.valueOf(ModerationStatus.NEW),pageable);
                break;
        }
        return getMapResponseForm(page);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    ObjectFactory<HttpSession> httpSessionFactory;
    @GetMapping("/my")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getPostMy(Integer offset, Integer limit, String status){
        checkOffsetAndLimit(offset,limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Page<Post> page = null;
        User user = null;
        String session = httpSessionFactory.getObject().getId();
        Long userID = Long.valueOf(Main.session.getOrDefault(session,0));
        if(userID != 0 || !Main.session.isEmpty()){
            user = userRepository.getOne(userID);
            switch (status){
                case("inactive"):
                    page = postRepository.findPostsByMy(user.getId(), 0,String.valueOf(ModerationStatus.NEW),pageable);
                    break;
                case("pending"):
                    page = postRepository.findPostsByMy(user.getId(), 1,String.valueOf(ModerationStatus.NEW),pageable);
                    break;
                case("declined"):
                    page = postRepository.findPostsByMy(user.getId(), 1,String.valueOf(ModerationStatus.DECLINED),pageable);
                    break;
                default:
                    page = postRepository.findPostsByMy(user.getId(), 1,String.valueOf(ModerationStatus.ACCEPTED),pageable);
                    break;
            }
        }
        return getMapResponseForm(page);
    }

    @Autowired
    private TagRepository tagRepository;
    @GetMapping("/byTag")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostByTag(Integer offset, Integer limit, String tag) throws ParseException {
        checkOffsetAndLimit(offset,limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Tag tagName = tagRepository.getTagByName(tag);
        Page<Post> page = postRepository.findByTag(tagName.getId(), pageable);
        return getMapResponseForm(page);
    }

    @PostMapping
    public Map<String,Object> addPost(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException {
        String timestamp = jsonObject.get("timestamp").toString();
        String active = jsonObject.get("active").toString();
        String title = jsonObject.get("title").toString();
        JSONArray tags =  jsonObject.getJSONArray("tags");
        String text = jsonObject.get("text").toString();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        User user = getAuthorizedUser();
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> errors = new HashMap<>();
        long nowTime = System.currentTimeMillis();
        long timestampLong = Long.parseLong(timestamp) * 1000;
        if(user == null) errors.put("title", "Пользователь не авторизован");
        if(title.isEmpty()) errors.put("title","Заголовок не установлен");
        if(title.length() < 3) errors.put("title","Заголовок публикации слишком короткий");
        if(text.length() < 50) errors.put("text", "Текст публикации слишком короткий");
        if(nowTime < timestampLong){
            timestampLong = nowTime;
        }
        if(errors.isEmpty()){
            Post post = new Post(title,text,active,sdf.format(timestampLong));
            if(Main.globalSettings.get("POST_PREMODERATION").equals("NO"))post.setModerationStatus(ModerationStatus.ACCEPTED);
            post.setUser(user);
            List<Tag2Post> tag2Posts = new ArrayList<>();
            tags.forEach(t -> {
                Tag tag = tagRepository.getTagByName(t.toString());
                if(tag == null){
                    tag = new Tag(t.toString().trim());
                    tagRepository.save(tag);
                }
                Tag2Post tag2Post = new Tag2Post(tag,post);
                tag2Posts.add(tag2Post);
            });
            post.setTag2Posts(tag2Posts);
            postRepository.save(post);
            map.put("result", true);
        }else{
            map.put("result", false);
            map.put("errors", errors);
        }
        return map;
    }

    @Autowired
    private Tag2PostRepository tag2PostRepository;
    @PutMapping("/{id}")
    public Map<String,Object> updatePost(@Valid @RequestBody JSONObject jsonObject, @PathVariable(value="id") Long id) throws NoSuchAlgorithmException {
        String timestamp = jsonObject.get("timestamp").toString();
        int active = jsonObject.getInteger("active");
        String title = jsonObject.get("title").toString();
        JSONArray tags =  jsonObject.getJSONArray("tags");
        String text = jsonObject.get("text").toString();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        User user = getAuthorizedUser();
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> errors = new HashMap<>();
        long nowTime = System.currentTimeMillis();
        long timestampLong = Long.parseLong(timestamp) * 1000;
        if(user == null) errors.put("title", "Пользователь не авторизован");
        if(title.isEmpty()) errors.put("title","Заголовок не установлен");
        if(title.length() < 3) errors.put("title","Заголовок публикации слишком короткий");
        if(text.length() < 50) errors.put("text", "Текст публикации слишком короткий");
        if(nowTime < timestampLong){
            timestampLong = nowTime;
        }
        if(errors.isEmpty()){
            Post post = postRepository.getOne(id);

            post.setText(text);
            //System.out.println(text);
            post.setTime(sdf.format(timestampLong));
            post.setIsActive(active);
            post.setTitle(title);
            tag2PostRepository.deleteTag2PostWithPostId(id);
            List<Tag2Post> tag2Posts = new ArrayList<>();
            tags.forEach(t -> {
                Tag tag = tagRepository.getTagByName(t.toString());
                if(tag == null){
                    tag = new Tag(t.toString().trim());
                    tagRepository.save(tag);
                }
                Tag2Post tag2Post = new Tag2Post(tag,post);
                tag2Posts.add(tag2Post);
            });
            post.setTag2Posts(tag2Posts);
            if(Main.globalSettings.get("POST_PREMODERATION").equals("NO")){post.setModerationStatus(ModerationStatus.ACCEPTED);
            }else{
                post.setModerationStatus(ModerationStatus.NEW);
            }
            postRepository.save(post);
            map.put("result", true);
        }else{
            map.put("result", false);
            map.put("errors", errors);
        }
        return map;
    }

    @Autowired
    private PostVoteRepository postVoteRepository;
    @PostMapping("/like")
    public Map<String,Object> like(@Valid @RequestBody JSONObject jsonObject){
        Map<String,Object> map = new HashMap<>();
        Long postId = jsonObject.getLong("post_id");
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PostVote lastPostVote;
        if(postId == null) map.put("result",false);
        if(user == null) map.put("result",false);
        if(map.isEmpty()){
            //List<PostVote> lastPostVotes = postVoteRepository.findPostVote(post.getId(),user.getId());
            List<PostVote> lastPostVotes = postVoteRepository.findPostVote(post,user);
            if(!lastPostVotes.isEmpty()){
                lastPostVote = lastPostVotes.get(0);
                if(lastPostVote.getValue().equals("1")){
                    map.put("result",false);
                }else{
                    lastPostVote.setValue("1");
                    postVoteRepository.save(lastPostVote);
                    map.put("result",true);
            }
            }else{
                PostVote postVote = new PostVote();
                postVote.setPost(post);
                postVote.setUser(user);
                postVote.setValue("1");
                postVote.setTime(sdf.format(System.currentTimeMillis()));
                postVoteRepository.save(postVote);
                map.put("result",true);
            }
        }
        return map;
    }

    @PostMapping("/dislike")
    public Map<String,Object> dislike(@Valid @RequestBody JSONObject jsonObject){
        Map<String,Object> map = new HashMap<>();
        Long postId = jsonObject.getLong("post_id");
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PostVote lastPostVote;
        if(postId == null) map.put("result",false);
        if(user == null) map.put("result",false);
        if(map.isEmpty()){
            List<PostVote> lastPostVotes = postVoteRepository.findPostVote(post,user);
            if(!lastPostVotes.isEmpty()){
                lastPostVote = lastPostVotes.get(0);
                if(lastPostVote.getValue().equals("-1")){
                    map.put("result",false);
                }else{
                    lastPostVote.setValue("-1");
                    postVoteRepository.save(lastPostVote);
                    map.put("result",true);
                }
            }else{
                PostVote postVote = new PostVote();
                postVote.setPost(post);
                postVote.setUser(user);
                postVote.setValue("-1");
                postVote.setTime(sdf.format(System.currentTimeMillis()));
                postVoteRepository.save(postVote);
                map.put("result",true);
            }
        }
        return map;
    }

    private Map<String, Object> getMapResponseForm(Page<Post> page) {
        long postCount = page.getTotalElements();
        ArrayList<Map> arrayList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        page.forEach(post -> {
            try {
                arrayList.add(post.getMapResponse());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        if (!page.isEmpty()) {
            map.put("count", postCount);
            map.put("posts", arrayList);
        } else {
            map.put("count", 0);
            map.put("posts", new ArrayList());
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

    private void checkOffsetAndLimit(Integer offset, Integer limit) {
        if (offset == null | limit == null) {
            offset = 0;
            limit = 10;
        } else {
            offset /= limit;
        }
        offsetInt = offset;
        limitInt = limit;
    }
}
