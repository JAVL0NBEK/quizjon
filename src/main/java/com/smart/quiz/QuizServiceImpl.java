package com.smart.quiz;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import com.smart.quiz.dto.UserEntity;
import com.smart.quiz.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
  private final SubjectRepository subjectRepository;
  private final Utils utils;
  private final UsersRepository usersRepository;

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
  public void processFile(MultipartFile file, String subject, String subDesc) throws RuntimeException {
    List<QuestionResponseDto> questions;
    try {
      questions = readQuestionsFromDocx(file);
    } catch (IOException e) {
      throw new RuntimeException("Faylni o'qishda xatolik yuz berdi.", e);
    }
    // Savollarni bazaga saqlash
    saveQuestionsToDatabase(questions, subject, subDesc);
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

     // Savolni aniqlash shartlarini kengaytiramiz

      if (Boolean.TRUE.equals(utils.isQuestionLine(line))) {
        if (currentQuestion != null) {
          questions.add(currentQuestion);
        }
        currentQuestion = new QuestionResponseDto();

        // Savol matnini tozalash
        String questionText = line;
        if (line.startsWith("$")) {
          questionText = line.substring(1).trim();
        } else if (line.matches("^\\d+\\..*")) {
          // Tartib raqamdan keyingi qismni olish
          questionText = line.replaceFirst("^\\d+\\.\\s*", "").trim();
        }
        currentQuestion.setQuestionText(questionText);
        currentQuestion.setOptions(new ArrayList<>());

      } else if (currentQuestion != null) {
        OptionResponseDto option = new OptionResponseDto();
        option.setOptionText(line.startsWith("#") ? line.substring(1) : line);
        option.setCorrect(line.startsWith("#"));
//        option.setCorrect(true);
        currentQuestion.getOptions().add(option);
      }
    }

    if (currentQuestion != null) {
      questions.add(currentQuestion);
    }

    return questions;
  }

  @Override
  public void saveQuestionsToDatabase(List<QuestionResponseDto> questions, String subject, String subDesc) {
    var subjectEntity = new SubjectEntity();
    subjectEntity.setSubjectName(subject);
    subjectEntity.setDescription(subDesc);
    var subjectResponse = subjectRepository.save(subjectEntity);
    for (QuestionResponseDto questionDto : questions) {
      var questionEntity = new QuestionsEntity();

      questionEntity.setQuestionText(questionDto.getQuestionText());
      questionEntity.setSubject(subjectResponse);

      // Variantlarni yaratish va mutable ro‘yxatga yig‘ish
      List<OptionResponseDto> options = questionDto.getOptions().stream()
          .map(optionDto -> {
            var option = new OptionResponseDto();
            option.setOptionText(optionDto.getOptionText());
            option.setCorrect(optionDto.isCorrect());
            return option;
          })
          .collect(Collectors.toList()); // Mutable List yaratish

      // Variantlarni aralashtirish
      Collections.shuffle(options);

      questionEntity.setOptions(quizMapper.toEntity(options));
      questionRepository.save(questionEntity);
    }


  }

  @Override
  public void update(Long id, QuestionsEntity requestDto) {
    log.info("Updating question with id:{} ", id);
    var entity = findOrFail(id);
    questionRepository.save(entity);
  }

  @Override
  public List<Long> getAllQuestionIds() {
    return questionRepository.findAll().stream().map(QuestionsEntity::getId).toList();
  }

  @Override
  public SubjectEntity addSubject(SubjectRequestDto subject) {
    return subjectRepository.save(quizMapper.toSubjectEntity(subject));
  }

  @Override
  public List<SubjectEntity> getAllSubjects(Long chatId) {
    return subjectRepository.findAllByChatId(chatId);
  }

  @Override
  public SubjectEntity getSubjectById(Long id) {
    return subjectRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("subjects.id",
        List.of(id.toString())));
  }

  @Override
  public List<QuestionsEntity> getQuestionsBySubjectId(Long subjectId) {
    return questionRepository.findSubjectById(subjectId).orElseThrow(()-> new ResourceNotFoundException("subjects.id",
        List.of(subjectId.toString())));
  }

  @Override
  public void addUserIfNotExists(Long subjectId, Long chatId) {
// 1. chatId bo‘yicha foydalanuvchi mavjudligini tekshirish
    boolean exists = usersRepository.existsByChatId(chatId);

    if (!exists) {
      // 2. subjectId bo‘yicha SubjectEntity ni olish
      SubjectEntity subject = subjectRepository.findById(subjectId)
          .orElseThrow(() -> new RuntimeException("Subject not found"));

      // 3. Yangi foydalanuvchini yaratish
      UserEntity newUser = new UserEntity();
      newUser.setChatId(chatId);
      newUser.setUserName("New User"); // Default qiymat, kerak bo‘lsa o‘zgartirish mumkin
      newUser.setSubjects(new ArrayList<>());
      newUser.getSubjects().add(subject); // Many-to-Many bo‘lgani uchun subject qo‘shamiz

      // 4. Yangi foydalanuvchini saqlash
      usersRepository.save(newUser);
    }
  }

  private QuestionsEntity findOrFail(Long id) {
    return questionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("question.id",
            List.of(id.toString())));
  }

}
