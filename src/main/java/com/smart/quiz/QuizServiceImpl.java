package com.smart.quiz;

import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.QuestionParseResult;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.SubjectRequestDto;
import com.smart.quiz.dto.UserEntity;
import com.smart.quiz.exception.InvalidFileFormatException;
import com.smart.quiz.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
  public String processFile(MultipartFile file, String subject, String subDesc, Long chatId) throws RuntimeException {
    List<QuestionResponseDto> questions;
    try {
      QuestionParseResult result = readQuestionsFromDocx(file);
      questions = result.getQuestions();

    } catch (IOException e) {
      throw new RuntimeException("Faylni o‘qishda xatolik yuz berdi.", e);
    }

    // ✅ Hammasi to‘g‘ri bo‘lsa, savollarni bazaga saqlaymiz:
    saveQuestionsToDatabase(questions, subject, subDesc, chatId);
    return "Fayl muvaffaqiyatli yuklandi va savollar bazaga saqlandi!";
  }

  private QuestionParseResult readQuestionsFromDocx(MultipartFile file) throws IOException {
    // 1. Fayl nomini tekshirish
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".docx")) {
      throw new InvalidFileFormatException(
          "Noto'g'ri fayl formati",
          List.of(
              "Faqat .docx formatidagi fayllar qabul qilinadi",
              "Siz yuborgan fayl: " + originalFilename,
              "Iltimos, faylni Word hujjat sifatida saqlang (.docx)"
          )
      );
    }

    // 2. Fayl tarkibini tekshirish (magic number)
    try (InputStream inputStream = file.getInputStream()) {
      byte[] header = new byte[4];
      if (inputStream.read(header) != 4 ||
          !Arrays.equals(header, new byte[]{0x50, 0x4B, 0x03, 0x04})) {
        throw new InvalidFileFormatException(
            "Fayl tarkibi noto'g'ri",
            List.of(
                "Fayl .docx formatida emas yoki buzilgan",
                "Haqiqiy .docx fayl yuboring"
            )
        );
      }
    }

    // 3. Asosiy qayta ishlash
    List<String> lines = new ArrayList<>();
    try (InputStream inputStream = file.getInputStream();
        XWPFDocument doc = new XWPFDocument(inputStream)) {

      for (XWPFParagraph paragraph : doc.getParagraphs()) {
        String text = paragraph.getText().trim();
        if (!text.isEmpty()) {
          lines.add(text);
        }
      }
    } catch (Exception e) {
      throw new InvalidFileFormatException(
          "DOCX faylni o'qib bo'lmadi",
          List.of(
              "Fayl buzilgan yoki parol bilan himoyalangan bo'lishi mumkin",
              "Xato tafsiloti: " + e.getMessage()
          )
      );
    }

    return parseQuestions(lines);
  }

  public QuestionParseResult parseQuestions(List<String> lines) {
    List<QuestionResponseDto> questions = new ArrayList<>();
    List<String> errorLines = new ArrayList<>();
    QuestionResponseDto currentQuestion = null;
    final int CHECK_LINES = 5; // Tekshiriladigan dastlabki qatorlar soni

    // Dastlabki 5 qatorni tekshirish
    boolean hasQuestion = false;
    for (int i = 0; i < Math.min(CHECK_LINES, lines.size()); i++) {
      String line = lines.get(i).trim();
      if (!line.isEmpty() && Boolean.TRUE.equals(utils.isQuestionLine(line))) {
        hasQuestion = true;
        break;
      }
    }

    if (!hasQuestion) {
      throw new RuntimeException(
          "Faylda savollar to'g'ri formatda emas. Iltimos, quyidagi qoidalarga amal qiling:\n\n" +
          "1. Savollarda tartib raqam bo`lishi shart:\n" +
          "2. Dastlabki 5 qatorda kamida 1 ta savol bo'lishi majburiy\n" +
          "3. Har bir savolda bitta to`g`ri javob bo`lishi shart\n\n"
      );
    }

    // Asosiy savollarni qayta ishlash
    for (String line : lines) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }

      if (Boolean.TRUE.equals(utils.isQuestionLine(line))) {
        if (currentQuestion != null) {
          questions.add(currentQuestion);
        }

        currentQuestion = new QuestionResponseDto();
        String questionText = parseQuestionText(line);
        currentQuestion.setQuestionText(questionText);
        currentQuestion.setOptions(new ArrayList<>());

      } else if (currentQuestion != null) {
        OptionResponseDto option = new OptionResponseDto();
        option.setOptionText(line.startsWith("#") ? line.substring(1) : line);
        option.setCorrect(line.startsWith("#"));
        currentQuestion.getOptions().add(option);
      }
    }

    if (currentQuestion != null) {
      questions.add(currentQuestion);
    }

    return new QuestionParseResult(questions, errorLines);
  }

  private String parseQuestionText(String line) {
    if (Boolean.TRUE.equals(utils.isQuestionLine(line))) {
      return line.substring(1).trim();
    } else if (line.matches("^\\d+\\..*")) {
      return line.replaceFirst("^\\d+\\.\\s*", "").trim();
    }
    return line;
  }

  @Override
  public void saveQuestionsToDatabase(List<QuestionResponseDto> questions, String subject, String subDesc, Long chatId) {
    var subjectEntity = addSubjectAndUser(subject, subDesc, chatId);

    // 1. Savollarni saqlash
    for (QuestionResponseDto questionDto : questions) {
      var questionEntity = new QuestionsEntity();
      questionEntity.setQuestionText(questionDto.getQuestionText());
      questionEntity.setSubject(subjectEntity);

      // 2. Variantlarni yaratish va shuffle qilish
      List<OptionResponseDto> options = questionDto.getOptions().stream()
          .map(optionDto -> {
            var option = new OptionResponseDto();
            option.setOptionText(optionDto.getOptionText());
            option.setCorrect(optionDto.isCorrect());
            return option;
          })
          .collect(Collectors.toList());

      Collections.shuffle(options); // Variantlarni aralashtirish
      questionEntity.setOptions(quizMapper.toEntity(options));
      questionRepository.save(questionEntity);
    }

  }

  @Override
  public SubjectEntity addSubjectAndUser(String subject, String subDesc, Long chatId){
    // 1. Foydalanuvchi chatId bo‘yicha bazadan qidiriladi
    UserEntity user = usersRepository.findByChatIdWithSubjects(chatId)
        .orElseGet(() -> {
          // 2. Agar user topilmasa, yangi user yaratamiz
          UserEntity newUser = new UserEntity();
          newUser.setUserName("User_" + chatId); // Ismni ixtiyoriy qilib qo'yamiz
          newUser.setChatId(chatId);
          newUser.setSubjects(new ArrayList<>());
          return usersRepository.save(newUser); // Yangi userni bazaga saqlaymiz
        });

    // 3. SubjectEntity yaratish yoki topish
    SubjectEntity subjectEntity = subjectRepository.findBySubjectName(subject)
        .orElseGet(() -> {
          SubjectEntity newSubject = new SubjectEntity();
          newSubject.setSubjectName(subject);
          newSubject.setDescription(subDesc);
          return subjectRepository.save(newSubject);
        });

    // 4. User va Subject bog‘langanligini tekshiramiz
    if (!user.getSubjects().contains(subjectEntity)) {
      user.getSubjects().add(subjectEntity);
      usersRepository.save(user);
    }

    return subjectEntity;
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

  private QuestionsEntity findOrFail(Long id) {
    return questionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("question.id",
            List.of(id.toString())));
  }

}
