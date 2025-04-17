package com.smart.quiz.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class QuestionParseResult {

  private final List<QuestionResponseDto> questions;
  private final List<String> errors;

  public QuestionParseResult(List<QuestionResponseDto> questions, List<String> errors) {
    this.questions = questions;
    this.errors = errors;
  }

}
