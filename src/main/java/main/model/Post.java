package main.model;

import lombok.Data;
import main.model.enums.ModerationStatus;


import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue
    private Long id;

    //likes & dislikes
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", nullable = false)
    private List<PostVote> postsVote;

    //tags on post
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", nullable = false)
    private List<Tag2Post> tag2Posts;

    //comments on post
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", nullable = false)
    private List<PostComment> postComments;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private int isActive;

    @Column(name = "moderation_status", length = 32, columnDefinition = "varchar(32) default 'NEW'" )
    @Enumerated(EnumType.STRING)
    private ModerationStatus moderationStatus;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private String time;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String text;

    @Column(name = "view_count", nullable = false, columnDefinition = "int")
    private int viewCount;

    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public ResponsePost getResponse(long postCount) throws ParseException{
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Long id = this.id;
        long timestamp = sdf.parse(this.time).getTime()/1000;
        UserShort user = this.user.getUserShort();
        String title = this.title;
        String announce = this.text;
        int likeCount = this.postsVote.size();
        int dislikeCount = this.postsVote.size();
        int commentCount = this.postComments.size();
        int viewCount = this.viewCount;
        return new ResponsePost(id,timestamp,user,title,announce,likeCount,dislikeCount,commentCount,viewCount);
    }
    public Map<String,Object> getMapResponse(long postCount) throws ParseException{
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String,Object> map = new HashMap<>();
        map.put("id",this.id);
        map.put("timestamp",sdf.parse(this.time).getTime()/1000);
        map.put("user",this.user.getUserShort());
        map.put("title",this.title);
        map.put("announce",this.text);
        map.put("likeCount",this.postsVote.size());
        map.put("dislikeCount",this.postsVote.size());
        map.put("commentCount",this.postComments.size());
        map.put("viewCount",this.viewCount);

        return map;
    }
/*
    public String getJSONObject() throws ParseException {
        String jo = getResponse(postCount).toString();
        System.out.println(jo);
        return jo;
    }

 */
}

