package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("v1/quiz")
public interface QuizApi {

  @GetMapping("/all")
  ResponseEntity<List<QuestionsEntity>> getAllQuestions();

  @GetMapping("/by-id")
  ResponseEntity<QuestionResponseDto> getQuestionById(
      @RequestParam Long id
  );

  @PostMapping()
  ResponseEntity<QuestionsEntity> saveQuestion(
      @RequestBody QuestionsEntity questionsEntity
  );

}
