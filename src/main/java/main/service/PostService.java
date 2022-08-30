package main.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import main.Main;
import main.model.*;
import main.model.enums.ModerationStatus;
import main.repo.*;
import main.specification.PostSpecification;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PostService {
    public static final String POPULAR = "popular";
    public static final String BEST = "best";
    public static final String EARLY = "early";
    public static final String TIME = "time";
    public static final String COUNT = "count";
    public static final String POSTS = "posts";
    public static final String INACTIVE = "inactive";
    public static final String PENDING = "pending";
    public static final String DECLINED = "declined";
    public static final String ACCEPTED = "accepted";
    public static final String TIMESTAMP = "timestamp";
    public static final String ACTIVE = "active";
    public static final String TITLE = "title";
    public static final String TAGS = "tags";
    public static final String TEXT = "text";
    public static final String NOT_AUTH = "Пользователь не авторизован";
    public static final String TITLE_NOT_INSTALLED = "Заголовок не установлен";
    public static final String TITLE_IS_SHORT = "Заголовок публикации слишком короткий";
    public static final String TEXT_IS_SHORT = "Текст публикации слишком короткий";
    public static final String POST_PREMODERATION = "POST_PREMODERATION";
    public static final String RESULT = "result";
    public static final String ERRORS = "errors";
    public static final String POST_ID = "post_id";
    public static final String ID = "id";
    public static final String USER = "user";
    public static final String LIKE_COUNT = "likeCount";
    public static final String DISLIKE_COUNT = "dislikeCount";
    public static final String VIEW_COUNT = "viewCount";
    public static final String COMMENTS = "comments";
    public static final String ANNOUNCE = "announce";
    public static final String COMMENT_COUNT = "commentCount";
    private int offsetInt;
    private int limitInt;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final Tag2PostRepository tag2PostRepository;
    private final PostVoteRepository postVoteRepository;
    final ObjectFactory<HttpSession> httpSessionFactory;
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static SimpleDateFormat DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd");

    public PostService(PostRepository postRepository, UserRepository userRepository, TagRepository tagRepository, Tag2PostRepository tag2PostRepository, PostVoteRepository postVoteRepository, ObjectFactory<HttpSession> httpSessionFactory) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.tag2PostRepository = tag2PostRepository;
        this.postVoteRepository = postVoteRepository;
        this.httpSessionFactory = httpSessionFactory;
    }

    public Map<String, Object> getApiPost(Integer offset, Integer limit, String mode) {
        Page<Post> page;
        checkOffsetAndLimit(offset, limit);
        switch (mode) {
            case (POPULAR):
                Pageable popular = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findPostsWithPagination(popular);
                break;
            case BEST:
                Pageable best = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findPostsWithPaginationBest(best);
                break;
            case (EARLY):
                Pageable early = PageRequest.of(offsetInt, limitInt, Sort.Direction.ASC, TIME);
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, early);
                break;
            default:
                Pageable pageable = PageRequest.of(offsetInt, limitInt, Sort.Direction.DESC, TIME);
                page = postRepository.findByIsActiveAndModerationStatus(1, ModerationStatus.ACCEPTED, pageable);
                break;
        }
        return getMapResponseForm(page);
    }

    public ResponseEntity<Map> getPost(Long id) {
        Post post = postRepository.getOne(id);
        if (id != null || post != null) {
            try {
                User user = getAuthorizedUser();
                if (user != null) {
                    if (!user.getId().equals(post.getUser().getId()) & user.getIsModerator() == 0) {
                        postRepository.plusOneToVisit(post.getId());
                    }
                } else {
                    postRepository.plusOneToVisit(post.getId());
                }
                return new ResponseEntity<>(getSinglePost(post), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    public Map<String, Object> getApiPostQuery(Integer offset, Integer limit, String query) {
        Map<String, Object> map = new HashMap<>();
        Page<Post> page;
        long postCount;
        checkOffsetAndLimit(offset, limit);
        if (query.length() > 2) {
            List<String> strings = List.of(query.trim().split(" "));
            List<String> searchList = new ArrayList<>();
            strings.forEach(c -> {
                String value = c.trim().replaceAll("[^A-Za-zА-Яа-я0-9]", "");
                if (value.length() > 2) searchList.add(value);
            });
            ArrayList<Map> arrayList = new ArrayList<>();
            if (searchList.size() == 1) {
                Pageable pageable = PageRequest.of(offsetInt, limitInt);
                page = postRepository.findByText(searchList.get(0), pageable);
                postCount = page.getTotalElements();
                page.forEach(post -> {
                    try {
                        arrayList.add(getMapResponse(post));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                page = getPostsWhereTextContainsAnyWord(searchList, offsetInt, limitInt);
                postCount = page.getTotalElements();
                page.forEach(post -> {
                    try {
                        arrayList.add(getMapResponse(post));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
            }
            map.put(COUNT, postCount);
            map.put(POSTS, arrayList);

        } else {
            map.put(COUNT, 0);
            map.put(POSTS, new ArrayList());
        }
        return map;
    }

    public Map<String, Object> getPostMy(Integer offset, Integer limit, String status) {
        checkOffsetAndLimit(offset, limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Page<Post> page = null;
        User user;
        String session = httpSessionFactory.getObject().getId();
        Long userID = Long.valueOf(Main.session.getOrDefault(session, 0));
        if (userID != 0 || !Main.session.isEmpty()) {
            user = userRepository.getOne(userID);
            switch (status) {
                case INACTIVE:
                    page = postRepository.findPostsByMy(user.getId(), 0, String.valueOf(ModerationStatus.NEW), pageable);
                    break;
                case PENDING:
                    page = postRepository.findPostsByMy(user.getId(), 1, String.valueOf(ModerationStatus.NEW), pageable);
                    break;
                case DECLINED:
                    page = postRepository.findPostsByMy(user.getId(), 1, String.valueOf(ModerationStatus.DECLINED), pageable);
                    break;
                default:
                    page = postRepository.findPostsByMy(user.getId(), 1, String.valueOf(ModerationStatus.ACCEPTED), pageable);
                    break;
            }
        }
        assert page != null;
        return getMapResponseForm(page);
    }

    public Page<Post> getPostsWhereTextContainsAnyWord(List<String> words, int offset, int limit) {
        Specification<Post> specification = null;
        for (String word : words) {
            Specification<Post> wordSpecification = PostSpecification.textContains(word);
            if (specification == null) {
                specification = wordSpecification;
            } else {
                specification = specification.or(wordSpecification);
            }
        }
        return postRepository.findAll(specification, PageRequest.of(offset, limit));
    }

    public Map<String, Object> getApiPostByDate(Integer offset, Integer limit, String date) throws ParseException {
        checkOffsetAndLimit(offset, limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(DATE_FORMAT.parse(date));
        endDate.add(Calendar.DAY_OF_MONTH, 1);
        String end = DATE_FORMAT.format(endDate.getTime());
        Page<Post> page = postRepository.findByDate(date, end, pageable);
        return getMapResponseForm(page);
    }

    public Map<String, Object> getPostModeration(Integer offset, Integer limit, String status) throws ParseException {
        checkOffsetAndLimit(offset, limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Page<Post> page;
        switch (status) {
            case DECLINED:
                page = postRepository.findPostsByModeration(1, String.valueOf(ModerationStatus.DECLINED), pageable);
                break;
            case ACCEPTED:
                page = postRepository.findPostsByModeration(1, String.valueOf(ModerationStatus.ACCEPTED), pageable);
                break;
            default:
                page = postRepository.findPostsByModeration(1, String.valueOf(ModerationStatus.NEW), pageable);
                break;
        }
        return getMapResponseForm(page);
    }

    public Map<String, Object> getApiPostByTag(Integer offset, Integer limit, String tag) {
        checkOffsetAndLimit(offset, limit);
        Pageable pageable = PageRequest.of(offsetInt, limitInt);
        Tag tagName = tagRepository.getTagByName(tag);
        Page<Post> page = postRepository.findByTag(tagName.getId(), pageable);
        return getMapResponseForm(page);
    }

    public Map<String, Object> addPost(JSONObject jsonObject) throws NoSuchAlgorithmException {
        String timestamp = jsonObject.get(TIMESTAMP).toString();
        String active = jsonObject.get(ACTIVE).toString();
        String title = jsonObject.get(TITLE).toString();
        JSONArray tags = jsonObject.getJSONArray(TAGS);
        String text = jsonObject.get(TEXT).toString();
        User user = getAuthorizedUser();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        long nowTime = System.currentTimeMillis();
        long timestampLong = Long.parseLong(timestamp) * 1000;
        if (user == null){
            errors.put(TITLE, NOT_AUTH);
        }
        if (title.isEmpty()) {
            errors.put(TITLE, TITLE_NOT_INSTALLED);
        }
        if (title.length() < 3){
            errors.put(TITLE, TITLE_IS_SHORT);
        }
        if (text.length() < 50) {
            errors.put(TEXT, TEXT_IS_SHORT);
        }
        if (nowTime < timestampLong) {
            timestampLong = nowTime;
        }
        if (errors.isEmpty()) {
            Post post = new Post(title, text, active, SIMPLE_DATE_FORMAT.format(timestampLong));
            if (!Main.globalSettings.get(POST_PREMODERATION)) {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
            }
            post.setUser(user);
            List<Tag2Post> tag2Posts = new ArrayList<>();
            tags.forEach(t -> {
                Tag tag = tagRepository.getTagByName(t.toString());
                if (tag == null) {
                    tag = new Tag(t.toString().trim());
                    tagRepository.save(tag);
                }
                Tag2Post tag2Post = new Tag2Post(tag, post);
                tag2Posts.add(tag2Post);
            });
            post.setTag2Posts(tag2Posts);
            postRepository.save(post);
            map.put(RESULT, true);
        } else {
            map.put(RESULT, false);
            map.put(ERRORS, errors);
        }
        return map;
    }

    public Map<String, Object> updatePost(JSONObject jsonObject, Long id) throws NoSuchAlgorithmException {
        String timestamp = jsonObject.get(TIMESTAMP).toString();
        int active = jsonObject.getInteger(ACTIVE);
        String title = jsonObject.get(TITLE).toString();
        JSONArray tags = jsonObject.getJSONArray(TAGS);
        String text = jsonObject.get(TEXT).toString();
        User user = getAuthorizedUser();
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> errors = new HashMap<>();
        long nowTime = System.currentTimeMillis();
        long timestampLong = Long.parseLong(timestamp) * 1000;
        if (user == null) {
            errors.put(TITLE, NOT_AUTH);
        }
        if (title.isEmpty()){
            errors.put(TITLE, TITLE_NOT_INSTALLED);
        }
        if (title.length() < 3){
            errors.put(TITLE, TITLE_IS_SHORT);
        }
        if (text.length() < 50) {
            errors.put(TITLE, TEXT_IS_SHORT);
        }
        if (nowTime < timestampLong) {
            timestampLong = nowTime;
        }
        if (errors.isEmpty()) {
            Post post = postRepository.getOne(id);
            post.setText(text);
            post.setTime(SIMPLE_DATE_FORMAT.format(timestampLong));
            post.setIsActive(active);
            post.setTitle(title);
            tag2PostRepository.deleteTag2PostWithPostId(id);
            List<Tag2Post> tag2Posts = new ArrayList<>();
            tags.forEach(t -> {
                Tag tag = tagRepository.getTagByName(t.toString());
                if (tag == null) {
                    tag = new Tag(t.toString().trim());
                    tagRepository.save(tag);
                }
                Tag2Post tag2Post = new Tag2Post(tag, post);
                tag2Posts.add(tag2Post);
            });
            post.setTag2Posts(tag2Posts);
            if (Main.globalSettings.get(POST_PREMODERATION)) {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
            } else {
                post.setModerationStatus(ModerationStatus.NEW);
            }
            postRepository.save(post);
            map.put(RESULT, true);
        } else {
            map.put(RESULT, false);
            map.put(ERRORS, errors);
        }
        return map;
    }

    public Map<String, Object> like(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        Long postId = jsonObject.getLong(POST_ID);
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        PostVote lastPostVote;
        if (postId == null){
            map.put(RESULT, false);
        }
        if (user == null){
            map.put(RESULT, false);
        }
        if (map.isEmpty()) {
            List<PostVote> lastPostVotes = postVoteRepository.findPostVote(post, user);
            if (!lastPostVotes.isEmpty()) {
                lastPostVote = lastPostVotes.get(0);
                if (lastPostVote.getValue().equals("1")) {
                    map.put(RESULT, false);
                } else {
                    lastPostVote.setValue("1");
                    postVoteRepository.save(lastPostVote);
                    map.put(RESULT, true);
                }
            } else {
                PostVote postVote = new PostVote();
                postVote.setPost(post);
                postVote.setUser(user);
                postVote.setValue("1");
                postVote.setTime(SIMPLE_DATE_FORMAT.format(System.currentTimeMillis()));
                postVoteRepository.save(postVote);
                map.put(RESULT, true);
            }
        }
        return map;
    }

    public Map<String, Object> dislike(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        Long postId = jsonObject.getLong(POST_ID);
        Post post = postRepository.getOne(postId);
        User user = getAuthorizedUser();
        PostVote lastPostVote;
        if (postId == null){
            map.put(RESULT, false);
        }
        if (user == null){
            map.put(RESULT, false);
        }
        if (map.isEmpty()) {
            List<PostVote> lastPostVotes = postVoteRepository.findPostVote(post, user);
            if (!lastPostVotes.isEmpty()) {
                lastPostVote = lastPostVotes.get(0);
                if (lastPostVote.getValue().equals("-1")) {
                    map.put(RESULT, false);
                } else {
                    lastPostVote.setValue("-1");
                    postVoteRepository.save(lastPostVote);
                    map.put(RESULT, true);
                }
            } else {
                PostVote postVote = new PostVote();
                postVote.setPost(post);
                postVote.setUser(user);
                postVote.setValue("-1");
                postVote.setTime(SIMPLE_DATE_FORMAT.format(System.currentTimeMillis()));
                postVoteRepository.save(postVote);
                map.put(RESULT, true);
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
                arrayList.add(getMapResponse(post));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
        if (!page.isEmpty()) {
            map.put(COUNT, postCount);
            map.put(POSTS, arrayList);
        } else {
            map.put(COUNT, 0);
            map.put(POSTS, new ArrayList());
        }
        return map;
    }

    private User getAuthorizedUser() {
        Long id = Long.valueOf(Main.session.getOrDefault(httpSessionFactory.getObject().getId(), 0));
        if (id > 0) {
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

    public static Map<String, Object> getSinglePost(Post singlePost) throws ParseException {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, singlePost.getId());
        map.put(ACTIVE, true);
        map.put(TIMESTAMP, SIMPLE_DATE_FORMAT.parse(singlePost.getTime()).getTime() / 1000);
        map.put(USER, singlePost.getUser().getUserShortMap());
        map.put(TITLE, singlePost.getTitle());
        map.put(TEXT, singlePost.getText());
        long likeCount = singlePost.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count();
        map.put(LIKE_COUNT, likeCount);
        long disLikeCount = singlePost.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count();
        map.put(DISLIKE_COUNT, disLikeCount);
        map.put(VIEW_COUNT, singlePost.getViewCount());
        map.put(COMMENTS, PostComment.getPostCommentsArray(singlePost.getPostComments()));
        ArrayList<String> tags = new ArrayList<>();
        singlePost.getTag2Posts().forEach(tag2Post -> tags.add(tag2Post.getTag().getName()));
        map.put(TAGS, tags);
        return map;
    }

    public Map<String, Object> getMapResponse(Post post) throws ParseException {
        Map<String, Object> map = new HashMap<>();
        map.put(ID, post.getId());
        map.put(TIMESTAMP, SIMPLE_DATE_FORMAT.parse(post.getTime()).getTime() / 1000);
        map.put(USER, post.getUser().getUserShortMap());
        map.put(TITLE, post.getTitle());
        map.put(ANNOUNCE, Jsoup.parse(post.getText()).text());
        long likeCount = post.getPostsVote().stream().filter(p -> p.getValue().equals("1")).count();
        map.put(LIKE_COUNT, likeCount);
        long disLikeCount = post.getPostsVote().stream().filter(p -> p.getValue().equals("-1")).count();
        map.put(DISLIKE_COUNT, disLikeCount);
        map.put(COMMENT_COUNT, post.getPostComments().size());
        map.put(VIEW_COUNT, post.getViewCount());
        return map;
    }
}

