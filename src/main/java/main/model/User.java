package main.model;

import com.google.gson.Gson;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

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

    public UserShort getUserShort() {

        UserShort userShort = new UserShort(this.id, this.name);
        return userShort;
    }
}

