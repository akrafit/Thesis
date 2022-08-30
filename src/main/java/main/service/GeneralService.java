package main.service;

import main.Main;
import main.config.ApiInit;
import main.model.*;
import main.model.enums.ModerationStatus;
import main.repo.*;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GeneralService {

    @Value("${spring.servlet.multipart.max-file-size}")
    String maxFileSize;
    public static final String POSTS_COUNT = "postsCount";
    public static final String LIKES_COUNT = "likesCount";
    public static final String DISLIKES_COUNT = "dislikesCount";
    public static final String VIEWS_COUNT = "viewsCount";
    public static final String FIRST_PUBLICATION = "firstPublication";
    public static final String STATISTICS_IS_PUBLIC = "STATISTICS_IS_PUBLIC";
    public static final String LETTERS = "abcdefghijklmnopqrstuvwxyz";
    public static final String SHORT_PASSWORD = "Пароль короче 6-ти символов";
    public static final String UPLOAD_USER = "/upload/user/";
    public static final String NO_AUTH = "Пользователь не авторизован";
    public static final String BUSY = "Этот e-mail уже зарегистрирован";
    public static final String IMAGE = "image";
    public static final String NOT_VALID = "Не допустимое расширение";
    public static final String LARGE_IMAGE = "Размер файла превышает допустимый размер";
    public static final String NOT_AUTH = "Пользователь не авторизован";
    public static final String YEARS = "years";
    public static final String POSTS = "posts";
    public static final String NAME = "name";
    public static final String TAGS = "tags";
    public static final String TITLE_ERROR = "Заголовок не установлен";
    public static final String SHORT_COMMENT = "Текст комментария не задан или слишком короткий";
    private final static String WEIGHT = "weight";
    public static final String UPLOAD = "/upload/";
    public static final String PARENT_ID = "parent_id";
    public static final String POST_ID = "post_id";
    public static final String TEXT = "text";
    public static final String AUTH = "Пользователь не авторизован";
    private final static String RESULT = "result";
    private final static String PASSWORD = "password";
    private final static String ERRORS = "errors";
    public static final String ID = "id";
    public static final String DECISION = "decision";
    public static final String ACCEPT = "accept";
    public static final String DECLINE = "decline";
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static String YES = "YES";
    private final static String NO = "NO";
    private final static String TITLE = "title";
    private final static String SUBTITLE = "subtitle";
    private final static String PHONE = "phone";
    private final static String EMAIL = "email";
    private final static String COPYRIGHT = "copyright";
    private final static String COPYRIGHT_FORM = "copyrightFrom";

    private static ApiInit apiInit;
    private final GlobalSettingRepository globalSettingRepository;
    private final ObjectFactory<HttpSession> httpSessionFactory;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostCommentRepository postCommentRepository;

    public GeneralService(ApiInit apiInit, GlobalSettingRepository globalSettingRepository, ObjectFactory<HttpSession> httpSessionFactory, UserRepository userRepository, PostRepository postRepository, TagRepository tagRepository, PostCommentRepository postCommentRepository) {
        this.apiInit = apiInit;
        this.globalSettingRepository = globalSettingRepository;
        this.httpSessionFactory = httpSessionFactory;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.postCommentRepository = postCommentRepository;
    }


    public Map<String, String> apiInit() {
        HashMap<String, String> map = new HashMap<>();
        map.put(TITLE, apiInit.getTitle());
        map.put(SUBTITLE, apiInit.getSubtitle());
        map.put(PHONE, apiInit.getPhone());
        map.put(EMAIL, apiInit.getEmail());
        map.put(COPYRIGHT, apiInit.getCopyright());
        map.put(COPYRIGHT_FORM, apiInit.getCopyrightFrom());
        return map;
    }

    public Map<String, Boolean> apiSettings() {
        HashMap<String, Boolean> map = new HashMap<>();
        Iterable<GlobalSetting> globalSettings = globalSettingRepository.findAll();
        globalSettings.forEach(globalSetting -> {
            map.put(globalSetting.getCode(), globalSetting.getValue().equals(YES));
            Main.globalSettings.put(globalSetting.getCode(), globalSetting.getValue().equals(YES));
        });
        return map;
    }

    public void updateSettings(Map<String, Object> map) {
        User user = getAuthorizedUser();
        if (user != null) {
            if (user.getIsModerator() == 1) {
                Iterable<GlobalSetting> globalSettings = globalSettingRepository.findAll();
                globalSettings.forEach(gs -> {
                    Boolean value = (Boolean) map.get(gs.getCode());
                    gs.setValue(value ? YES : NO);
                    Main.globalSettings.put(gs.getCode(), value);
                });
                globalSettingRepository.saveAll(globalSettings);
            }
        }
    }

    public Map<String, Object> getCalendar() throws ParseException {
        Map<String, Object> returnMap = new HashMap<>();
        Map<String, Object> posts = new HashMap<>();
        List<Object[]> countList = postRepository.countAllPostGroupByDay();
        for (Object[] ob : countList) {
            String data = ob[0].toString().substring(0,10);
            posts.put(data, ob[1]);
        }
        ArrayList<Integer> year = new ArrayList<>();
        List<String> yearBefore = postRepository.AllPostGroupByYear();
        yearBefore.forEach(s -> {
            year.add(Integer.valueOf(s));
        });
        returnMap.put(YEARS, year);
        returnMap.put(POSTS, posts);
        return returnMap;
    }

    public Map<String, Object> getApiTag() {
        double postCount = (double) postRepository.countAllActivePosts();
        List<Map> response = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        List<Tag> tags = tagRepository.findAllTag();
        List<Tag> newTags = new ArrayList<>();
        AtomicBoolean write = new AtomicBoolean(false);
        if (!tags.isEmpty()) {

            tags.forEach(tag -> {
                tag.getTag2Posts().forEach(tag2Post -> {
                    Post post = tag2Post.getPost();
                    if (post.getModerationStatus().equals(ModerationStatus.ACCEPTED) && post.getIsActive() == 1){
                        write.set(true);
                    }
                });
                if(write.get()){
                    newTags.add(tag);
                    write.set(false);
                }

            });

            Tag maxValueTag = newTags.stream().max(Comparator.comparing(tag -> tag.getTag2Posts().size())).get();
            double dWeightMax = 1 / (maxValueTag.getTag2Posts().size() / postCount);
            newTags.forEach(tag -> {
                Map<String, Object> mapForArray = new HashMap<>();
                String name = tag.getName();
                Double weight = tag.getTag2Posts().size() * dWeightMax / postCount;
                mapForArray.put(NAME, name);
                if (weight < 0.30) {
                    weight = 0.4;
                }
                mapForArray.put(WEIGHT, weight);
                response.add(mapForArray);
            });
            map.put(TAGS, response);
        } else {
            map.put(TAGS, null);
        }
        return map;
    }

    public ResponseEntity<Object> uploadImage(MultipartFile image) throws IOException, NoSuchAlgorithmException {
        User user = getAuthorizedUser();
        Map<String, Object> checkedImage = checkImage(image, user);
        if (!checkedImage.isEmpty()) {
            return new ResponseEntity<>(checkedImage, HttpStatus.BAD_REQUEST);
        }
        String tripleFolder = UPLOAD + generateTripleFolder();
        Path path = Paths.get(tripleFolder);
        Files.createDirectories(path);
        File myFile = new File("/.." + tripleFolder + image.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(myFile);
        fos.write(image.getBytes());
        fos.close();
        return new ResponseEntity<>(tripleFolder + image.getOriginalFilename(), HttpStatus.OK);
    }

    public Map<String, Object> addComment(Map<String, Object> objectMap) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        String pId = objectMap.get(PARENT_ID).toString();
        int parentId = 0;
        if(pId != null){
           parentId = Integer.parseInt(pId);
        }
        Long postId = (Long) objectMap.get(POST_ID);
        String text = objectMap.get(TEXT).toString();
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        String cleanText = text.replaceAll("<.*?>", "");
        if (user == null){
            errors.put(TEXT, AUTH);
        }
        if (cleanText.isEmpty()){
            errors.put(TEXT, TITLE_ERROR);
        }
        if (cleanText.length() < 3){
            errors.put(TEXT, SHORT_COMMENT);
        }

        if (!errors.isEmpty()) {
            map.put(RESULT, false);
            map.put(ERRORS, errors);
            return map;
        }
        PostComment postComment = new PostComment(SIMPLE_DATE_FORMAT.format(System.currentTimeMillis()), text, post, user);
        if (parentId > 0){
            postComment.setParentId(parentId);
        }
        postCommentRepository.save(postComment);
        map.put(ID, postComment.getId());
        return map;
    }

    public Map<String, Object> postModeration(Map<String, Object> objectMap) {
        Map<String, Object> map = new HashMap<>();
        Long id = Long.parseLong(String.valueOf(objectMap.get(POST_ID)));
        String decision = objectMap.get(DECISION).toString();
        Post post = postRepository.getOne(id);
        User user = getAuthorizedUser();
        assert user != null;
        assert post != null;
        if (user.getIsModerator() == 1) {
            post.setModeratorId(user.getId());
            if (decision.equals(ACCEPT)) {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
            }
            if (decision.equals(DECLINE)) {
                post.setModerationStatus(ModerationStatus.DECLINED);
            }
        } else {
            map.put(RESULT, false);
            return map;
        }
        postRepository.save(post);
        map.put(RESULT, true);
        return map;
    }

    public Map<String, Object> userStatistics() throws ParseException {
        Map<String, Object> map = new HashMap<>();
        User user = getAuthorizedUser();
        assert user != null;
        List<Post> userPost = user.getPostsAuthor();
        if(userPost.size() > 0) {
            map.put(POSTS_COUNT, userPost.size());
            long likesCount = userPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count()).sum();
            long disLikesCount = userPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count()).sum();
            long viewsCount = userPost.stream().mapToLong(Post::getViewCount).sum();
            map.put(LIKES_COUNT, likesCount);
            map.put(DISLIKES_COUNT, disLikesCount);
            map.put(VIEWS_COUNT, viewsCount);
            String firstPost = postRepository.findFirstPostUser(user.getId());
            Long firstPostTime = SIMPLE_DATE_FORMAT.parse(firstPost).getTime() / 1000;
            map.put(FIRST_PUBLICATION, firstPostTime);
            return map;
        }else{
            map.put(POSTS_COUNT, 0);
            map.put(LIKES_COUNT, 0);
            map.put(DISLIKES_COUNT, 0);
            map.put(VIEWS_COUNT, 0);
            map.put(FIRST_PUBLICATION, 0);
            return map;
        }
    }

    public ResponseEntity<Map> allStatistics() throws ParseException {
        Map<String, Object> map = new HashMap<>();
        if (!Main.globalSettings.get(STATISTICS_IS_PUBLIC)) {
            User user = getAuthorizedUser();
            if (user == null) return new ResponseEntity<>(HttpStatus.valueOf(401));
            if (user.getIsModerator() != 1) return new ResponseEntity<>(HttpStatus.valueOf(401));
        }
        List<Post> allPost = postRepository.getAllPost();
        map.put(POSTS_COUNT, allPost.size());
        long likesCount = allPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count()).sum();
        long disLikesCount = allPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count()).sum();
        long viewsCount = allPost.stream().mapToLong(Post::getViewCount).sum();
        map.put(LIKES_COUNT, likesCount);
        map.put(DISLIKES_COUNT, disLikesCount);
        map.put(VIEWS_COUNT, viewsCount);
        String firstPost = postRepository.findFirstPost();
        Long firstPostTime = SIMPLE_DATE_FORMAT.parse(firstPost).getTime() / 1000;
        map.put(FIRST_PUBLICATION, firstPostTime);
        return new ResponseEntity<>(map, HttpStatus.OK);
    }


    private String generateTripleFolder() {
        String letters = LETTERS;
        String[] folder = new String[6];
        for (int i = 0; i < 6; i++) {
            Random random = new Random();
            int j = random.nextInt(letters.length());
            char ch = letters.charAt(j);
            folder[i] = String.valueOf(ch);
        }
        return folder[0] + folder[1] + "/" + folder[2] + folder[3] + "/" + folder[4] + folder[5] + "/";
    }

    public Map<String, Object> changeProfile(String name, String email, String password, String removePhoto, MultipartFile photo)
            throws NoSuchAlgorithmException, IOException {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        User user = getAuthorizedUser();
        User userControl = null;
        if (user != null) {
            if (name != null) {
                user.setName(name);
            }
            if (email != null) {
                userControl = userRepository.findByEmail(email);
                if (userControl != null) {
                    if (userControl.getId().equals(user.getId())) userControl = null;
                } else {
                    user.setEmail(email);
                }
            }
            if (password != null) {
                if (password.trim().length() < 6) {
                    errors.put(PASSWORD, SHORT_PASSWORD);
                } else {
                    user.setPassword(AuthService.getHashCode(password));
                }
            }
            if (photo != null) {
                Map<String, Object> checkedImage = checkImage(photo, user);
                if (!checkedImage.isEmpty()) {
                    return checkedImage;
                }
                String tripleFolder = UPLOAD_USER + user.getId().toString() + "/";
                Path path = Paths.get(tripleFolder);
                Files.createDirectories(path);
                File myFile = new File("/.." + tripleFolder + photo.getOriginalFilename());
                resizeImageAndWrite(photo, myFile);
                user.setPhoto(tripleFolder + photo.getOriginalFilename());
            }
            if (removePhoto != null) {
                if (removePhoto.equals("1")) user.setPhoto("");
            }
        } else {
            errors.put(EMAIL, NO_AUTH);
        }
        if (userControl != null) errors.put(EMAIL, BUSY);
        if (!errors.isEmpty()) {
            map.put(RESULT, false);
            map.put(ERRORS, errors);
            return map;
        }
        map.put(RESULT, true);
        userRepository.save(user);
        return map;
    }

    private void resizeImageAndWrite(MultipartFile photo, File newFile) {
        BufferedImage image;
        try {
            image = ImageIO.read(photo.getInputStream());
            BufferedImage result = Scalr.resize(image, 36, 36);
            String imageExtension = Objects.requireNonNull(photo.getOriginalFilename()).replaceAll(".+\\.", "");
            ImageIO.write(result, imageExtension, newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Map<String, Object> checkImage(MultipartFile image, User user) {
        long imageSize = image.getSize();
        Long maxImageSize = checkDigit(maxFileSize);
        String imageExtension = Objects.requireNonNull(image.getOriginalFilename()).replaceAll(".+\\.", "");
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        switch (imageExtension) {
            case ("jpg"):
            case ("jpeg"):
            case ("png"):
                break;
            default:
                errors.put(IMAGE, NOT_VALID);
                break;
        }
        if (imageSize > maxImageSize) {
            errors.put(IMAGE, LARGE_IMAGE);
        }

        if (user == null) {
            errors.put(IMAGE, NOT_AUTH);
        }
        if (!errors.isEmpty()) {
            map.put(RESULT, false);
            map.put(ERRORS, errors);
        }
        return map;
    }

    public static Long checkDigit(String content) {
        return Long.parseLong(content.replaceAll("[^0-9]", "")) * 1000000;
    }

    private User getAuthorizedUser() {
        Long id = Long.valueOf(Main.session.getOrDefault(httpSessionFactory.getObject().getId(), 0));
        if (id > 0) {
            return userRepository.getOne(id);
        }
        return null;
    }
}
