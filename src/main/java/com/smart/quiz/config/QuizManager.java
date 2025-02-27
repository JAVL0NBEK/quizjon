package com.smart.quiz.config;

import com.smart.quiz.QuizService;
import com.smart.quiz.dto.OptionResponseDto;
import com.smart.quiz.dto.QuestionResponseDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizManager {

  private final QuizService quizService;
  private final QuizBot quizBot; // QuizBot obyektini qo‘shamiz

  private Long currentQuestionId = 1L;
  private int correctAnswersCount = 0; // To'g'ri javoblar soni
  private int wrongAnswersCount = 0; // Noto'g'ri javoblar soni

  @Autowired
  public QuizManager(QuizService quizService,@Lazy QuizBot quizBot) {
    this.quizService = quizService;
    this.quizBot = quizBot;
  }

  public SendPoll startQuiz(Long userId) {
    currentQuestionId = 1L; // Testni boshlash
    correctAnswersCount = 0;
    wrongAnswersCount = 0;

    return getQuestionMessage(userId);
  }

  public SendMessage exitBot(Long userId) {
    SendMessage message = new SendMessage();
    message.setChatId(userId.toString());
    message.setText("👋 Botdan chiqdingiz. Qaytadan boshlash uchun /start ni yuboring.");
    return message;
  }

  public SendPoll getQuestionMessage(Long chatId) {
    QuestionResponseDto question = quizService.getQuestionById(currentQuestionId);

    SendPoll poll = new SendPoll();
    poll.setChatId(chatId.toString());
    poll.setQuestion(currentQuestionId + ". " + question.getQuestionText());

    List<String> options = question.getOptions().stream()
        .map(option -> trimOptionText(option.getOptionText())) // ⚡ Uzun variantlarni qisqartirish
        .toList();

    poll.setOptions(options);
    poll.setType("quiz");
    poll.setCorrectOptionId(findCorrectOptionIndex(question));
    poll.setIsAnonymous(false);

    return poll;
  }

  public void processPollAnswer(Long userId, Integer selectedOption) {
    QuestionResponseDto question = quizService.getQuestionById(currentQuestionId);
    boolean isCorrect = selectedOption.equals(findCorrectOptionIndex(question));

    if (isCorrect) {
      correctAnswersCount++;
    } else {
      wrongAnswersCount++;
    }

    currentQuestionId++;

    try {
      if (quizService.hasNextQuestion(currentQuestionId)) {
        quizBot.execute(getQuestionMessage(userId)); // ✅ execute() ni QuizBot orqali chaqiramiz
      } else {
        quizBot.execute(sendStatistics(userId)); // ✅ execute() ni QuizBot orqali chaqiramiz
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

  public SendMessage sendStatistics(Long userId) {
    long totalQuestions = correctAnswersCount + wrongAnswersCount;
    double correctPercentage = (double) correctAnswersCount / totalQuestions * 100;

    String statsMessage = String.format(
        "📊 *Statistika:*\n\n" +
        "• 📌 Umumiy savollar: %d\n" +
        "• ✅ To'g'ri javoblar: %d (%.2f%%)\n" +
        "• ❌ Noto'g'ri javoblar: %d",
        totalQuestions,
        correctAnswersCount,
        correctPercentage,
        wrongAnswersCount
    );

    SendMessage message = new SendMessage();
    message.setChatId(userId.toString());
    message.setText(statsMessage);
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
