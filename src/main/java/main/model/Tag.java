package main.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Table(name = "tags")
@NoArgsConstructor
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private List<Tag2Post> tag2Posts;

    public Tag(String name) {
        this.name = name;
    }
}
