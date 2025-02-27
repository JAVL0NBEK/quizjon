package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QuizService {

  List<QuestionsEntity> getAllQuestions();

  QuestionResponseDto getQuestionById(Long id);

  QuestionsEntity addQuestion(QuestionsEntity question);

  boolean isOptionCorrect(Long optionId);

  void processFile(MultipartFile file);

  void saveQuestionsToDatabase(List<QuestionResponseDto> questions);

  boolean hasNextQuestion(Long questionId);

  void update(Long id, QuestionsEntity requestDto);
}
