package com.smart.quiz;

import com.smart.quiz.dto.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<UserEntity, Long> {

  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.subjects WHERE u.chatId = :chatId")
  Optional<UserEntity> findByChatIdWithSubjects(@Param("chatId") Long chatId);

}
