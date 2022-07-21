package es.lavanda.filebot.bot.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import es.lavanda.filebot.bot.model.TelegramFilebotExecution;
import es.lavanda.lib.common.model.FilebotExecutionIDTO;
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
    
    @RabbitListener(queues = "telegram-execution")
    public void consumeMessageFeedFilms(TelegramFilebotExecution filebotExecutionIDTO) {
        log.info("Reading message of the queue filebot-telegram: {}", filebotExecutionIDTO);
        filebotService.processNotProcessing(filebotExecutionIDTO);
        log.info("Finish message of the queue filebot-telegram");
    }
}
