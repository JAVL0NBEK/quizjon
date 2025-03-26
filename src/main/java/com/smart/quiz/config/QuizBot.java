package com.smart.quiz.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizBot extends TelegramLongPollingBot {

  private final QuizManager quizManager;
  private final Map<Long, Document> userStateMap = new HashMap<>();
  private final RestTemplate restTemplate = new RestTemplate(); // API so‘rov uchun


  public QuizBot(@Value("${telegram.bot.token}") String botToken, QuizManager quizManager) {
    super(botToken);
    this.quizManager = quizManager;
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      if (update.hasMessage()) {
        Message message = update.getMessage();

        if (message.hasText()) {
          handleTextMessage(message);
        } else if (message.hasDocument()) {
          handleDocumentUpload(message);
        }

      } else if (update.hasPollAnswer()) {
        handlePollAnswer(update.getPollAnswer());

      } else if (update.hasCallbackQuery()) {
        handleCallbackQuery(update.getCallbackQuery());
      }

    } catch (Exception e) {
      logError(e);
    }
  }

  // ✅ CallbackQuery qayta ishlash (Inline tugmalar uchun)
  private void handleCallbackQuery(CallbackQuery callbackQuery) throws TelegramApiException {
    Long chatId = callbackQuery.getMessage().getChatId();
    String data = callbackQuery.getData();
    quizManager.processCallbackQuery(chatId, data);

    if ("upload_file".equals(data)) {
      requestDocumentUpload(chatId);
    }

    AnswerCallbackQuery answer = new AnswerCallbackQuery();
    answer.setCallbackQueryId(callbackQuery.getId());
    execute(answer);
  }

  // ✅ Foydalanuvchi fayl yuborganida uni qabul qilish
  private void handleDocumentUpload(Message message) {
    Document document = message.getDocument();
    Long chatId = message.getChatId();

    if (document.getFileName().endsWith(".docx")) {
      sendMessage(chatId, "📂 Fayl qabul qilindi: " + document.getFileName() +
                          "\n✏️ Iltimos, fan nomini kiriting:");
      userStateMap.put(chatId, document);
    } else {
      sendMessage(chatId, "❌ Faqat .docx formatdagi fayllarni yuklang!");
    }
  }

  // ✅ Foydalanuvchi fan nomini kiritgandan keyin APIga yuborish
  private void handleSubjectInput(Long chatId, String subjectName) {
    Document document = userStateMap.get(chatId);

    sendMessage(chatId, "📤 Fayl va fan nomi qabul qilindi!" +
                        "\nFayl: " + document.getFileName() +
                        "\nFan: " + subjectName +
                        "\n✅ APIga yuborilmoqda...");

    sendToApi(document, subjectName, chatId);
    userStateMap.remove(chatId);
  }

  // ✅ Inline tugma orqali fayl yuklashni so‘rash
  private void requestDocumentUpload(Long chatId) throws TelegramApiException {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("📂 Iltimos, .docx formatdagi faylni yuklang:");

    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

    InlineKeyboardButton uploadButton = new InlineKeyboardButton();
    uploadButton.setText("📤 Fayl yuklash");
    uploadButton.setCallbackData("upload_file");

    keyboard.add(Collections.singletonList(uploadButton));
    inlineKeyboardMarkup.setKeyboard(keyboard);

    message.setReplyMarkup(inlineKeyboardMarkup);
    execute(message);
  }


  private void handlePollAnswer(PollAnswer pollAnswer) {
    Long userId = pollAnswer.getUser().getId();
    Integer selectedOption = pollAnswer.getOptionIds().getFirst(); // Foydalanuvchi tanlagan variant
    quizManager.processPollAnswer(userId, selectedOption);
  }

  private void sendMessage(Long chatId, String text) {
    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText(text);

    try {
      execute(message);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  // ✅ Foydalanuvchi buyruqlarini qayta ishlash
  private void handleTextMessage(Message message) throws TelegramApiException {
    Long chatId = message.getChatId();
    String text = message.getText();

    switch (text.toLowerCase()) {
      case "/start":
        execute(quizManager.sendQuestionFormatInfo(chatId));
        break;
      case "/stop":
        execute(quizManager.sendStatistics(chatId));
        break;
      case "/quiz":
        execute(quizManager.startQuiz(chatId));
        break;
      case "/share":
        execute(quizManager.shareBot(chatId));
        break;
      case "/exit":
        execute(quizManager.exitBot(chatId));
        break;
      case "/create":
        requestDocumentUpload(chatId);
        break;
      default:
        // Deep linkni qayta ishlash
        if (text.toLowerCase().startsWith("/start ")) {
          String param = text.toLowerCase().substring(7).trim();
          execute(quizManager.handleInvite(chatId, param));
        }
        else if (userStateMap.containsKey(chatId)) {
          handleSubjectInput(chatId, text);
        } else {
          sendMessage(chatId, "❌ Noto‘g‘ri buyruq. /start dan foydalaning.");
        }
    }
  }

  // ✅ APIga fayl va fan nomini yuborish
  private void sendToApi(Document document, String subjectName, Long chatId) {
    try {
      String fileUrl = execute(new GetFile(document.getFileId())).getFileUrl(getBotToken());
      byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new ByteArrayResource(fileBytes) {
        @Override
        public String getFilename() {
          return document.getFileName();
        }
      });
      body.add("subject", subjectName);
      body.add("subDesc", "Test");
      body.add("chatId", chatId.toString());

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
      String apiUrl = "http://172.20.0.92:8080/v1/quiz/upload-document"; // Kompyuteringiz IP manzili

      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

      sendMessage(chatId, "✅ API javobi: " + response.getBody());
    } catch (Exception e) {
      sendMessage(chatId, "❌ APIga yuborishda xatolik: " + e.getMessage());
      logError(e);
    }
  }

  private void logError(Exception e) {
    log.error("Xatolik yuz berdi: {}", e.getMessage());
  }

  @Override
  public String getBotUsername() {
    return "quizjon_bot"; // Bot nomi
  }
}
