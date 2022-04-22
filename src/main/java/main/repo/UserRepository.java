package main.repo;

import main.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends CrudRepository<User,Long> {


    @Query("select u from User u where u.email = :email")
    User findByEmail(@Param("email")String email);

    User getOne(Long id);

    @Query("select u from User u where u.code = :code")
    User findByCode(@Param("code")String code);
}
