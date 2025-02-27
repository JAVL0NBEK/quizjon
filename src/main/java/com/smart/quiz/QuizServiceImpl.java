package com.smart.quiz;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.OptionsEntity;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizServiceImpl implements QuizService {

  private final QuestionRepository questionRepository;
  private final QuizMapper quizMapper;

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

  @Override
  public void processFile(MultipartFile file) throws RuntimeException {
    List<QuestionResponseDto> questions = null;
    try {
      questions = readQuestionsFromDocx(file);
    } catch (IOException e) {
      throw new RuntimeException("Faylni o'qishda xatolik yuz berdi.", e);
    }
    // Savollarni bazaga saqlash
    saveQuestionsToDatabase(questions);
  }

  private List<QuestionResponseDto> readQuestionsFromDocx(MultipartFile file) throws IOException {
    List<String> lines = new ArrayList<>();

    try (InputStream inputStream = file.getInputStream();
        XWPFDocument doc = new XWPFDocument(inputStream)) {

      for (XWPFParagraph paragraph : doc.getParagraphs()) {
        String text = paragraph.getText().trim();
        if (!text.isEmpty()) {
          lines.add(text);
        }
      }
    }

    // Endi lines ni parse qilish va List<QuestionResponseDto> ga aylantirish mumkin
    return parseQuestions(lines);
  }

  private List<QuestionResponseDto> parseQuestions(List<String> lines) {
    List<QuestionResponseDto> questions = new ArrayList<>();
    QuestionResponseDto currentQuestion = null;

    for (String line : lines) {
      line = line.trim();

      // Agar qator bo‘sh bo‘lsa, e'tiborga olinmaydi
      if (line.isEmpty()) {
        continue;
      }

      if (line.endsWith("?") || line.endsWith(":") || line.endsWith("...")  || line.startsWith("...")
          || line.contains("__") || line.endsWith("!")) { // Savolni ajratib olish
        if (currentQuestion != null) {
          questions.add(currentQuestion);
        }
        currentQuestion = new QuestionResponseDto();
        currentQuestion.setQuestionText(line.trim());
        currentQuestion.setOptions(new ArrayList<>());

      } else if (currentQuestion != null) { // Noto‘g‘ri javob
        OptionResponseDto option = new OptionResponseDto();
        option.setOptionText(line);
        option.setCorrect(true);
        currentQuestion.getOptions().add(option);
      }
    }

    if (currentQuestion != null) {
      questions.add(currentQuestion);
    }

    return questions;
  }

  @Override
  public void saveQuestionsToDatabase(List<QuestionResponseDto> questions) {
    for (QuestionResponseDto questionDto : questions) {
      var questionEntity = new QuestionsEntity();
      questionEntity.setQuestionText(questionDto.getQuestionText());

      var options = questionDto.getOptions().stream()
          .map(optionDto -> {
            var option = new OptionResponseDto();
            option.setOptionText(optionDto.getOptionText());
            option.setCorrect(optionDto.isCorrect());
            return option;
          })
          .toList();

      questionEntity.setOptions(quizMapper.toEntity(options));
      questionRepository.save(questionEntity);
    }
  }

  @Override
  public boolean hasNextQuestion(Long questionId) {
    return questionRepository.existsById(questionId);
  }

  @Override
  public void update(Long id, QuestionsEntity requestDto) {
    log.info("Updating question with id:{} ", id);
    var entity = findOrFail(id);
    questionRepository.save(entity);
  }

  private QuestionsEntity findOrFail(Long id) {
    return questionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("question.id",
            List.of(id.toString())));
  }

}
