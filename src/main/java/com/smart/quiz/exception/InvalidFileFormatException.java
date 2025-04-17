package com.smart.quiz.exception;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidFileFormatException extends RuntimeException {
  private final List<String> details;

  public InvalidFileFormatException(String message, List<String> details) {
    super(message + ": " + String.join("; ", details));
    this.details = details;
  }
}
