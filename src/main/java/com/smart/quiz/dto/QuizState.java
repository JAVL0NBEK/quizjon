package com.smart.quiz.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class QuizState {
  // Getter va Setter lar
  private Long subjectId; // Tanlangan fan IDsi
  private Map<String, List<Long>> sections; // Dinamik bo‘limlar
  private String currentSection; // Joriy bo‘lim
  private int currentQuestionIndex; // Joriy savol indeksi
  private int correctAnswersCount; // To‘g‘ri javoblar soni
  private int wrongAnswersCount; // Noto‘g‘ri javoblar soni
  private boolean active; // Quiz faol yoki yo‘qligi

  public QuizState() {
    this.currentQuestionIndex = 0;
    this.correctAnswersCount = 0;
    this.wrongAnswersCount = 0;
    this.active = false;
    this.sections = new HashMap<>();
  }

  public void incrementQuestionIndex() { this.currentQuestionIndex++; }

  public void incrementCorrectAnswers() { this.correctAnswersCount++; }

  public void incrementWrongAnswers() { this.wrongAnswersCount++; }

}