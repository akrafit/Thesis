package main.repo;

import main.model.Post;
import main.model.PostVote;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostVoteRepository extends CrudRepository<PostVote,Long> {
}