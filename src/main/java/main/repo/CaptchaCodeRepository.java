package main.repo;

import main.model.CaptchaCode;
import main.model.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaptchaCodeRepository extends CrudRepository<CaptchaCode,Long> {

    @Query("select c from CaptchaCode c")
    List<CaptchaCode> findAllCaptchaCode();

    @Query("select c from CaptchaCode c where c.code = :captcha")
    CaptchaCode findCaptchaCode(@Param("captcha")String captcha);
}
