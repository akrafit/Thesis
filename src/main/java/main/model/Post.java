package main.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import main.model.enums.ModerationStatus;

import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "posts")
@NoArgsConstructor
public class Post {
    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private List<PostVote> postsVote;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private List<Tag2Post> tag2Posts;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "post_id", nullable = false, insertable = false, updatable = false)
    private List<PostComment> postComments;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    private int isActive;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Column(name = "moderation_status", length = 32, columnDefinition = "varchar(32) default 'NEW'")
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

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Post(String title, String text, String active, String date) {
        this.moderationStatus = ModerationStatus.NEW;
        this.title = title;
        this.text = text;
        this.isActive = Integer.parseInt(active);
        this.time = date;
    }
}

