package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
public class QuizController implements QuizApi {

  private final QuizService quizService;

  @Override
  public ResponseEntity<SubjectEntity> addSubject(SubjectRequestDto subject) {
    return ResponseEntity.ok(quizService.addSubject(subject));
  }

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
  public ResponseEntity<String> uploadFile(MultipartFile file, String subject, String subDesc, Long chatId) {
    try {
      // Faylni qabul qilish va AI orqali ishlash
      quizService.processFile(file, subject, subDesc, chatId);
      return ResponseEntity.ok("Fayl muvaffaqiyatli yuklandi va savollar bazaga saqlandi!");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body("Xatolik: " + e.getMessage());
    }
  }

  @Override
  public ResponseEntity<Void> update(Long id, QuestionsEntity requestDto) {
    log.info("Updating question with id ", id);
    quizService.update(id, requestDto);
    return ResponseEntity.ok().build();
  }

}
