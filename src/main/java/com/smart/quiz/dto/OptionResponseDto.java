package com.smart.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionResponseDto {
  private Long id;
  private String optionText;
  private boolean isCorrect;
}
