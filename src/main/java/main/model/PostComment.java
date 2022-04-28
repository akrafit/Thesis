package main.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Data
@Table(name = "post_comments")
@NoArgsConstructor
public class PostComment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "parent_id", columnDefinition = "int")
    private int parentId;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private String time;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public PostComment(String time, String text, Post post, User user) {
        this.time = time;
        this.text = text;
        this.post = post;
        this.user = user;
    }

    public static ArrayList<Map> getPostCommentsArray(List<PostComment> postComment) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ArrayList<Map> arrayList = new ArrayList<>();
        postComment.forEach(PC -> {
            Long time = null;
            Map<String, Object> map = new HashMap<>();
            try {
                time = sdf.parse(PC.getTime()).getTime() / 1000;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            map.put("id", PC.getId());
            map.put("timestamp", time);
            map.put("text", PC.getText());
            map.put("user", PC.getUser().getUserShortMapPhoto());
            arrayList.add(map);
        });
        return arrayList;
    }

}
