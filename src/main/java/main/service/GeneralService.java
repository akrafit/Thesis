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

@Service
public class GeneralService {

    private static ApiInit apiInit;
    private final GlobalSettingRepository globalSettingRepository;
    private final ObjectFactory<HttpSession> httpSessionFactory;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostCommentRepository postCommentRepository;

    @Value("${spring.servlet.multipart.max-file-size}")
    String maxFileSize;
    SimpleDateFormat sdfFromMysql = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat sdfYear = new java.text.SimpleDateFormat("yyyy");


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
        map.put("title", apiInit.getTitle());
        map.put("subtitle", apiInit.getSubtitle());
        map.put("phone", apiInit.getPhone());
        map.put("email", apiInit.getEmail());
        map.put("copyright", apiInit.getCopyright());
        map.put("copyrightFrom", apiInit.getCopyrightFrom());
        return map;
    }

    public Map<String, Boolean> apiSettings() {
        HashMap<String, Boolean> map = new HashMap<>();
        Iterable<GlobalSetting> globalSettings = globalSettingRepository.findAll();
        globalSettings.forEach(globalSetting -> {
            map.put(globalSetting.getCode(), globalSetting.getValue().equals("YES"));
            Main.globalSettings.put(globalSetting.getCode(), globalSetting.getValue().equals("YES"));
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
                    gs.setValue(value ? "YES" : "NO");
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
        returnMap.put("years", year);
        returnMap.put("posts", posts);
        return returnMap;
    }

    public Map<String, Object> getApiTag() {
        double postCount = (double) postRepository.countAllActivePosts();
        List<Map> response = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        List<Tag> tags = tagRepository.findAllTag();
        if (!tags.isEmpty()) {
            Tag maxValueTag = tags.stream().max(Comparator.comparing(tag -> tag.getTag2Posts().size())).get();
            double dWeightMax = 1 / (maxValueTag.getTag2Posts().size() / postCount);
            tags.forEach(tag -> {
                Map<String, Object> mapForArray = new HashMap<>();
                String name = tag.getName();
                Double weight = tag.getTag2Posts().size() * dWeightMax / postCount;
                mapForArray.put("name", name);
                mapForArray.put("weight", weight);
                response.add(mapForArray);
            });
            map.put("tags", response);
        } else {
            map.put("tags", null);
        }
        return map;
    }

    public ResponseEntity<Object> uploadImage(MultipartFile image) throws IOException, NoSuchAlgorithmException {
        User user = getAuthorizedUser();
        Map<String, Object> checkedImage = checkImage(image, user);
        if (!checkedImage.isEmpty()) {
            return new ResponseEntity<>(checkedImage, HttpStatus.BAD_REQUEST);
        }
        String tripleFolder = "/upload/" + generateTripleFolder();
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
        int parentId = (int) objectMap.get("parent_id");
        Long postId = (Long) objectMap.get("post_id");
        String text = objectMap.get("text").toString();
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        String cleanText = text.replaceAll("<.*?>", "");
        if (user == null){
            errors.put("text", "Пользователь не авторизован");
        }
        if (cleanText.isEmpty()){
            errors.put("text", "Заголовок не установлен");
        }
        if (cleanText.length() < 3){
            errors.put("text", "Текст комментария не задан или слишком короткий");
        }
        if (!errors.isEmpty()) {
            map.put("result", false);
            map.put("errors", errors);
            return map;
        }
        PostComment postComment = new PostComment(sdfFromMysql.format(System.currentTimeMillis()), text, post, user);
        if (parentId > 0){
            postComment.setParentId(parentId);
        }
        postCommentRepository.save(postComment);
        map.put("id", postComment.getId());
        return map;
    }

    public Map<String, Object> postModeration(Map<String, Object> objectMap) {
        Map<String, Object> map = new HashMap<>();
        Long id = Long.parseLong(String.valueOf(objectMap.get("post_id")));
        String decision = objectMap.get("decision").toString();
        Post post = postRepository.getOne(id);
        User user = getAuthorizedUser();
        assert user != null;
        assert post != null;
        if (user.getIsModerator() == 1) {
            post.setModeratorId(user.getId());
            if (decision.equals("accept")) {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
            }
            if (decision.equals("decline")) {
                post.setModerationStatus(ModerationStatus.DECLINED);
            }
        } else {
            map.put("result", false);
            return map;
        }
        postRepository.save(post);
        map.put("result", true);
        return map;
    }

    public Map<String, Object> userStatistics() throws ParseException {
        Map<String, Object> map = new HashMap<>();
        User user = getAuthorizedUser();
        assert user != null;
        List<Post> userPost = user.getPostsAuthor();
        map.put("postsCount", userPost.size());
        long likesCount = userPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count()).sum();
        long disLikesCount = userPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count()).sum();
        long viewsCount = userPost.stream().mapToLong(Post::getViewCount).sum();
        map.put("likesCount", likesCount);
        map.put("dislikesCount", disLikesCount);
        map.put("viewsCount", viewsCount);
        String firstPost = postRepository.findFirstPostUser(user.getId());
        Long firstPostTime = sdfFromMysql.parse(firstPost).getTime() / 1000;
        map.put("firstPublication", firstPostTime);
        return map;
    }

    public ResponseEntity<Map> allStatistics() throws ParseException {
        Map<String, Object> map = new HashMap<>();
        if (!Main.globalSettings.get("STATISTICS_IS_PUBLIC")) {
            User user = getAuthorizedUser();
            if (user == null) return new ResponseEntity<>(HttpStatus.valueOf(401));
            if (user.getIsModerator() != 1) return new ResponseEntity<>(HttpStatus.valueOf(401));
        }
        List<Post> allPost = postRepository.getAllPost();
        map.put("postsCount", allPost.size());
        long likesCount = allPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count()).sum();
        long disLikesCount = allPost.stream().mapToLong(post -> post.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count()).sum();
        long viewsCount = allPost.stream().mapToLong(Post::getViewCount).sum();
        map.put("likesCount", likesCount);
        map.put("dislikesCount", disLikesCount);
        map.put("viewsCount", viewsCount);
        String firstPost = postRepository.findFirstPost();
        Long firstPostTime = sdfFromMysql.parse(firstPost).getTime() / 1000;
        map.put("firstPublication", firstPostTime);
        return new ResponseEntity<>(map, HttpStatus.OK);
    }


    private String generateTripleFolder() {
        String letters = "abcdefghijklmnopqrstuvwxyz";
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
                    errors.put("password", "Пароль короче 6-ти символов");
                } else {
                    user.setPassword(AuthService.getHashCode(password));
                }
            }
            if (photo != null) {
                Map<String, Object> checkedImage = checkImage(photo, user);
                if (!checkedImage.isEmpty()) {
                    return checkedImage;
                }
                String tripleFolder = "/upload/user/" + user.getId().toString() + "/";
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
            errors.put("email", "Пользователь не авторизован");
        }
        if (userControl != null) errors.put("email", "Этот e-mail уже зарегистрирован");
        if (!errors.isEmpty()) {
            map.put("result", false);
            map.put("errors", errors);
            return map;
        }
        map.put("result", true);
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
                errors.put("image", "Не допустимое расширение");
                break;
        }
        if (imageSize > maxImageSize) {
            errors.put("image", "Размер файла превышает допустимый размер");
        }

        if (user == null) {
            errors.put("image", "Пользователь не авторизован");
        }
        if (!errors.isEmpty()) {
            map.put("result", false);
            map.put("errors", errors);
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
