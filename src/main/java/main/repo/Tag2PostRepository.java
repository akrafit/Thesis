package main.repo;

import main.model.Tag2Post;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface Tag2PostRepository extends CrudRepository<Tag2Post,Long> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM tag2post WHERE tag2post.post_id=:id",
            nativeQuery = true)
    void deleteTag2PostWithPostId(@Param("id")Long id);
}
