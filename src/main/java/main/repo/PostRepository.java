package main.repo;

import main.model.Post;
import main.model.PostComment;
import main.model.User;
import main.model.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends PagingAndSortingRepository<Post,Long>, JpaSpecificationExecutor<Post> {
    @Query("select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED'")
    long countAllActivePosts();

    Page<Post> findByIsActiveAndModerationStatus(int isActive, ModerationStatus moderationStatus, Pageable pageable);

    /*
    @Query("select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and ")
    Page<Post> findByTextAndIsActiveAndModerationStatus(String texts, int isActive, ModerationStatus moderationStatus, Pageable pageable);
*/
    //Page<Post> findByNameStartingWith(String name);
    //Page<Post> findByNameEndingWith(String name);
 /*
    @Query(value = "?1",
            countQuery = "SELECT count(*) FROM posts",
            nativeQuery = true)
    List<Post> findByTexts(String textQuery);

    @Query(value = "SELECT * FROM posts where posts.text like '%' ?1 '%'",
            countQuery = "SELECT count(*) FROM posts",
            nativeQuery = true)
    List<Post> findByTexts(String textQuery);

@Query("select p from Post p where p.text like concat('%', ?1, '%')")
    List<Post> findByText(String name);
*/

    @Query("select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like %:name%")
    List<Post> findByTexts(@Param("name")String name);

    @Query(value = "select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like concat('%', ?1, '%')",
           countQuery = "select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like concat('%', ?1, '%')")
        Page<Post> findByText(@Param("name") String name, Pageable pageable);

    Page<Post> findByTextContains(String text, Pageable pageable);

/*
    @Query("select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like concat('%', ?1, '%')")
    Page<Post> findTextByTextList(Collection<String> text, Pageable pageable);
*/
    @Query(value = "SELECT * FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED' " +
            "group by posts.id order by (SELECT count(post_id) FROM post_comments where post_id = posts.id) desc",
            countQuery = "SELECT count(*) FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED'",
            nativeQuery = true)
    Page<Post> findPostsWithPagination(Pageable pageable);

    @Query(value = "SELECT * FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED' " +
            "group by posts.id order by (SELECT count(post_id) FROM post_votes where post_id = posts.id) desc",
            countQuery = "SELECT count(*) FROM posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED'",
            nativeQuery = true)
    Page<Post> findPostsWithPaginationBest(Pageable pageable);

    Post getOne(Long id);


}
