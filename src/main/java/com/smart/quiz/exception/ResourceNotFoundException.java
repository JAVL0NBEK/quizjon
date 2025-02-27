package com.smart.quiz.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
  private final List<String> details;

  public ResourceNotFoundException(String message, List<String> details) {
    super(message);
    this.details = details;
  }

}
