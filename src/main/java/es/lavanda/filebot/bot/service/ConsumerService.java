package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
import es.lavanda.filebot.bot.model.TelegramMessage;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;
import es.lavanda.lib.common.model.TelegramFilebotExecutionODTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsumerService {

    private final FilebotService filebotService;

    @RabbitListener(queues = "filebot-telegram")
    public void consumeMessageFeedFilms(FilebotExecutionIDTO filebotExecutionIDTO) {
        log.info("Reading message of the queue filebot-telegram: {}", filebotExecutionIDTO);
        filebotService.run(filebotExecutionIDTO);
        log.info("Finish message of the queue filebot-telegram");
    }

    @RabbitListener(queues = "telegram-query-tmdb-resolution")
    public void consumeMessageFeedFilms(TelegramFilebotExecutionODTO telegramFilebotExecutionODTO) {
        log.info("Reading message of the queue telegram-query-tmdb-resolution: {}", telegramFilebotExecutionODTO);
        filebotService.recieveTMDBData(telegramFilebotExecutionODTO);
        log.info("Finish message of the queue telegram-query-tmdb-resolution");
    }

    @RabbitListener(queues = "filebot-telegram-messages")
    public void consumeMessageForSendMessages(TelegramMessage telegramMessage) {
        log.info("Reading message of the queue filebot-telegram-messages: {}", telegramMessage);
        filebotService.sendMessage(telegramMessage);
        log.info("Finish message of the queue filebot-telegram-messages");
    }
}
