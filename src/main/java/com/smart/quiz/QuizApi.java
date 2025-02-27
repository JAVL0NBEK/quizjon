package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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

  @PostMapping(value = "/upload-document",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file);

  @PutMapping("/{id}")
  ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody QuestionsEntity requestDto);

}
