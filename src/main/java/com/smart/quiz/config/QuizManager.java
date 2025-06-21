package com.smart.quiz.config;

import com.smart.quiz.QuizService;
import com.smart.quiz.StatsRepository;
import com.smart.quiz.UsersRepository;
import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuestionsEntity;
import com.smart.quiz.dto.QuizState;
import com.smart.quiz.dto.StatsEntity;
import com.smart.quiz.dto.SubjectEntity;
import com.smart.quiz.dto.UserEntity;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizManager {

  private final QuizService quizService;
  private final QuizBot quizBot; // QuizBot obyektini qo‘shamiz
  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  public static final Long ADMIN_CHAT_ID = 778149769L;
  // Har bir foydalanuvchi uchun holatni saqlash uchun Map
  private final Map<Long, QuizState> userStates = new ConcurrentHashMap<>();

  // Bo‘limlar ro‘yxati (masalan, har birida 50 ta savol)
  private final Map<String, List<Long>> sections = new HashMap<>();
  private final StatsRepository statsRepository;
  private final UsersRepository usersRepository;

  @Autowired
  public QuizManager(QuizService quizService,@Lazy QuizBot quizBot, StatsRepository statsRepository,
      UsersRepository usersRepository) {
    this.quizService = quizService;
    this.quizBot = quizBot;
    this.usersRepository = usersRepository;
    initializeSections(); // Bo‘limlarni boshlang‘ich holatda yuklash
    this.statsRepository = statsRepository;
  }

  // Quizni boshlash: Faol quiz mavjudligini tekshiradi
  public SendMessage startQuiz(Long chatId) {
    QuizState existingState = userStates.get(chatId);

    // Agar foydalanuvchi allaqachon quizda bo‘lsa
    if (existingState != null && existingState.isActive()) {
      return createMessage(chatId, "❌ Siz allaqachon quizdasiz! Avval uni tugating yoki /stop buyrug‘idan foydalaning.");
    }

    // Yangi holat yaratib, fanlarni ko‘rsatamiz
    userStates.put(chatId, new QuizState());
    return showSubjects(chatId);
  }

  // Fanlar ro‘yxatini ko‘rsatish
  private SendMessage showSubjects(Long chatId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());

    boolean hasAccess = usersRepository.findByChatId(chatId)
        .map(UserEntity::isAccess)
        .orElse(false);

    List<SubjectEntity> subjects = quizService.getAllSubjects(chatId);
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    if (!hasAccess) {
      message.setText("📚 Sizda quizdan foydalanish uchun ruxsat yo`q!");
    }
    else if (subjects.isEmpty()){
      message.setText("📚 Sizda quiz savollar mavjud emas! /create buyrug‘idan foydalaning quydagi "
                      + "linkda quiz savollarini to`g`ri yaratish bo`yicha namuna ko`rsatilgan");
    } else {
      message.setText("📚 Fanlardan birini tanlang:");
      for (SubjectEntity subject : subjects) {
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(subject.getSubjectName());
        selectButton.setCallbackData("subject_" + subject.getId());

        InlineKeyboardButton shareButton = new InlineKeyboardButton();
        shareButton.setText("📤 Ulashish");
        shareButton.setCallbackData("share_subject_" + subject.getId());

        keyboard.add(List.of(selectButton, shareButton));
      }

      InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
      markup.setKeyboard(keyboard);
      message.setReplyMarkup(markup);

    }

    return message;
  }

  // Fanlar ro‘yxatini ko‘rsatish
  public SendMessage accessChange(Long chatId) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());

    List<UserEntity> users = usersRepository.findAll();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    if (users.isEmpty()){
      message.setText("📚 Quizdan foydalanayotganlar mavjud emas");
    } else {
      message.setText("📚 Foydalanuvchilar:");
      for (UserEntity user : users) {
        InlineKeyboardButton selectButton = new InlineKeyboardButton();
        selectButton.setText(user.getUserName());
        selectButton.setCallbackData("user_" + user.getId());

        InlineKeyboardButton shareButton = new InlineKeyboardButton();
        if (!user.isAccess()) {
          shareButton.setText("📤 blokdan chiqarish");
        } else {
          shareButton.setText("📤 bloklash");
        }
        shareButton.setCallbackData("permission_management_" + user.getId());

        keyboard.add(List.of(selectButton, shareButton));
      }

      InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
      markup.setKeyboard(keyboard);
      message.setReplyMarkup(markup);

    }

    return message;
  }

  private void initializeSections() {
    List<Long> allQuestionIds = quizService.getAllQuestionIds();
    int sectionSize = 50;
    int sectionCount = (int) Math.ceil((double) allQuestionIds.size() / sectionSize);

    for (int i = 0; i < sectionCount; i++) {
      int start = i * sectionSize;
      int end = Math.min(start + sectionSize, allQuestionIds.size());
      sections.put("Bo‘lim " + (i + 1), allQuestionIds.subList(start, end));
    }
  }

  // Callback query ni qayta ishlash
  public void processCallbackQuery(Long userId, String callbackData) throws TelegramApiException {
    QuizState state = userStates.get(userId);

    if (state != null && state.isActive() && state.getCurrentSection() != null) {
      quizBot.execute(createMessage(userId, "❌ Siz allaqachon quizdasiz! Avval tugating yoki /stop buyrug‘idan foydalaning."));
      return;
    }

    // Fanni tanlash
    if (callbackData.startsWith("subject_")) {
      Long subjectId = Long.parseLong(callbackData.replace("subject_", ""));
      startSubjectQuiz(userId, subjectId);
    }
    // Fanni ulashish
    else if (callbackData.startsWith("share_subject_")) {
      Long subjectId = Long.parseLong(callbackData.replace("share_subject_", ""));
      quizBot.execute(shareSubject(userId, subjectId));
    }
    //permission_management_
    else if (callbackData.startsWith("permission_management_")) {
      Long chatId = Long.parseLong(callbackData.replace("permission_management_", ""));
      quizBot.execute(permissionManagement(chatId));
    }
    // Bo‘limni tanlash
    else if (callbackData.startsWith("section_")) {
      String selectedSection = callbackData.replace("section_", "");
      if (state.getSections().containsKey(selectedSection)) {
        state.setCurrentSection(selectedSection);
        state.setCurrentQuestionIndex(0);
        state.setCorrectAnswersCount(0);
        state.setWrongAnswersCount(0);
        state.setActive(true);
        quizBot.execute(getQuestionMessage(userId));
      } else {
        quizBot.execute(createMessage(userId, "❌ Bunday bo‘lim mavjud emas!"));
      }
    }

  }

  private void startSubjectQuiz(Long userId, Long subjectId) throws TelegramApiException {
    QuizState state = userStates.get(userId);
    List<QuestionsEntity> questions = quizService.getQuestionsBySubjectId(subjectId);
    //LinkedHashSet

    if (questions.isEmpty()) {
      quizBot.execute(createMessage(userId, "❌ Ushbu fanda savollar mavjud emas!"));
      return;
    }

    Map<String, List<Long>> sections = createSections(questions);
    state.setSections(sections);
    state.setSubjectId(subjectId);
    quizBot.execute(showSections(userId));
  }

  private SendMessage shareSubject(Long userId, Long subjectId) {
    String botUsername = quizBot.getBotUsername();
    String shareLink = String.format("https://t.me/%s?start=subject_%d", botUsername, subjectId);

    SubjectEntity subject = quizService.getSubjectById(subjectId); // Fan nomini olish uchun
    String shareMessage = String.format("""
            📢 Do‘stlaringizni %s fanidan quizga taklif qiling!
            Quyidagi havolani ularga yuboring:
            %s
            
            Ular ham ushbu testni sinab ko‘rishlari mumkin!
            """, subject.getSubjectName(), shareLink);

    return createMessage(userId, shareMessage);
  }

  private SendMessage permissionManagement(Long userId) {

    usersRepository.findById(userId).ifPresent(user -> {
      user.setAccess(!user.isAccess());
      usersRepository.save(user);
    });

    String shareMessage = "Foydalanuvchi bloklandi. Tekshirish uchun /check buyrug`ini kiriting";

    return createMessage(ADMIN_CHAT_ID, shareMessage);
  }

  // Savollarni bo‘limlarga bo‘lish
  private Map<String, List<Long>> createSections(List<QuestionsEntity> questions) {
    Map<String, List<Long>> sections = new HashMap<>();
    int sectionSize = 50; // Har bir bo‘limda 50 ta savol
    int sectionCount = (int) Math.ceil((double) questions.size() / sectionSize);

    for (int i = 0; i < sectionCount; i++) {
      int start = i * sectionSize;
      int end = Math.min(start + sectionSize, questions.size());
      List<Long> questionIds = questions.subList(start, end).stream()
          .map(QuestionsEntity::getId)
          .toList();
      sections.put("Bo‘lim " + (i + 1), questionIds);
    }
    return sections;
  }

  // Tanlangan fan bo‘yicha bo‘limlarni ko‘rsatish
  private SendMessage showSections(Long userId) {
    QuizState state = userStates.get(userId);
    SendMessage message = new SendMessage();
    message.setChatId(userId.toString());
    message.setText("📚 Bo‘limni tanlang:");

    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    List<String> sortedSections = new ArrayList<>(state.getSections().keySet());
    Collections.sort(sortedSections);

    for (String section : sortedSections) {
      InlineKeyboardButton button = new InlineKeyboardButton();
      button.setText(section);
      button.setCallbackData("section_" + section);
      keyboard.add(List.of(button));
    }

    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
    markup.setKeyboard(keyboard);
    message.setReplyMarkup(markup);

    return message;
  }

  public SendMessage exitBot(Long userId) {
    QuizState state = userStates.remove(userId); // Holatni tozalash
    if (state != null && state.isActive()) {
      return createMessage(userId, "👋 Quiz to‘xtatildi. Qaytadan boshlash uchun /quiz buyrug‘idan foydalaning.");
    }
    return createMessage(userId, "👋 Botdan chiqdingiz. Qaytadan boshlash uchun /start ni yuboring.");
  }

  public SendMessage sendResults(Long userId, Integer statCount) {
    List<StatsEntity> stats = statsRepository.getByUserId(userId)
        .stream()
        .sorted(Comparator.comparing(StatsEntity::getCreatedAt).reversed())
        .limit(statCount)
        .toList();

    StringBuilder message = new StringBuilder("📊 Test natijalari:\n\n");
    int count = 1;
    for (StatsEntity result : stats) {
      message.append("📘 ").append(count++).append("-fan: ").append(result.getSubjectName())
          .append("\n")
          .append("🔹 ").append(result.getCurrentSection()).append("\n")
          .append("📅 Sana: ").append(result.getCreatedAt().format(DATE_TIME_FORMATTER)).append("\n")
          .append("🧮 Jami: ").append(result.getTotalQuestions()).append(" ta savol belgilandi\n")
          .append("✅ To‘g‘ri javoblar: ").append(result.getCorrectAnswersCount()).append(" (")
          .append(result.getCorrectPercentage()).append(")").append("\n")
          .append("❌ Noto‘g‘ri javoblar: ").append(result.getWrongAnswersCount()).append("\n")
          .append("-------------------------\n");
    }

    return createMessage(userId, message.toString());
  }

  public SendPoll getQuestionMessage(Long userId) {
    QuizState state = userStates.get(userId);
    List<Long> sectionQuestions = state.getSections().get(state.getCurrentSection());
    Long currentQuestionId = sectionQuestions.get(state.getCurrentQuestionIndex());

    QuestionResponseDto question = quizService.getQuestionById(currentQuestionId);

    SendPoll poll = new SendPoll();
    poll.setChatId(userId.toString());
    poll.setQuestion((state.getCurrentQuestionIndex() + 1) + ". " + question.getQuestionText());
    poll.setOptions(question.getOptions().stream()
        .map(option -> trimOptionText(option.getOptionText()))
        .toList());
    poll.setType("quiz");
    poll.setCorrectOptionId(findCorrectOptionIndex(question));
    poll.setIsAnonymous(false);

    return poll;
  }

  public void processPollAnswer(Long userId, Integer selectedOption) {
    QuizState state = userStates.get(userId);
    List<Long> sectionQuestions = state.getSections().get(state.getCurrentSection());
    Long currentQuestionId = sectionQuestions.get(state.getCurrentQuestionIndex());

    QuestionResponseDto question = quizService.getQuestionById(currentQuestionId);
    boolean isCorrect = selectedOption.equals(findCorrectOptionIndex(question));

    if (isCorrect) {
      state.incrementCorrectAnswers();
    } else {
      state.incrementWrongAnswers();
    }

    state.incrementQuestionIndex();

    try {
      if (state.getCurrentQuestionIndex() < sectionQuestions.size()) {
        quizBot.execute(getQuestionMessage(userId));
      } else {
        quizBot.execute(sendStatistics(userId));
        state.setActive(false); // Quiz tugadi
        userStates.remove(userId); // Holatni o‘chirish
      }
    } catch (TelegramApiException e) {
      log.error("Xatolik yuz berdi: {}", e.getMessage());
    }
  }

  private int findCorrectOptionIndex(QuestionResponseDto question) {
    List<OptionResponseDto> options = question.getOptions();
    for (int i = 0; i < options.size(); i++) {
      if (options.get(i).isCorrect()) {
        return i;
      }
    }
    return -1;
  }

  private String trimOptionText(String option) {
    return option.length() > 100 ? option.substring(0, 97) + "..." : option;
  }

  public SendMessage createMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    return message;
  }

  // Statistika yuborish
  public SendMessage sendStatistics(Long userId) {
    QuizState state = userStates.get(userId);
    SendMessage message = new SendMessage();
    message.setChatId(userId.toString());

    if (state == null || !state.isActive()) {
      message.setText("❌ Hozirda faol quiz mavjud emas! Quizni boshlash uchun /quiz buyrug‘idan foydalaning.");
    } else {
      long totalQuestions = state.getCorrectAnswersCount() + state.getWrongAnswersCount();
      double correctPercentage = totalQuestions > 0 ? (double) state.getCorrectAnswersCount() / totalQuestions * 100 : 0.0;
      DecimalFormat df = new DecimalFormat("#0.0");
      String formattedPercentage = df.format(correctPercentage) + "%";
      String statsMessage = String.format(
          "📊 *Statistika (%s):*\n\n" +
          "• 📌 Umumiy savollar: %d\n" +
          "• ✅ To'g'ri javoblar: %d (%s)\n" +
          "• ❌ Noto'g'ri javoblar: %d\n\n" +
          "👇 Yana quiz ishlamoqchi bo‘lsangiz /quiz yoki pastdagi tugmani bosing:",
          state.getCurrentSection(),
          totalQuestions,
          state.getCorrectAnswersCount(),
          formattedPercentage,
          state.getWrongAnswersCount()
      );
      var subject = quizService.getBySubjectId(state.getSubjectId());
      StatsEntity statsEntity = new StatsEntity();
      statsEntity.setSubjectId(state.getSubjectId());
      statsEntity.setUserId(userId.toString());
      subject.ifPresent(
          subjectEntity -> statsEntity.setSubjectName(subjectEntity.getSubjectName()));
      statsEntity.setCurrentSection(state.getCurrentSection());
      statsEntity.setTotalQuestions(totalQuestions);
      statsEntity.setCorrectAnswersCount((long) state.getCorrectAnswersCount());
      statsEntity.setWrongAnswersCount((long) state.getWrongAnswersCount());
      statsEntity.setCorrectPercentage(formattedPercentage);
      statsEntity.setCreatedAt(LocalDateTime.now());
      quizService.addStats(statsEntity);

      message.setText(statsMessage);
      state.setActive(false);
      userStates.remove(userId);
    }

    message.setParseMode("Markdown");

    // 🔘 ReplyKeyboardMarkup bilan oddiy /quiz tugmasi
    ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
    KeyboardButton quizButton = new KeyboardButton("/quiz");

    KeyboardRow keyboardRow = new KeyboardRow();
    keyboardRow.add(quizButton);
    List<KeyboardRow> keyboardRows = new ArrayList<>();
    keyboardRows.add(keyboardRow);

    keyboard.setKeyboard(keyboardRows);
    keyboard.setResizeKeyboard(true);
    keyboard.setOneTimeKeyboard(true); // Tugma faqat bir marta ko‘rinadi

    message.setReplyMarkup(keyboard);

    return message;
  }

  public SendMessage sendQuestionFormatInfo(Long chatId) {
    String infoMessage = """
        📚 *Quizjon_bot'ga xush kelibsiz!*
        """;

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(infoMessage);
    message.setParseMode("Markdown");

    return message;
  }

  // Botni ulashish uchun havola yaratish
  public SendMessage shareBot(Long chatId) {
    String botUsername = quizBot.getBotUsername(); // Bot nomini dinamik olish
    String shareLink = String.format("https://t.me/%s?start=%d", botUsername, chatId);

    String shareMessage = """
        📢 Do‘stlaringizni quizga taklif qiling!
        Quyidagi havolani ularga yuboring:
        %s
        
        Ular ham sizning testingizni sinab ko‘rishlari mumkin!
        """.formatted(shareLink);

    return createMessage(chatId, shareMessage);
  }

  // Deep link orqali kelgan taklifni qayta ishlash
  public SendMessage handleInvite(Long chatId, String param, String userName) {
    if (param.startsWith("subject_")) {
      try {
        Long subjectId = Long.parseLong(param.replace("subject_", ""));
        SubjectEntity subject = quizService.getSubjectById(subjectId);
        //agar user yoki subject topilmasa yaratadi bir biriga ham bog`laydi
        quizService.addSubjectAndUser(subject.getSubjectName(), subject.getDescription(), chatId, userName);

        userStates.put(chatId, new QuizState());
        startSubjectQuiz(chatId, subjectId);
        return createMessage(chatId, String.format("👋 %s fanidan quizga xush kelibsiz!", subject.getSubjectName()));
      } catch (NumberFormatException | TelegramApiException e) {
        return createMessage(chatId, "❌ Taklif havolasi noto‘g‘ri!");
      }
    }
    return createMessage(chatId, "❌ Noto‘g‘ri taklif havolasi!");
  }

}
