package main.model;

import com.google.gson.Gson;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @Column(nullable = false)
    @GeneratedValue
    private Long id;

    //posts where we moderator
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "moderator_id")
    public List<Post> postsModerator;

    //posts where we author
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private List<Post> postsAuthor;

    //likes & dislikes
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private List<PostVote> postsVote;

    //users comments on post
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private List<PostComment> userPostComments;

    @Column(name = "is_moderator", nullable = false, columnDefinition = "TINYINT(1)")
    private String isModerator;

    @Column(name = "reg_time", columnDefinition = "DATETIME")
    private String regTime;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String name;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String email;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String password;

    @Column(columnDefinition = "varchar(255)")
    private String code;

    @Column(length = 65535, columnDefinition = "text")
    private String photo;

    public Map<String, Object> getUserShortMap() {
        Map<String, Object> userShortMap = new HashMap<>();
        userShortMap.put("id",this.getId());
        userShortMap.put("name",this.getName());
        return userShortMap;
    }

    public Map<String, Object> getUserShortMapPhoto() {
        Map<String, Object> userShortMapPhoto = new HashMap<>();
        userShortMapPhoto.put("id",this.getId());
        userShortMapPhoto.put("name",this.getName());
        //userShortMap.put("photo",this.getPhoto());
        userShortMapPhoto.put("photo","777");
        return userShortMapPhoto;
    }

}

