package main.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.cage.Cage;
import main.Main;
import main.model.*;
import main.model.enums.ModerationStatus;
import main.repo.*;
import main.specification.PostSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private ApiInit apiInit;
    private List<Tag> tags;
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    private int offsetInt;
    private int limitInt;

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

    @GetMapping("/calendar")
    public Map<String,Object> getCalendar() throws ParseException {
        Map<String, Object> returnMap = new HashMap<>();
        Map<String, Object> posts = new HashMap<>();
        SimpleDateFormat fromMysql = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfYear = new java.text.SimpleDateFormat("yyyy");
        List<Object[]> countList = postRepository.countAllPostGroupByDay();
        for (Object[] ob : countList){
            String data = sdf.format(fromMysql.parse(String.valueOf(ob[0])).getTime());
            posts.put(data, ob[1]);
        }
        ArrayList<Integer> year = new ArrayList<>();
        List<String> yearBefore = postRepository.AllPostGroupByYear();
        yearBefore.forEach(s -> {
            try {
                year.add(Integer.valueOf(sdfYear.format(fromMysql.parse(String.valueOf(s)).getTime())));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        returnMap.put("years", year);
        returnMap.put("posts", posts);
        return returnMap;
    }

    @GetMapping("/post/search")
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

    @GetMapping("/post/byDate")
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

    @GetMapping("/post/moderation")
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

    @GetMapping("/post/my")
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

    @GetMapping("/post/byTag")
    @ResponseStatus(HttpStatus.OK)
    public  Map<String, Object> getApiPostByTag(Integer offset, Integer limit, String tag) throws ParseException {
        checkOffsetAndLimit(offset,limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Tag tagName = tagRepository.getTagByName(tag);
        Page<Post> page = postRepository.findByTag(tagName.getId(), pageable);
        return getMapResponseForm(page);
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

    @Autowired
    ObjectFactory<HttpSession> httpSessionFactory;
    @GetMapping("/auth/check")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> getApiAuthCheck(){
        String session = httpSessionFactory.getObject().getId();
        int moderationCount = postRepository.countAllPostIsModeration();
        Map<String, Object> map = new HashMap<>();
        Long userID = Long.valueOf(Main.session.getOrDefault(session,0));
        if(userID != 0 || !Main.session.isEmpty()){
            User user = userRepository.getOne(userID);
            map.put("result", true);
            map.put("user", user.getUserForAuth(moderationCount));
        }else{
            map.put("result", false);
        }
        return map;
    }

    @Autowired
    private CaptchaCodeRepository captchaCodeRepository;
    @GetMapping("/auth/captcha")
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
    @PostMapping("/auth/login")
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

    @PostMapping("/auth/register")
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

    @PostMapping("/post")
    public Map<String,Object> addPost(@Valid @RequestBody JSONObject jsonObject) throws NoSuchAlgorithmException{
        String timestamp = jsonObject.get("timestamp").toString();
        String active = jsonObject.get("active").toString();
        String title = jsonObject.get("title").toString();
        JSONArray tags =  jsonObject.getJSONArray("tags");
        String text = jsonObject.get("text").toString();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        User user = getAuthorizedUser();
        //User user = userRepository.getOne(1L);
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
    @PutMapping("/post/{id}")
    public Map<String,Object> updatePost(@Valid @RequestBody JSONObject jsonObject, @PathVariable(value="id") Long id) throws NoSuchAlgorithmException {
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
            Post post = postRepository.getOne(id);
            post.setText(text);
            System.out.println(text);
            post.setTime(sdf.format(timestampLong));
            post.setIsActive(Integer.parseInt(active));
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
            postRepository.save(post);
            map.put("result", true);
        }else{
            map.put("result", false);
            map.put("errors", errors);
        }
    return map;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> uploadImage(@RequestParam("image") MultipartFile image) throws IOException, NoSuchAlgorithmException {
        User user = getAuthorizedUser();
        Map<String,Object> checkedImage = checkImage(image,user);
        if (!checkedImage.isEmpty()){
            return new ResponseEntity<>(checkedImage,HttpStatus.BAD_REQUEST);
        }
        String tripleFolder = "/upload/" + generateTripleFolder();
        Path path = Paths.get(tripleFolder);
        Files.createDirectories(path);
        File myFile = new File("/.." + tripleFolder + image.getOriginalFilename());
        myFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(myFile);
        fos.write(image.getBytes());
        fos.close();
        return new ResponseEntity<>( tripleFolder + image.getOriginalFilename(),HttpStatus.OK);
    }

    private String generateTripleFolder(){
        String letters = "abcdefghijklmnopqrstuvwxyz";
        String[] folder = new String[6];
        for (int i = 0; i < 6; i++){
            Random random = new Random();
            int j = random.nextInt(letters.length());
            char ch = letters.charAt(j);
            folder[i] = String.valueOf(ch);
        }
        return folder[0] + folder[1] + "/" + folder[2] + folder[3] + "/" + folder[4] + folder[5] + "/";
    }


    private User getAuthorizedUser() {
        Long id = Long.valueOf(Main.session.getOrDefault(httpSessionFactory.getObject().getId(),0));
        if(id > 0) {
            return userRepository.getOne(id);
        }
        return null;
    }

    public Map<String, Object> checkImage(MultipartFile image, User user){
        Long imageSize = image.getSize();
        Long maxImageSize = checkDigit(maxFileSize);
        String imageExtension = Objects.requireNonNull(image.getOriginalFilename()).replaceAll(".+\\.","");
        Map<String,Object> map = new HashMap<>();
        Map<String,Object> errors = new HashMap<>();
        switch (imageExtension){
            case("jpg"):
                break;
            case("jpeg"):
                break;
            case("png"):
                break;
            default:
                map.put("result", false);
                errors.put("image", "Не допустимое расширение");
                map.put("errors", errors);
                break;
        }
        if(imageSize > maxImageSize){
            map.put("result", false);
            errors.put("image", "Размер файла превышает допустимый размер");
            map.put("errors", errors);
        }

        if(user == null){
            map.put("result", false);
            errors.put("image", "Пользователь не авторизован");
            map.put("errors", errors);
        }
        return map;
    }

    public static boolean checkText(String content) {
        return content.matches("[а-яА-Яa-zA-Z0-9]+");
    }
    public String getHashCode(String password) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] bytes = md5.digest(password.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        for (Byte b:bytes){
            stringBuilder.append(String.format("%02X",b));
        }
        return String.valueOf(stringBuilder);
    }

    public static Long checkDigit(String content) {
        return Long.parseLong(content.replaceAll("[^0-9]","")) * 1000000;
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
    private Integer[] checkOffsetAndLimit(Integer offset, Integer limit) {
        Integer[] offsetLimit = new Integer[2];
        if (offset == null | limit == null) {
            offset = 0;
            limit = 10;
        } else {
            offset /= limit;
        }
        offsetInt = offset;
        limitInt = limit;
        return offsetLimit;
    }
}
