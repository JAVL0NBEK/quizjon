package com.smart.quiz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

  @Value("${telegram.bot.token}")
  private String botToken;

  @Bean
  public QuizBot quizBot(QuizManager quizManager) {
    return new QuizBot(botToken, quizManager);
  }

  @Bean
  public TelegramBotsApi telegramBotsApi(QuizBot quizBot) throws TelegramApiException {
    var botsApi = new TelegramBotsApi(DefaultBotSession.class);
    botsApi.registerBot(quizBot);
    return botsApi;
  }

}
