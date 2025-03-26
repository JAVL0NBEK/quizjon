package com.smart.quiz;

import com.smart.quiz.dto.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<UserEntity, Long> {

  boolean existsByChatId(Long chatId);

  @EntityGraph(attributePaths = "subjects")
  Optional<UserEntity> findByChatId(Long chatId);

}
