package main.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String name;

    //tags on Tag2Post
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "tag_id", nullable = false)
    private List<Tag2Post> tag2Posts;


}
