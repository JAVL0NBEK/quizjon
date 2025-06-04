package com.smart.quiz;

import com.smart.quiz.dto.StatsEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StatsRepository extends JpaRepository<StatsEntity, Long> {

  @Query("""
  select s from StatsEntity s
    where s.userId = :userId
  """)
  List<StatsEntity> getByUserId(Long userId);

}
