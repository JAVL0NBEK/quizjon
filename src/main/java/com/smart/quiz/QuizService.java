package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import java.util.List;

public interface QuizService {

  List<QuestionsEntity> getAllQuestions();

  QuestionResponseDto getQuestionById(Long id);

  QuestionsEntity addQuestion(QuestionsEntity question);

  boolean isOptionCorrect(Long optionId);
}
