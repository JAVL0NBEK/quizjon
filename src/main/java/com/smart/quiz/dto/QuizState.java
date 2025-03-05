package com.smart.quiz.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizState {
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

  // Getter va Setter lar
  public Long getSubjectId() { return subjectId; }
  public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
  public Map<String, List<Long>> getSections() { return sections; }
  public void setSections(Map<String, List<Long>> sections) { this.sections = sections; }
  public String getCurrentSection() { return currentSection; }
  public void setCurrentSection(String currentSection) { this.currentSection = currentSection; }
  public int getCurrentQuestionIndex() { return currentQuestionIndex; }
  public void setCurrentQuestionIndex(int index) { this.currentQuestionIndex = index; }
  public void incrementQuestionIndex() { this.currentQuestionIndex++; }
  public int getCorrectAnswersCount() { return correctAnswersCount; }
  public void setCorrectAnswersCount(int count) { this.correctAnswersCount = count; }
  public void incrementCorrectAnswers() { this.correctAnswersCount++; }
  public int getWrongAnswersCount() { return wrongAnswersCount; }
  public void setWrongAnswersCount(int count) { this.wrongAnswersCount = count; }
  public void incrementWrongAnswers() { this.wrongAnswersCount++; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }

}