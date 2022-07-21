package es.lavanda.filebot.bot.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.aws.autoconfigure.context.ContextInstanceDataAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import es.lavanda.filebot.bot.exception.FilebotBotException;
import es.lavanda.filebot.bot.handler.FilebotHandler;
import es.lavanda.filebot.bot.service.FilebotService;
import es.lavanda.lib.common.config.CommonConfigurator;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableMongoAuditing
@Import(CommonConfigurator.class)
@EnableAutoConfiguration(exclude = { ContextInstanceDataAutoConfiguration.class })
@Slf4j
@EnableScheduling
public class AppBeans {

    @Autowired
    private FilebotService filebotService;

    @Autowired
    private FilebotBotConfig botConfig;

    @PostConstruct
    public void start() {
        log.info("Instantiate Telegram Bots API...");
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            FilebotHandler filebotHandler = new FilebotHandler(
                    filebotService, botConfig);
            filebotService.setFilebotHandler(filebotHandler);
            botsApi.registerBot(
                    filebotHandler);
        } catch (TelegramApiException e) {
            log.error("Exception instantiate Telegram Bot!", e);
            throw new FilebotBotException(e);
        }
    }
}
