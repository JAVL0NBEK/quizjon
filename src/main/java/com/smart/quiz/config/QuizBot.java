package com.smart.quiz.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.quiz.dto.UploadState;
import com.smart.quiz.dto.UploadStep;
import java.util.HashMap;
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
import org.springframework.web.client.HttpClientErrorException;
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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class QuizBot extends TelegramLongPollingBot {

  private final QuizManager quizManager;
  private final Map<Long, UploadState> userStateMap = new HashMap<>();
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
          // Fayl yuborilganda, avval /create buyrug'i yuborilganligini tekshiramiz
          if (!userStateMap.containsKey(message.getChatId())) {
            sendMessage(message.getChatId(), "❌ Iltimos, oldin /create buyrug'ini kiriting. ");
            return;
          }
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
    Long chatId = message.getChatId();
    Document document = message.getDocument();

    // Qo'shimcha tekshiruv (agar kerak bo'lsa)
    if (!userStateMap.containsKey(chatId)) {
      sendMessage(chatId, "❌ Iltimos, avval /create buyrug'ini yuboring.");
      return;
    }

    UploadState state = userStateMap.get(chatId);
    state.setDocument(document);
    state.setStep(UploadStep.WAITING_FOR_SUBJECT);

    // Endi fan nomini so'rang
    sendMessage(chatId, "✅ Fayl qabul qilindi!\n\nEndi quiz qaysi fan yoki mavzuga tegishli ekanligini kiriting:");
  }

  // ✅ Foydalanuvchi fan nomini kiritgandan keyin APIga yuborish
  private void handleSubjectInput(Long chatId, String subjectName, String userName) {
    UploadState state = userStateMap.get(chatId);

    if (state != null && state.getStep() == UploadStep.WAITING_FOR_SUBJECT && state.getDocument() != null) {
      state.setSubject(subjectName);
      sendMessage(chatId, "📤 Fayl, fan nomi va savollar soni qabul qilindi!" +
                          "\nFayl: " + state.getDocument().getFileName() +
                          "\nFan: " + state.getSubject() +
                          "\n✅ Bazaga saqlash uchun yuborilmoqda...");

      sendToApi(state.getDocument(), state.getSubject(), chatId, userName);
      userStateMap.remove(chatId); // Holatni tozalash
    } else {
      sendMessage(chatId, "❌ Oldin fayl yuklang.");
    }
  }

  private void handleStatInput(Long chatId, String text)  throws TelegramApiException {
    try {
      int count = Integer.parseInt(text.trim());
      //userStateMap.remove(chatId); // step tugadi
      execute(quizManager.sendResults(chatId, count));
    } catch (NumberFormatException e) {
      sendMessage(chatId, "❌ Iltimos, faqat son kiriting. Masalan: 3");
    }
  }

  //
  // ✅ Inline tugma orqali fayl yuklashni so‘rash
  private void requestDocumentUpload(Long chatId) throws TelegramApiException {

    // Yangi holat yaratish
    userStateMap.put(chatId, new UploadState());

    SendMessage message = new SendMessage();
    message.setChatId(chatId);
    message.setText("📎 Iltimos, quiz savollarini o'z ichiga olgan faylni yuboring (docx formatida).");
    execute(message);
  }

  private void handlePollAnswer(PollAnswer pollAnswer) {
    Long userId = pollAnswer.getUser().getId();
    Integer selectedOption = pollAnswer.getOptionIds().get(0); // Foydalanuvchi tanlagan variant
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
    String userName = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : message.getFrom().getFirstName();

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
      case "/check":
        execute(quizManager.accessChange(chatId));
        break;
      case "/result":
        userStateMap.put(chatId, new UploadState(UploadStep.WAITING_FOR_RESULT_COUNT));
        sendMessage(chatId, "📊 Ohirgi nechta natijangizni ko‘rmoqchisiz?(masalan 3)");
        break;
      case "/create":
        requestDocumentUpload(chatId);
        break;
      default:
        // Deep linkni qayta ishlash
        if (text.toLowerCase().startsWith("/start ")) {
          String param = text.toLowerCase().substring(7).trim();
          execute(quizManager.handleInvite(chatId, param, userName));
        }
        else if (userStateMap.containsKey(chatId)) {
          UploadState userState = userStateMap.get(chatId);

          if (userState.getStep() == UploadStep.WAITING_FOR_SUBJECT && userState.getSubject() == null) {
            handleSubjectInput(chatId, text, userName);
          } else if (userState.getStep() == UploadStep.WAITING_FOR_RESULT_COUNT) {
            handleStatInput(chatId, text); // faqat result uchun
          }
        } else {
          sendMessage(chatId, "❌ Noto‘g‘ri buyruq. /start dan foydalaning.");
        }
    }
  }

  // ✅ APIga fayl va fan nomini yuborish
  private void sendToApi(Document document, String subjectName, Long chatId, String userName) {
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
      body.add("userName", userName);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
      String apiUrl = "http://109.172.36.54:5000/v1/quiz/upload-document"; // Kompyuteringiz IP manzili

      ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

      sendMessage(chatId, "✅ Server javobi: " + response.getBody());
    } catch (HttpClientErrorException e) {
      // API dan qaytgan xatolik uchun
      String errorMessage = e.getResponseBodyAsString();
      try {
        // JSON javobni parse qilish
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(errorMessage);
        errorMessage = root.path("message").asText();
      } catch (Exception ignored) {
        // Agar JSON parse qilib bo'lmasa, oddiy matn sifatida ko'rsatamiz
      }

      // Xatolik matnini tozalash
      errorMessage = errorMessage
          .replace("\"", "")
          .replace("Xatolik:", "")
          .trim();

      sendMessage(chatId, "❌ " + errorMessage);
    } catch (Exception e) {
      // Boshqa xatoliklar uchun
      sendMessage(chatId, e.getMessage());
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
