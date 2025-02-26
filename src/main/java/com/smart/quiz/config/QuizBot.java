package com.smart.quiz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
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
        handleMessage(update.getMessage());
      } else if (update.hasCallbackQuery()) {
        handleCallbackQuery(update.getCallbackQuery());
      }
    } catch (Exception e) {
      logError(e);
    }
  }

  private void handleMessage(Message message) throws TelegramApiException {
    var chatId = message.getChatId();
    var text = message.getText();

    if ("/start".equals(text)) {
      execute(quizManager.getQuestionMessage(chatId));
    } else {
      execute(createMessage(chatId, null));
    }
  }

  private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
    var chatId = callbackQuery.getMessage().getChatId();
    var messageId = callbackQuery.getMessage().getMessageId();
    var data = callbackQuery.getData();

    if (data.startsWith("option_")) {
      var optionId = Long.parseLong(data.split("_")[1]);

      try {
        // Javobni qayta ishlash va xabarni yangilash
        EditMessageText message = quizManager.handleAnswer(chatId, messageId, optionId);
        execute(message);

        SendMessage nextQuestionMessage = quizManager.getQuestionMessage(chatId);
        execute(nextQuestionMessage); // Keyingi savolni yuborish

      } catch (TelegramApiException e) {
        e.printStackTrace();
      }
    } else if (data.startsWith("disabled_")) {
      // Bloklangan tugmani bosgan bo'lsa, hech narsa qilmaymiz
      execute(createMessage(chatId, "Bu tugma faol emas!"));
    } else {
      // Agar allaqachon javob bergan bo'lsa, qayta ishlamaymiz
      execute(createMessage(chatId, "Siz allaqachon javob bergansiz!"));
    }

  }

  private SendMessage createMessage(Long chatId, String info) {
    var message = new SendMessage();
    message.setChatId(chatId.toString());
    if (info==null || info.isEmpty()){
      message.setText("Buyruq noto'g'ri. /start buyrug'ini yuboring.");
    } else {
      message.setText(info);
    }
    return message;
  }

  private void logError(Exception e) {
    log.error("Xatolik yuz berdi: {}", e.getMessage());
  }

  @Override
  public String getBotUsername() {
    return "Test"; // Bot nomi
  }
}
