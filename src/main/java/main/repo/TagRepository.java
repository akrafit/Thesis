package main.repo;

import main.model.Post;
import main.model.Tag;
import main.model.Tag2Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagRepository extends CrudRepository<Tag,Long> {

    @Query("select t from Tag t")
    List<Tag> findAllTag();


    @Query("select t from Tag t where t.name = :tag")
    Tag getTagByName(@Param("tag")String tag);
}
