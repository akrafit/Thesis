package main.model;

import com.google.gson.Gson;
import lombok.Data;


@Data
public class ResponsePost{
    private Long id;
    private long timestamp;
    private UserShort user;
    private String title;
    private String announce;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    private int viewCount;

    public ResponsePost(Long id, long timestamp, UserShort user, String title, String announce, int likeCount, int dislikeCount, int commentCount, int viewCount) {
        this.id = id;
        this.timestamp = timestamp;
        this.user = user;
        this.title = title;
        this.announce = announce;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
    }
}
