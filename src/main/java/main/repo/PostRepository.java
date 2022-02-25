package main.repo;

import main.model.Post;
import main.model.PostComment;
import main.model.User;
import main.model.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends CrudRepository<Post,Long> {
    @Query("select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED'")
    long countAllActivePosts();

    Page<Post> findByIsActiveAndModerationStatus(int isActive, ModerationStatus moderationStatus, Pageable pageable);

    @Query(value = "SELECT * FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED' " +
            "group by posts.id order by (SELECT count(post_id) FROM post_comments where post_id = posts.id) desc",
            countQuery = "SELECT count(*) FROM posts",
            nativeQuery = true)
    Page <Post> findPostsWithPagination(Pageable pageable);

    @Query(value = "SELECT * FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED' " +
            "group by posts.id order by (SELECT count(post_id) FROM post_votes where post_id = posts.id) desc",
            countQuery = "SELECT count(*) FROM posts",
            nativeQuery = true)
    Page <Post> findPostsWithPaginationBest(Pageable pageable);
}
