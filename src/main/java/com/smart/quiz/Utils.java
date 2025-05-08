package com.smart.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Utils {

  public Boolean isQuestionLine(String line){
    // Tartib raqam bilan boshlanadigan holat
    return line != null &&
           (line.trim().matches("^\\d+(\\)|\\.|\\s).*") || line.trim().startsWith("$"));
  }
}
