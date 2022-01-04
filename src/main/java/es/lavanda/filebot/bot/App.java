package es.lavanda.filebot.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import es.lavanda.filebot.bot.handler.FilebotHandler;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class App {

	public static void main(String[] args) {		
		SpringApplication.run(App.class, args);
		log.info(" --------- App SpringBoot Started ------- ");
	}
 }