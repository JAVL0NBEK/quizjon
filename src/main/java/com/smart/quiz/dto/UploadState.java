package com.smart.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UploadState {

  private Document document;
  private String subject;
  private Integer questionCount;

}
