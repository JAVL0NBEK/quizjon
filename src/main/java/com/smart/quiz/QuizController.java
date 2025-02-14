package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuizController implements QuizApi {

  private final QuizService quizService;

  @Override
  public ResponseEntity<List<QuestionsEntity>> getAllQuestions() {
    return ResponseEntity.ok(quizService.getAllQuestions());
  }

  @Override
  public ResponseEntity<QuestionResponseDto> getQuestionById(Long id) {
    return ResponseEntity.ok(quizService.getQuestionById(id));
  }

  @Override
  public ResponseEntity<QuestionsEntity> saveQuestion(QuestionsEntity questionsEntity) {
    return ResponseEntity.ok(quizService.addQuestion(questionsEntity));
  }

}
