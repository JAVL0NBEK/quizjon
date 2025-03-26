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

  void processFile(MultipartFile file, String subject, String subDesc, Long chatId);

  void saveQuestionsToDatabase(List<QuestionResponseDto> questions, String subject, String subDesc, Long chatId);

  void update(Long id, QuestionsEntity requestDto);

  List<Long> getAllQuestionIds();

  SubjectEntity addSubject(SubjectRequestDto subjectEntity);

  List<SubjectEntity> getAllSubjects(Long chatId);

  SubjectEntity getSubjectById(Long id);

  List<QuestionsEntity> getQuestionsBySubjectId(Long subjectId);

  void addUserIfNotExists(Long subjectId, Long chatId);

  SubjectEntity addSubjectAndUser(String subject, String subDesc, Long chatId);

}
