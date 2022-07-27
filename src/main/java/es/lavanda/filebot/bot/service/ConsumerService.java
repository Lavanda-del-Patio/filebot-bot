package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
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
}
