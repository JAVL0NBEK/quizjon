package com.smart.quiz;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.OptionsEntity;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

  private final QuestionRepository questionRepository;

  @Override
  public List<QuestionsEntity> getAllQuestions() {
    return questionRepository.findAll();
  }
  @Transactional
  @Override
  public QuestionResponseDto getQuestionById(Long id) {

    var question = questionRepository.findById(id).orElseGet(QuestionsEntity::new);
    var dto = new QuestionResponseDto();
    dto.setId(question.getId());
    dto.setQuestionText(question.getQuestionText());

    var options = question.getOptions().stream()
        .map(option -> new OptionResponseDto(option.getId(), option.getOptionText(), option.isCorrect()))
        .toList();

    dto.setOptions(options);
    return dto;
  }

  @Override
  public QuestionsEntity addQuestion(QuestionsEntity question) {
    return questionRepository.save(question);
  }

  @Override
  public boolean isOptionCorrect(Long optionId) {
    return questionRepository.findOptionById(optionId)
        .map(OptionsEntity::isCorrect)
        .orElse(false);
  }
}
