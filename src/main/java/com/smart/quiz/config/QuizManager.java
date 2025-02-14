package com.smart.quiz.config;

import com.smart.quiz.QuizService;
import com.smart.quiz.dto.OptionResponseDto;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

@Component
public class QuizManager {

  private final QuizService quizService;
  private Long currentQuestionId = 1L;
  private int correctAnswersCount = 0; // To'g'ri javoblar soni
  private int wrongAnswersCount = 0; // Noto'g'ri javoblar soni

  @Autowired
  public QuizManager(QuizService quizService) {
    this.quizService = quizService;
  }

  public SendMessage getQuestionMessage(Long chatId) {

    try{
      var question = quizService.getQuestionById(currentQuestionId);
      var message = createMessage(chatId, question.getQuestionText());
      var keyboard = createKeyboard(question.getOptions());
      message.setReplyMarkup(keyboard);
      return message;
    } catch (Exception e) {
      return createStatisticsMessage(chatId);
    }

  }

  public EditMessageText handleAnswer(Long chatId, Integer messageId, Long optionId) {
    // Joriy savolni olish
    var question = quizService.getQuestionById(currentQuestionId);
    var options = question.getOptions();

    // Tanlangan variantni belgilash
    boolean isCorrect = quizService.isOptionCorrect(optionId);

    if (isCorrect) {
      correctAnswersCount++; // To'g'ri javoblar sonini oshiramiz
    } else {
      wrongAnswersCount++; // Noto'g'ri javoblar sonini oshiramiz
    }

    // Yangilangan tugmalar
    var updatedKeyboard = new InlineKeyboardMarkup();
    var rows = options.stream()
        .map(option -> {
          var button = new InlineKeyboardButton();

          if (option.getId().equals(optionId)) {
            // Tanlangan variant
            button.setText((isCorrect ? "✅ " : "❌ ") + option.getOptionText());
          } else if (option.isCorrect()) {
            // To'g'ri javob (agar boshqa variant tanlangan bo'lsa)
            button.setText("✅ " + option.getOptionText());
          } else {
            // Boshqa variantlar
            button.setText(option.getOptionText());
          }

          button.setCallbackData("option_" + option.getId());
          return List.of(button);
        })
        .toList();

    updatedKeyboard.setKeyboard(rows);

    // Xabar matnini va tugmalarni yangilash
    var message = new EditMessageText();
    message.setChatId(chatId.toString());
    message.setMessageId(messageId);
    message.setText(question.getQuestionText()); // Savol matni o'zgarmaydi
    message.setReplyMarkup(updatedKeyboard);

    currentQuestionId++; // Keyingi savolga o'tish

    return message;
  }

  private SendMessage createStatisticsMessage(Long chatId) {
    long totalQuestions = currentQuestionId - 1; // Umumiy savollar soni
    double correctPercentage = (double) correctAnswersCount / totalQuestions * 100;

    String statisticsMessage = String.format(
        "📊 *Statistika:*\n" +
        "• Umumiy savollar: %d\n" +
        "• To'g'ri javoblar: %d (%.2f%%)\n" +
        "• Noto'g'ri javoblar: %d",
        totalQuestions,
        correctAnswersCount,
        correctPercentage,
        wrongAnswersCount
    );

    var message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(statisticsMessage);
    message.setParseMode("Markdown"); // Markdown formati orqali bold text kiritish
    return message;
  }

  private InlineKeyboardMarkup createKeyboard(List<OptionResponseDto> options) {
    var markup = new InlineKeyboardMarkup();
    var rows = options.stream()
        .map(option -> {
          var button = new InlineKeyboardButton(option.getOptionText());
          button.setCallbackData("option_" + option.getId());
          return List.of(button);
        })
        .toList();

    markup.setKeyboard(rows);
    return markup;
  }

  private SendMessage createMessage(Long chatId, String text) {
    var message = new SendMessage();
    message.setChatId(chatId.toString());
    message.setText(text);
    return message;
  }
}
