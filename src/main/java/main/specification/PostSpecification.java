package main.specification;

import main.model.Post;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Expression;

public class PostSpecification {
    public static Specification<Post> textContains(String searchWord) {
        return (root, query, builder) -> {
            Expression<String> searchLowerCase = builder.lower(root.get("text"));
            return builder.like(searchLowerCase, "%" + searchWord.toLowerCase() + "%");
        };
    }
}
