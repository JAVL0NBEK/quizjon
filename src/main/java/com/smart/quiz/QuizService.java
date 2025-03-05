package com.smart.quiz;

import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface QuizService {

  List<QuestionsEntity> getAllQuestions();

  QuestionResponseDto getQuestionById(Long id);

  QuestionsEntity addQuestion(QuestionsEntity question);

  boolean isOptionCorrect(Long optionId);

  void processFile(MultipartFile file, String subject, String subDesc);

  void saveQuestionsToDatabase(List<QuestionResponseDto> questions, String subject, String subDesc);

  boolean hasNextQuestion(Long questionId);

  void update(Long id, QuestionsEntity requestDto);

  List<Long> getAllQuestionIds();

  SubjectEntity addSubject(SubjectRequestDto subjectEntity);

  List<SubjectEntity> getAllSubjects();

  SubjectEntity getSubjectById(Long id);

  List<QuestionsEntity> getQuestionsBySubjectId(Long subjectId);
}
