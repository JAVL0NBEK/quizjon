package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @Override
  public ResponseEntity<String> uploadFile(MultipartFile file) {
    try {
      // Faylni qabul qilish va AI orqali ishlash
      quizService.processFile(file);
      return ResponseEntity.ok("Fayl muvaffaqiyatli yuklandi va savollar bazaga saqlandi!");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Xatolik: " + e.getMessage());
    }
  }

}
