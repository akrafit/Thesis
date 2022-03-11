package main.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "tag2post")
public class Tag2Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private Tag tag;

}
