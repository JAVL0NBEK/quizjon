package com.smart.quiz.dto;

import lombok.Setter;

public class QuizState {
  @Setter
  private String currentSection;
  @Setter
  private int currentQuestionIndex = 0;
  private int correctAnswersCount = 0;
  private int wrongAnswersCount = 0;
  private boolean active = false; // Quiz faol yoki yo‘qligini ko‘rsatadi

  public String getCurrentSection() {
    return currentSection;
  }

  public int getCurrentQuestionIndex() {
    return currentQuestionIndex;
  }

  public void incrementQuestionIndex() {
    this.currentQuestionIndex++;
  }

  public int getCorrectAnswersCount() {
    return correctAnswersCount;
  }

  public void incrementCorrectAnswers() {
    this.correctAnswersCount++;
  }

  public int getWrongAnswersCount() {
    return wrongAnswersCount;
  }

  public void incrementWrongAnswers() {
    this.wrongAnswersCount++;
  }

  public void setCorrectAnswersCount(int correctAnswersCount) {
    this.correctAnswersCount = correctAnswersCount;
  }

  public void setWrongAnswersCount(int wrongAnswersCount) {
    this.wrongAnswersCount = wrongAnswersCount;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

}