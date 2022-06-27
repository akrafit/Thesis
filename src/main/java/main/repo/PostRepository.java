package main.repo;

import main.model.Post;
import main.model.PostComment;
import main.model.User;
import main.model.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PostRepository extends PagingAndSortingRepository<Post,Long>, JpaSpecificationExecutor<Post> {
    @Query("select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED'")
    long countAllActivePosts();

    @Query("select count(p) from Post p where p.moderationStatus = 'NEW'")
    int countAllPostIsModeration();

    @Query(value = "SELECT time, COUNT(id) FROM thesis_db.posts where posts.is_active = 1 and posts.moderation_status = 'ACCEPTED'" +
            " group by time",
            nativeQuery = true)
    List<Object[]> countAllPostGroupByDay();

    @Query(value = "SELECT DATE_FORMAT(time,'%Y') FROM thesis_db.posts GROUP BY DATE_FORMAT(time,'%Y')",
            nativeQuery = true)
    List<String> AllPostGroupByYear();

    Page<Post> findByIsActiveAndModerationStatus(int isActive, ModerationStatus moderationStatus, Pageable pageable);

   // @Query("select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like %:name%")
   // List<Post> findByTexts(@Param("name")String name);

    @Query(value = "select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like concat('%', ?1, '%')",
           countQuery = "select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.text like concat('%', ?1, '%')")
        Page<Post> findByText( String name, Pageable pageable);

    //Page<Post> findByTextContains(String text, Pageable pageable);

    @Query(value = "SELECT * FROM posts where posts.is_active = :active and posts.moderation_status = :status order by time desc",
            countQuery = "SELECT count(*) FROM posts where posts.is_active = :active and posts.moderation_status = :status",
            nativeQuery = true)
    Page<Post> findPostsByModeration(@Param("active")Integer active, @Param("status")String status, Pageable pageable);

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

    @Query(value = "select p from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.time > :date and p.time < :endDate",
            countQuery = "select count(p) from Post p where p.isActive = 1 and p.moderationStatus = 'ACCEPTED' and p.time > :date and p.time < :endDate")
    Page<Post> findByDate(@Param("date")String date, @Param("endDate")String endDate, Pageable pageable);

    @Query(value = "SELECT * FROM posts JOIN tag2post ON tag2post.post_id = posts.id WHERE tag_id = :tag and moderation_status = 'ACCEPTED'",
    countQuery = "SELECT count(*) FROM posts JOIN tag2post ON tag2post.post_id = posts.id WHERE tag_id = :tag and moderation_status = 'ACCEPTED'",
    nativeQuery = true)
    Page<Post> findByTag(@Param("tag")Long tag, Pageable pageable);

    @Query(value = "SELECT * FROM posts where posts.is_active = :active and posts.moderation_status = :status and posts.user_id = :user",
            countQuery = "SELECT count(*) FROM posts where posts.is_active = :active and posts.moderation_status = :status and posts.user_id = :user",
            nativeQuery = true)
    Page<Post> findPostsByMy(@Param("user")Long user, @Param("active")Integer active, @Param("status")String status, Pageable pageable);

    @Transactional
    @Modifying
    @Query(value = "UPDATE posts SET view_count = view_count + 1 WHERE id = :id",
            nativeQuery = true)
    void plusOneToVisit(@Param("id")Long id);

    @Query(value = "SELECT MIN(time) FROM posts WHERE user_id = :id",
            nativeQuery = true)
    String findFirstPostUser(@Param("id")Long id);

    @Query(value = "SELECT MIN(time) FROM posts",
            nativeQuery = true)
    String findFirstPost();

    @Query(value = "select p from Post p")
    List<Post> getAllPost();
}
