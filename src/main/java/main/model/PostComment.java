package main.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "post_comments")
public class PostComment{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "parent_id", columnDefinition = "int")
    private String parentId;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private String time;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;
}
