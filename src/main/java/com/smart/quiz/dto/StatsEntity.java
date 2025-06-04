package com.smart.quiz.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stats")
@RequiredArgsConstructor
@AllArgsConstructor
public class StatsEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "subject_id", nullable = false)
  private Long subjectId;

  @Column(name = "subject_name", nullable = false)
  private String subjectName;

  @Column(name = "current_section", nullable = false)
  private String currentSection;

  @Column(name = "total_question", nullable = false)
  private Long totalQuestions;

  @Column(name = "correct_answer_count", nullable = false)
  private Long correctAnswersCount;

  @Column(name = "wrong_answer_count", nullable = false)
  private Long wrongAnswersCount;

  @Column(name = "correct_percentage", nullable = false)
  private String correctPercentage;

  @Column(name = "created_at", nullable = false)
  private LocalDate createdAt;
}
