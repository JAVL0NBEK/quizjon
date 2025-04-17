package com.smart.quiz;

import com.smart.quiz.dto.SubjectEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

  @Query(
      """
      select s from SubjectEntity s
      join s.users u
      where u.chatId = :chatId
      """
  )
  List<SubjectEntity> findAllByChatId(@Param("chatId") Long chatId);

  Optional<SubjectEntity> findBySubjectName(String subjectName);

}
