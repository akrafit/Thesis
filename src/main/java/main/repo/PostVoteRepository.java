package main.repo;

import main.model.Post;
import main.model.PostVote;
import main.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostVoteRepository extends CrudRepository<PostVote,Long> {
    @Query(value = "select p from PostVote p where p.post = :post and p.user = :user")
    List<PostVote> findPostVote(@Param("post") Post post, @Param("user") User user);
}
