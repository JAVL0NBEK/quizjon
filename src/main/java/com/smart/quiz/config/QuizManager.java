package com.smart.quiz.config;

import com.smart.quiz.QuizService;
import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.QuestionResponseDto;
import com.smart.quiz.dto.QuizState;
import java.util.ArrayList;
import java.util.Collections;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizManager {

  private final QuizService quizService;
  private final QuizBot quizBot; // QuizBot obyektini qo‘shamiz

  // Har bir foydalanuvchi uchun holatni saqlash uchun Map
  private final Map<Long, QuizState> userStates = new ConcurrentHashMap<>();

  // Bo‘limlar ro‘yxati (masalan, har birida 30 ta savol)
  private final Map<String, List<Long>> sections = new HashMap<>();

  @Autowired
  public QuizManager(QuizService quizService,@Lazy QuizBot quizBot) {
    this.quizService = quizService;
    this.quizBot = quizBot;
    initializeSections(); // Bo‘limlarni boshlang‘ich holatda yuklash
  }

  // Quizni boshlash: Faol quiz mavjudligini tekshiradi
  public SendMessage startQuiz(Long userId) {
    QuizState existingState = userStates.get(userId);

    // Agar foydalanuvchi allaqachon quizda bo‘lsa
    if (existingState != null && existingState.isActive()) {
      return createMessage(userId, "❌ Siz allaqachon quizdasiz! Avval uni tugating yoki /stop buyrug‘idan foydalaning.");
    }

    // Yangi holat yaratamiz va bo‘lim tanlashni taklif qilamiz
    userStates.put(userId, new QuizState());
    SendMessage message = new SendMessage();
    message.setChatId(userId.toString());
    message.setText("📚 Bo‘limni tanlang:");

    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    List<String> sortedSections = new ArrayList<>(sections.keySet());
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

  private void initializeSections() {
    List<Long> allQuestionIds = quizService.getAllQuestionIds();
    int sectionSize = 30;
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

    // Agar foydalanuvchi quizda bo‘lsa va yangi bo‘lim tanlamoqchi bo‘lsa
    if (state != null && state.isActive()) {
      quizBot.execute(createMessage(userId, "❌ Siz allaqachon quizdasiz! Avval uni tugating yoki /stop buyrug‘idan foydalaning."));
      return;
    }

    if (callbackData.startsWith("section_")) {
      String selectedSection = callbackData.replace("section_", "");
      if (sections.containsKey(selectedSection)) {
        state = userStates.get(userId);
        state.setCurrentSection(selectedSection);
        state.setCurrentQuestionIndex(0);
        state.setCorrectAnswersCount(0);
        state.setWrongAnswersCount(0);
        state.setActive(true); // Quiz faol holatga o‘tadi
        quizBot.execute(getQuestionMessage(userId));
      } else {
        quizBot.execute(createMessage(userId, "❌ Bunday bo‘lim mavjud emas!"));
      }
    }
  }

  public SendMessage exitBot(Long userId) {
    QuizState state = userStates.remove(userId); // Holatni tozalash
    if (state != null && state.isActive()) {
      return createMessage(userId, "👋 Quiz to‘xtatildi. Qaytadan boshlash uchun /quiz buyrug‘idan foydalaning.");
    }
    return createMessage(userId, "👋 Botdan chiqdingiz. Qaytadan boshlash uchun /start ni yuboring.");
  }

  public SendPoll getQuestionMessage(Long userId) {
    QuizState state = userStates.get(userId);
    List<Long> sectionQuestions = sections.get(state.getCurrentSection());
    Long currentQuestionId = sectionQuestions.get(state.getCurrentQuestionIndex());

    QuestionResponseDto question = quizService.getQuestionById(currentQuestionId);

    SendPoll poll = new SendPoll();
    poll.setChatId(userId.toString());
    poll.setQuestion((state.getCurrentQuestionIndex() + 1) + ". " + question.getQuestionText());

    List<String> options = question.getOptions().stream()
        .map(option -> trimOptionText(option.getOptionText()))
        .toList();

    poll.setOptions(options);
    poll.setType("quiz");
    poll.setCorrectOptionId(findCorrectOptionIndex(question));
    poll.setIsAnonymous(false);

    return poll;
  }

  public void processPollAnswer(Long userId, Integer selectedOption) {
    QuizState state = userStates.get(userId);
    List<Long> sectionQuestions = sections.get(state.getCurrentSection());
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
        userStates.remove(userId); // Holatni tozalash
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

      String statsMessage = String.format(
          "📊 *Statistika (%s):*\n\n" +
          "• 📌 Umumiy savollar: %d\n" +
          "• ✅ To'g'ri javoblar: %d (%.2f%%)\n" +
          "• ❌ Noto'g'ri javoblar: %d",
          state.getCurrentSection(),
          totalQuestions,
          state.getCorrectAnswersCount(),
          correctPercentage,
          state.getWrongAnswersCount()
      );
      message.setText(statsMessage);
      state.setActive(false); // Statistika ko‘rsatilgandan keyin quiz faol emas
    }

    message.setParseMode("Markdown");
    return message;
  }

  public SendMessage sendQuestionFormatInfo(Long chatId) {
    String infoMessage = """
        📌 *Savollarni to‘g‘ri formatda yozish bo‘yicha qo‘llanma:*

        ✅ *Savollar* quyidagi belgilardan biri bilan tugashi kerak:
        - `?` (Savol belgisidan foydalanish shart)
        - `:` (Ikki nuqta)
        - `...` (Uch nuqta)
        - `!` (Undov belgisi)
        - `__` (Pasti chiziqchalar savol matni orasida qatnashishi mumkin)

        ❌ *Noto‘g‘ri yozilgan savollar* variant sifatida qabul qilinishi mumkin.

        📝 *Misol:*
        ❌ Noto‘g‘ri: `Dunyodagi eng kichik qush`
        ✅ To‘g‘ri: `Dunyodagi eng kichik qush?`
        ✅ To‘g‘ri: `Dunyodagi eng kichik qush:`
        ✅ To‘g‘ri: `Dunyodagi eng kichik qush...`
        ✅ To‘g‘ri: `Dunyodagi eng kichik qush!`
        ✅ To‘g‘ri: `Dunyodagi eng ____ qush`
        
        *📢 Iltimos, savollaringizni to‘g‘ri shaklda yozing!*
        """;

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(infoMessage);
    message.setParseMode("Markdown");

    return message;
  }

}
