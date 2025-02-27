package com.smart.quiz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizBot extends TelegramLongPollingBot {

  private final QuizManager quizManager;

  public QuizBot(@Value("${telegram.bot.token}") String botToken, QuizManager quizManager) {
    super(botToken);
    this.quizManager = quizManager;
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      if (update.hasMessage() && update.getMessage().hasText()) {
        handleCommand(update.getMessage().getChatId(), update.getMessage().getText());
      } else if (update.hasPollAnswer()) {
        handlePollAnswer(update.getPollAnswer());
      }
    } catch (Exception e) {
      logError(e);
    }
  }

  // ✅ Buyruqlarni switch-case orqali boshqarish
  private void handleCommand(Long chatId, String command) throws TelegramApiException {
    SendMessage message;

    switch (command.toLowerCase()) {
      case "/start":
        execute(quizManager.sendQuestionFormatInfo(chatId));
        break;
      case "/stop":
        execute(quizManager.sendStatistics(chatId));
        break;
      case "/quiz":
        execute(quizManager.startQuiz(chatId));
        break;
      case "/exit":
        execute(quizManager.exitBot(chatId));
        break;
      default:
        message = quizManager.createMessage(chatId, "❌ Noto‘g‘ri buyruq. Iltimos, /start dan foydalaning.");
        execute(message);
    }
  }

  private void handlePollAnswer(PollAnswer pollAnswer) {
    Long userId = pollAnswer.getUser().getId();
    Integer selectedOption = pollAnswer.getOptionIds().getFirst(); // Foydalanuvchi tanlagan variant
    quizManager.processPollAnswer(userId, selectedOption);
  }

  private void logError(Exception e) {
    log.error("Xatolik yuz berdi: {}", e.getMessage());
  }

  @Override
  public String getBotUsername() {
    return "Test"; // Bot nomi
  }
}
