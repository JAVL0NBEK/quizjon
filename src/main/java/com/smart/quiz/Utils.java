package com.smart.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Utils {

  public Boolean isQuestionLine(String line){
    // Tartib raqam bilan boshlanadigan holat
    return line.startsWith("$") ||
           line.endsWith("?") ||
           line.endsWith(":") ||
           line.endsWith("...") ||
           line.startsWith("...") ||
           line.contains("__") ||
           line.endsWith("!") ||
           line.matches("^\\d+\\..*");
  }
}
