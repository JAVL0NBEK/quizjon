package com.smart.quiz;

import com.smart.quiz.dto.OptionsEntity;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionsEntity, Long> {

  @Query("SELECT o FROM OptionsEntity o WHERE o.id = :optionId")
  Optional<OptionsEntity> findOptionById(@Param("optionId") Long optionId);
}
