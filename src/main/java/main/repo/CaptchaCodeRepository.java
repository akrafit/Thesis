package main.repo;

import main.model.CaptchaCode;
import main.model.Post;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaptchaCodeRepository extends CrudRepository<CaptchaCode,Long> {
}
